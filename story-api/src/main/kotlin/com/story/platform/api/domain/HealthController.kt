package com.story.platform.api.domain

import com.story.platform.core.common.AvailabilityChecker
import com.story.platform.core.common.model.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(
    private val availabilityChecker: AvailabilityChecker,
) {

    @GetMapping("/health/liveness")
    suspend fun livenessCheck(): ResponseEntity<ApiResponse<String>> = availabilityChecker.livenessCheck()

    @GetMapping("/health/readiness")
    suspend fun readinessCheck(): ResponseEntity<ApiResponse<String>> = availabilityChecker.readinessCheck()

}
