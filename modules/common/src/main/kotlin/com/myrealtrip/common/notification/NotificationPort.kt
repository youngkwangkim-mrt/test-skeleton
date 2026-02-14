package com.myrealtrip.common.notification

/**
 * 알림 발송 포트.
 *
 * 인프라 모듈에서 채널별 구현체(Slack, Email 등)를 제공합니다.
 */
interface NotificationPort {

    /** 이 포트가 [event]를 처리할 수 있는지 판별합니다. */
    fun supports(event: NotificationEvent): Boolean

    /** [event]를 해당 채널로 발송합니다. */
    fun send(event: NotificationEvent)
}
