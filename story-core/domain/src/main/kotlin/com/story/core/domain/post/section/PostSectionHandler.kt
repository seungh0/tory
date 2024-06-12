package com.story.core.domain.post.section

interface PostSectionHandler {

    fun sectionType(): PostSectionType

    suspend fun makeContents(
        workspaceId: String,
        requests: Collection<PostSectionContentCommand>,
    ): Map<PostSectionContentCommand, PostSectionContentEntity>

    suspend fun makeContentResponse(contents: Collection<PostSectionContentEntity>): Map<PostSectionContentEntity, PostSectionContent>

}