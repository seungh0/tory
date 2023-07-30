package com.story.platform.api.domain.resource

import com.story.platform.core.common.model.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ResourceRetrieveApi(
    private val resourceRetrieveHandler: ResourceRetrieveHandler,
) {

    /**
     * 워크스페이스에서 사용할 수 있는 리소스 목록을 조회합니다
     */
    @GetMapping("/v1/resources")
    suspend fun listResources(): ApiResponse<List<ResourceApiResponse>> {
        val resourceTypes = resourceRetrieveHandler.listResources()
        return ApiResponse.ok(resourceTypes)
    }

}
