package com.myrealtrip.infrastructure

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Clock

@Configuration
@EntityScan(basePackages = ["com.myrealtrip.infrastructure.persistence", "com.myrealtrip.domain"])
@EnableJpaRepositories(basePackages = ["com.myrealtrip.infrastructure.persistence", "com.myrealtrip.domain"])
class InfrastructureConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
