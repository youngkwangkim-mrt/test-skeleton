package com.myrealtrip.common.utils

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.KnownException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Throws [KnownException] if the [value] is false.
 *
 * Similar to [require], but throws [KnownException] with [ErrorCode.ILLEGAL_ARGUMENT].
 *
 * @param value the condition to check
 * @param lazyMessage the message to include in the exception if the condition is false
 * @throws KnownException if [value] is false
 */
@OptIn(ExperimentalContracts::class)
inline fun knownRequired(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw KnownException(ErrorCode.ILLEGAL_ARGUMENT, message.toString())
    }
}

/**
 * Throws [KnownException] if the [value] is null, otherwise returns the non-null value.
 *
 * Similar to [requireNotNull], but throws [KnownException] with [ErrorCode.ILLEGAL_ARGUMENT].
 *
 * @param value the value to check for null
 * @param lazyMessage the message to include in the exception if the value is null
 * @return the non-null value
 * @throws KnownException if [value] is null
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any> knownRequiredNotNull(value: T?, lazyMessage: () -> Any): T {
    contract {
        returns() implies (value != null)
    }
    if (value == null) {
        val message = lazyMessage()
        throw KnownException(ErrorCode.ILLEGAL_ARGUMENT, message.toString())
    }
    return value
}

/**
 * Throws [KnownException] if the [value] is null or blank, otherwise returns the non-blank string.
 *
 * @param value the string to check
 * @param lazyMessage the message to include in the exception if the value is null or blank
 * @return the non-blank string
 * @throws KnownException if [value] is null or blank
 */
@OptIn(ExperimentalContracts::class)
inline fun <Any> knownNotBlank(value: String?, lazyMessage: () -> Any): String {
    contract {
        returns() implies (value != null)
    }
    if (value.isNullOrBlank()) {
        val message = lazyMessage()
        throw KnownException(ErrorCode.ILLEGAL_ARGUMENT, message.toString())
    }
    return value
}