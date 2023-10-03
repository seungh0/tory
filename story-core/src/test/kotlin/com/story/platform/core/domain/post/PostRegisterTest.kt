package com.story.platform.core.domain.post

import com.story.platform.core.IntegrationTest
import com.story.platform.core.common.distribution.XLargeDistributionKey
import com.story.platform.core.lib.TestCleaner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList

@IntegrationTest
internal class PostRegisterTest(
    private val postCreator: PostCreator,
    private val postRepository: PostRepository,
    private val postReverseRepository: PostReverseRepository,
    private val testCleaner: TestCleaner,
) : FunSpec({

    afterEach {
        testCleaner.cleanUp()
    }

    context("신규 포스트를 등록한다") {
        test("유저가 새로운 포스트를 등록합니다") {
            // given
            val postSpaceKey = PostSpaceKey(
                workspaceId = "story",
                componentId = "post",
                spaceId = "commentId",
            )
            val accountId = "accountId"
            val title = "포스트 제목"
            val content = """
                포스트 내용
                입니다
            """.trimIndent()
            val extra = mapOf("key" to "value")

            // when
            postCreator.createPost(
                postSpaceKey = postSpaceKey,
                accountId = accountId,
                title = title,
                content = content,
                extra = extra,
            )

            // then
            val posts = postRepository.findAll().toList()
            posts shouldHaveSize 1
            posts[0].also {
                it.key.workspaceId shouldBe postSpaceKey.workspaceId
                it.key.componentId shouldBe postSpaceKey.componentId
                it.key.spaceId shouldBe postSpaceKey.spaceId
                it.key.slotId shouldBe 1L
                it.key.postId shouldNotBe null
                it.accountId shouldBe accountId
                it.title shouldBe title
                it.content shouldBe content
                it.extra shouldBe extra
            }

            val postReverses = postReverseRepository.findAll().toList()
            postReverses shouldHaveSize 1
            postReverses[0].also {
                it.key.workspaceId shouldBe postSpaceKey.workspaceId
                it.key.componentId shouldBe postSpaceKey.componentId
                it.key.distributionKey shouldBe XLargeDistributionKey.makeKey(accountId).key
                it.key.accountId shouldBe accountId
                it.key.spaceId shouldBe postSpaceKey.spaceId
                it.key.postId shouldNotBe null
                it.title shouldBe title
                it.content shouldBe content
                it.extra shouldBe extra
            }
        }
    }

})
