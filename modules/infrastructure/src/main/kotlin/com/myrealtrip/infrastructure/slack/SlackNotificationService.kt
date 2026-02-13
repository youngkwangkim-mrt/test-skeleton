package com.myrealtrip.infrastructure.slack

import com.myrealtrip.common.notification.message.MessageColor
import com.myrealtrip.common.notification.message.SlackMessage
import com.myrealtrip.common.notification.message.slackMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

/**
 * 고수준 Slack 알림 서비스.
 *
 * 편의 메서드(`notifySuccess`, `notifyWarning`, `notifyError`, `notifyInfo`)와
 * DSL 기반 [SlackMessage] 전송을 모두 지원합니다.
 * 전송 시점에 footer(traceId, appName, profile)가 자동 추가됩니다.
 */
@Service
class SlackNotificationService(
    private val slackClient: SlackClient,
    private val properties: SlackProperties,
) {

    /** [SlackMessage] 전송. 채널 미지정 시 기본 채널 사용. */
    fun notify(channel: String? = null, message: SlackMessage): SlackResponse {
        if (!properties.enabled) {
            logger.info { "Slack is disabled. Message: ${message.text}" }
            return SlackResponse.DISABLED
        }

        val targetChannel = resolveChannel(channel, message.channel)
        return slackClient.send(targetChannel, message)
    }

    /** 성공 알림 (녹색). */
    fun notifySuccess(channel: String? = null, title: String, message: String): SlackResponse =
        notifyWithColor(channel, title, message, MessageColor.SUCCESS)

    /** 경고 알림 (노란색). */
    fun notifyWarning(channel: String? = null, title: String, message: String): SlackResponse =
        notifyWithColor(channel, title, message, MessageColor.WARNING)

    /** 정보 알림 (파란색). */
    fun notifyInfo(channel: String? = null, title: String, message: String): SlackResponse =
        notifyWithColor(channel, title, message, MessageColor.INFO)

    /** 에러 알림 (빨간색, 스택트레이스·Root Cause 포함). */
    fun notifyError(channel: String? = null, title: String, error: Throwable): SlackResponse {
        val rootCause = generateSequence(error) { it.cause }.last()
        val stackTrace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
            .toString()
            .lines()
            .take(MAX_STACK_TRACE_LINES)
            .joinToString("\n")
            .take(MAX_STACK_TRACE_CHARS)

        val message = slackMessage {
            text(title)
            attachment(MessageColor.DANGER) {
                section { markdown("*$title*") }
                section {
                    markdown("*에러:* `${error.javaClass.simpleName}`\n")
                    markdown("*메시지:* ${error.message ?: "N/A"}")
                }
                if (rootCause !== error) {
                    section {
                        markdown("*Root Cause:* `${rootCause.javaClass.simpleName}`\n")
                        markdown("*메시지:* ${rootCause.message ?: "N/A"}")
                    }
                }
                divider()
                section { markdown("*Stack Trace:*\n```$stackTrace```") }
            }
        }
        return notify(channel, message)
    }

    /** 스레드 답장. [threadTs]는 원본 메시지 응답의 `ts` 값. */
    fun reply(channel: String, threadTs: String, message: SlackMessage): SlackResponse =
        notify(channel, message.copy(threadTs = threadTs))

    private fun notifyWithColor(
        channel: String?,
        title: String,
        message: String,
        color: MessageColor,
    ): SlackResponse {
        val slackMsg = slackMessage {
            attachment(color) {
                fallback(message)
                section { markdown("*$title*\n$message") }
            }
        }
        return notify(channel, slackMsg)
    }

    private fun resolveChannel(paramChannel: String?, messageChannel: String?): String {
        return paramChannel?.takeIf { it.isNotBlank() }
            ?: messageChannel?.takeIf { it.isNotBlank() }
            ?: properties.defaultChannel?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Channel must be specified. Set slack.default-channel or pass channel parameter.",
            )
    }

    companion object {
        private const val MAX_STACK_TRACE_LINES = 10
        private const val MAX_STACK_TRACE_CHARS = 2900
    }
}
