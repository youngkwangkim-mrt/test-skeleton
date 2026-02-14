package com.myrealtrip.common.notification

import com.myrealtrip.common.notification.message.SlackMessage
import com.myrealtrip.common.notification.message.slackMessage

/**
 * 알림 이벤트.
 *
 * 채널별 서브타입으로 분기하여 [NotificationPort] 구현체가 처리합니다.
 */
sealed class NotificationEvent {

    /**
     * Slack 알림 이벤트.
     *
     * [SlackMessage] DSL 또는 간편 팩토리를 통해 생성합니다.
     */
    data class Slack(
        val message: SlackMessage,
    ) : NotificationEvent() {

        companion object {

            /** 제목·본문으로 기본 메시지를 생성하는 간편 팩토리. */
            operator fun invoke(
                channel: String? = null,
                title: String,
                message: String,
            ): Slack = Slack(
                message = slackMessage {
                    channel?.let { channel(it) }
                    text(title)
                    section {
                        markdown("*$title*\n")
                        markdown(message)
                    }
                }
            )

        }
    }

    /** 이메일 알림 이벤트. 템플릿 기반으로 렌더링되어 발송됩니다. */
    data class Email(
        val source: String? = null,
        val destination: String,
        val subject: String,
        val template: String,
        val variables: Map<String, Any> = emptyMap(),
    ) : NotificationEvent()

    /** SMS 알림 이벤트. */
    data class Sms(
        val icc: String,
        val phone: String,
        val message: String,
        val title: String? = null
    ) : NotificationEvent()
}
