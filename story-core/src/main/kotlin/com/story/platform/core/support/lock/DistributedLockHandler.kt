package com.story.platform.core.support.lock

interface DistributedLockHandler {

    suspend fun executeInCriticalSection(
        distributedLock: DistributedLock,
        lockKey: String,
        runnable: () -> Any?,
    ): Any?

}
