package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.Attachment
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.HeaderBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.PlainTextObject

/**
 * Attachment 빌더 (색상 바 지원)
 *
 * Block Kit 자체는 색상 바를 지원하지 않으므로,
 * Attachment 내에 blocks를 넣어서 색상 바와 Block Kit을 함께 사용합니다.
 */
@SlackDsl
class AttachmentBuilder(private val color: String) {

    private val blocks: MutableList<LayoutBlock> = mutableListOf()

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
     * Context 블록 추가 (Footer 역할로 사용 가능)
     */
    fun context(block: ContextBlockBuilder.() -> Unit) {
        blocks.add(ContextBlockBuilder().apply(block).build())
    }

    internal fun build(): Attachment {
        return Attachment.builder()
            .color(color)
            .blocks(blocks)
            .build()
    }
}
