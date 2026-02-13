package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock

/**
 * Slack 메시지 도메인 모델
 *
 * @property channel 채널 (예: "#general", "C1234567890")
 * @property text fallback 텍스트 (알림에 표시)
 * @property blocks Block Kit 블록 목록
 * @property attachments Attachment 목록 (색상 바 지원)
 * @property threadTs 스레드 답장 시 원본 메시지의 ts
 */
data class SlackMessage(
    val channel: String? = null,
    val text: String? = null,
    val blocks: List<LayoutBlock> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val threadTs: String? = null,
) {

    /**
     * 메시지가 비어있는지 확인
     */
    fun isEmpty(): Boolean =
        text.isNullOrBlank() && blocks.isEmpty() && attachments.isEmpty()

    /**
     * 메시지가 유효한지 확인 (채널 또는 텍스트/블록 중 하나 이상 필요)
     */
    fun isValid(): Boolean = !isEmpty()
}
