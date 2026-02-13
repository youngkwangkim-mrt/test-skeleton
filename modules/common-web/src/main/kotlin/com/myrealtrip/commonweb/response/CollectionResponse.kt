package com.myrealtrip.commonweb.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Standard collection response format
 */
data class CollectionResponse(
    @param:JsonProperty("size") val size: Int,
    @param:JsonProperty("content") val content: Any
) {
    constructor(content: Collection<*>) : this(content.size, content)
    constructor(content: Map<*, *>) : this(content.size, content)
}
