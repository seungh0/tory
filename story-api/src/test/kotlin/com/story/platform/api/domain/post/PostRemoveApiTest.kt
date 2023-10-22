package com.story.platform.api.domain.post

import com.ninjasquad.springmockk.MockkBean
import com.story.platform.api.ApiTest
import com.story.platform.api.DocsTest
import com.story.platform.api.FunSpecDocsTest
import com.story.platform.api.lib.PageHeaderSnippet.Companion.pageHeaderSnippet
import com.story.platform.api.lib.RestDocsUtils
import com.story.platform.api.lib.RestDocsUtils.getDocumentRequest
import com.story.platform.api.lib.RestDocsUtils.getDocumentResponse
import com.story.platform.api.lib.WebClientUtils
import com.story.platform.api.lib.isTrue
import com.story.platform.core.domain.post.PostSpaceKey
import io.mockk.coEvery
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient

@DocsTest
@ApiTest(PostRemoveApi::class)
class PostRemoveApiTest(
    private val webTestClient: WebTestClient,

    @MockkBean
    private val postRemoveHandler: PostRemoveHandler,
) : FunSpecDocsTest({

    test("기존에 등록된 포스트를 삭제한다") {
        // given
        val postSpaceKey = PostSpaceKey(
            workspaceId = "story",
            componentId = "user-post",
            spaceId = "user-space-id"
        )

        val postId = 30000L

        coEvery {
            postRemoveHandler.removePost(
                postSpaceKey = postSpaceKey,
                accountId = any(),
                postId = postId,
            )
        } returns Unit

        // when
        val exchange = webTestClient.delete()
            .uri(
                "/v1/resources/posts/components/{componentId}/spaces/{spaceId}/posts/{postId}",
                postSpaceKey.componentId,
                postSpaceKey.spaceId,
                postId,
            )
            .headers(WebClientUtils.authenticationHeaderWithRequestAccountId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()

        // then
        exchange.expectStatus().isOk
            .expectBody()
            .jsonPath("$.ok").isTrue()
            .consumeWith(
                document(
                    "post.remove",
                    getDocumentRequest(),
                    getDocumentResponse(),
                    pageHeaderSnippet(),
                    RestDocsUtils.authenticationHeaderWithRequestAccountIdDocumentation,
                    pathParameters(
                        parameterWithName("componentId").description("포스트 컴포넌트 ID"),
                        parameterWithName("spaceId").description("포스트 공간 ID"),
                        parameterWithName("postId").description("포스트 ID"),
                    ),
                    responseFields(
                        fieldWithPath("ok")
                            .type(JsonFieldType.BOOLEAN).description("성공 여부")
                    )
                )
            )
    }

})
