package com.myrealtrip.commonweb.utils

import com.myrealtrip.common.TraceHeader.APP_TRACE_ID
import org.slf4j.MDC
import java.util.*

/**
 * 
 */
object MdcUtil {

    fun getAppTraceId(): String = MDC.get(APP_TRACE_ID) ?: UUID.randomUUID().toString()

    fun getTraceId(): String = MDC.get("traceId") ?: ""

}