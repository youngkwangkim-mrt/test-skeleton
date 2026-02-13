package com.myrealtrip.commonweb.response

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page

/**
 * Standard page response format
 */
data class PageResponse(
    @param:JsonProperty("content") val content: Collection<Any>,
    @param:JsonProperty("pageInfo") val pageInfo: PageInfo
) {

    constructor(page: Page<*>) : this(
        content = page.content,
        pageInfo = PageInfo(page)
    )

    /**
     * Page information
     */
    data class PageInfo(
        @param:JsonProperty("totalPages") val totalPages: Int,
        @param:JsonProperty("totalElements") val totalElements: Long,
        @param:JsonProperty("pageNumber") val pageNumber: Int,
        @param:JsonProperty("pageElements") val pageElements: Int,
        @param:JsonProperty("isFirst") val isFirst: Boolean,
        @param:JsonProperty("isLast") val isLast: Boolean,
        @param:JsonProperty("isEmpty") val isEmpty: Boolean
    ) {
        constructor(page: Page<*>) : this(
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            pageNumber = page.number,
            pageElements = page.numberOfElements,
            isFirst = page.isFirst,
            isLast = page.isLast,
            isEmpty = page.isEmpty
        )
    }
}
