package com.story.core.domain.post

import com.story.core.common.error.InvalidCursorException
import com.story.core.common.error.NotSupportedException
import com.story.core.common.model.CursorDirection
import com.story.core.common.model.Slice
import com.story.core.common.model.dto.CursorRequest
import com.story.core.common.utils.CursorUtils
import com.story.core.domain.post.section.PostSectionCassandraRepository
import com.story.core.domain.post.section.PostSectionEntity
import com.story.core.domain.post.section.PostSectionManager
import com.story.core.domain.post.section.PostSectionPartitionKey
import com.story.core.support.cache.CacheType
import com.story.core.support.cache.Cacheable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.StringUtils
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.stereotype.Service

@Service
class PostReader(
    private val postCassandraRepository: PostCassandraRepository,
    private val postReverseCassandraRepository: PostReverseCassandraRepository,
    private val postSequenceRepository: PostSequenceRepository,
    private val postSectionCassandraRepository: PostSectionCassandraRepository,
    private val postSectionManager: PostSectionManager,
    private val postRepository: PostRepository,
) {

    @Cacheable(
        cacheType = CacheType.POST,
        key = "'workspaceId:' + {#postSpaceKey.workspaceId} + ':componentId:' + {#postSpaceKey.componentId} + ':spaceId:' + {#postSpaceKey.spaceId} + ':parentId:' + {#postId.parentId} + ':postNo:' + {#postId.postNo}",
    )
    suspend fun getPost(
        postSpaceKey: PostSpaceKey,
        postId: PostId,
    ): PostWithSections {
        return postRepository.findPostWithSections(
            postSpaceKey = postSpaceKey,
            postId = postId,
        ) ?: throw PostNotExistsException(message = "해당하는 Space($postSpaceKey)에 포스트($postId)가 존재하지 않습니다")
    }

    suspend fun listPosts(
        postSpaceKey: PostSpaceKey,
        parentId: PostId?,
        cursorRequest: CursorRequest,
        sortBy: PostSortBy,
    ): Slice<PostWithSections, String> {
        val (slot: Long, posts: List<PostEntity>) = when (sortBy to cursorRequest.direction) {
            PostSortBy.LATEST to CursorDirection.NEXT, PostSortBy.OLDEST to CursorDirection.PREVIOUS -> listNextPosts(
                cursorRequest = cursorRequest,
                postSpaceKey = postSpaceKey,
                parentId = parentId,
            )

            PostSortBy.LATEST to CursorDirection.PREVIOUS, PostSortBy.OLDEST to CursorDirection.NEXT -> listPreviousPosts(
                cursorRequest = cursorRequest,
                postSpaceKey = postSpaceKey,
                parentId = parentId,
            )

            else -> throw NotSupportedException("지원하지 않는 SortBy($sortBy)-Direction(${cursorRequest.direction}) 입니다")
        }

        if (posts.size > cursorRequest.pageSize) {
            val postSections = posts.subList(0, cursorRequest.pageSize.coerceAtMost(posts.size))
                .groupBy { post -> PostSlotAssigner.assign(postNo = post.key.postNo) }
                .flatMap { (slotId, posts) ->
                    postSectionCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdAndKeyPostNoIn(
                        workspaceId = postSpaceKey.workspaceId,
                        componentId = postSpaceKey.componentId,
                        spaceId = postSpaceKey.spaceId,
                        parentId = parentId?.serialize() ?: StringUtils.EMPTY,
                        slotId = slotId,
                        postNos = posts.map { post -> post.key.postNo },
                    ).toList()
                }.groupBy { postSection -> postSection.key.postNo }

            return Slice(
                data = posts.subList(0, cursorRequest.pageSize.coerceAtMost(posts.size))
                    .map { post ->
                        PostWithSections.of(
                            post = post,
                            sections = postSectionManager.makePostSectionContentResponse(
                                postSections[post.key.postNo] ?: emptyList()
                            )
                        )
                    },
                cursor = CursorUtils.getCursor(
                    listWithNextCursor = posts,
                    pageSize = cursorRequest.pageSize,
                    keyGenerator = { post -> post?.key?.postNo?.toString() }
                )
            )
        }

        val morePosts = when (cursorRequest.direction) {
            CursorDirection.NEXT -> {
                postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotId(
                    workspaceId = postSpaceKey.workspaceId,
                    componentId = postSpaceKey.componentId,
                    spaceId = postSpaceKey.spaceId,
                    parentId = parentId?.serialize() ?: StringUtils.EMPTY,
                    slotId = slot - 1,
                    pageable = CassandraPageRequest.first(cursorRequest.pageSize - posts.size + 1),
                ).toList()
            }

            CursorDirection.PREVIOUS -> {
                postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdOrderByKeyPostNoAsc(
                    workspaceId = postSpaceKey.workspaceId,
                    componentId = postSpaceKey.componentId,
                    spaceId = postSpaceKey.spaceId,
                    parentId = parentId?.serialize() ?: StringUtils.EMPTY,
                    slotId = slot + 1,
                    pageable = CassandraPageRequest.first(cursorRequest.pageSize - posts.size + 1),
                ).toList()
            }
        }

        val data = posts + morePosts.subList(0, (cursorRequest.pageSize - posts.size).coerceAtMost(morePosts.size))

        val postSections = getPostSections(data.subList(0, cursorRequest.pageSize.coerceAtMost(posts.size)))

        return Slice(
            data = data.map { post ->
                PostWithSections.of(
                    post = post,
                    sections = postSectionManager.makePostSectionContentResponse(
                        postSections[post.key.postNo] ?: emptyList()
                    )
                )
            },
            cursor = CursorUtils.getCursor(
                listWithNextCursor = morePosts,
                pageSize = cursorRequest.pageSize - posts.size,
                keyGenerator = { post -> post?.key?.postNo?.toString() }
            )
        )
    }

    private suspend fun listNextPosts(
        cursorRequest: CursorRequest,
        parentId: PostId?,
        postSpaceKey: PostSpaceKey,
    ): Pair<Long, List<PostEntity>> {
        if (cursorRequest.cursor == null) {
            val lastSlotId = PostSlotAssigner.assign(
                postNo = postSequenceRepository.getLastSequence(
                    postSpaceKey = postSpaceKey,
                    parentId = parentId
                )
            )
            return lastSlotId to postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotId(
                workspaceId = postSpaceKey.workspaceId,
                componentId = postSpaceKey.componentId,
                spaceId = postSpaceKey.spaceId,
                parentId = parentId?.serialize() ?: StringUtils.EMPTY,
                slotId = lastSlotId,
                pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1),
            ).toList()
        }

        val currentSlot = PostSlotAssigner.assign(
            postNo = cursorRequest.cursor.toLongOrNull()
                ?: throw InvalidCursorException("잘못된 CursorResponse(${cursorRequest.cursor})입니다")
        )
        return currentSlot to postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdAndKeyPostNoLessThan(
            workspaceId = postSpaceKey.workspaceId,
            componentId = postSpaceKey.componentId,
            spaceId = postSpaceKey.spaceId,
            parentId = parentId?.serialize() ?: StringUtils.EMPTY,
            slotId = currentSlot,
            pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1),
            postNo = cursorRequest.cursor.toLongOrNull()
                ?: throw InvalidCursorException("잘못된 CursorResponse(${cursorRequest.cursor})입니다"),
        ).toList()
    }

    private suspend fun listPreviousPosts(
        cursorRequest: CursorRequest,
        parentId: PostId?,
        postSpaceKey: PostSpaceKey,
    ): Pair<Long, List<PostEntity>> {
        if (cursorRequest.cursor == null) {
            val firstSlotId = PostSlotAssigner.FIRST_SLOT_ID
            return firstSlotId to postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdOrderByKeyPostNoAsc(
                workspaceId = postSpaceKey.workspaceId,
                componentId = postSpaceKey.componentId,
                spaceId = postSpaceKey.spaceId,
                parentId = parentId?.serialize() ?: StringUtils.EMPTY,
                slotId = firstSlotId,
                pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1),
            ).toList()
        }
        val currentSlot = PostSlotAssigner.assign(
            postNo = cursorRequest.cursor.toLongOrNull()
                ?: throw InvalidCursorException("잘못된 CursorResponse(${cursorRequest.cursor})입니다"),
        )
        return currentSlot to postCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdAndKeyPostNoGreaterThanOrderByKeyPostNoAsc(
            workspaceId = postSpaceKey.workspaceId,
            componentId = postSpaceKey.componentId,
            spaceId = postSpaceKey.spaceId,
            parentId = parentId?.serialize() ?: StringUtils.EMPTY,
            slotId = currentSlot,
            postNo = cursorRequest.cursor.toLongOrNull()
                ?: throw InvalidCursorException("잘못된 CursorResponse(${cursorRequest.cursor})입니다"),
            pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1),
        ).toList()
    }

    suspend fun listOwnerPosts(
        workspaceId: String,
        componentId: String,
        ownerId: String,
        cursorRequest: CursorRequest,
    ): Slice<PostWithSections, String> = coroutineScope {
        val posts = postReverses(
            workspaceId = workspaceId,
            componentId = componentId,
            ownerId = ownerId,
            cursorRequest = cursorRequest,
        )

        val postSections = getPostReverseSections(
            posts.subList(0, cursorRequest.pageSize.coerceAtMost(posts.size))
        )

        return@coroutineScope Slice(
            data = posts.subList(0, cursorRequest.pageSize.coerceAtMost(posts.size))
                .map { post ->
                    PostWithSections.of(
                        post = post,
                        sections = postSectionManager.makePostSectionContentResponse(
                            postSections[post.key.postNo] ?: emptyList()
                        )
                    )
                },
            cursor = CursorUtils.getCursor(
                listWithNextCursor = posts,
                pageSize = cursorRequest.pageSize,
                keyGenerator = { post -> post?.key?.postNo?.toString() }
            )
        )
    }

    private suspend fun postReverses(
        workspaceId: String,
        componentId: String,
        ownerId: String,
        cursorRequest: CursorRequest,
    ): List<PostReverse> {
        if (cursorRequest.cursor.isNullOrBlank()) {
            return postReverseCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeyDistributionKeyAndKeyOwnerId(
                workspaceId = workspaceId,
                componentId = componentId,
                distributionKey = PostDistributionKey.makeKey(ownerId),
                ownerId = ownerId,
                pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1)
            ).toList()
        }
        return postReverseCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeyDistributionKeyAndKeyOwnerIdAndKeyPostNoLessThan(
            workspaceId = workspaceId,
            componentId = componentId,
            distributionKey = PostDistributionKey.makeKey(ownerId),
            ownerId = ownerId,
            postNo = cursorRequest.cursor.toLong(),
            pageable = CassandraPageRequest.first(cursorRequest.pageSize + 1)
        ).toList()
    }

    private suspend fun getPostSections(
        posts: List<PostEntity>,
    ): Map<Long, List<PostSectionEntity>> = coroutineScope {
        return@coroutineScope posts.groupBy { post -> PostSectionPartitionKey.from(post) }
            .map { (sectionPartition, posts) ->
                async {
                    postSectionCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdAndKeyPostNoIn(
                        workspaceId = sectionPartition.workspaceId,
                        componentId = sectionPartition.componentId,
                        spaceId = sectionPartition.spaceId,
                        parentId = sectionPartition.parentId,
                        slotId = sectionPartition.slotId,
                        postNos = posts.map { post -> post.key.postNo },
                    ).toList()
                }
            }.awaitAll().flatten().groupBy { postSection -> postSection.key.postNo }
    }

    private suspend fun getPostReverseSections(
        posts: List<PostReverse>,
    ): Map<Long, List<PostSectionEntity>> = coroutineScope {
        return@coroutineScope posts.groupBy { post -> PostSectionPartitionKey.from(post) }
            .map { (sectionPartition, posts) ->
                async {
                    postSectionCassandraRepository.findAllByKeyWorkspaceIdAndKeyComponentIdAndKeySpaceIdAndKeyParentIdAndKeySlotIdAndKeyPostNoIn(
                        workspaceId = sectionPartition.workspaceId,
                        componentId = sectionPartition.componentId,
                        spaceId = sectionPartition.spaceId,
                        parentId = sectionPartition.parentId,
                        slotId = sectionPartition.slotId,
                        postNos = posts.map { post -> post.key.postNo },
                    ).toList()
                }
            }.awaitAll().flatten().groupBy { postSection -> postSection.key.postNo }
    }

}