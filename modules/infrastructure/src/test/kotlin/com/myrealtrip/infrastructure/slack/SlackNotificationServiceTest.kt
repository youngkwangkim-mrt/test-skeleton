package com.myrealtrip.infrastructure.slack

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.myrealtrip.infrastructure.slack.message.SlackColor
import com.myrealtrip.infrastructure.slack.message.SlackMessage
import com.myrealtrip.infrastructure.slack.message.slackMessage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC

class SlackNotificationServiceTest {

    private lateinit var slackClient: SlackClient
    private lateinit var service: SlackNotificationService

    @BeforeEach
    fun setUp(): Unit {
        slackClient = mock()
    }

    @Nested
    inner class NotifyTests {

        @Test
        fun `should send message to specified channel`(): Unit {
            // given
            val properties = createProperties(defaultChannel = null)
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Test message") }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when
            service.notify("#alerts", message)

            // then
            val channelCaptor = argumentCaptor<String>()
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(channelCaptor.capture(), messageCaptor.capture())

            assertThat(channelCaptor.firstValue).isEqualTo("#alerts")
            assertThat(messageCaptor.firstValue.text).isEqualTo("Test message")
        }

        @Test
        fun `should use default channel when channel not specified`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#default-channel")
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Test message") }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#default-channel"))

            // when
            service.notify(message = message)

            // then
            val channelCaptor = argumentCaptor<String>()
            verify(slackClient).send(channelCaptor.capture(), any())
            assertThat(channelCaptor.firstValue).isEqualTo("#default-channel")
        }

        @Test
        fun `should use message channel when parameter channel is null`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#default-channel")
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage {
                channel("#message-channel")
                text("Test message")
            }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#message-channel"))

            // when
            service.notify(message = message)

            // then
            val channelCaptor = argumentCaptor<String>()
            verify(slackClient).send(channelCaptor.capture(), any())
            assertThat(channelCaptor.firstValue).isEqualTo("#message-channel")
        }

        @Test
        fun `should throw exception when no channel available`(): Unit {
            // given
            val properties = createProperties(defaultChannel = null)
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Test message") }

            // when & then
            assertThatThrownBy { service.notify(message = message) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Channel must be specified")
        }

        @Test
        fun `should return DISABLED response when slack is disabled`(): Unit {
            // given
            val properties = createProperties(enabled = false)
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Test message") }

            // when
            val response = service.notify("#alerts", message)

            // then
            assertThat(response).isEqualTo(SlackResponse.DISABLED)
            verify(slackClient, never()).send(any(), any())
        }
    }

    @Nested
    inner class NotifySuccessTests {

        @Test
        fun `should send success message with green color bar`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when
            service.notifySuccess(title = "Deploy Complete", message = "v1.2.3 deployed")

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.text).isEqualTo("Deploy Complete")
            assertThat(sentMessage.attachments).hasSize(1)
            assertThat(sentMessage.attachments[0].color).isEqualTo(SlackColor.SUCCESS.value)
        }
    }

    @Nested
    inner class NotifyErrorTests {

        @Test
        fun `should send error message with red color bar and stack trace`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#errors")
            service = SlackNotificationService(slackClient, properties)
            val error = RuntimeException("Something went wrong")
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#errors"))

            // when
            service.notifyError(title = "Error Occurred", error = error)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.text).isEqualTo("Error Occurred")
            assertThat(sentMessage.attachments).hasSize(1)
            assertThat(sentMessage.attachments[0].color).isEqualTo(SlackColor.DANGER.value)
        }
    }

    @Nested
    inner class NotifyWarningTests {

        @Test
        fun `should send warning message with yellow color bar`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when
            service.notifyWarning(title = "Disk Usage Warning", message = "85% used")

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.text).isEqualTo("Disk Usage Warning")
            assertThat(sentMessage.attachments).hasSize(1)
            assertThat(sentMessage.attachments[0].color).isEqualTo(SlackColor.WARNING.value)
        }
    }

    @Nested
    inner class NotifyInfoTests {

        @Test
        fun `should send info message with blue color bar`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#info")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#info"))

            // when
            service.notifyInfo(title = "System Update", message = "Scheduled maintenance")

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.text).isEqualTo("System Update")
            assertThat(sentMessage.attachments).hasSize(1)
            assertThat(sentMessage.attachments[0].color).isEqualTo(SlackColor.INFO.value)
        }
    }

    @Nested
    inner class ReplyTests {

        @Test
        fun `should send reply with thread timestamp`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#general")
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Reply message") }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#channel"))

            // when
            service.reply("#channel", "1234567890.123456", message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())
            assertThat(messageCaptor.firstValue.threadTs).isEqualTo("1234567890.123456")
        }

        @Test
        fun `should return DISABLED response when slack is disabled`(): Unit {
            // given
            val properties = createProperties(enabled = false)
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Reply message") }

            // when
            val response = service.reply("#channel", "1234567890.123456", message)

            // then
            assertThat(response).isEqualTo(SlackResponse.DISABLED)
            verify(slackClient, never()).send(any(), any())
        }
    }

    @Nested
    inner class FooterTests {

        @AfterEach
        fun tearDown(): Unit {
            MDC.clear()
        }

        @Test
        fun `should append footer context block to message`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Test message") }
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.blocks).isNotEmpty
            assertThat(sentMessage.blocks.last()).isInstanceOf(ContextBlock::class.java)
        }

        @Test
        fun `should include traceId from MDC in footer`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // MDC는 slackMessage 생성 전에 설정해야 함
            MDC.put("traceId", "test-trace-id-12345")

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Test message") }
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            val footerBlock = sentMessage.blocks.last() as ContextBlock
            val footerText = (footerBlock.elements[0] as MarkdownTextObject).text

            assertThat(footerText).contains("test-trace-id-12345")
        }

        @Test
        fun `should use traceId key from MDC`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // MDC는 slackMessage 생성 전에 설정해야 함
            MDC.put("traceId", "another-trace-id")

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Test message") }
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            val footerBlock = sentMessage.blocks.last() as ContextBlock
            val footerText = (footerBlock.elements[0] as MarkdownTextObject).text

            assertThat(footerText).contains("another-trace-id")
        }

        @Test
        fun `should show NA when no traceId in MDC`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // MDC가 비어있는 상태에서 메시지 생성
            MDC.clear()

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Test message") }
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            val footerBlock = sentMessage.blocks.last() as ContextBlock
            val footerText = (footerBlock.elements[0] as MarkdownTextObject).text

            assertThat(footerText).contains("N/A")
        }

        @Test
        fun `should include appName and activeProfile in footer`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Test message") }
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            val footerBlock = sentMessage.blocks.last() as ContextBlock
            val footerText = (footerBlock.elements[0] as MarkdownTextObject).text

            // format: _{appName}:{activeProfile} | x-b3-traceid: {traceId}_
            assertThat(footerText).contains(":")
            assertThat(footerText).contains("x-b3-traceid:")
        }

        @Test
        fun `should append footer to reply messages`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#general")
            service = SlackNotificationService(slackClient, properties)
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#channel"))

            // MDC는 slackMessage 생성 전에 설정해야 함
            MDC.put("traceId", "reply-trace-id")

            // when - footer는 slackMessage 생성 시점에 추가됨
            val message = slackMessage { text("Reply message") }
            service.reply("#channel", "1234567890.123456", message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            assertThat(sentMessage.blocks).isNotEmpty
            assertThat(sentMessage.blocks.last()).isInstanceOf(ContextBlock::class.java)

            val footerBlock = sentMessage.blocks.last() as ContextBlock
            val footerText = (footerBlock.elements[0] as MarkdownTextObject).text
            assertThat(footerText).contains("reply-trace-id")
        }

        @Test
        fun `should preserve existing blocks when appending footer`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage {
                header("Test Header")
                section { markdown("Test content") }
                divider()
            }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            // 3 original blocks + 1 footer block
            assertThat(sentMessage.blocks).hasSize(4)
            assertThat(sentMessage.blocks.last()).isInstanceOf(ContextBlock::class.java)
        }

        @Test
        fun `should convert text to section block when blocks are empty`(): Unit {
            // given
            val properties = createProperties(defaultChannel = "#alerts")
            service = SlackNotificationService(slackClient, properties)
            val message = slackMessage { text("Simple text message") }
            whenever(slackClient.send(any(), any())).thenReturn(SlackResponse("ts", "#alerts"))

            // when
            service.notify(message = message)

            // then
            val messageCaptor = argumentCaptor<SlackMessage>()
            verify(slackClient).send(any(), messageCaptor.capture())

            val sentMessage = messageCaptor.firstValue
            // 1 text section block + 1 footer block
            assertThat(sentMessage.blocks).hasSize(2)
            assertThat(sentMessage.blocks[0]).isInstanceOf(SectionBlock::class.java)
            assertThat(sentMessage.blocks[1]).isInstanceOf(ContextBlock::class.java)

            val textBlock = sentMessage.blocks[0] as SectionBlock
            val textContent = (textBlock.text as MarkdownTextObject).text
            assertThat(textContent).isEqualTo("Simple text message")
        }
    }

    private fun createProperties(
        enabled: Boolean = true,
        defaultChannel: String? = null,
    ): SlackProperties {
        return SlackProperties(
            botToken = "xoxb-test-token",
            defaultChannel = defaultChannel,
            enabled = enabled,
        )
    }
}
