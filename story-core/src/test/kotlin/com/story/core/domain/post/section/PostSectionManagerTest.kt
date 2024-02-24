package com.story.core.domain.post.section

import com.story.core.common.json.toJson
import com.story.core.domain.file.FileType
import com.story.core.domain.post.PostSpaceKey
import com.story.core.domain.post.section.image.ImagePostSectionContent
import com.story.core.domain.post.section.image.ImagePostSectionContentRequest
import com.story.core.domain.post.section.image.ImagePostSectionHandler
import com.story.core.domain.post.section.text.TextPostSectionContent
import com.story.core.domain.post.section.text.TextPostSectionContentRequest
import com.story.core.domain.post.section.text.TextPostSectionHandler
import com.story.core.infrastructure.file.FileProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PostSectionManagerTest : StringSpec({

    val manager = PostSectionManager(
        postSectionHandlerFinder = { sectionType ->
            when (sectionType) {
                PostSectionType.TEXT -> TextPostSectionHandler()
                PostSectionType.IMAGE -> ImagePostSectionHandler(
                    properties = FileProperties(
                        properties = mapOf(
                            FileType.IMAGE to FileProperties(
                                domain = "https://cdn.story.com"
                            )
                        )
                    )
                )

            }
        }
    )

    "포스트 섹션을 생성합니다" {
        // given
        val request1 = TextPostSectionContentRequest(
            content = "토키에요",
            priority = 1,
        )
        val request2 = TextPostSectionContentRequest(
            content = "뽀미에요",
            priority = 2,
        )

        val request3 = ImagePostSectionContentRequest(
            priority = 3,
            path = "/pictures",
            fileName = "flower.png",
        )

        // when
        val sut = manager.makePostSections(
            requests = listOf(request1, request2, request3),
            postSpaceKey = PostSpaceKey(
                workspaceId = "story",
                componentId = "user-post",
                spaceId = "tokki",
            ),
            ownerId = "tokki",
            postId = 1000L,
            parentId = null,
        )

        // then
        sut shouldHaveSize 3
        sut.forEach { section ->
            section.key.workspaceId shouldBe "story"
            section.key.componentId shouldBe "user-post"
            section.key.spaceId shouldBe "tokki"
            section.key.parentId shouldBe ""
            section.key.slotId shouldBe 10L
            section.key.postId shouldBe 1000L
        }

        sut[0].sectionType shouldBe PostSectionType.TEXT
        sut[0].key.priority shouldBe 1L
        sut[0].data shouldBe TextPostSectionContent(content = "토키에요").toJson()

        sut[1].sectionType shouldBe PostSectionType.TEXT
        sut[1].key.priority shouldBe 2L
        sut[1].data shouldBe TextPostSectionContent(content = "뽀미에요").toJson()

        sut[2].sectionType shouldBe PostSectionType.IMAGE
        sut[2].key.priority shouldBe 3L
        sut[2].data shouldBe ImagePostSectionContent(
            path = "/pictures",
            width = 0,
            height = 0,
            fileSize = 0,
            fileName = "flower.png",
        ).toJson()
    }

})