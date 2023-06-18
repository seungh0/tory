package com.story.platform.api.domain.post

import com.story.platform.api.domain.authentication.AuthenticationHandler
import com.story.platform.core.common.error.BadRequestException
import com.story.platform.core.common.model.ApiResponse
import com.story.platform.core.common.model.CursorRequest
import com.story.platform.core.common.model.CursorResult
import com.story.platform.core.domain.post.PostRetriever
import com.story.platform.core.domain.post.PostSpaceKey
import com.story.platform.core.domain.post.PostSpaceType
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
class PostRetrieveApi(
    private val postRetriever: PostRetriever,
    private val authenticationHandler: AuthenticationHandler,
) {

    /**
     * 특정 포스트를 조회한다
     */
    @GetMapping("/v1/spaces/{spaceType}/{spaceId}/posts/{postId}")
    suspend fun getPost(
        @PathVariable spaceType: PostSpaceType,
        @PathVariable spaceId: String,
        @PathVariable postId: String,
        serverWebExchange: ServerWebExchange,
    ): ApiResponse<PostApiResponse> {
        val authentication = authenticationHandler.handleAuthentication(serverWebExchange = serverWebExchange)
        val post = postRetriever.getPost(
            postSpaceKey = PostSpaceKey(
                serviceType = authentication.serviceType,
                spaceType = spaceType,
                spaceId = spaceId,
            ),
            postId = postId.toLongOrNull() ?: throw BadRequestException("잘못된 PostId($postId)이 요청되었습니다"),
        )
        return ApiResponse.success(PostApiResponse.of(post))
    }

    /**
     * 포스트 목록을 조회한다
     */
    @GetMapping("/v1/spaces/{spaceType}/{spaceId}/posts")
    suspend fun listPosts(
        @PathVariable spaceType: PostSpaceType,
        @PathVariable spaceId: String,
        @Valid cursorRequest: CursorRequest,
        serverWebExchange: ServerWebExchange,
    ): ApiResponse<CursorResult<PostApiResponse, String>> {
        val authentication = authenticationHandler.handleAuthentication(serverWebExchange = serverWebExchange)
        val posts = postRetriever.listPosts(
            postSpaceKey = PostSpaceKey(
                serviceType = authentication.serviceType,
                spaceType = spaceType,
                spaceId = spaceId,
            ),
            cursorRequest = cursorRequest,
        )

        val result = CursorResult.of(
            data = posts.data.map { post -> PostApiResponse.of(post) },
            cursor = posts.cursor,
        )

        return ApiResponse.success(result)
    }

}
