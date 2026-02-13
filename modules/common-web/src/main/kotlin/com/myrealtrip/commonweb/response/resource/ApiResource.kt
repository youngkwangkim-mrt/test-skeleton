package com.myrealtrip.commonweb.response.resource

import com.fasterxml.jackson.annotation.JsonProperty
import com.myrealtrip.common.codes.ResponseCode
import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.codes.response.SuccessCode
import com.myrealtrip.commonweb.response.NoOffsetPageResponse
import com.myrealtrip.commonweb.response.PageResponse
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity

/**
 * Standard API resource format
 */
data class ApiResource<T>(
    @param:JsonProperty("status") val status: Status,
    @param:JsonProperty("meta") val meta: Meta,
    @param:JsonProperty("data") val data: T,
) {

    fun toResponseEntity(): ResponseEntity<ApiResource<T>> = ResponseEntity.status(status.status).body(this)

    companion object {

        @JvmStatic
        fun success(): ResponseEntity<ApiResource<String>> = of(SuccessCode.SUCCESS, SuccessCode.SUCCESS.message)

        @JvmStatic
        fun <T> success(data: T): ResponseEntity<ApiResource<T>> = of(SuccessCode.SUCCESS, data)

        @JvmStatic
        fun error(): ResponseEntity<ApiResource<String>> = of(ErrorCode.SERVER_ERROR, ErrorCode.SERVER_ERROR.message)

        @JvmStatic
        fun <T> error(data: T): ResponseEntity<ApiResource<T>> = of(ErrorCode.SERVER_ERROR, data)

        @JvmStatic
        fun <T> of(data: T): ResponseEntity<ApiResource<T>> = of(SuccessCode.SUCCESS, data)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> of(code: ResponseCode, data: T): ResponseEntity<ApiResource<T>> =
            when (data) {
                is Collection<*> -> ofCollection(data, code)
                is Map<*, *> -> ofMap(data, code)
                is Page<*> -> ofPage(data, code)
                else -> createResource(code, data).toResponseEntity()
            } as ResponseEntity<ApiResource<T>>

        @JvmStatic
        @JvmOverloads
        fun <T> ofCollection(
            collection: Collection<T>,
            code: ResponseCode = SuccessCode.SUCCESS
        ): ResponseEntity<ApiResource<Collection<T>>> =
            createResource(code, collection, Meta(size = collection.size)).toResponseEntity()

        @JvmStatic
        @JvmOverloads
        fun ofMap(
            map: Map<*, *>,
            code: ResponseCode = SuccessCode.SUCCESS
        ): ResponseEntity<ApiResource<Map<*, *>>> =
            createResource(code, map, Meta(size = map.size)).toResponseEntity()

        @JvmStatic
        @JvmOverloads
        fun <T : Any> ofPage(
            page: Page<T>,
            code: ResponseCode = SuccessCode.SUCCESS
        ): ResponseEntity<ApiResource<List<T>>> =
            createResource(code, page.content, Meta(pageInfo = PageResponse.PageInfo(page))).toResponseEntity()

        @JvmStatic
        @JvmOverloads
        fun <T : Any> ofNoOffsetPage(
            page: Page<T>,
            lastIndex: String,
            code: ResponseCode = SuccessCode.SUCCESS
        ): ResponseEntity<ApiResource<List<T>>> =
            createResource(
                code,
                page.content,
                Meta(offsetInfo = NoOffsetPageResponse.OffsetInfo(lastIndex, page))
            ).toResponseEntity()

        private fun <T> createResource(code: ResponseCode, data: T, meta: Meta = Meta()): ApiResource<T> =
            ApiResource(status = Status.of(code), meta = meta, data = data)
    }
}
