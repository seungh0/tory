package com.story.platform.api.domain.component

import com.story.platform.core.domain.component.ComponentCreator
import com.story.platform.core.domain.component.ComponentResponse
import com.story.platform.core.domain.resource.ResourceId
import org.springframework.stereotype.Service

@Service
class ComponentCreateHandler(
    private val componentCreator: ComponentCreator,
) {

    suspend fun createComponent(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
        description: String,
    ): ComponentResponse {
        return componentCreator.createComponent(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId,
            description = description,
        )
    }

}
