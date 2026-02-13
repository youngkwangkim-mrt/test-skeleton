package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.composition.TextObject
import com.slack.api.model.block.element.BlockElement
import com.slack.api.model.block.element.ButtonElement
import com.slack.api.model.block.element.ImageElement

/**
 * Section 블록 빌더
 *
 * Section은 텍스트와 선택적 액세서리(버튼, 이미지 등)를 포함합니다.
 */
@SlackDsl
class SectionBlockBuilder {

    private var textObject: TextObject? = null
    private var fields: MutableList<TextObject> = mutableListOf()
    private var accessoryElement: BlockElement? = null

    /**
     * Plain text 설정
     */
    fun text(text: String) {
        textObject = PlainTextObject.builder()
            .text(text)
            .emoji(true)
            .build()
    }

    /**
     * Markdown 텍스트 설정
     */
    fun markdown(text: String) {
        textObject = MarkdownTextObject.builder()
            .text(text)
            .build()
    }

    /**
     * 2열 필드 추가 (최대 10개)
     */
    fun fields(vararg texts: String) {
        texts.forEach { text ->
            fields.add(
                MarkdownTextObject.builder()
                    .text(text)
                    .build()
            )
        }
    }

    /**
     * 액세서리 설정 (버튼, 이미지 등)
     */
    fun accessory(block: AccessoryBuilder.() -> Unit) {
        accessoryElement = AccessoryBuilder().apply(block).build()
    }

    internal fun build(): SectionBlock {
        return SectionBlock.builder()
            .apply {
                textObject?.let { text(it) }
                if (fields.isNotEmpty()) fields(fields)
                accessoryElement?.let { accessory(it) }
            }
            .build()
    }
}

/**
 * 액세서리 빌더 (버튼, 이미지)
 */
@SlackDsl
class AccessoryBuilder {

    private var element: BlockElement? = null

    /**
     * 버튼 액세서리
     */
    fun button(text: String, block: ButtonBuilder.() -> Unit = {}) {
        element = ButtonBuilder(text).apply(block).build()
    }

    /**
     * 이미지 액세서리
     */
    fun image(url: String, altText: String) {
        element = ImageElement.builder()
            .imageUrl(url)
            .altText(altText)
            .build()
    }

    internal fun build(): BlockElement? = element
}

/**
 * 버튼 빌더
 */
@SlackDsl
class ButtonBuilder(private val text: String) {

    private var actionId: String? = null
    private var value: String? = null
    private var url: String? = null
    private var style: ButtonStyle = ButtonStyle.DEFAULT

    fun actionId(id: String) {
        this.actionId = id
    }

    fun value(value: String) {
        this.value = value
    }

    fun url(url: String) {
        this.url = url
    }

    fun style(style: ButtonStyle) {
        this.style = style
    }

    internal fun build(): ButtonElement {
        return ButtonElement.builder()
            .text(
                PlainTextObject.builder()
                    .text(text)
                    .emoji(true)
                    .build()
            )
            .apply {
                actionId(actionId ?: "button_${text.hashCode()}")
                this@ButtonBuilder.value?.let { value(it) }
                this@ButtonBuilder.url?.let { url(it) }
                this@ButtonBuilder.style.value?.let { style(it) }
            }
            .build()
    }
}
