package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.Attachment
import com.slack.api.model.block.*
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import org.slf4j.MDC

/**
 * Slack 메시지 DSL 빌더
 *
 * 사용 예시:
 * ```kotlin
 * val message = slackMessage {
 *     channel("#general")
 *     text("Hello, World!")
 *
 *     header("알림")
 *
 *     section {
 *         markdown("*중요한 내용*")
 *     }
 *
 *     divider()
 *
 *     attachment(SlackColor.SUCCESS) {
 *         section {
 *             fields("*상태*", "성공", "*시간*", "2024-01-01")
 *         }
 *     }
 *
 *     context {
 *         markdown("_작성자: Bot_")
 *     }
 * }
 * ```
 */
@SlackDsl
class SlackMessageBuilder {

    private var channel: String? = null
    private var text: String? = null
    private var threadTs: String? = null
    private val blocks: MutableList<LayoutBlock> = mutableListOf()
    private val attachments: MutableList<Attachment> = mutableListOf()

    /**
     * 채널 설정
     */
    fun channel(channel: String) {
        this.channel = channel
    }

    /**
     * 기본 텍스트 설정 (알림 미리보기용)
     */
    fun text(text: String) {
        this.text = text
    }

    /**
     * 스레드 답장 설정
     */
    fun threadTs(ts: String) {
        this.threadTs = ts
    }

    /**
     * Header 블록 추가
     */
    fun header(text: String) {
        blocks.add(
            HeaderBlock.builder()
                .text(
                    PlainTextObject.builder()
                        .text(text)
                        .emoji(true)
                        .build()
                )
                .build()
        )
    }

    /**
     * Section 블록 추가
     */
    fun section(block: SectionBlockBuilder.() -> Unit) {
        blocks.add(SectionBlockBuilder().apply(block).build())
    }

    /**
     * Divider 블록 추가
     */
    fun divider() {
        blocks.add(DividerBlock())
    }

    /**
     * Actions 블록 추가
     */
    fun actions(block: ActionsBlockBuilder.() -> Unit) {
        blocks.add(ActionsBlockBuilder().apply(block).build())
    }

    /**
     * Context 블록 추가
     */
    fun context(block: ContextBlockBuilder.() -> Unit) {
        blocks.add(ContextBlockBuilder().apply(block).build())
    }

    /**
     * Attachment 추가 (색상 바 지원)
     *
     * Block Kit 자체는 색상 바를 지원하지 않으므로,
     * 색상 바가 필요한 경우 attachment를 사용합니다.
     *
     * @param color 색상 바 색상 (SlackColor enum 또는 hex 코드)
     */
    fun attachment(
        color: SlackColor = SlackColor.DEFAULT,
        block: AttachmentBuilder.() -> Unit,
    ) {
        attachments.add(AttachmentBuilder(color.value).apply(block).build())
    }

    /**
     * Attachment 추가 (커스텀 hex 색상)
     *
     * @param hexColor hex 색상 코드 (예: "#FF5733")
     */
    fun attachment(
        hexColor: String,
        block: AttachmentBuilder.() -> Unit,
    ) {
        attachments.add(AttachmentBuilder(hexColor).apply(block).build())
    }

    internal fun build(): SlackMessage {
        val finalBlocks = buildFinalBlocks()

        return SlackMessage(
            channel = channel,
            text = text,
            blocks = finalBlocks,
            attachments = attachments.toList(),
            threadTs = threadTs,
        )
    }

    /**
     * 최종 블록 리스트를 생성합니다.
     *
     * - blocks가 비어있고 text만 있는 경우, text를 Section block으로 변환
     * - 마지막에 traceId와 timestamp가 포함된 footer context block 추가
     */
    private fun buildFinalBlocks(): List<LayoutBlock> {
        val contentBlocks = if (blocks.isEmpty() && !text.isNullOrBlank()) {
            listOf(buildTextSection(text!!))
        } else {
            blocks.toList()
        }

        return contentBlocks + buildFooterContext()
    }

    /**
     * text를 Section block으로 변환
     */
    private fun buildTextSection(text: String): LayoutBlock {
        return SectionBlock.builder()
            .text(
                MarkdownTextObject.builder()
                    .text(text)
                    .build()
            )
            .build()
    }

    /**
     * appName, activeProfile, traceId를 포함한 footer context block 생성
     *
     * 형식: _{appName}:{activeProfile} | x-b3-traceid: {traceId}_
     */
    private fun buildFooterContext(): LayoutBlock {
        val traceId = MDC.get(TRACE_ID_KEY)
            ?: "N/A"

        return ContextBlock.builder()
            .elements(
                listOf(
                    MarkdownTextObject.builder()
                        .text("_${appName}:${activeProfile} | x-b3-traceid: $traceId _")
                        .build()
                )
            )
            .build()
    }

    companion object {
        private const val TRACE_ID_KEY = "traceId"

        /**
         * Spring application name (SlackClientConfig에서 초기화)
         */
        var appName: String = "unknown"
            internal set

        /**
         * Spring active profile (SlackClientConfig에서 초기화)
         */
        var activeProfile: String = "unknown"
            internal set
    }
}

/**
 * Slack 메시지 DSL 진입점
 *
 * @param block 메시지 빌더 DSL
 * @return 빌드된 SlackMessage
 */
fun slackMessage(block: SlackMessageBuilder.() -> Unit): SlackMessage {
    return SlackMessageBuilder().apply(block).build()
}
