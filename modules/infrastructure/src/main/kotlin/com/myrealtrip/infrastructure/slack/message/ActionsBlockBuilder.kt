package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.block.ActionsBlock
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.BlockElement
import com.slack.api.model.block.element.ButtonElement

/**
 * Actions 블록 빌더
 *
 * Actions 블록은 버튼, 셀렉트 메뉴 등 인터랙티브 요소를 포함합니다.
 */
@SlackDsl
class ActionsBlockBuilder {

    private val elements: MutableList<BlockElement> = mutableListOf()

    /**
     * 버튼 추가
     *
     * @param text 버튼 텍스트
     * @param style 버튼 스타일 (DEFAULT, PRIMARY, DANGER)
     * @param block 버튼 설정 (actionId, value, url)
     */
    fun button(
        text: String,
        style: ButtonStyle = ButtonStyle.DEFAULT,
        block: ActionButtonBuilder.() -> Unit = {},
    ) {
        elements.add(
            ActionButtonBuilder(text, style).apply(block).build()
        )
    }

    internal fun build(): ActionsBlock {
        return ActionsBlock.builder()
            .elements(elements)
            .build()
    }
}

/**
 * Actions 블록용 버튼 빌더
 */
@SlackDsl
class ActionButtonBuilder(
    private val text: String,
    private val style: ButtonStyle,
) {

    private var actionId: String? = null
    private var value: String? = null
    private var url: String? = null

    fun actionId(id: String) {
        this.actionId = id
    }

    fun value(value: String) {
        this.value = value
    }

    fun url(url: String) {
        this.url = url
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
                actionId(actionId ?: "action_${text.hashCode()}")
                this@ActionButtonBuilder.value?.let { value(it) }
                this@ActionButtonBuilder.url?.let { url(it) }
                this@ActionButtonBuilder.style.value?.let { style(it) }
            }
            .build()
    }
}
