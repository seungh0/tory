package com.story.core.domain.post

import com.story.core.FunSpecIntegrationTest
import com.story.core.IntegrationTest
import com.story.core.common.error.NoPermissionException
import com.story.core.common.json.toJson
import com.story.core.domain.post.section.PostSectionRepository
import com.story.core.domain.post.section.PostSectionSlotAssigner
import com.story.core.domain.post.section.PostSectionType
import com.story.core.domain.post.section.text.TextPostSectionContent
import com.story.core.domain.post.section.text.TextPostSectionContentRequest
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

@IntegrationTest
internal class PostModifierTest(
    private val postModifier: PostModifier,
    private val postRepository: PostRepository,
    private val postReverseRepository: PostReverseRepository,
    private val postSectionRepository: PostSectionRepository,
) : FunSpecIntegrationTest({

    context("등록된 포스트를 수정한다") {
        test("기존에 등록된 포스트를 수정합니다") {
            // given
            val title = "포스트 제목"

            val post = PostFixture.create()
            val postRev = PostReverse.of(post)

            postRepository.save(post)
            postReverseRepository.save(postRev)

            val section1 = TextPostSectionContentRequest(
                content = "컨텐츠 내용 - 1",
                priority = 1L,
            )
            val section2 = TextPostSectionContentRequest(
                content = "컨텐츠 내용 - 2",
                priority = 2L,
            )

            // when
            postModifier.patchPost(
                postSpaceKey = PostSpaceKey(
                    workspaceId = post.key.workspaceId,
                    componentId = post.key.componentId,
                    spaceId = post.key.spaceId,
                ),
                postId = post.key.postId,
                accountId = post.accountId,
                title = title,
                sections = listOf(section1, section2),
            )

            // then
            val posts = postRepository.findAll().toList()
            posts shouldHaveSize 1
            posts[0].also {
                it.key.workspaceId shouldBe post.key.workspaceId
                it.key.componentId shouldBe post.key.componentId
                it.key.spaceId shouldBe post.key.spaceId
                it.key.slotId shouldBe post.key.slotId
                it.key.postId shouldBe post.key.postId
                it.accountId shouldBe post.accountId
                it.title shouldBe title
            }

            val postReverses = postReverseRepository.findAll().toList()
            postReverses shouldHaveSize 1
            postReverses[0].also {
                it.key.workspaceId shouldBe post.key.workspaceId
                it.key.accountId shouldBe post.accountId
                it.key.spaceId shouldBe post.key.spaceId
                it.key.postId shouldBe post.key.postId
                it.title shouldBe title
            }

            val postSections = postSectionRepository.findAll().toList()
            postSections shouldHaveSize 2
            postSections[0].also {
                it.key.workspaceId shouldBe post.key.workspaceId
                it.key.componentId shouldBe post.key.componentId
                it.key.spaceId shouldBe post.key.spaceId
                it.key.slotId shouldBe PostSectionSlotAssigner.assign(postId = post.key.postId)
                it.key.priority shouldBe 1L
                it.sectionType shouldBe PostSectionType.TEXT
                it.data shouldBe TextPostSectionContent(
                    content = section1.content
                ).toJson()
            }
            postSections[1].also {
                it.key.workspaceId shouldBe post.key.workspaceId
                it.key.componentId shouldBe post.key.componentId
                it.key.spaceId shouldBe post.key.spaceId
                it.key.slotId shouldBe PostSectionSlotAssigner.assign(postId = post.key.postId)
                it.key.priority shouldBe 2L
                it.sectionType shouldBe PostSectionType.TEXT
                it.data shouldBe TextPostSectionContent(
                    content = section2.content
                ).toJson()
            }
        }

        test("존재하지 않는 포스트의 경우 포스트 정보를 수정할 수 없습니다") {
            // given
            val title = "포스트 제목"
            val content = "포스트 내용"

            // when & then
            shouldThrowExactly<PostNotExistsException> {
                postModifier.patchPost(
                    postSpaceKey = PostSpaceKey(
                        workspaceId = "story",
                        componentId = "post",
                        spaceId = "50000",
                    ),
                    postId = 10000L,
                    accountId = "accountId",
                    title = title,
                    sections = emptyList(),
                )
            }
        }

        test("포스트의 작성자만이 포스트를 수정할 수 있습니다") {
            // given
            val title = "포스트 제목"

            val post = PostFixture.create(accountId = "accountId")
            val postRev = PostReverse.of(post)

            postRepository.save(post)
            postReverseRepository.save(postRev)

            // when & then
            shouldThrowExactly<NoPermissionException> {
                postModifier.patchPost(
                    postSpaceKey = PostSpaceKey(
                        workspaceId = post.key.workspaceId,
                        componentId = post.key.componentId,
                        spaceId = post.key.spaceId,
                    ),
                    postId = post.key.postId,
                    accountId = "another Account Id",
                    title = title,
                    sections = emptyList(),
                )
            }
        }
    }

})