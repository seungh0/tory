package com.story.core.domain.component

import com.story.core.common.model.Slice
import com.story.core.common.model.dto.CursorRequest
import com.story.core.common.utils.CursorUtils
import com.story.core.domain.resource.ResourceId
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.stereotype.Service

@Service
class ComponentReader(
    private val componentReadRepository: ComponentReadRepository,
) {

    suspend fun getComponent(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
    ): Component? {
        return componentReadRepository.findById(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId,
        )
    }

    suspend fun listComponents(
        workspaceId: String,
        resourceId: ResourceId,
        cursorRequest: CursorRequest,
    ): Slice<Component, String> {
        val components = listComponentsWithCursor(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = cursorRequest.cursor,
            pageSize = cursorRequest.pageSize,
        )

        return Slice.of(
            data = components.subList(0, cursorRequest.pageSize.coerceAtMost(components.size)),
            cursor = CursorUtils.getCursor(
                listWithNextCursor = components,
                pageSize = cursorRequest.pageSize,
                keyGenerator = { component -> component?.componentId }
            )
        )
    }

    private suspend fun listComponentsWithCursor(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String?,
        pageSize: Int,
    ): List<Component> {
        if (componentId == null) {
            return componentReadRepository.findAllByKeyWorkspaceIdAndKeyResourceId(
                workspaceId = workspaceId,
                resourceId = resourceId,
                pageable = CassandraPageRequest.first(pageSize + 1),
            )
        }

        return componentReadRepository.findAllByKeyWorkspaceIdAndKeyResourceIdAndKeyComponentIdLessThan(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId,
            pageable = CassandraPageRequest.first(pageSize + 1),
        )
    }

}