package com.myrealtrip.commonweb.aop.logtrace

import com.myrealtrip.common.TraceHeader.APP_TRACE_ID
import org.slf4j.MDC
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val FIRST_LEVEL = 1

/**
 * Immutable trace identifier for tracking method call hierarchy.
 *
 * @property id unique trace ID (from MDC or generated UUID v7)
 * @property level current depth in call hierarchy (1 = top level)
 */
data class TraceId @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = MDC.get(APP_TRACE_ID) ?: Uuid.generateV7().toString(),
    val level: Int = FIRST_LEVEL,
) {
    val isFirstLevel: Boolean
        get() = level == FIRST_LEVEL

    fun nextLevel(): TraceId = copy(level = level + 1)

    fun prevLevel(): TraceId = copy(level = level - 1)

}
