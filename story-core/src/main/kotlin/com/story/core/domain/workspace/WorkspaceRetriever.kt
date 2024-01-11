package com.story.core.domain.workspace

import com.story.core.infrastructure.cache.CacheType
import com.story.core.infrastructure.cache.Cacheable
import org.springframework.stereotype.Service

@Service
class WorkspaceRetriever(
    private val workspaceRepository: WorkspaceRepository,
) {

    @Cacheable(
        cacheType = CacheType.WORKSPACE,
        key = "'workspaceId:' + {#workspaceId}",
    )
    suspend fun getWorkspace(
        workspaceId: String,
    ): WorkspaceResponse {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw WorkspaceNotExistsException(message = "워크스페이스($workspaceId)가 존재하지 않습니다")
        return WorkspaceResponse.of(workspace = workspace)
    }

}