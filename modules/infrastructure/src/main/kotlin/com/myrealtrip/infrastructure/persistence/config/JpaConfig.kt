package com.myrealtrip.infrastructure.persistence.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional

/**
 * JPA Auditing 설정
 */
@Configuration
@EnableJpaAuditing
class JpaConfig(
    @param:Value("\${spring.application.name:system}")
    private val name: String = "",
) {

    @Bean
    @ConditionalOnMissingBean
    fun auditorProvider(): AuditorAware<String> =
        AuditorAware { Optional.of(name) }
}