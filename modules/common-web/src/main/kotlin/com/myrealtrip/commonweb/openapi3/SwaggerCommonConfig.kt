package com.myrealtrip.commonweb.openapi3

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
abstract class SwaggerCommonConfig {

    @Value("\${spring.application.version:}")
    protected lateinit var version: String

    @Value("\${spring.application.name:}")
    protected lateinit var appName: String

    @ConditionalOnMissingBean
    @Bean
    fun openApiConfig(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$appName API")
                    .description("$appName API")
                    .version(version)
            )
    }

}