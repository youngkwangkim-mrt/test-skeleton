package com.myrealtrip.skeletonapiapp._test

import com.myrealtrip.common.notification.NotificationEvent
import com.myrealtrip.common.notification.message.ButtonStyle
import com.myrealtrip.common.notification.message.MessageColor
import com.myrealtrip.common.notification.message.slackMessage

import com.myrealtrip.commonweb.response.resource.ApiResource
import com.myrealtrip.commonweb.utils.EnvironmentUtil
import com.myrealtrip.infrastructure.notification.NotificationEventPublisher
import com.myrealtrip.infrastructure.slack.SlackNotificationService
import com.myrealtrip.infrastructure.slack.SlackResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@Profile("local")
@RestController
@RequestMapping("/_test/slack")
class TestSlackController(
    private val notificationEventPublisher: NotificationEventPublisher,
    private val slackNotificationService: SlackNotificationService,
    private val environmentUtil: EnvironmentUtil,
) {

    /**
     * 간단한 텍스트 메시지 전송
     * GET /_test/slack/text?channel=#general&message=Hello
     */
    @GetMapping("/text")
    fun sendText(
        @RequestParam channel: String,
        @RequestParam title: String,
        @RequestParam message: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending text message to $channel: $title - $message" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                channel = channel,
                title = title,
                message = message,
            ),
        )

        return ApiResource.success()
    }

    /**
     * Block Kit 메시지 전송
     * GET /_test/slack/blocks?channel=#general
     */
    @GetMapping("/blocks")
    fun sendBlocks(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending block message to $channel" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                message = slackMessage {
                    channel(channel)
                    header("Test Block Kit Message")

                    section {
                        markdown("*애플리케이션:* `skeleton-api-app`\n*환경:* Local")
                    }

                    divider()

                    section {
                        fields(
                            "*Status*", "Running",
                            "*Version*", environmentUtil.version(),
                            "*Profile*", environmentUtil.activeProfile(),
                        )
                    }

                    context {
                        markdown("Sent from TestSlackController")
                    }
                },
            ),
        )

        return ApiResource.success()
    }

    /**
     * 색상 바가 있는 메시지 전송
     * GET /_test/slack/attachment?channel=#general&color=SUCCESS
     */
    @GetMapping("/attachment")
    fun sendAttachment(
        @RequestParam channel: String,
        @RequestParam(defaultValue = "SUCCESS") color: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending attachment message to $channel with color: $color" }

        val messageColor = when (color.uppercase()) {
            "SUCCESS" -> MessageColor.SUCCESS
            "WARNING" -> MessageColor.WARNING
            "DANGER" -> MessageColor.DANGER
            "INFO" -> MessageColor.INFO
            else -> MessageColor.DEFAULT
        }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                message = slackMessage {
                    channel(channel)
                    attachment(messageColor) {
                        section {
                            markdown("*Color Bar Test*\nThis message has a ${color.lowercase()} color bar.")
                        }
                        context {
                            markdown("Color: `${messageColor.value}`")
                        }
                    }
                },
            ),
        )

        return ApiResource.success()
    }

    /**
     * 편의 메서드 테스트 - Success
     * GET /_test/slack/success?channel=#general
     */
    @GetMapping("/success")
    fun sendSuccess(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<SlackResponse>> {
        logger.info { "Sending success message to $channel" }

        val response = slackNotificationService.notifySuccess(
            channel = channel,
            title = "Test Success",
            message = "This is a success notification from TestSlackController",
        )

        return ApiResource.success(response)
    }

    /**
     * 편의 메서드 테스트 - Warning
     * GET /_test/slack/warning?channel=#general
     */
    @GetMapping("/warning")
    fun sendWarning(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<SlackResponse>> {
        logger.info { "Sending warning message to $channel" }

        val response = slackNotificationService.notifyWarning(
            channel = channel,
            title = "Test Warning",
            message = "This is a warning notification from TestSlackController",
        )

        return ApiResource.success(response)
    }

    /**
     * 편의 메서드 테스트 - Error
     * GET /_test/slack/error?channel=#general
     */
    @GetMapping("/error")
    fun sendError(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<SlackResponse>> {
        logger.info { "Sending error message to $channel" }

        val error = RuntimeException("Test exception from TestSlackController")

        val response = slackNotificationService.notifyError(
            channel = channel,
            title = "Test Error",
            error = error,
        )

        return ApiResource.success(response)
    }

    /**
     * 편의 메서드 테스트 - Info
     * GET /_test/slack/info?channel=#general
     */
    @GetMapping("/info")
    fun sendInfo(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending info message to $channel" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                channel = channel,
                title = "Test Info",
                message = "This is an info notification from TestSlackController",
            ),
        )

        return ApiResource.success()
    }

    /**
     * 버튼이 있는 메시지 전송
     * GET /_test/slack/actions?channel=#general
     */
    @GetMapping("/actions")
    fun sendActions(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending actions message to $channel" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                message = slackMessage {
                    channel(channel)
                    header("Action Buttons Test")

                    section {
                        markdown("Click the buttons below to test interactions.")
                    }

                    actions {
                        button("Primary Button", ButtonStyle.PRIMARY) {
                            actionId("test_primary")
                            value("primary_clicked")
                        }
                        button("Danger Button", ButtonStyle.DANGER) {
                            actionId("test_danger")
                            value("danger_clicked")
                        }
                        button("Link Button") {
                            url("https://github.com")
                        }
                    }
                },
            ),
        )

        return ApiResource.success()
    }

    /**
     * 스레드 답장 테스트
     * POST /_test/slack/thread?channel=#general&parentTs=1234567890.123456&message=Reply
     */
    @PostMapping("/thread")
    fun sendThreadReply(
        @RequestParam channel: String,
        @RequestParam parentTs: String,
        @RequestParam message: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending thread reply to $channel, parentTs: $parentTs" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                message = slackMessage {
                    channel(channel)
                    threadTs(parentTs)
                    text(message)
                },
            ),
        )

        return ApiResource.success()
    }

    /**
     * 배포 알림 시뮬레이션
     * GET /_test/slack/deploy?channel=#general
     */
    @GetMapping("/deploy")
    fun sendDeployNotification(
        @RequestParam channel: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Sending deploy notification to $channel" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                message = slackMessage {
                    channel(channel)
                    text("Deployment notification")

                    header("Deployment Complete")

                    section {
                        markdown("*Application:* `skeleton-api-app`\n*Version:* `v1.0.0`\n*Environment:* Local")
                    }

                    divider()

                    attachment(MessageColor.SUCCESS) {
                        section {
                            fields(
                                "*Status*", "Success",
                                "*Duration*", "2m 30s",
                            )
                        }
                        context {
                            markdown("Deployed by *TestSlackController* at `${java.time.LocalDateTime.now()}`")
                        }
                    }

                    actions {
                        button("View Logs", ButtonStyle.PRIMARY) {
                            url("https://logs.example.com")
                        }
                        button("Rollback", ButtonStyle.DANGER) {
                            actionId("rollback")
                            value("v1.0.0")
                        }
                    }
                },
            ),
        )

        return ApiResource.success()
    }

    /**
     * NotificationEventPublisher를 통한 이벤트 기반 알림 전송
     * GET /_test/slack/event?channel=#general&title=Test&message=Hello
     */
    @GetMapping("/event")
    fun sendViaEvent(
        @RequestParam channel: String,
        @RequestParam title: String,
        @RequestParam message: String,
    ): ResponseEntity<ApiResource<String>> {
        logger.info { "Publishing notification event: $title" }

        notificationEventPublisher.publish(
            NotificationEvent.Slack(
                channel = channel,
                title = title,
                message = message,
            ),
        )

        return ApiResource.success()
    }
}
