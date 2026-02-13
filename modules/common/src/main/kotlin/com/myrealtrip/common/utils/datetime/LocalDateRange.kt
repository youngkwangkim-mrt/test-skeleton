package com.myrealtrip.common.utils.datetime

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [LocalDate] with a start and end date.
 *
 * The range is inclusive on both ends: [start, end].
 *
 * Usage:
 * ```kotlin
 * val range = LocalDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
 *
 * // Check if date is within range using 'in' operator
 * val isInYear = LocalDate.of(2025, 6, 15) in range  // true
 *
 * // Calculate duration
 * val totalDays = range.daysBetween()  // 364
 * ```
 *
 * @property start the start date of the range (inclusive)
 * @property end the end date of the range (inclusive)
 * @throws IllegalArgumentException if start is after end
 */
@JvmRecord
@ConsistentCopyVisibility
data class LocalDateRange private constructor(
    val start: LocalDate,
    val end: LocalDate,
) {

    init {
        require(!start.isAfter(end)) {
            "start must be before or equal to end, start: $start, end: $end"
        }
    }

    companion object {
        /**
         * Creates a [LocalDateRange] with the specified [start] and [end] dates.
         *
         * @param start the start date of the range
         * @param end the end date of the range
         * @return a new [LocalDateRange] instance
         * @throws IllegalArgumentException if start is after end
         */
        @JvmStatic
        fun from(start: LocalDate, end: LocalDate): LocalDateRange {
            return LocalDateRange(start, end)
        }

        /**
         * Creates a [LocalDateRange] using operator syntax: `LocalDateRange(start, end)`.
         */
        operator fun invoke(start: LocalDate, end: LocalDate): LocalDateRange {
            return from(start, end)
        }
    }

    // =============================================================================
    // Containment Checks
    // =============================================================================

    /**
     * Checks if the specified [date] is within this range.
     *
     * Both start and end dates are included (closed interval).
     * Supports the `in` operator for idiomatic Kotlin usage.
     *
     * Usage:
     * ```kotlin
     * val range = LocalDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
     * val isInRange = LocalDate.of(2025, 6, 15) in range  // true
     * ```
     *
     * @param date the date to check
     * @return true if [date] is within [start, end], false otherwise
     */
    operator fun contains(date: LocalDate): Boolean {
        return !date.isBefore(start) && !date.isAfter(end)
    }

    // =============================================================================
    // Range Operations
    // =============================================================================

    /**
     * Checks if this range overlaps with another range.
     *
     * Two ranges overlap if they share at least one common date.
     *
     * @param other the other range to check
     * @return true if the ranges overlap, false otherwise
     */
    fun overlaps(other: LocalDateRange): Boolean {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end)
    }

    /**
     * Returns the intersection of this range with another range.
     *
     * @param other the other range to intersect with
     * @return the intersection as a new [LocalDateRange], or null if no overlap
     */
    fun intersect(other: LocalDateRange): LocalDateRange? {
        if (!overlaps(other)) return null

        val intersectStart = maxOf(this.start, other.start)
        val intersectEnd = minOf(this.end, other.end)
        return from(intersectStart, intersectEnd)
    }

    // =============================================================================
    // Duration Calculations
    // =============================================================================

    /**
     * Returns the period between start and end dates.
     *
     * @return [Period] consisting of years, months, and days
     */
    fun period(): Period {
        return Period.between(start, end)
    }

    /**
     * Returns the total number of days between start and end dates.
     *
     * @return the total days
     */
    fun daysBetween(): Long {
        return ChronoUnit.DAYS.between(start, end)
    }

    /**
     * Returns the total number of weeks between start and end dates.
     *
     * Fractional weeks are truncated.
     *
     * @return the number of whole weeks
     */
    fun weeksBetween(): Long {
        return ChronoUnit.WEEKS.between(start, end)
    }

    /**
     * Returns the total number of months between start and end dates.
     *
     * Fractional months are truncated.
     *
     * @return the number of whole months
     */
    fun monthsBetween(): Long {
        return ChronoUnit.MONTHS.between(start, end)
    }

    /**
     * Returns the total number of years between start and end dates.
     *
     * Fractional years are truncated.
     *
     * @return the number of whole years
     */
    fun yearsBetween(): Long {
        return ChronoUnit.YEARS.between(start, end)
    }

}