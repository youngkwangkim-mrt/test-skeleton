package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.ResponseCode

/**
 * Checked business exception for recoverable business errors.
 *
 * Use this exception when:
 * - The caller is expected to handle the error
 * - The error is part of normal business flow
 * - Recovery is possible
 *
 * For unrecoverable errors, use [BizRuntimeException] instead.
 *
 * @property code the response code representing the error type
 * @property message the error message (defaults to code's message)
 * @property cause the underlying cause of this exception
 * @property logStackTrace whether to log stack trace when handling this exception
 */
open class BizException @JvmOverloads constructor(
    override val code: ResponseCode,
    override val message: String = code.message,
    override val cause: Throwable? = null,
    override val logStackTrace: Boolean = false,
) : Exception(message, cause), BizExceptionInfo
