package com.myrealtrip.infrastructure.notification

import com.myrealtrip.common.notification.NotificationEvent
import com.myrealtrip.common.notification.NotificationPort
import com.myrealtrip.infrastructure.slack.SlackNotificationService
import org.springframework.stereotype.Component

@Component
class SlackNotificationAdapter(
    private val slackNotificationService: SlackNotificationService,
) : NotificationPort {

    override fun supports(event: NotificationEvent): Boolean =
        event is NotificationEvent.Slack

    override fun send(event: NotificationEvent) {
        if (event !is NotificationEvent.Slack) return
        slackNotificationService.notify(message = event.message)
    }
}

