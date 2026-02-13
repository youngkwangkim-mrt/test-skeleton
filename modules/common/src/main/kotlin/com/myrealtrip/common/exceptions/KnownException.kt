package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.ResponseCode

/**
 * Exception for expected/known error conditions that should not be logged.
 *
 * Use this exception for expected business errors like:
 * - Resource not found
 * - Validation failures
 * - User input errors
 *
 * The GlobalExceptionHandler will not log the stack trace for this exception type.
 */
open class KnownException @JvmOverloads constructor(
    override val code: ResponseCode,
    override val message: String = code.message,
    cause: Throwable? = null,
) : BizRuntimeException(
    code = code,
    message = message,
    cause = cause,
    logStackTrace = false,
)
