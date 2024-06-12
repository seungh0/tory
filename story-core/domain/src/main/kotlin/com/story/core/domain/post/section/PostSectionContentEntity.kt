package com.story.core.domain.post.section

import com.story.core.common.json.toJson
import org.springframework.data.annotation.Transient

interface PostSectionContentEntity {

    fun makeData() = this.toJson()

    @Transient
    fun sectionType(): PostSectionType

}