package com.story.platform.api.domain.component

import com.story.platform.core.common.model.CursorResult
import com.story.platform.core.common.model.dto.CursorRequest
import com.story.platform.core.domain.component.ComponentRetriever
import com.story.platform.core.domain.resource.ResourceId
import org.springframework.stereotype.Service

@Service
class ComponentRetrieveHandler(
    private val componentRetriever: ComponentRetriever,
) {

    suspend fun getComponent(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
    ): ComponentApiResponse {
        val component = componentRetriever.getComponent(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId
        )
        return ComponentApiResponse.of(component)
    }

    suspend fun listComponents(
        workspaceId: String,
        resourceId: ResourceId,
        cursorRequest: CursorRequest,
    ): CursorResult<ComponentApiResponse, String> {
        val components = componentRetriever.listComponents(
            workspaceId = workspaceId,
            resourceId = resourceId,
            cursorRequest = cursorRequest,
        )
        return CursorResult.of(
            data = components.data.map { component -> ComponentApiResponse.of(component) },
            cursor = components.cursor,
        )
    }

}