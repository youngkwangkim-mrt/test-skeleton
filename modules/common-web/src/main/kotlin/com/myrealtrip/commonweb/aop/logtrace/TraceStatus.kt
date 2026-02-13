package com.myrealtrip.commonweb.aop.logtrace

/**
 * Captures the state of a trace operation for measuring elapsed time.
 *
 * @property traceId the trace identifier with call hierarchy level
 * @property startNanos start time in nanoseconds for elapsed time calculation
 * @property message the method signature being traced
 */
data class TraceStatus(
    val traceId: TraceId,
    val startNanos: Long,
    val message: String,
)
