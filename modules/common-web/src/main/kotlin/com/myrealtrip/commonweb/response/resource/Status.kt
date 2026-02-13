package com.myrealtrip.commonweb.response.resource

import com.fasterxml.jackson.annotation.JsonProperty
import com.myrealtrip.common.codes.ResponseCode

/**
 * Standard status response format
 */
data class Status(
    @param:JsonProperty("status") val status: Int,
    @param:JsonProperty("code") val code: String,
    @param:JsonProperty("message") val message: String
) {
    companion object {
        @JvmStatic
        fun of(responseCode: ResponseCode): Status =
            Status(responseCode.status, responseCode.name, responseCode.message)
    }
}
