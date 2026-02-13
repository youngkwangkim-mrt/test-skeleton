package com.myrealtrip.infrastructure.slack

/**
 * Slack 메시지 전송 결과
 *
 * @property ts 메시지 타임스탬프 (스레드 답장 시 threadTs로 사용)
 * @property channel 채널 ID
 */
data class SlackResponse(
    val ts: String,
    val channel: String,
) {

    companion object {
        /**
         * Slack 비활성화 시 반환되는 응답
         */
        val DISABLED = SlackResponse(ts = "", channel = "")
    }
}
