package com.myrealtrip.infrastructure.slack.message

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.HeaderBlock
import com.slack.api.model.block.SectionBlock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * SlackMessageBuilder 테스트
 *
 * 모든 메시지에는 자동으로 footer (traceId + timestamp) context block이 추가됩니다.
 */
class SlackMessageBuilderTest {

    @Test
    fun `should build simple text message`(): Unit {
        // given & when
        val message = slackMessage {
            channel("#general")
            text("Hello, World!")
        }

        // then
        assertThat(message.channel).isEqualTo("#general")
        assertThat(message.text).isEqualTo("Hello, World!")
        // text만 있으면 Section block으로 변환 + footer block 추가
        assertThat(message.blocks).hasSize(2)
        assertThat(message.blocks[0]).isInstanceOf(SectionBlock::class.java)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)
        assertThat(message.attachments).isEmpty()
    }

    @Test
    fun `should build message with header block`(): Unit {
        // given & when
        val message = slackMessage {
            header("Test Header")
        }

        // then - header + footer
        assertThat(message.blocks).hasSize(2)
        assertThat(message.blocks[0]).isInstanceOf(HeaderBlock::class.java)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)

        val headerBlock = message.blocks[0] as HeaderBlock
        assertThat(headerBlock.text.text).isEqualTo("Test Header")
    }

    @Test
    fun `should build message with section block`(): Unit {
        // given & when
        val message = slackMessage {
            section {
                markdown("*Bold* text")
            }
        }

        // then - section + footer
        assertThat(message.blocks).hasSize(2)
        assertThat(message.blocks[0]).isInstanceOf(SectionBlock::class.java)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)
    }

    @Test
    fun `should build message with divider block`(): Unit {
        // given & when
        val message = slackMessage {
            divider()
        }

        // then - divider + footer
        assertThat(message.blocks).hasSize(2)
        assertThat(message.blocks[0]).isInstanceOf(DividerBlock::class.java)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)
    }

    @Test
    fun `should build message with multiple blocks`(): Unit {
        // given & when
        val message = slackMessage {
            header("Title")
            section {
                markdown("Content")
            }
            divider()
            context {
                markdown("Footer")
            }
        }

        // then - 4 content blocks + 1 footer block
        assertThat(message.blocks).hasSize(5)
        assertThat(message.blocks[0]).isInstanceOf(HeaderBlock::class.java)
        assertThat(message.blocks[1]).isInstanceOf(SectionBlock::class.java)
        assertThat(message.blocks[2]).isInstanceOf(DividerBlock::class.java)
        assertThat(message.blocks[3]).isInstanceOf(ContextBlock::class.java) // user's context
        assertThat(message.blocks[4]).isInstanceOf(ContextBlock::class.java) // auto footer
    }

    @Test
    fun `should build message with attachment and color bar`(): Unit {
        // given & when
        val message = slackMessage {
            attachment(SlackColor.SUCCESS) {
                section {
                    markdown("Success message")
                }
            }
        }

        // then - attachment만 있으면 footer만 blocks에 추가
        assertThat(message.blocks).hasSize(1)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)
        assertThat(message.attachments).hasSize(1)
        assertThat(message.attachments[0].color).isEqualTo("#36a64f")
        assertThat(message.attachments[0].blocks).hasSize(1)
    }

    @Test
    fun `should build message with multiple attachments`(): Unit {
        // given & when
        val message = slackMessage {
            attachment(SlackColor.SUCCESS) {
                section { markdown("Success") }
            }
            attachment(SlackColor.DANGER) {
                section { markdown("Error") }
            }
        }

        // then
        assertThat(message.attachments).hasSize(2)
        assertThat(message.attachments[0].color).isEqualTo(SlackColor.SUCCESS.value)
        assertThat(message.attachments[1].color).isEqualTo(SlackColor.DANGER.value)
    }

    @Test
    fun `should build message with custom hex color`(): Unit {
        // given & when
        val message = slackMessage {
            attachment("#7B68EE") {
                section { markdown("Custom color") }
            }
        }

        // then
        assertThat(message.attachments).hasSize(1)
        assertThat(message.attachments[0].color).isEqualTo("#7B68EE")
    }

    @Test
    fun `should build message with thread timestamp`(): Unit {
        // given & when
        val message = slackMessage {
            threadTs("1234567890.123456")
            text("Reply message")
        }

        // then
        assertThat(message.threadTs).isEqualTo("1234567890.123456")
    }

    @Test
    fun `should build complete deployment notification message`(): Unit {
        // given & when
        val message = slackMessage {
            channel("#deployments")
            text("Deployment notification")

            header("Deployment Complete")

            section {
                markdown("*Application:* `user-service`\n*Version:* `v2.3.1`")
            }

            divider()

            attachment(SlackColor.SUCCESS) {
                section {
                    fields(
                        "*Environment*", "Production",
                        "*Duration*", "3m 24s"
                    )
                }
                context {
                    markdown("Deployed by @john.doe")
                }
            }

            actions {
                button("View Logs", ButtonStyle.PRIMARY) {
                    url("https://logs.example.com")
                }
                button("Rollback", ButtonStyle.DANGER) {
                    actionId("rollback_deployment")
                }
            }
        }

        // then
        assertThat(message.channel).isEqualTo("#deployments")
        assertThat(message.text).isEqualTo("Deployment notification")
        // 4 content blocks + 1 footer block
        assertThat(message.blocks).hasSize(5)
        assertThat(message.blocks.last()).isInstanceOf(ContextBlock::class.java)
        assertThat(message.attachments).hasSize(1)
        assertThat(message.attachments[0].color).isEqualTo(SlackColor.SUCCESS.value)
    }
}
