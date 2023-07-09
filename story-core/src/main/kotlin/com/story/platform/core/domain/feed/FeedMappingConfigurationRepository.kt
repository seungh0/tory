package com.story.platform.core.domain.feed

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FeedMappingConfigurationRepository :
    CoroutineCrudRepository<FeedMappingConfiguration, FeedMappingConfigurationPrimaryKey>
