package com.myrealtrip.common.notification.message

/**
 * Slack SDK에 의존하지 않는 순수 Kotlin 메시지 모델.
 *
 * 인프라 모듈의 `SlackMessageConverter`가 이 모델을 Slack SDK 객체로 변환합니다.
 *
 * @see com.myrealtrip.common.notification.message.slackMessage DSL 진입점
 */
data class SlackMessage(
    val channel: String? = null,
    val text: String? = null,
    val blocks: List<Block> = emptyList(),
    val attachments: List<MessageAttachment> = emptyList(),
    val threadTs: String? = null,
) {
    fun isEmpty(): Boolean = text.isNullOrBlank() && blocks.isEmpty() && attachments.isEmpty()
    fun isValid(): Boolean = !isEmpty()
}

// -- Block --

sealed interface Block {

    data class Header(val text: String) : Block

    data class Section(
        val text: TextContent? = null,
        val fields: List<String> = emptyList(),
        val accessory: Accessory? = null,
    ) : Block

    data object Divider : Block

    data class Actions(val elements: List<ButtonElement>) : Block

    data class Context(val elements: List<ContextElement>) : Block
}

// -- Text --

sealed interface TextContent {
    val text: String

    data class Plain(override val text: String) : TextContent
    data class Markdown(override val text: String) : TextContent
}

// -- Context element --

sealed interface ContextElement {
    data class Text(val text: String) : ContextElement
    data class Markdown(val text: String) : ContextElement
    data class Image(val url: String, val altText: String) : ContextElement
}

// -- Accessory --

sealed interface Accessory {

    data class Button(
        val text: String,
        val style: ButtonStyle = ButtonStyle.DEFAULT,
        val actionId: String? = null,
        val value: String? = null,
        val url: String? = null,
    ) : Accessory

    data class Image(val url: String, val altText: String) : Accessory
}

// -- Button --

enum class ButtonStyle(val value: String?) {
    DEFAULT(null),
    PRIMARY("primary"),
    DANGER("danger"),
}

data class ButtonElement(
    val text: String,
    val style: ButtonStyle = ButtonStyle.DEFAULT,
    val actionId: String? = null,
    val value: String? = null,
    val url: String? = null,
)

// -- Attachment --

data class MessageAttachment(
    val color: MessageColor = MessageColor.DEFAULT,
    val fallback: String? = null,
    val blocks: List<Block> = emptyList(),
)
