package com.myrealtrip.infrastructure.slack

import com.myrealtrip.common.notification.message.SlackMessage
import com.myrealtrip.common.utils.coroutine.retryBlocking
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.SlackApiTextResponse
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.chat.ChatUpdateRequest
import com.slack.api.model.block.LayoutBlock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/** Slack SDK 래퍼. API 호출·재시도·에러 매핑을 캡슐화합니다. */
@Component
class SlackClient(
    private val methodsClient: MethodsClient,
    private val properties: SlackProperties,
    private val slackFooterProvider: SlackFooterProvider,
) {

    /** [SlackMessage] 전송. footer context block이 자동 추가됩니다. */
    fun send(channel: String? = null, message: SlackMessage): SlackResponse {
        val targetChannel = channel ?: message.channel
        ?: throw IllegalArgumentException("Channel must be specified either in parameter or message")

        logger.debug { "Sending message to channel: $targetChannel" }

        val sdkBlocks = SlackMessageConverter.toLayoutBlocks(message.blocks)
        val sdkAttachments = SlackMessageConverter.toAttachments(message.attachments)
        val blocksWithFooter = sdkBlocks + slackFooterProvider.buildFooterBlock()

        val request = ChatPostMessageRequest.builder()
            .channel(targetChannel)
            .apply {
                message.text?.let { text(it) }
                if (sdkAttachments.isNotEmpty()) attachments(sdkAttachments)
                if (blocksWithFooter.isNotEmpty()) blocks(blocksWithFooter)
                message.threadTs?.let { threadTs(it) }
            }
            .build()

        return executeAndMap(targetChannel, { methodsClient.chatPostMessage(request) }) { it.ts to it.channel }
            .also { logger.debug { "Message sent successfully, ts: ${it.ts}" } }
    }

    /** 기존 메시지 업데이트. */
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

        return executeAndMap(channel, { methodsClient.chatUpdate(request) }) { it.ts to it.channel }
    }

    /** retry 실행 → isOk 검사 → [SlackResponse] 변환을 통합한 공통 메서드. */
    private fun <T : SlackApiTextResponse> executeAndMap(
        channel: String,
        action: () -> T,
        resultExtractor: (T) -> Pair<String, String>,
    ): SlackResponse {
        val response = retryBlocking(
            maxAttempts = properties.retry.maxAttempts,
            delay = properties.retry.backoffMs.milliseconds,
            backoffMultiplier = 2.0,
        ) {
            val resp = action()
            if (resp.error == "rate_limited") {
                logger.warn { "Rate limited by Slack API" }
                throw SlackException(slackError = "rate_limited", message = "Rate limited by Slack API")
            }
            resp
        }

        if (!response.isOk) {
            throw mapError(response.error, response.warning, channel)
        }

        val (ts, ch) = resultExtractor(response)
        return SlackResponse(ts = ts, channel = ch)
    }

    private fun mapError(error: String, warning: String?, channel: String): SlackException =
        when (error) {
            "channel_not_found" -> SlackException.channelNotFound(channel)
            "not_in_channel" -> SlackException.notInChannel(channel)
            "invalid_auth", "account_inactive", "token_revoked" -> SlackException.invalidAuth()
            else -> SlackException.of(error, warning)
        }
}
