package com.myrealtrip.infrastructure.slack

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.BizRuntimeException

/**
 * Slack API 호출 시 발생하는 예외
 *
 * @property slackError Slack API에서 반환한 에러 코드
 */
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

        /**
         * Slack API 응답에서 예외 생성
         */
        fun of(error: String, warning: String? = null): SlackException {
            val message = buildString {
                append("Slack API error: $error")
                if (!warning.isNullOrBlank()) {
                    append(" (warning: $warning)")
                }
            }
            return SlackException(slackError = error, message = message)
        }

        /**
         * 채널을 찾을 수 없는 경우
         */
        fun channelNotFound(channel: String): SlackException =
            SlackException(
                slackError = "channel_not_found",
                message = "Slack channel not found: $channel",
            )

        /**
         * 봇이 채널에 초대되지 않은 경우
         */
        fun notInChannel(channel: String): SlackException =
            SlackException(
                slackError = "not_in_channel",
                message = "Bot is not in channel: $channel. Please invite the bot to the channel.",
            )

        /**
         * 인증 실패
         */
        fun invalidAuth(): SlackException =
            SlackException(
                slackError = "invalid_auth",
                message = "Invalid Slack bot token. Please check your configuration.",
            )
    }
}
