package com.myrealtrip.common.notification.message

/**
 * [SlackMessage] DSL 진입점.
 *
 * ```kotlin
 * val message = slackMessage {
 *     channel("#general")
 *     text("Hello, World!")
 *     header("알림")
 *     section { markdown("*중요한 내용*") }
 *     divider()
 *     attachment(MessageColor.SUCCESS) {
 *         section { fields("*상태*", "성공", "*시간*", "2024-01-01") }
 *     }
 *     context { markdown("_작성자: Bot_") }
 * }
 * ```
 */
fun slackMessage(block: SlackMessageBuilder.() -> Unit): SlackMessage =
    SlackMessageBuilder().apply(block).build()

// -- Top-level builder --

@SlackDsl
class SlackMessageBuilder {

    private var channel: String? = null
    private var text: String? = null
    private var threadTs: String? = null
    private val blocks = mutableListOf<Block>()
    private val attachments = mutableListOf<MessageAttachment>()

    fun channel(channel: String) {
        this.channel = channel
    }

    fun text(text: String) {
        this.text = text
    }

    fun threadTs(ts: String) {
        this.threadTs = ts
    }

    fun header(text: String) {
        blocks.add(Block.Header(text))
    }

    fun divider() {
        blocks.add(Block.Divider)
    }

    fun section(block: SectionBuilder.() -> Unit) {
        blocks.add(SectionBuilder().apply(block).build())
    }

    fun actions(block: ActionsBuilder.() -> Unit) {
        blocks.add(ActionsBuilder().apply(block).build())
    }

    fun context(block: ContextBuilder.() -> Unit) {
        blocks.add(ContextBuilder().apply(block).build())
    }

    fun attachment(color: MessageColor = MessageColor.DEFAULT, block: MessageAttachmentBuilder.() -> Unit) {
        attachments.add(MessageAttachmentBuilder(color).apply(block).build())
    }

    internal fun build(): SlackMessage {
        val contentBlocks = if (blocks.isEmpty() && !text.isNullOrBlank()) {
            listOf(Block.Section(text = TextContent.Markdown(text!!)))
        } else {
            blocks.toList()
        }

        return SlackMessage(
            channel = channel,
            text = text,
            blocks = contentBlocks,
            attachments = attachments.toList(),
            threadTs = threadTs,
        )
    }
}

// -- Section builder --

@SlackDsl
class SectionBuilder {

    private var textContent: TextContent? = null
    private val fieldsList = mutableListOf<String>()
    private var accessoryElement: Accessory? = null

    fun text(text: String) {
        textContent = when (val current = textContent) {
            is TextContent.Plain -> TextContent.Plain(current.text + text)
            else -> TextContent.Plain(text)
        }
    }

    fun markdown(text: String) {
        textContent = when (val current = textContent) {
            is TextContent.Markdown -> TextContent.Markdown(current.text + text)
            else -> TextContent.Markdown(text)
        }
    }

    fun fields(vararg texts: String) {
        fieldsList.addAll(texts)
    }

    fun accessory(block: AccessoryBuilder.() -> Unit) {
        accessoryElement = AccessoryBuilder().apply(block).build()
    }

    internal fun build(): Block.Section = Block.Section(
        text = textContent,
        fields = fieldsList.toList(),
        accessory = accessoryElement,
    )
}

// -- Accessory builder --

@SlackDsl
class AccessoryBuilder {

    private var element: Accessory? = null

    fun button(text: String, style: ButtonStyle = ButtonStyle.DEFAULT, block: ButtonBuilder.() -> Unit = {}) {
        element = ButtonBuilder(text, style).apply(block).buildAccessory()
    }

    fun image(url: String, altText: String) {
        element = Accessory.Image(url, altText)
    }

    internal fun build(): Accessory? = element
}

// -- Actions builder --

@SlackDsl
class ActionsBuilder {

    private val elements = mutableListOf<ButtonElement>()

    fun button(text: String, style: ButtonStyle = ButtonStyle.DEFAULT, block: ButtonBuilder.() -> Unit = {}) {
        elements.add(ButtonBuilder(text, style).apply(block).buildElement())
    }

    internal fun build(): Block.Actions = Block.Actions(elements.toList())
}

// -- Context builder --

@SlackDsl
class ContextBuilder {

    private val elements = mutableListOf<ContextElement>()

    fun text(text: String) {
        elements.add(ContextElement.Text(text))
    }

    fun markdown(text: String) {
        elements.add(ContextElement.Markdown(text))
    }

    fun image(url: String, altText: String) {
        elements.add(ContextElement.Image(url, altText))
    }

    internal fun build(): Block.Context = Block.Context(elements.toList())
}

// -- Attachment builder --

@SlackDsl
class MessageAttachmentBuilder(private val color: MessageColor) {

    private var fallback: String? = null
    private val blocks = mutableListOf<Block>()

    fun fallback(text: String) {
        this.fallback = text
    }

    fun header(text: String) {
        blocks.add(Block.Header(text))
    }

    fun divider() {
        blocks.add(Block.Divider)
    }

    fun section(block: SectionBuilder.() -> Unit) {
        blocks.add(SectionBuilder().apply(block).build())
    }

    fun actions(block: ActionsBuilder.() -> Unit) {
        blocks.add(ActionsBuilder().apply(block).build())
    }

    fun context(block: ContextBuilder.() -> Unit) {
        blocks.add(ContextBuilder().apply(block).build())
    }

    internal fun build(): MessageAttachment = MessageAttachment(
        color = color,
        fallback = fallback,
        blocks = blocks.toList(),
    )
}

// -- Button builder --

@SlackDsl
class ButtonBuilder(
    private val text: String,
    private val style: ButtonStyle = ButtonStyle.DEFAULT,
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

    internal fun buildElement(): ButtonElement = ButtonElement(
        text = text,
        style = style,
        actionId = actionId,
        value = value,
        url = url,
    )

    internal fun buildAccessory(): Accessory.Button = Accessory.Button(
        text = text,
        style = style,
        actionId = actionId,
        value = value,
        url = url,
    )
}
