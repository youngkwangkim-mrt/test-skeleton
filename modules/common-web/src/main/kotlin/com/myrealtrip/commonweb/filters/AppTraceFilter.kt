package com.myrealtrip.commonweb.filters

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.FILTER_EXCLUDE_PATH
import com.myrealtrip.common.TraceHeader.APP_RESPONSE_TIMESTAMP
import com.myrealtrip.common.TraceHeader.APP_TRACE_ID
import com.myrealtrip.common.TraceHeader.X_B3_TRACE_ID
import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val filterLogger = KotlinLogging.logger {}

@OptIn(ExperimentalUuidApi::class)
@WebFilter(filterName = "AppTraceFilter", urlPatterns = ["/**"])
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class AppTraceFilter(
    private val tracer: Tracer
) : OncePerRequestFilter() {

    init {
        filterLogger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        filterLogger.trace { "# REQ START #######################################################################################################" }
        val startNanos = System.nanoTime()
        val appTraceId = Uuid.generateV7().toString()

        setupTraceContext(response, appTraceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            val processTimeMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
            logProcessTime(processTimeMs)
            response.setHeader(APP_RESPONSE_TIMESTAMP, System.currentTimeMillis().toString())
            filterLogger.trace { "# REQ END   #######################################################################################################" }
            MDC.clear()
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return FILTER_EXCLUDE_PATH.any { path.startsWith(it) || path.endsWith(it) }
    }

    private fun setupTraceContext(response: HttpServletResponse, appTraceId: String) {
        MDC.put(APP_TRACE_ID, appTraceId)
        response.setHeader(APP_TRACE_ID, appTraceId)
        response.setHeader(X_B3_TRACE_ID, currentTraceId())
    }

    private fun currentTraceId(): String =
        tracer.currentTraceContext().context()?.traceId() ?: ""

    private fun logProcessTime(processTimeMs: Long) {
        if (processTimeMs >= WARN_PROCESS_TIME_MS) {
            filterLogger.warn { "# Process time: ${processTimeMs}ms (exceeded ${WARN_PROCESS_TIME_MS}ms)" }
        } else {
            filterLogger.debug { "# Process time: ${processTimeMs}ms" }
        }
    }

    companion object {
        private const val WARN_PROCESS_TIME_MS = 8_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
