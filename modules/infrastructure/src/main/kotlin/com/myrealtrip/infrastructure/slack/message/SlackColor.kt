package com.myrealtrip.infrastructure.slack.message

/**
 * Slack Attachment 색상 바 색상
 *
 * Attachment의 왼쪽 색상 바에 사용됩니다.
 * Block Kit 자체는 색상 바를 지원하지 않으므로, Attachment 내에 Block을 넣어 사용합니다.
 */
enum class SlackColor(val value: String) {
    DEFAULT("#dddddd"),
    SUCCESS("#36a64f"),
    WARNING("#ff9800"),
    DANGER("#ff0000"),
    INFO("#439FE0"),
    ;

    companion object {

        /**
         * 커스텀 HEX 색상
         *
         * @param hex "#RRGGBB" 형식의 색상 코드
         * @return HEX 색상 문자열
         */
        fun hex(hex: String): String = hex
    }
}
