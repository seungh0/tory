package com.story.platform.core.domain.subscription

import com.story.platform.core.common.enums.ServiceType
import com.story.platform.core.infrastructure.kafka.KafkaProducerConfig
import com.story.platform.core.infrastructure.kafka.KafkaTopicFinder
import com.story.platform.core.infrastructure.kafka.TopicType
import com.story.platform.core.support.json.toJson
import com.story.platform.core.support.lock.DistributeLock
import com.story.platform.core.support.lock.DistributedLockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class SubscriptionSubscriber(
    private val reactiveCassandraOperations: ReactiveCassandraOperations,
    private val subscriptionCoroutineRepository: SubscriptionCoroutineRepository,
    private val subscriberCounterCoroutineRepository: SubscriberCounterCoroutineRepository,
    private val subscriberIdGenerator: SubscriberIdGenerator,
    @Qualifier(KafkaProducerConfig.ACK_ALL_KAFKA_TEMPLATE)
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    @DistributeLock(
        lockType = DistributedLockType.SUBSCRIBE,
        key = "'serviceType:' + {#serviceType} + ':subscriptionType:' + {#subscriptionType} + ':targetId:' + {#targetId} + ':subscriberId:' + {#subscriberId}",
    )
    suspend fun subscribe(
        serviceType: ServiceType,
        subscriptionType: SubscriptionType,
        targetId: String,
        subscriberId: String,
    ) {
        val primaryKey = SubscriptionPrimaryKey(
            serviceType = serviceType,
            subscriptionType = subscriptionType,
            subscriberId = subscriberId,
            targetId = targetId,
        )
        val subscriptionReverse = subscriptionCoroutineRepository.findById(primaryKey)
        if ((subscriptionReverse != null) && subscriptionReverse.isActivated()) {
            return
        }

        val subscriber = Subscriber.of(
            serviceType = serviceType,
            subscriptionType = subscriptionType,
            targetId = targetId,
            slotId = subscriptionReverse?.slotId ?: SubscriberSlotAssigner.assign(
                subscriberIdGenerator.generate(
                    serviceType = serviceType,
                    subscriptionType = subscriptionType,
                    targetId = targetId
                )
            ),
            subscriberId = subscriberId,
        )

        withContext(Dispatchers.IO) {
            val jobs = mutableListOf<Job>()

            reactiveCassandraOperations.batchOps()
                .insert(subscriber)
                .insert(Subscription.of(subscriber = subscriber))
                .insert(SubscriberDistributed.of(subscriber = subscriber))
                .execute()
                .awaitSingleOrNull()

            jobs += launch {
                subscriberCounterCoroutineRepository.increase(
                    SubscriberCounterPrimaryKey(
                        serviceType = serviceType,
                        subscriptionType = subscriptionType,
                        targetId = targetId,
                    )
                )
            }

            jobs.joinAll()
        }

        val event = SubscriptionEvent.created(
            serviceType = serviceType,
            subscriptionType = subscriptionType,
            subscriberId = subscriberId,
            targetId = targetId,
        )
        kafkaTemplate.send(KafkaTopicFinder.getTopicName(TopicType.SUBSCRIPTION), event.toJson())
    }

}
