package com.myrealtrip.commonweb.interceptors

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.annoatations.LogResponseBody
import com.myrealtrip.commonweb.utils.HttpServletUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.System.Logger.Level

private val logger = KotlinLogging.logger {}

/**
 * Interceptor to log response body
 *
 * [LogResponseBody] annotation is used to log response body.
 *
 * @see LogResponseBody
 */
class LogResponseBodyInterceptor : HandlerInterceptor {

    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (handler !is HandlerMethod) return

        handler.getMethodAnnotation(LogResponseBody::class.java)
            ?.takeIf { it.value }
            ?.let { logResponseBody(it) }
    }

    private fun logResponseBody(annotation: LogResponseBody) {
        val responseBody = HttpServletUtil.getResponseBody()
        val output = truncateIfNeeded(responseBody, annotation.maxLength, annotation.printAll)

        when (annotation.logLevel) {
            Level.INFO -> logger.info { output }
            Level.DEBUG -> logger.debug { output }
            Level.TRACE -> logger.trace { output }
            Level.WARNING -> logger.warn { output }
            Level.ERROR -> logger.error { output }
            Level.OFF -> Unit
            else -> logger.info { output }
        }
    }

    private fun truncateIfNeeded(text: String, maxLength: Int, printAll: Boolean): String {
        if (printAll || text.length <= maxLength) {
            return text
        }
        val end = maxLength - TRUNCATION_SUFFIX.length
        return text.substring(0, end) + TRUNCATION_SUFFIX
    }

    companion object {
        private const val TRUNCATION_SUFFIX = " ...(truncated)..."
    }
}
