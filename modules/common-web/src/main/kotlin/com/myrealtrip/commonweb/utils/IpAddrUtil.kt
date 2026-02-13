package com.myrealtrip.commonweb.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.InetAddress
import java.net.UnknownHostException

private val logger = KotlinLogging.logger {}

/**
 * IP address utility class
 */
object IpAddrUtil {

    private val IP_HEADER_CANDIDATES = arrayOf(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    )

    /**
     * Server IP address
     */
    val serverIp: String by lazy {
        try {
            InetAddress.getLocalHost().hostAddress.trim()
        } catch (e: UnknownHostException) {
            logger.warn { "UnknownHostException while getting server IP" }
            "unknown"
        }
    }

    /**
     * Server IP last octet
     */
    val serverIpLastOctet: String by lazy {
        serverIp.substringAfterLast(".")
    }

    /**
     * Get client IP address from current request context
     *
     * @return client IP address
     * @throws IllegalStateException if no request context is available
     */
    fun getClientIp(): String {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw IllegalStateException("No request context available")
        return getClientIp(attributes.request)
    }

    /**
     * Get client IP address from request
     *
     * @param request HTTP servlet request
     * @return client IP address
     */
    fun getClientIp(request: HttpServletRequest): String {
        for (header in IP_HEADER_CANDIDATES) {
            val ip = request.getHeader(header)
            if (!ip.isNullOrBlank() && !ip.startsWith("unknown", ignoreCase = true)) {
                return extractFirstIp(ip)
            }
        }
        return request.remoteAddr
    }

    /**
     * Extract first IP from comma-separated IP list (e.g., X-Forwarded-For)
     */
    private fun extractFirstIp(ip: String): String {
        return ip.split(",").first().trim()
    }

}
