package com.story.platform.core.common.model.dto

import com.story.platform.core.common.error.ErrorCode

data class ApiResponse<T>(
    val ok: Boolean,
    val error: String? = null,
    val result: T?,
) {

    companion object {
        fun <T> ok(result: T): ApiResponse<T> = ApiResponse(ok = true, result = result)

        fun <T> fail(
            error: ErrorCode,
            errorMessage: String? = null,
        ): ApiResponse<T> = ApiResponse(
            ok = false,
            error = if (errorMessage.isNullOrBlank()) error.code else errorMessage,
            result = null,
        )

        val OK: ApiResponse<Nothing?> = ok(result = null)
    }

}
