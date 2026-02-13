package com.myrealtrip.common.utils.datetime

import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [LocalTime] with a start and end time.
 *
 * The range is inclusive on both ends: [start, end].
 *
 * Usage:
 * ```kotlin
 * val range = LocalTimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0))
 *
 * // Check if time is within range using 'in' operator
 * val isWorkHour = LocalTime.of(12, 0) in range  // true
 *
 * // Calculate duration
 * val workHours = range.hoursBetween()  // 9
 * ```
 *
 * @property start the start time of the range (inclusive)
 * @property end the end time of the range (inclusive)
 * @throws IllegalArgumentException if start is after end
 */
@JvmRecord
@ConsistentCopyVisibility
data class LocalTimeRange private constructor(
    val start: LocalTime,
    val end: LocalTime,
) {

    init {
        require(!start.isAfter(end)) {
            "start must be before or equal to end, start: $start, end: $end"
        }
    }

    companion object {
        /**
         * Creates a [LocalTimeRange] with the specified [start] and [end] times.
         *
         * @param start the start time of the range
         * @param end the end time of the range
         * @return a new [LocalTimeRange] instance
         * @throws IllegalArgumentException if start is after end
         */
        @JvmStatic
        fun from(start: LocalTime, end: LocalTime): LocalTimeRange {
            return LocalTimeRange(start, end)
        }

        /**
         * Creates a [LocalTimeRange] using operator syntax: `LocalTimeRange(start, end)`.
         */
        operator fun invoke(start: LocalTime, end: LocalTime): LocalTimeRange {
            return from(start, end)
        }
    }

    // =============================================================================
    // Containment Checks
    // =============================================================================

    /**
     * Checks if the specified [time] is within this range.
     *
     * Both start and end times are included (closed interval).
     * Supports the `in` operator for idiomatic Kotlin usage.
     *
     * Usage:
     * ```kotlin
     * val range = LocalTimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0))
     * val isInRange = LocalTime.of(12, 0) in range  // true
     * ```
     *
     * @param time the time to check
     * @return true if [time] is within [start, end], false otherwise
     */
    operator fun contains(time: LocalTime): Boolean {
        return !time.isBefore(start) && !time.isAfter(end)
    }

    // =============================================================================
    // Range Operations
    // =============================================================================

    /**
     * Checks if this range overlaps with another range.
     *
     * Two ranges overlap if they share at least one common time point.
     *
     * @param other the other range to check
     * @return true if the ranges overlap, false otherwise
     */
    fun overlaps(other: LocalTimeRange): Boolean {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end)
    }

    /**
     * Returns the intersection of this range with another range.
     *
     * @param other the other range to intersect with
     * @return the intersection as a new [LocalTimeRange], or null if no overlap
     */
    fun intersect(other: LocalTimeRange): LocalTimeRange? {
        if (!overlaps(other)) return null

        val intersectStart = maxOf(this.start, other.start)
        val intersectEnd = minOf(this.end, other.end)
        return from(intersectStart, intersectEnd)
    }

    // =============================================================================
    // Duration Calculations
    // =============================================================================

    /**
     * Returns the duration between start and end times.
     *
     * @return [Duration] representing the time span of this range
     */
    fun duration(): Duration {
        return Duration.between(start, end)
    }

    /**
     * Returns the number of whole hours between start and end times.
     *
     * Fractional hours are truncated.
     *
     * @return the number of hours (e.g., 2 hours 30 minutes returns 2)
     */
    fun hoursBetween(): Long {
        return ChronoUnit.HOURS.between(start, end)
    }

    /**
     * Returns the total number of minutes between start and end times.
     *
     * @return the total minutes (e.g., 2 hours 30 minutes returns 150)
     */
    fun minutesBetween(): Long {
        return ChronoUnit.MINUTES.between(start, end)
    }

    /**
     * Returns the total number of seconds between start and end times.
     *
     * @return the total seconds
     */
    fun secondsBetween(): Long {
        return ChronoUnit.SECONDS.between(start, end)
    }

}