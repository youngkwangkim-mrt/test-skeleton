package com.myrealtrip.commonweb.filters

import io.micrometer.tracing.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig(
    private val tracer: Tracer
) {

    @Bean
    fun contentCachingFilter(): ContentCachingFilter = ContentCachingFilter()

    @Bean
    fun appTraceFilter(): AppTraceFilter = AppTraceFilter(tracer)

}