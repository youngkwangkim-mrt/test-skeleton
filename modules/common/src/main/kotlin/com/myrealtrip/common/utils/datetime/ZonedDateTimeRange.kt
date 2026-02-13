package com.myrealtrip.common.utils.datetime

import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [ZonedDateTime] with a start and end datetime.
 *
 * The range is inclusive on both ends: [start, end].
 *
 * Usage:
 * ```kotlin
 * val range = ZonedDateTimeRange(
 *     ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")),
 *     ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, ZoneId.of("Asia/Seoul"))
 * )
 *
 * // Check if datetime is within range using 'in' operator
 * val isWorkTime = ZonedDateTime.now() in range
 *
 * // Calculate duration
 * val workHours = range.hoursBetween()  // 9
 * ```
 *
 * @property start the start datetime of the range (inclusive)
 * @property end the end datetime of the range (inclusive)
 * @throws IllegalArgumentException if start is after end
 */
@JvmRecord
@ConsistentCopyVisibility
data class ZonedDateTimeRange private constructor(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
) {

    init {
        require(!start.isAfter(end)) {
            "start must be before or equal to end, start: $start, end: $end"
        }
    }

    companion object {
        /**
         * Creates a [ZonedDateTimeRange] with the specified [start] and [end] datetimes.
         *
         * @param start the start datetime of the range
         * @param end the end datetime of the range
         * @return a new [ZonedDateTimeRange] instance
         * @throws IllegalArgumentException if start is after end
         */
        @JvmStatic
        fun from(start: ZonedDateTime, end: ZonedDateTime): ZonedDateTimeRange {
            return ZonedDateTimeRange(start, end)
        }

        /**
         * Creates a [ZonedDateTimeRange] using operator syntax: `ZonedDateTimeRange(start, end)`.
         */
        operator fun invoke(start: ZonedDateTime, end: ZonedDateTime): ZonedDateTimeRange {
            return from(start, end)
        }
    }

    // =============================================================================
    // Containment Checks
    // =============================================================================

    /**
     * Checks if the specified [dateTime] is within this range.
     *
     * Both start and end datetimes are included (closed interval).
     * Supports the `in` operator for idiomatic Kotlin usage.
     *
     * Usage:
     * ```kotlin
     * val range = ZonedDateTimeRange(
     *     ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")),
     *     ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, ZoneId.of("Asia/Seoul"))
     * )
     * val isInRange = ZonedDateTime.now() in range
     * ```
     *
     * @param dateTime the datetime to check
     * @return true if [dateTime] is within [start, end], false otherwise
     */
    operator fun contains(dateTime: ZonedDateTime): Boolean {
        return !dateTime.isBefore(start) && !dateTime.isAfter(end)
    }

    // =============================================================================
    // Range Operations
    // =============================================================================

    /**
     * Checks if this range overlaps with another range.
     *
     * Two ranges overlap if they share at least one common datetime point.
     *
     * @param other the other range to check
     * @return true if the ranges overlap, false otherwise
     */
    fun overlaps(other: ZonedDateTimeRange): Boolean {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end)
    }

    /**
     * Returns the intersection of this range with another range.
     *
     * @param other the other range to intersect with
     * @return the intersection as a new [ZonedDateTimeRange], or null if no overlap
     */
    fun intersect(other: ZonedDateTimeRange): ZonedDateTimeRange? {
        if (!overlaps(other)) return null

        val intersectStart = maxOf(this.start, other.start)
        val intersectEnd = minOf(this.end, other.end)
        return from(intersectStart, intersectEnd)
    }

    // =============================================================================
    // Duration Calculations
    // =============================================================================

    /**
     * Returns the duration between start and end datetimes.
     *
     * @return [Duration] representing the time span of this range
     */
    fun duration(): Duration {
        return Duration.between(start, end)
    }

    /**
     * Returns the total number of days between start and end datetimes.
     *
     * Fractional days are truncated.
     *
     * @return the number of whole days
     */
    fun daysBetween(): Long {
        return ChronoUnit.DAYS.between(start, end)
    }

    /**
     * Returns the total number of hours between start and end datetimes.
     *
     * Fractional hours are truncated.
     *
     * @return the number of whole hours
     */
    fun hoursBetween(): Long {
        return ChronoUnit.HOURS.between(start, end)
    }

    /**
     * Returns the total number of minutes between start and end datetimes.
     *
     * @return the total minutes
     */
    fun minutesBetween(): Long {
        return ChronoUnit.MINUTES.between(start, end)
    }

    /**
     * Returns the total number of seconds between start and end datetimes.
     *
     * @return the total seconds
     */
    fun secondsBetween(): Long {
        return ChronoUnit.SECONDS.between(start, end)
    }

    // =============================================================================
    // Zone Operations
    // =============================================================================

    /**
     * Returns the time offset difference between the start and end datetimes.
     *
     * @return the [ZoneOffset] representing the offset difference
     */
    fun timeOffset(): ZoneOffset {
        return ZoneOffset.ofTotalSeconds(end.offset.totalSeconds - start.offset.totalSeconds)
    }

    /**
     * Converts this [ZonedDateTimeRange] to a [LocalDateTimeRange].
     *
     * The timezone information is stripped from both start and end.
     *
     * @return a new [LocalDateTimeRange] with the local datetime values
     */
    fun toLocalDateTimeRange(): LocalDateTimeRange {
        return LocalDateTimeRange.from(start.toLocalDateTime(), end.toLocalDateTime())
    }

}
