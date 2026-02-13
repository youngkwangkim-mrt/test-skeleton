package com.myrealtrip.commonweb.handlers

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.exceptions.KnownException
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.stereotype.Component
import java.lang.reflect.Method

private val logger = KotlinLogging.logger {}

/**
 * Async exception handler that logs [KnownException] at DEBUG level.
 */
@Component
class GlobalSimpleAsyncUncaughtExceptionHandler : SimpleAsyncUncaughtExceptionHandler() {

    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    override fun handleUncaughtException(
        ex: Throwable,
        method: Method,
        vararg params: Any?,
    ) {
        if (ex is KnownException) {
            logger.debug(ex) { ex.describe() }
        } else {
            super.handleUncaughtException(ex, method, *params)
        }
    }
}