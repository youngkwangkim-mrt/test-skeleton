package com.myrealtrip.common.utils.extensions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Extracts Unix timestamp (milliseconds) from UUID v7.
 *
 * UUID v7 stores the Unix timestamp in the first 48 bits.
 */
@OptIn(ExperimentalUuidApi::class)
fun Uuid.extractTimestamp(): Long {
    val msb = this.toLongs { mostSignificantBits, _ -> mostSignificantBits }
    return msb ushr 16
}

/**
 * Extracts [LocalDateTime] from UUID v7.
 *
 * @param zoneId the timezone to use (default: system default)
 */
@JvmOverloads
@OptIn(ExperimentalUuidApi::class)
fun Uuid.extractLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(extractTimestamp()).atZone(zoneId).toLocalDateTime()
