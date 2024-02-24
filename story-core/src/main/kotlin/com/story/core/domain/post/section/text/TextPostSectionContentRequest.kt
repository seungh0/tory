package com.story.core.domain.post.section.text

import com.story.core.domain.post.section.PostSectionContentRequest
import com.story.core.domain.post.section.PostSectionType

data class TextPostSectionContentRequest(
    override val priority: Long,
    val content: String,
    val extra: Map<String, Any> = emptyMap(),
) : PostSectionContentRequest {

    override fun sectionType() = PostSectionType.TEXT

}
