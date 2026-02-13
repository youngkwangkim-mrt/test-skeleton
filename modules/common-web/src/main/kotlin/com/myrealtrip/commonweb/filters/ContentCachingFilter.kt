package com.myrealtrip.commonweb.filters

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

private val filterLogger = KotlinLogging.logger {}

/**
 * Request/Response Body 를 Caching 하는 Filter
 */
@WebFilter(filterName = "ContentCachingFilter", urlPatterns = ["/**"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class ContentCachingFilter : OncePerRequestFilter() {

    init {
        filterLogger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cachedRequest = CachedBodyHttpServletRequest(request)
        val cachedResponse = ContentCachingResponseWrapper(response)

        try {
            filterChain.doFilter(cachedRequest, cachedResponse)
        } finally {
            cachedResponse.copyBodyToResponse()
        }
    }
}
