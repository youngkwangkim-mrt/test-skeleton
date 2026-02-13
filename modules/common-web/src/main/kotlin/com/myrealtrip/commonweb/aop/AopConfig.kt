package com.myrealtrip.commonweb.aop

import com.myrealtrip.commonweb.aop.allowedip.CheckIpAspect
import com.myrealtrip.commonweb.aop.logtrace.LogTrace
import com.myrealtrip.commonweb.aop.logtrace.LogTraceAspect
import com.myrealtrip.commonweb.aop.logtrace.ThreadLocalLogTrace
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AopConfig {

    @Bean
    fun logTrace(): LogTrace = ThreadLocalLogTrace()

    @Bean
    fun logTraceAspect(): LogTraceAspect = LogTraceAspect(logTrace())

    @Bean
    fun allowedIpAspect(): CheckIpAspect = CheckIpAspect()
}
