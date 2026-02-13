package com.myrealtrip.commonweb.interceptors

import com.myrealtrip.common.INTERCEPTOR_EXCLUDE_PATH
import com.myrealtrip.commonweb.utils.EnvironmentUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class InterceptorConfig(
    private val environmentUtil: EnvironmentUtil
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(logInterceptor(environmentUtil))
            .order(1)
            .addPathPatterns("/**")
            .excludePathPatterns(INTERCEPTOR_EXCLUDE_PATH)
        registry.addInterceptor(logResponseBodyInterceptor())
            .order(2)
            .addPathPatterns("/**")
            .excludePathPatterns(INTERCEPTOR_EXCLUDE_PATH)
    }

    @Bean
    fun logInterceptor(environmentUtil: EnvironmentUtil): LogInterceptor {
        return LogInterceptor(environmentUtil)
    }

    @Bean
    fun logResponseBodyInterceptor(): LogResponseBodyInterceptor {
        return LogResponseBodyInterceptor()
    }


}