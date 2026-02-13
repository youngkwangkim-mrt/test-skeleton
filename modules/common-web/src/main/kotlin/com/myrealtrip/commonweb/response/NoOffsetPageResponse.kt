package com.myrealtrip.commonweb.response

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page

/**
 * Standard no offset page response format
 */
data class NoOffsetPageResponse(
    @param:JsonProperty("content") val content: Collection<Any>,
    @param:JsonProperty("offsetInfo") val offsetInfo: OffsetInfo,
) {

    constructor(lastIndex: String, page: Page<*>) : this(
        content = page.content,
        offsetInfo = OffsetInfo(lastIndex, page)
    )

    /**
     * Offset information for cursor-based pagination
     */
    data class OffsetInfo(
        @param:JsonProperty("lastIndex") val lastIndex: String,
        @param:JsonProperty("totalPages") val totalPages: Int,
        @param:JsonProperty("totalElements") val totalElements: Long,
        @param:JsonProperty("pageNumber") val pageNumber: Int,
        @param:JsonProperty("pageElements") val pageElements: Int,
        @param:JsonProperty("isLast") val isLast: Boolean,
        @param:JsonProperty("isEmpty") val isEmpty: Boolean,
    ) {
        constructor(lastIndex: String, page: Page<*>) : this(
            lastIndex = lastIndex,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            pageNumber = page.number,
            pageElements = page.numberOfElements,
            isLast = page.isLast,
            isEmpty = page.isEmpty
        )
    }
}
