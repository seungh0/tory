package com.story.platform.api.domain.post

import com.story.platform.core.common.error.InvalidArgumentsException
import jakarta.validation.constraints.NotBlank

data class PostPatchApiRequest(
    @field:NotBlank
    val accountId: String = "",
    val title: String?,
    val content: String?,
    val extraJson: String? = null,
) {

    init {
        if (title != null && title.isBlank()) {
            throw InvalidArgumentsException("title($title)가 빈 값일 수 없습니다")
        }
    }

}
