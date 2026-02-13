package com.myrealtrip.skeletonapiapp.config

import com.myrealtrip.commonweb.openapi3.SwaggerCommonConfig
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class SwaggerConfig : SwaggerCommonConfig() {

    @Bean
    fun flightsInternationalGroupedOpenApiConfig(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("Users")
            .displayName("01. Users")
            .pathsToMatch("/api/v1/users/**")
            .build()
    }

    @Profile("local")
    @Bean
    fun globalGroupedOpenApiConfig(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("99. Global")
            .pathsToMatch("/_global/**")
            .build()
    }

    @Profile("local")
    @Bean
    fun testGroupedOpenApiConfig(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("99. Test")
            .pathsToMatch("/_test/**")
            .build()
    }

}