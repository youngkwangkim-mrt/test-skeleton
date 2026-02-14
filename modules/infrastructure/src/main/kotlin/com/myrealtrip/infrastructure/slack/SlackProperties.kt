package com.myrealtrip.infrastructure.slack

import org.springframework.boot.context.properties.ConfigurationProperties

/** Slack 알림 설정 프로퍼티. */
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

    /** 재시도 설정. */
    data class RetryProperties(
        val maxAttempts: Int = 3,
        val backoffMs: Long = 1000,
        val maxBackoffMs: Long = 10000,
    )
}
