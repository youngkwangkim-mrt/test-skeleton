package com.myrealtrip.common.utils.datetime

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [LocalDateTime] with a start and end datetime.
 *
 * The range is inclusive on both ends: [start, end].
 *
 * Usage:
 * ```kotlin
 * val range = LocalDateTimeRange(
 *     LocalDateTime.of(2025, 1, 1, 9, 0),
 *     LocalDateTime.of(2025, 1, 1, 18, 0)
 * )
 *
 * // Check if datetime is within range using 'in' operator
 * val isWorkTime = LocalDateTime.of(2025, 1, 1, 12, 0) in range  // true
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
data class LocalDateTimeRange private constructor(
    val start: LocalDateTime,
    val end: LocalDateTime,
) {

    init {
        require(!start.isAfter(end)) {
            "start must be before or equal to end, start: $start, end: $end"
        }
    }

    companion object {
        /**
         * Creates a [LocalDateTimeRange] with the specified [start] and [end] datetimes.
         *
         * @param start the start datetime of the range
         * @param end the end datetime of the range
         * @return a new [LocalDateTimeRange] instance
         * @throws IllegalArgumentException if start is after end
         */
        @JvmStatic
        fun from(start: LocalDateTime, end: LocalDateTime): LocalDateTimeRange {
            return LocalDateTimeRange(start, end)
        }

        /**
         * Creates a [LocalDateTimeRange] using operator syntax: `LocalDateTimeRange(start, end)`.
         */
        operator fun invoke(start: LocalDateTime, end: LocalDateTime): LocalDateTimeRange {
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
     * val range = LocalDateTimeRange(
     *     LocalDateTime.of(2025, 1, 1, 9, 0),
     *     LocalDateTime.of(2025, 1, 1, 18, 0)
     * )
     * val isInRange = LocalDateTime.of(2025, 1, 1, 12, 0) in range  // true
     * ```
     *
     * @param dateTime the datetime to check
     * @return true if [dateTime] is within [start, end], false otherwise
     */
    operator fun contains(dateTime: LocalDateTime): Boolean {
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
    fun overlaps(other: LocalDateTimeRange): Boolean {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end)
    }

    /**
     * Returns the intersection of this range with another range.
     *
     * @param other the other range to intersect with
     * @return the intersection as a new [LocalDateTimeRange], or null if no overlap
     */
    fun intersect(other: LocalDateTimeRange): LocalDateTimeRange? {
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
}