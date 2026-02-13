package com.myrealtrip.commonweb.interceptors

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.annoatations.ExcludeRequestLog
import com.myrealtrip.commonweb.utils.EnvironmentUtil
import com.myrealtrip.commonweb.utils.HttpServletUtil
import com.myrealtrip.commonweb.utils.IpAddrUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

private val logger = KotlinLogging.logger {}

/**
 * Interceptor to log request info
 *
 * [ExcludeRequestLog] annotation is used to exclude request info logging.
 *
 * @see ExcludeRequestLog
 */
class LogInterceptor(
    private val environmentUtil: EnvironmentUtil,
) : HandlerInterceptor {

    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (shouldSkipLogging(handler)) {
            return true
        }
        logRequestInfo(request)
        return true
    }

    private fun shouldSkipLogging(handler: Any): Boolean =
        environmentUtil.isProduction() &&
                handler is HandlerMethod &&
                handler.hasMethodAnnotation(ExcludeRequestLog::class.java)

    private fun logRequestInfo(request: HttpServletRequest) {
        val message = buildString {
            appendLine("# ==> REQUEST INFO ::")
            appendLine("ServerIp = ${IpAddrUtil.serverIp} , ClientIp = ${IpAddrUtil.getClientIp(request)}")
            appendLine("RequestURI = ${request.method} ${request.requestURI}")
            appendLine("Headers:")
            request.headerNames.asIterator().asSequence()
                .filter { isLoggableHeader(it) }
                .forEach { appendLine("  $it = ${request.getHeader(it)}") }
            appendLineIfNotBlank("RequestParameters", HttpServletUtil.getRequestParams())
            appendLineIfNotBlank("RequestBody", HttpServletUtil.getRequestBody())
        }
        logger.info { message }
    }

    private fun isLoggableHeader(headerName: String): Boolean {
        val name = headerName.lowercase()
        return name in LOGGABLE_HEADERS || LOGGABLE_PREFIXES.any { name.startsWith(it) }
    }

    private fun StringBuilder.appendLineIfNotBlank(label: String, value: String?) {
        if (!value.isNullOrBlank()) {
            appendLine("$label = $value")
        }
    }

    companion object {
        private val LOGGABLE_HEADERS = setOf(
            "authorization", "content-type", "accept", "referer",
            "b3", "traceparent", "tracestate"
        )
        private val LOGGABLE_PREFIXES = listOf("x-b3-", "x-request-")
    }
}
