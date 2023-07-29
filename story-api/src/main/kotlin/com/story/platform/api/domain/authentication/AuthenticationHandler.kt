package com.story.platform.api.domain.authentication

import com.story.platform.core.common.http.HttpHeaderType
import com.story.platform.core.common.http.getApiKey
import com.story.platform.core.domain.authentication.AuthenticationKeyEmptyException
import com.story.platform.core.domain.authentication.AuthenticationKeyInactivatedException
import com.story.platform.core.domain.authentication.AuthenticationKeyInvalidException
import com.story.platform.core.domain.authentication.AuthenticationKeyNotExistsException
import com.story.platform.core.domain.authentication.AuthenticationKeyRetriever
import com.story.platform.core.domain.authentication.AuthenticationResponse
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class AuthenticationHandler(
    private val authenticationKeyRetriever: AuthenticationKeyRetriever,
) {

    suspend fun handleAuthentication(
        serverWebExchange: ServerWebExchange,
        allowedDisabledAuthenticationKey: Boolean = false,
    ): AuthenticationResponse {
        val apiKey = serverWebExchange.getApiKey()
            ?: throw AuthenticationKeyEmptyException("API Key 헤더(${HttpHeaderType.X_STORY_API_KEY.header})가 비어있습니다")

        try {
            val authentication = authenticationKeyRetriever.getAuthenticationKey(authenticationKey = apiKey)
            if (!allowedDisabledAuthenticationKey && !authentication.isActivated()) {
                throw AuthenticationKeyInactivatedException("비활성화된 인증 키(${authentication.authenticationKey})입니다. [워크스페이스(${authentication.workspaceId}) 현재 상태: ${authentication.status}]")
            }
            return authentication
        } catch (exception: AuthenticationKeyNotExistsException) {
            throw AuthenticationKeyInvalidException("등록되지 않은 ApiKey($apiKey)입니다")
        }
    }

}
