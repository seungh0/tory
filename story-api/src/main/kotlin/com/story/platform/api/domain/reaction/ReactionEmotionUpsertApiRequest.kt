package com.story.platform.api.domain.reaction

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReactionEmotionUpsertApiRequest(
    @field:Size(max = 100)
    @field:NotBlank
    val emotionId: String,
)
