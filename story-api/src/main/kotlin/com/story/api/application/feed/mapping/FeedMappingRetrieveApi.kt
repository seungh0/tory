package com.story.api.application.feed.mapping

import com.story.api.config.auth.AuthContext
import com.story.api.config.auth.RequestAuthContext
import com.story.core.common.model.dto.ApiResponse
import com.story.core.domain.resource.ResourceId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedMappingRetrieveApi(
    private val feedMappingRetrieveHandler: FeedMappingRetrieveHandler,
) {

    @GetMapping("/v1/resources/feeds/components/{feedComponentId}/mappings/{sourceResourceId}/{sourceComponentId}")
    suspend fun connectFeedMapping(
        @PathVariable feedComponentId: String,
        @PathVariable sourceResourceId: String,
        @PathVariable sourceComponentId: String,
        @RequestAuthContext authContext: AuthContext,
    ): ApiResponse<FeedMappingListApiResponse> {
        val feedMappings = feedMappingRetrieveHandler.listConnectedFeedMappings(
            workspaceId = authContext.workspaceId,
            sourceResourceId = ResourceId.findByCode(sourceResourceId),
            sourceComponentId = sourceComponentId,
        )
        return ApiResponse.ok(feedMappings)
    }

}