package com.myrealtrip.commonweb.response.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.myrealtrip.commonweb.response.NoOffsetPageResponse
import com.myrealtrip.commonweb.response.PageResponse
import com.myrealtrip.commonweb.utils.MdcUtil

/**
 * Standard meta response format (null fields are not serialized)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Meta(
    @param:JsonProperty("x-b3-traceid") val traceId: String = MdcUtil.getTraceId(),
    @param:JsonProperty("appTraceId") val appTraceId: String = MdcUtil.getAppTraceId(),
    @param:JsonProperty("responseTs") val responseTs: Long = System.currentTimeMillis(),
    @param:JsonProperty("size") val size: Int? = null,
    @param:JsonProperty("pageInfo") val pageInfo: PageResponse.PageInfo? = null,
    @param:JsonProperty("offsetInfo") val offsetInfo: NoOffsetPageResponse.OffsetInfo? = null,
)
