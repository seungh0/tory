package com.story.platform.core.domain.event

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EventHistoryRepository : CoroutineCrudRepository<EventHistory, EventHistoryPrimaryKey>