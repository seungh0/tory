package com.story.platform.api.domain.authentication

import com.story.platform.core.domain.authentication.AuthenticationKeyModifier
import com.story.platform.core.domain.authentication.AuthenticationKeyStatus
import org.springframework.stereotype.Service

@Service
class AuthenticationKeyModifyHandler(
    private val authenticationKeyModifier: AuthenticationKeyModifier,
) {

    suspend fun patchAuthenticationKey(
        workspaceId: String,
        authenticationKey: String,
        description: String?,
        status: AuthenticationKeyStatus?,
    ) {
        authenticationKeyModifier.patchAuthenticationKey(
            workspaceId = workspaceId,
            authenticationKey = authenticationKey,
            description = description,
            status = status,
        )
    }

}
