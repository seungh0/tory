package com.story.api.application.component

import com.story.core.domain.component.ComponentStatus

data class ComponentGetRequest(
    val filterStatus: ComponentStatus? = ComponentStatus.ENABLED,
)