package com.story.core.domain.subscription

import com.story.core.FunSpecIntegrationTest
import com.story.core.IntegrationTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

@IntegrationTest
internal class SubscriptionUnSubscriberTest(
    private val subscriptionUnSubscriber: SubscriptionUnSubscriber,
    private val subscriberRepository: SubscriberRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : FunSpecIntegrationTest({

    context("구독을 취소한다") {
        test("등록한 구독 정보를 취소한다") {
            // given
            val workspaceId = "story"
            val componentId = "follow"
            val targetId = "10000"
            val subscriberId = "2000"

            subscriberRepository.save(
                SubscriberFixture.create(
                    workspaceId = workspaceId,
                    componentId = componentId,
                    subscriberId = subscriberId,
                    targetId = targetId,
                    slotId = 1L,
                )
            )

            subscriptionRepository.save(
                SubscriptionFixture.create(
                    workspaceId = workspaceId,
                    componentId = componentId,
                    subscriberId = subscriberId,
                    targetId = targetId,
                    slotId = 1L,
                )
            )

            // when
            subscriptionUnSubscriber.removeSubscription(
                workspaceId = workspaceId,
                componentId = componentId,
                targetId = targetId,
                subscriberId = subscriberId,
            )

            // then
            val subscribers = subscriberRepository.findAll().toList()
            subscribers shouldHaveSize 0

            val subscriptions = subscriptionRepository.findAll().toList()
            subscriptions shouldHaveSize 1
            subscriptions[0].also {
                it.key.workspaceId shouldBe workspaceId
                it.key.componentId shouldBe componentId
                it.key.subscriberId shouldBe subscriberId
                it.key.targetId shouldBe targetId
                it.slotId shouldBe 1L
                it.status shouldBe SubscriptionStatus.DELETED
            }
        }

        test("구독 정보가 없을 때 구독 정보를 취소하는 경우 멱등성을 갖는다") {
            // given
            val workspaceId = "story"
            val componentId = "follow"
            val targetId = "10000"
            val subscriberId = "2000"

            // when
            subscriptionUnSubscriber.removeSubscription(
                workspaceId = workspaceId,
                componentId = componentId,
                targetId = targetId,
                subscriberId = subscriberId,
            )

            // then
            val subscribers: List<Subscriber> = subscriberRepository.findAll().toList()
            subscribers shouldHaveSize 0

            val subscriptions = subscriptionRepository.findAll().toList()
            subscriptions shouldHaveSize 0
        }

        test("구독 취소시 이미 구독 취소 이력이 있다면 멱등성을 갖는다") {
            // given
            val workspaceId = "story"
            val componentId = "follow"
            val targetId = "10000"
            val subscriberId = "2000"

            subscriptionRepository.save(
                SubscriptionFixture.create(
                    workspaceId = workspaceId,
                    componentId = componentId,
                    subscriberId = subscriberId,
                    targetId = targetId,
                    slotId = 1L,
                    status = SubscriptionStatus.DELETED,
                )
            )

            // when
            subscriptionUnSubscriber.removeSubscription(
                workspaceId = workspaceId,
                componentId = componentId,
                targetId = targetId,
                subscriberId = subscriberId,
            )

            // then
            val subscribers = subscriberRepository.findAll().toList()
            subscribers shouldHaveSize 0

            val subscriptions = subscriptionRepository.findAll().toList()
            subscriptions shouldHaveSize 1
            subscriptions[0].also {
                it.key.workspaceId shouldBe workspaceId
                it.key.componentId shouldBe componentId
                it.key.subscriberId shouldBe subscriberId
                it.key.targetId shouldBe targetId
                it.slotId shouldBe 1L
                it.status shouldBe SubscriptionStatus.DELETED
            }
        }
    }

})