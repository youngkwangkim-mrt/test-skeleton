package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.ContextBlockElement
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.ImageElement

/**
 * Context 블록 빌더
 *
 * Context 블록은 부가 정보(작은 텍스트, 이미지)를 표시합니다.
 */
@SlackDsl
class ContextBlockBuilder {

    private val elements: MutableList<ContextBlockElement> = mutableListOf()

    /**
     * Plain text 추가
     */
    fun text(text: String) {
        elements.add(
            PlainTextObject.builder()
                .text(text)
                .emoji(true)
                .build()
        )
    }

    /**
     * Markdown 텍스트 추가
     */
    fun markdown(text: String) {
        elements.add(
            MarkdownTextObject.builder()
                .text(text)
                .build()
        )
    }

    /**
     * 이미지 추가
     */
    fun image(url: String, altText: String) {
        elements.add(
            ImageElement.builder()
                .imageUrl(url)
                .altText(altText)
                .build()
        )
    }

    internal fun build(): ContextBlock {
        return ContextBlock.builder()
            .elements(elements)
            .build()
    }
}
