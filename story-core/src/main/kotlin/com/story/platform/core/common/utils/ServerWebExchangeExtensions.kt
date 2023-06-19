package com.story.platform.core.common.utils

import com.story.platform.core.common.enums.HttpHeaderType
import org.springframework.web.server.ServerWebExchange

fun ServerWebExchange.getApiKey(): String? {
    return this.request.headers.getFirst(HttpHeaderType.X_STORY_API_KEY.header)
}