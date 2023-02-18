package com.story.platform.core.domain.subscription

import com.story.platform.core.common.enums.ServiceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.stereotype.Service

@Service
class SubscriptionUnSubscriber(
    private val reactiveCassandraOperations: ReactiveCassandraOperations,
    private val subscriptionReverseCoroutineRepository: SubscriptionReverseCoroutineRepository,
    private val subscriptionCoroutineRepository: SubscriptionCoroutineRepository,
    private val subscriptionCounterCoroutineRepository: SubscriptionCounterCoroutineRepository,
) {

    suspend fun unsubscribe(
        serviceType: ServiceType,
        subscriptionType: SubscriptionType,
        targetId: String,
        subscriberId: String,
    ) {
        val subscriptionReverse = subscriptionReverseCoroutineRepository.findById(
            SubscriptionReversePrimaryKey(
                serviceType = serviceType,
                subscriptionType = subscriptionType,
                subscriberId = subscriberId,
                targetId = targetId,
            )
        )

        if (subscriptionReverse == null || subscriptionReverse.isDeleted()) {
            return
        }

        val jobs = mutableListOf<Job>()
        withContext(Dispatchers.IO) {
            jobs.add(launch {
                val subscription = subscriptionCoroutineRepository.findById(
                    SubscriptionPrimaryKey(
                        serviceType = serviceType,
                        subscriptionType = subscriptionType,
                        targetId = targetId,
                        slotId = subscriptionReverse.slotId,
                        subscriberId = subscriberId,
                    )
                )
                subscriptionReverse.delete()
                reactiveCassandraOperations.batchOps()
                    .delete(subscription)
                    .insert(subscriptionReverse)
                    .execute()
                    .awaitSingleOrNull()
            })

            jobs.add(launch {
                subscriptionCounterCoroutineRepository.decrease(
                    SubscriptionCounterPrimaryKey(
                        serviceType = serviceType,
                        subscriptionType = subscriptionType,
                        targetId = targetId,
                    )
                )
            })
            jobs.joinAll()
        }
    }

}
