package com.story.platform.api.domain.component

import com.ninjasquad.springmockk.MockkBean
import com.story.platform.api.ApiTest
import com.story.platform.api.DocsTest
import com.story.platform.api.domain.authentication.AuthenticationHandler
import com.story.platform.api.domain.workspace.WorkspaceRetrieveHandler
import com.story.platform.api.lib.PageHeaderSnippet
import com.story.platform.api.lib.RestDocsUtils
import com.story.platform.api.lib.WebClientUtils
import com.story.platform.core.domain.authentication.AuthenticationKeyResponse
import com.story.platform.core.domain.authentication.AuthenticationKeyStatus
import com.story.platform.core.domain.component.ComponentResponse
import com.story.platform.core.domain.component.ComponentStatus
import com.story.platform.core.domain.resource.ResourceId
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation
import org.springframework.test.web.reactive.server.WebTestClient

@DocsTest
@ApiTest(ComponentCreateApi::class)
class ComponentCreateApiTest(
    private val webTestClient: WebTestClient,

    @MockkBean
    private val componentCreateHandler: ComponentCreateHandler,

    @MockkBean
    private val authenticationHandler: AuthenticationHandler,

    @MockkBean
    private val workspaceRetrieveHandler: WorkspaceRetrieveHandler,
) : StringSpec({

    beforeEach {
        coEvery { authenticationHandler.handleAuthentication(any()) } returns AuthenticationKeyResponse(
            workspaceId = "twitter",
            authenticationKey = "api-key",
            status = AuthenticationKeyStatus.ENABLED,
            description = "",
        )
        coEvery { workspaceRetrieveHandler.validateEnabledWorkspace(any()) } returns Unit
    }

    "신규 컴포넌트를 등록합니다" {
        // given
        val resourceId = ResourceId.SUBSCRIPTIONS
        val componentId = "follow"
        val description = "following"

        val request = ComponentCreateApiRequest(
            description = description,
        )

        coEvery {
            componentCreateHandler.createComponent(
                workspaceId = any(),
                resourceId = resourceId,
                componentId = componentId,
                description = description,
            )
        } returns ComponentResponse(
            componentId = componentId,
            description = description,
            status = ComponentStatus.ENABLED,
        )

        // when
        val exchange = webTestClient.post()
            .uri("/v1/resources/{resourceId}/components/{componentId}", resourceId.code, componentId)
            .headers(WebClientUtils.authenticationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()

        // then
        exchange.expectStatus().isOk
            .expectBody()
            .consumeWith(
                WebTestClientRestDocumentation.document(
                    "COMPONENT-CREATE-API",
                    RestDocsUtils.getDocumentRequest(),
                    RestDocsUtils.getDocumentResponse(),
                    PageHeaderSnippet.pageHeaderSnippet(),
                    RequestDocumentation.pathParameters(
                        RequestDocumentation.parameterWithName("resourceId").description("Resource Id"),
                        RequestDocumentation.parameterWithName("componentId").description("Component Id"),
                    ),
                    PayloadDocumentation.requestFields(
                        PayloadDocumentation.fieldWithPath("description").type(JsonFieldType.STRING)
                            .description("Description").optional(),
                    ),
                    PayloadDocumentation.responseFields(
                        PayloadDocumentation.fieldWithPath("ok")
                            .type(JsonFieldType.BOOLEAN).description("ok"),
                    )
                )
            )
    }

})