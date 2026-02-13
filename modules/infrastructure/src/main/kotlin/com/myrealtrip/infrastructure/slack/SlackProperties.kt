package com.myrealtrip.infrastructure.slack

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Slack 알림 설정 프로퍼티
 *
 * @property botToken Bot User OAuth Token (xoxb-...)
 * @property defaultChannel 기본 채널 (미지정 시 notify() 호출 시 채널 필수)
 * @property enabled 활성화 여부 (false 시 메시지 전송하지 않음)
 * @property retry 재시도 설정
 */
@ConfigurationProperties(prefix = "slack")
data class SlackProperties(
    val botToken: String = "",
    val defaultChannel: String? = null,
    val enabled: Boolean = true,
    val retry: RetryProperties = RetryProperties(),
) {

    init {
        if (enabled && botToken.isBlank()) {
            throw IllegalArgumentException("Slack bot token is required when slack is enabled")
        }
    }

    /**
     * 재시도 설정
     *
     * @property maxAttempts 최대 재시도 횟수
     * @property backoffMs 초기 백오프 시간 (ms)
     * @property maxBackoffMs 최대 백오프 시간 (ms)
     */
    data class RetryProperties(
        val maxAttempts: Int = 3,
        val backoffMs: Long = 1000,
        val maxBackoffMs: Long = 10000,
    )
}
