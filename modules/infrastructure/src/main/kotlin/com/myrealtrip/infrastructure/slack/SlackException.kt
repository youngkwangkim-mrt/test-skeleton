package com.myrealtrip.infrastructure.slack

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.BizRuntimeException

/** Slack API 호출 시 발생하는 예외. */
class SlackException(
    val slackError: String,
    message: String,
    cause: Throwable? = null,
) : BizRuntimeException(
    code = ErrorCode.CALL_RESPONSE_ERROR,
    message = message,
    cause = cause,
    logStackTrace = true,
) {

    companion object {

        fun of(error: String, warning: String? = null): SlackException {
            val message = buildString {
                append("Slack API error: $error")
                if (!warning.isNullOrBlank()) append(" (warning: $warning)")
            }
            return SlackException(slackError = error, message = message)
        }

        fun channelNotFound(channel: String) = SlackException(
            slackError = "channel_not_found",
            message = "Slack channel not found: $channel",
        )

        fun notInChannel(channel: String) = SlackException(
            slackError = "not_in_channel",
            message = "Bot is not in channel: $channel. Please invite the bot to the channel.",
        )

        fun invalidAuth() = SlackException(
            slackError = "invalid_auth",
            message = "Invalid Slack bot token. Please check your configuration.",
        )
    }
}
