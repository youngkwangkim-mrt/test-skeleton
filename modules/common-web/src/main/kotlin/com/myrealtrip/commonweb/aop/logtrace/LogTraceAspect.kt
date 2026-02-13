package com.myrealtrip.commonweb.aop.logtrace

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order

private val logger = KotlinLogging.logger {}

/**
 * Aspect that traces method execution with timing information.
 *
 * Logs method entry/exit with call hierarchy visualization.
 */
@Aspect
@Order(2)
class LogTraceAspect(
    private val logTrace: LogTrace,
) {
    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    @Around("com.myrealtrip.commonweb.aop.logtrace.TracePointcuts.all()")
    fun traceMethod(joinPoint: ProceedingJoinPoint): Any? {

        val status = logTrace.begin(joinPoint.signature.toShortString())

        return try {
            joinPoint.proceed().also { logTrace.end(status) }
        } catch (e: Exception) {
            logTrace.exception(status, e)
            throw e
        }
    }
}
