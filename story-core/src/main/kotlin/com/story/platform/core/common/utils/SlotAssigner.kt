package com.story.platform.core.common.utils

object SlotAssigner {

    fun assign(
        id: Long,
        firstSlotId: Long,
        slotSize: Int,
    ): Long {
        require(value = id >= MIN_ID, lazyMessage = { "id($id)는 최소 ${MIN_ID}보다 커야합니다" })
        return ((id - MIN_ID) / slotSize) + firstSlotId
    }

    private const val MIN_ID = 1L

}