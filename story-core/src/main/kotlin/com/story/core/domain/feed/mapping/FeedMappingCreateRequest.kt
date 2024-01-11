package com.story.core.domain.feed.mapping

import com.story.core.domain.resource.ResourceId

data class FeedMappingCreateRequest(
    val workspaceId: String,
    val feedComponentId: String,
    val sourceResourceId: ResourceId,
    val sourceComponentId: String,
    val subscriptionComponentId: String,
    val description: String,
) {

    fun toConfiguration() = FeedMappingConfiguration.of(
        workspaceId = workspaceId,
        feedComponentId = feedComponentId,
        sourceResourceId = sourceResourceId,
        sourceComponentId = sourceComponentId,
        subscriptionComponentId = subscriptionComponentId,
        description = description,
    )

}