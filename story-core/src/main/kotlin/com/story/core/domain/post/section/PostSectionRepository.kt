package com.story.core.domain.post.section

import com.story.core.infrastructure.cassandra.CassandraBasicRepository
import kotlinx.coroutines.flow.Flow

interface PostSectionRepository : CassandraBasicRepository<PostSection, PostSectionPrimaryKey> {

    fun findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentKeyAndKeySlotIdAndKeyPostId(
        workspaceId: String,
        componentId: String,
        spaceId: String,
        parentKey: String,
        slotId: Long,
        postId: Long,
    ): Flow<PostSection>

    fun findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentKeyAndKeySlotIdAndKeyPostIdIn(
        workspaceId: String,
        componentId: String,
        spaceId: String,
        parentKey: String,
        slotId: Long,
        postIds: Collection<Long>,
    ): Flow<PostSection>

}
