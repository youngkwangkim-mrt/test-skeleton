package com.myrealtrip.infrastructure.notification

import com.myrealtrip.common.notification.NotificationEvent
import com.myrealtrip.common.notification.NotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

@Component
class NotificationEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val adapters: List<NotificationPort>,
) {

    fun publish(event: NotificationEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handle(event: NotificationEvent) {
        logger.debug { "NotificationEvent received, class: ${event::class.simpleName}" }
        try {
            val adapter = adapters.firstOrNull { it.supports(event) }
            if (adapter == null) {
                logger.warn { "No adapter found for NotificationEvent, class: ${event::class.simpleName}" }
                return
            }
            adapter.send(event)
        } catch (e: Exception) {
            logger.error(e) { "NotificationEvent handling failed, class: ${event::class.simpleName}, message: ${e.message}" }
        }
    }
}
