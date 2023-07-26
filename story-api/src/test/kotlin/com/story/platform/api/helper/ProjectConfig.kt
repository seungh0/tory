package com.story.platform.api.helper

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension

class ProjectConfig : AbstractProjectConfig() {
    @ExperimentalKotest
    override var testCoroutineDispatcher = true
    override val coroutineDebugProbes = true

    @ExperimentalKotest
    override val concurrentTests = 2
    override fun extensions() = listOf(SpringExtension)
}
