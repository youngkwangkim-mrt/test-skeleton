package com.myrealtrip.infrastructure.slack

import com.myrealtrip.common.notification.message.*
import com.slack.api.model.Attachment
import com.slack.api.model.block.*
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.composition.TextObject
import com.slack.api.model.block.element.BlockElement
import com.slack.api.model.block.element.ButtonElement
import com.slack.api.model.block.element.ImageElement

/** [SlackMessage] → Slack SDK 타입 변환기. */
object SlackMessageConverter {

    fun toLayoutBlocks(blocks: List<Block>): List<LayoutBlock> = blocks.map(::toLayoutBlock)

    fun toAttachments(attachments: List<MessageAttachment>): List<Attachment> =
        attachments.map(::toAttachment)

    private fun toLayoutBlock(block: Block): LayoutBlock = when (block) {
        is Block.Header -> HeaderBlock.builder()
            .text(plainText(block.text))
            .build()

        is Block.Section -> SectionBlock.builder()
            .apply {
                block.text?.let { text(toTextObject(it)) }
                if (block.fields.isNotEmpty()) {
                    fields(block.fields.map { markdown(it) })
                }
                block.accessory?.let { accessory(toAccessoryElement(it)) }
            }
            .build()

        is Block.Divider -> DividerBlock()

        is Block.Actions -> ActionsBlock.builder()
            .elements(block.elements.map { buildButton(it.text, it.style, it.actionId, it.value, it.url) })
            .build()

        is Block.Context -> ContextBlock.builder()
            .elements(block.elements.map(::toContextElement))
            .build()
    }

    private fun toTextObject(content: TextContent): TextObject = when (content) {
        is TextContent.Plain -> plainText(content.text)
        is TextContent.Markdown -> markdown(content.text)
    }

    private fun toAccessoryElement(accessory: Accessory): BlockElement = when (accessory) {
        is Accessory.Button -> buildButton(accessory.text, accessory.style, accessory.actionId, accessory.value, accessory.url)
        is Accessory.Image -> ImageElement.builder().imageUrl(accessory.url).altText(accessory.altText).build()
    }

    private fun toContextElement(element: ContextElement): ContextBlockElement = when (element) {
        is ContextElement.Text -> plainText(element.text)
        is ContextElement.Markdown -> markdown(element.text)
        is ContextElement.Image -> ImageElement.builder().imageUrl(element.url).altText(element.altText).build()
    }

    private fun toAttachment(attachment: MessageAttachment): Attachment = Attachment.builder()
        .color(attachment.color.value)
        .apply { attachment.fallback?.let { fallback(it) } }
        .blocks(toLayoutBlocks(attachment.blocks))
        .build()

    // -- Shared builders --

    private fun buildButton(
        text: String,
        style: ButtonStyle,
        actionId: String?,
        value: String?,
        url: String?,
    ): ButtonElement = ButtonElement.builder()
        .text(plainText(text))
        .actionId(actionId ?: "button_${text.hashCode()}")
        .apply {
            value?.let { value(it) }
            url?.let { url(it) }
            style.value?.let { style(it) }
        }
        .build()

    private fun plainText(text: String): PlainTextObject =
        PlainTextObject.builder().text(text).emoji(true).build()

    private fun markdown(text: String): MarkdownTextObject =
        MarkdownTextObject.builder().text(text).build()
}