package com.myrealtrip.common.codes

/**
 * Response code interface
 */
interface ResponseCode {

    /**
     * http status code
     */
    val status: Int

    /**
     * response message
     */
    val message: String

    /**
     * response code name (Enum)
     */
    val name: String

    fun isSuccess(): Boolean = status in 200..299

    fun isClientError(): Boolean = status in 400..499

    fun isServerError(): Boolean = status in 500..599

    fun isError(): Boolean = isClientError() || isServerError()

    fun isBusinessError(): Boolean = status == 406

    fun description(): String = "[$name] $message"

}