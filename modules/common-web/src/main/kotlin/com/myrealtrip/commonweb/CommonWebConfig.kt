package com.myrealtrip.commonweb

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.support.ContextPropagatingTaskDecorator


@Configuration
@ComponentScan("com.myrealtrip.commonweb")
@EnableConfigurationProperties
class CommonWebConfig {

    @Bean
    fun contextPropagatingTaskDecorator(): ContextPropagatingTaskDecorator {
        return ContextPropagatingTaskDecorator()
    }

} 