package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.ResponseCode

/**
 * Common contract for business exceptions.
 *
 * This interface defines the shared properties for both checked ([BizException])
 * and unchecked ([BizRuntimeException]) business exceptions.
 */
interface BizExceptionInfo {

    /**
     * The response code representing the error type.
     */
    val code: ResponseCode

    /**
     * The error message describing the exception.
     */
    val message: String

    /**
     * Whether to log the stack trace when handling this exception.
     *
     * When `true`, the exception handler should log the full stack trace.
     * When `false`, only the exception message should be logged.
     */
    val logStackTrace: Boolean

    /**
     * Returns a descriptive string representation of the exception.
     *
     * Format: `ClassName[code]: message`
     *
     * @return formatted description string
     */
    fun describe(): String = "${this::class.simpleName}[$code]: $message"
}
