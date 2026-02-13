package com.myrealtrip.infrastructure.slack

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/** Slack 클라이언트 설정. `slack.enabled=true`(기본값)일 때만 Bean 생성. */
@Configuration
@EnableConfigurationProperties(SlackProperties::class)
@ConditionalOnProperty(name = ["slack.enabled"], havingValue = "true", matchIfMissing = true)
class SlackClientConfig {

    @Bean
    fun slack(): Slack {
        logger.info { "Initializing Slack client" }
        return Slack.getInstance()
    }

    @Bean
    fun slackMethodsClient(slack: Slack, properties: SlackProperties): MethodsClient {
        logger.info { "Creating Slack MethodsClient (defaultChannel=${properties.defaultChannel ?: "none"})" }
        return slack.methods(properties.botToken)
    }
}
