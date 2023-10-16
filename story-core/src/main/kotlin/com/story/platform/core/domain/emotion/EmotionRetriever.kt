package com.story.platform.core.domain.emotion

import com.story.platform.core.common.model.Slice
import com.story.platform.core.common.model.dto.CursorResponse
import com.story.platform.core.common.utils.mapToSet
import com.story.platform.core.domain.emotion.EmotionPolicy.EMOTION_MAX_COUNT_PER_COMPONENT
import com.story.platform.core.domain.resource.ResourceId
import kotlinx.coroutines.flow.toList
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.stereotype.Service

@Service
class EmotionRetriever(
    private val emotionRepository: EmotionRepository,
) {

    suspend fun validateExistsEmotions(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
        emotionIds: Set<String>,
    ) {
        val keys = emotionIds.map { emotionId ->
            EmotionPrimaryKey(
                workspaceId = workspaceId,
                resourceId = resourceId,
                componentId = componentId,
                emotionId = emotionId,
            )
        }
        val emotions = emotionRepository.findAllById(keys).toList()

        if (emotions.size < emotionIds.size) {
            val existsEmotionIds = emotions.mapToSet { emotion -> emotion.key.emotionId }
            throw EmotionNotExistsException("워크스페이스($workspaceId)의 리소스/컴포넌트($resourceId/$componentId)에 존재하지 않는 이모션(${emotionIds - existsEmotionIds})이 존재합니다")
        }
    }

    suspend fun getEmotions(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
        emotionIds: Set<String>,
    ): Map<String, EmotionResponse> {
        val emotions = emotionRepository.findAllByKeyWorkspaceIdAndKeyResourceIdAndKeyComponentIdAndKeyEmotionIdIn(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId,
            emotionIds = emotionIds,
        ).toList()
        return emotions.associateBy { emotion -> emotion.key.emotionId }
            .mapValues { emotion -> EmotionResponse.of(emotion.value) }
    }

    suspend fun listEmotions(
        workspaceId: String,
        resourceId: ResourceId,
        componentId: String,
    ): Slice<EmotionResponse, String> {
        val emotions = emotionRepository.findAllByKeyWorkspaceIdAndKeyResourceIdAndKeyComponentId(
            workspaceId = workspaceId,
            resourceId = resourceId,
            componentId = componentId,
            pageable = CassandraPageRequest.first(EMOTION_MAX_COUNT_PER_COMPONENT),
        ).toList()

        return Slice.of(
            data = emotions.map { emotion -> EmotionResponse.of(emotion = emotion) },
            cursor = CursorResponse.noMore(),
        )
    }

}
