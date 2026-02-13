package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.ResponseCode

/**
 * Unchecked business exception for unrecoverable business errors.
 *
 * Use this exception when:
 * - The error indicates a bug or unexpected condition
 * - Recovery is not expected
 * - The caller should not be forced to handle it
 *
 * For recoverable errors, use [BizException] instead.
 *
 * @property code the response code representing the error type
 * @property message the error message (defaults to code's message)
 * @property cause the underlying cause of this exception
 * @property logStackTrace whether to log stack trace when handling this exception
 */
open class BizRuntimeException @JvmOverloads constructor(
    override val code: ResponseCode,
    override val message: String = code.message,
    override val cause: Throwable? = null,
    override val logStackTrace: Boolean = false,
) : RuntimeException(message, cause), BizExceptionInfo
