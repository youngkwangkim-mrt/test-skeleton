package com.myrealtrip.infrastructure.slack

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.infrastructure.slack.message.SlackColor
import com.myrealtrip.infrastructure.slack.message.SlackMessage
import com.myrealtrip.infrastructure.slack.message.slackMessage
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

/**
 * 고수준 Slack 알림 서비스
 *
 * 편의 메서드와 비동기 전송을 지원하는 고수준 API를 제공합니다.
 * slackMessage DSL로 생성된 모든 메시지에는 자동으로 traceId가 포함된 footer가 추가됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * // 1. DSL로 메시지 전송
 * slackNotificationService.notify("#alerts", slackMessage {
 *     header("알림")
 *     section { markdown("*중요* 내용") }
 * })
 *
 * // 2. 편의 메서드 사용
 * slackNotificationService.notifySuccess("#alerts", "배포 완료", "v1.2.3")
 * slackNotificationService.notifyError("#errors", "에러 발생", exception)
 *
 * // 3. 스레드 답장
 * val response = slackNotificationService.notify("#channel", message)
 * slackNotificationService.reply("#channel", response.ts, replyMessage)
 * ```
 */
@Service
class SlackNotificationService(
    private val slackClient: SlackClient,
    private val properties: SlackProperties,
) {

    /**
     * 메시지 전송
     *
     * @param channel 채널 (null이면 기본 채널 사용)
     * @param message SlackMessage
     * @return 전송 결과 (ts 값으로 스레드 답장 가능)
     * @throws IllegalStateException 채널이 지정되지 않은 경우
     */
    fun notify(channel: String? = null, message: SlackMessage): SlackResponse {
        if (!properties.enabled) {
            logger.info { "Slack is disabled. Message: ${message.text}" }
            return SlackResponse.DISABLED
        }

        val targetChannel = resolveChannel(channel, message.channel)
        return slackClient.send(targetChannel, message)
    }

    /**
     * 비동기 메시지 전송
     */
    @Async
    fun notifyAsync(channel: String? = null, message: SlackMessage) {
        notify(channel, message)
    }

    /**
     * 성공 알림 (녹색 스타일)
     *
     * @param channel 채널 (null이면 기본 채널 사용)
     * @param title 제목
     * @param message 메시지 내용
     */
    fun notifySuccess(channel: String? = null, title: String, message: String): SlackResponse {
        val slackMessage = slackMessage {
            text(title)
            attachment(SlackColor.SUCCESS) {
                section {
                    markdown("*$title*\n$message")
                }
            }
        }
        return notify(channel, slackMessage)
    }

    /**
     * 성공 알림 (녹색 스타일) - 비동기
     */
    @Async
    fun notifySuccessAsync(channel: String? = null, title: String, message: String) {
        notifySuccess(channel, title, message)
    }

    /**
     * 에러 알림 (빨간색 스타일, 스택트레이스 포함)
     *
     * @param channel 채널 (null이면 기본 채널 사용)
     * @param title 제목
     * @param error 에러
     */
    fun notifyError(channel: String? = null, title: String, error: Throwable): SlackResponse {
        val stackTrace = getStackTraceString(error)
        val truncatedStackTrace = stackTrace.take(MAX_STACK_TRACE_LENGTH)

        val slackMessage = slackMessage {
            text(title)
            attachment(SlackColor.DANGER) {
                header(title)
                section {
                    markdown("*에러:* `${error.javaClass.simpleName}`\n*메시지:* ${error.message ?: "N/A"}")
                }
                divider()
                section {
                    markdown("*:*\n```$truncatedStackTrace```")
                }
            }
        }
        return notify(channel, slackMessage)
    }

    /**
     * 에러 알림 (빨간색 스타일) - 비동기
     */
    @Async
    fun notifyErrorAsync(channel: String? = null, title: String, error: Throwable) {
        notifyError(channel, title, error)
    }

    /**
     * 경고 알림 (노란색 스타일)
     *
     * @param channel 채널 (null이면 기본 채널 사용)
     * @param title 제목
     * @param message 메시지 내용
     */
    fun notifyWarning(channel: String? = null, title: String, message: String): SlackResponse {
        val slackMessage = slackMessage {
            text(title)
            attachment(SlackColor.WARNING) {
                section {
                    markdown("*$title*\n$message")
                }
            }
        }
        return notify(channel, slackMessage)
    }

    /**
     * 경고 알림 (노란색 스타일) - 비동기
     */
    @Async
    fun notifyWarningAsync(channel: String? = null, title: String, message: String) {
        notifyWarning(channel, title, message)
    }

    /**
     * 정보 알림 (파란색 스타일)
     *
     * @param channel 채널 (null이면 기본 채널 사용)
     * @param title 제목
     * @param message 메시지 내용
     */
    fun notifyInfo(channel: String? = null, title: String, message: String): SlackResponse {
        val slackMessage = slackMessage {
            text(title)
            attachment(SlackColor.INFO) {
                section {
                    markdown("*$title*\n$message")
                }
            }
        }
        return notify(channel, slackMessage)
    }

    /**
     * 정보 알림 (파란색 스타일) - 비동기
     */
    @Async
    fun notifyInfoAsync(channel: String? = null, title: String, message: String) {
        notifyInfo(channel, title, message)
    }

    /**
     * 스레드 답장
     *
     * @param channel 채널
     * @param threadTs 원본 메시지의 timestamp (응답의 ts 값)
     * @param message 답장 메시지
     * @return 전송 결과
     */
    fun reply(channel: String, threadTs: String, message: SlackMessage): SlackResponse {
        if (!properties.enabled) {
            logger.info { "Slack is disabled. Reply message: ${message.text}" }
            return SlackResponse.DISABLED
        }

        val messageWithThread = message.copy(threadTs = threadTs)
        return slackClient.send(channel, messageWithThread)
    }

    /**
     * 스레드 답장 - 비동기
     */
    @Async
    fun replyAsync(channel: String, threadTs: String, message: SlackMessage) {
        reply(channel, threadTs, message)
    }

    private fun resolveChannel(paramChannel: String?, messageChannel: String?): String {
        return paramChannel
            ?: messageChannel
            ?: properties.defaultChannel
            ?: throw IllegalStateException(
                "Channel must be specified. Set slack.default-channel or pass channel parameter."
            )
    }

    private fun getStackTraceString(error: Throwable): String {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    companion object {
        private const val MAX_STACK_TRACE_LENGTH = 2000
    }
}
