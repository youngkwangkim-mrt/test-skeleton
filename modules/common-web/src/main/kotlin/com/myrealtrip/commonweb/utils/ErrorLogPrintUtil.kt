package com.myrealtrip.commonweb.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.codes.ResponseCode
import jakarta.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

/**
 * Utility for logging error information.
 */
object ErrorLogPrintUtil {

    /**
     * Logs error with request context.
     *
     * @param request HttpServletRequest
     * @param code ResponseCode
     * @param e Exception
     * @param printTrace whether to include stack trace
     */
    @JvmOverloads
    @JvmStatic
    fun logError(
        request: HttpServletRequest,
        code: ResponseCode,
        e: Exception,
        printTrace: Boolean = false,
    ) {
        val rootCause = getRootCause(e)
        val message = buildErrorMessage(request, code, e, rootCause)

        if (printTrace) {
            logger.error(e) { message }
        } else {
            logger.error { message }
        }
    }

    private fun buildErrorMessage(
        request: HttpServletRequest,
        code: ResponseCode,
        exception: Exception,
        rootCause: Throwable,
    ): String = buildString {
        appendLine("ERROR INFO ::")
        appendLine("RequestURI: ${request.method}, ${request.requestURI}")
        appendLine("ServerIp = ${IpAddrUtil.serverIp}, ClientIp = ${IpAddrUtil.getClientIp(request)}")
        appendLine(code.description())
        appendLine("Exception: ${exception.javaClass.simpleName} , Cause: ${exception.message}")
        append("RootCause: ${rootCause.javaClass.simpleName} , Cause: ${rootCause.message}")
    }

    private tailrec fun getRootCause(throwable: Throwable): Throwable {
        val cause = throwable.cause
        return if (cause != null && cause !== throwable) {
            getRootCause(cause)
        } else {
            throwable
        }
    }
}