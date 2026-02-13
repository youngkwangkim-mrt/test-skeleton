package com.myrealtrip.infrastructure.client

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class HttpExchangeMethodAspect {

    @Around("@within(org.springframework.web.service.annotation.HttpExchange)")
    fun captureMethodName(joinPoint: ProceedingJoinPoint): Any? {
        HttpExchangeMethodContext.set(joinPoint.signature.name)
        return try {
            joinPoint.proceed()
        } finally {
            HttpExchangeMethodContext.clear()
        }
    }
}
