package com.myrealtrip.infrastructure.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.chat.ChatUpdateRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.block.LayoutBlock
import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.infrastructure.slack.message.SlackMessage
import com.myrealtrip.common.utils.coroutine.runBlockingWithMDC
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Slack SDK 래퍼 클래스
 *
 * Slack API 호출을 캡슐화하고 에러 처리를 담당합니다.
 */
@Component
class SlackClient(
    private val methodsClient: MethodsClient,
    private val properties: SlackProperties,
) {

    /**
     * SlackMessage 전송
     *
     * Block Kit과 Attachment를 모두 지원하는 통합 전송 메서드입니다.
     *
     * @param channel 채널 (메시지에 채널이 없을 경우 필수)
     * @param message SlackMessage 객체
     * @return 전송 결과
     * @throws SlackException Slack API 에러 시
     * @throws IllegalArgumentException 채널이 지정되지 않은 경우
     */
    fun send(channel: String? = null, message: SlackMessage): SlackResponse {
        val targetChannel = channel ?: message.channel
            ?: throw IllegalArgumentException("Channel must be specified either in parameter or message")

        logger.debug { "Sending message to channel: $targetChannel" }

        val request = ChatPostMessageRequest.builder()
            .channel(targetChannel)
            .apply {
                message.text?.let { text(it) }
                if (message.blocks.isNotEmpty()) blocks(message.blocks)
                if (message.attachments.isNotEmpty()) attachments(message.attachments)
                message.threadTs?.let { threadTs(it) }
            }
            .build()

        return executePostMessage(request)
    }

    /**
     * 텍스트 메시지 전송
     *
     * @param channel 채널 (예: "#general", "C1234567890")
     * @param text 메시지 텍스트
     * @param threadTs 스레드 답장 시 원본 메시지의 ts
     * @return 전송 결과
     * @throws SlackException Slack API 에러 시
     */
    fun sendText(
        channel: String,
        text: String,
        threadTs: String? = null,
    ): SlackResponse {
        logger.debug { "Sending text message to channel: $channel" }

        val request = ChatPostMessageRequest.builder()
            .channel(channel)
            .text(text)
            .apply { threadTs?.let { threadTs(it) } }
            .build()

        return executePostMessage(request)
    }

    /**
     * Block Kit 메시지 전송
     *
     * @param channel 채널
     * @param blocks Block Kit 블록 목록
     * @param text fallback 텍스트 (알림용)
     * @param threadTs 스레드 답장 시 원본 메시지의 ts
     * @return 전송 결과
     * @throws SlackException Slack API 에러 시
     */
    fun sendBlocks(
        channel: String,
        blocks: List<LayoutBlock>,
        text: String? = null,
        threadTs: String? = null,
    ): SlackResponse {
        logger.debug { "Sending block message to channel: $channel, blocks: ${blocks.size}" }

        val request = ChatPostMessageRequest.builder()
            .channel(channel)
            .blocks(blocks)
            .apply {
                text?.let { text(it) }
                threadTs?.let { threadTs(it) }
            }
            .build()

        return executePostMessage(request)
    }

    /**
     * 메시지 업데이트
     *
     * @param channel 채널 ID
     * @param ts 업데이트할 메시지의 ts
     * @param text 새 텍스트
     * @param blocks 새 블록 (optional)
     * @return 업데이트 결과
     * @throws SlackException Slack API 에러 시
     */
    fun update(
        channel: String,
        ts: String,
        text: String,
        blocks: List<LayoutBlock>? = null,
    ): SlackResponse {
        logger.debug { "Updating message in channel: $channel, ts: $ts" }

        val request = ChatUpdateRequest.builder()
            .channel(channel)
            .ts(ts)
            .text(text)
            .apply { blocks?.let { blocks(it) } }
            .build()

        val response = methodsClient.chatUpdate(request)

        if (!response.isOk) {
            throw mapError(response.error, response.warning, channel)
        }

        return SlackResponse(
            ts = response.ts,
            channel = response.channel,
        )
    }

    private fun executePostMessage(request: ChatPostMessageRequest): SlackResponse {
        val response = runBlockingWithMDC { executeWithRetry { methodsClient.chatPostMessage(request) } }

        if (!response.isOk) {
            throw mapError(response.error, response.warning, request.channel)
        }

        logger.debug { "Message sent successfully, ts: ${response.ts}" }
        return SlackResponse(
            ts = response.ts,
            channel = response.channel,
        )
    }

    private suspend fun executeWithRetry(action: () -> ChatPostMessageResponse): ChatPostMessageResponse {
        var lastException: Exception? = null
        var attempt = 0

        while (attempt < properties.retry.maxAttempts) {
            try {
                val response = action()

                // Rate limit 처리
                if (response.error == "rate_limited") {
                    val retryAfter = response.httpResponseHeaders
                        ?.get("Retry-After")
                        ?.firstOrNull()
                        ?.toLongOrNull()
                        ?: 1L
                    logger.warn { "Rate limited, retrying after ${retryAfter}s (attempt ${attempt + 1})" }
                    delay(retryAfter * 1000)
                    attempt++
                    continue
                }

                return response
            } catch (e: Exception) {
                lastException = e
                val backoff = calculateBackoff(attempt)
                logger.warn { "Slack API call failed, retrying in ${backoff}ms (attempt ${attempt + 1}): ${e.message}" }
                delay(backoff)
                attempt++
            }
        }

        throw SlackException(
            slackError = "max_retries_exceeded",
            message = "Max retries exceeded after ${properties.retry.maxAttempts} attempts",
            cause = lastException,
        )
    }

    private fun calculateBackoff(attempt: Int): Long {
        val backoff = properties.retry.backoffMs * (1 shl attempt)
        return minOf(backoff, properties.retry.maxBackoffMs)
    }

    private fun mapError(error: String, warning: String?, channel: String): SlackException {
        return when (error) {
            "channel_not_found" -> SlackException.channelNotFound(channel)
            "not_in_channel" -> SlackException.notInChannel(channel)
            "invalid_auth", "account_inactive", "token_revoked" -> SlackException.invalidAuth()
            else -> SlackException.of(error, warning)
        }
    }
}
