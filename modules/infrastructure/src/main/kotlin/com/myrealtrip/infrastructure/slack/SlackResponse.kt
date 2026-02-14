package com.myrealtrip.infrastructure.slack

/** Slack 메시지 전송 결과. [ts]는 스레드 답장 시 threadTs로 사용. */
data class SlackResponse(
    val ts: String,
    val channel: String,
) {

    companion object {
        val DISABLED = SlackResponse(ts = "", channel = "")
    }
}
