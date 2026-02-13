package com.myrealtrip.common.utils.datetime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private val DEFAULT_SEARCH_PERIOD: Period = Period.ofDays(1)
private val DEFAULT_MAX_SEARCH_PERIOD: Period = Period.ofMonths(6)
private val REFERENCE_DATE: LocalDate = LocalDate.of(2000, 1, 1)

/**
 * Represents a date range for search queries with built-in safeguards.
 *
 * This class provides automatic adjustment of date ranges to prevent heavy database queries.
 * When the specified range is invalid (start after end) or exceeds the maximum allowed period,
 * it automatically adjusts the start date in non-strict mode, or throws an exception in strict mode.
 *
 * Usage:
 * ```kotlin
 * // Create with factory methods
 * val recent = SearchDates.lastDays(7)
 * val custom = SearchDates.of(startDate, endDate)
 *
 * // Use in repository
 * repository.findByDateBetween(searchDates.startDate, searchDates.endDate)
 *
 * // Check if date is within range
 * if (targetDate in searchDates) { ... }
 *
 * // Convert to LocalDateRange
 * val range = searchDates.toLocalDateRange()
 * ```
 *
 * @property startDate the start date of the search range (inclusive)
 * @property endDate the end date of the search range (inclusive)
 * @property strict if true, throws exception on invalid dates; if false, auto-adjusts
 * @property searchPeriod the default period used when auto-adjusting invalid start date
 * @property maxSearchPeriod the maximum allowed search period
 */
@JvmRecord
@ConsistentCopyVisibility
data class SearchDates private constructor(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val strict: Boolean,
    val searchPeriod: Period,
    val maxSearchPeriod: Period,
) {

    companion object {
        /**
         * Creates a [SearchDates] with the specified parameters.
         *
         * In non-strict mode:
         * - If startDate is after endDate, startDate is adjusted to `endDate - searchPeriod`
         * - If the range exceeds maxSearchPeriod, startDate is adjusted to `endDate - maxSearchPeriod`
         *
         * In strict mode:
         * - Throws [IllegalArgumentException] if startDate is after endDate
         * - Throws [IllegalArgumentException] if the range exceeds maxSearchPeriod
         *
         * @param startDate the start date (default: yesterday)
         * @param endDate the end date (default: today)
         * @param strict whether to throw exceptions on invalid input (default: false)
         * @param searchPeriod the default search period for adjustment (default: 1 day)
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         * @throws IllegalArgumentException in strict mode when dates are invalid
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            startDate: LocalDate = LocalDate.now().minus(DEFAULT_SEARCH_PERIOD),
            endDate: LocalDate = LocalDate.now(),
            strict: Boolean = false,
            searchPeriod: Period = DEFAULT_SEARCH_PERIOD,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            var adjustedStartDate = startDate

            // Validate start is before or equal to end
            if (startDate.isAfter(endDate)) {
                require(!strict) { "startDate must be before or equal to endDate, startDate: $startDate, endDate: $endDate" }
                adjustedStartDate = endDate.minus(searchPeriod)
            }

            // Validate search period does not exceed max
            val maxSearchDays = ChronoUnit.DAYS.between(
                REFERENCE_DATE, REFERENCE_DATE.plus(maxSearchPeriod)
            )
            val searchDays = ChronoUnit.DAYS.between(adjustedStartDate, endDate)
            if (searchDays > maxSearchDays) {
                require(!strict) { "Search period must not exceed $maxSearchPeriod, current: $searchDays days" }
                adjustedStartDate = endDate.minus(maxSearchPeriod)
            }

            return SearchDates(adjustedStartDate, endDate, strict, searchPeriod, maxSearchPeriod)
        }

        /**
         * Creates a [SearchDates] using operator syntax: `SearchDates(startDate, endDate)`.
         */
        operator fun invoke(
            startDate: LocalDate = LocalDate.now().minus(DEFAULT_SEARCH_PERIOD),
            endDate: LocalDate = LocalDate.now(),
            strict: Boolean = false,
            searchPeriod: Period = DEFAULT_SEARCH_PERIOD,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            return of(startDate, endDate, strict, searchPeriod, maxSearchPeriod)
        }

        /**
         * Creates a [SearchDates] for today only.
         *
         * @return a new [SearchDates] instance with start and end both set to today
         */
        @JvmStatic
        fun today(): SearchDates {
            val today = LocalDate.now()
            return of(
                startDate = today,
                endDate = today,
                strict = true,
                searchPeriod = Period.ZERO,
                maxSearchPeriod = DEFAULT_MAX_SEARCH_PERIOD,
            )
        }

        /**
         * Creates a [SearchDates] for yesterday only.
         *
         * @return a new [SearchDates] instance with start and end both set to yesterday
         */
        @JvmStatic
        fun yesterday(): SearchDates {
            val yesterday = LocalDate.now().minusDays(1)
            return of(
                startDate = yesterday,
                endDate = yesterday,
                strict = true,
                searchPeriod = Period.ZERO,
                maxSearchPeriod = DEFAULT_MAX_SEARCH_PERIOD,
            )
        }

        /**
         * Creates a [SearchDates] for the last N days from today.
         *
         * @param days the number of days to look back
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun lastDays(
            days: Int,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            return of(
                startDate = today.minusDays(days.toLong()),
                endDate = today,
                strict = false,
                searchPeriod = Period.ofDays(days),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the last N weeks from today.
         *
         * @param weeks the number of weeks to look back
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun lastWeeks(
            weeks: Int,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            return of(
                startDate = today.minusWeeks(weeks.toLong()),
                endDate = today,
                strict = false,
                searchPeriod = Period.ofWeeks(weeks),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the last N months from today.
         *
         * @param months the number of months to look back
         * @param maxSearchPeriod the maximum allowed period (default: 3 months)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun lastMonths(
            months: Int,
            maxSearchPeriod: Period = Period.ofMonths(3),
        ): SearchDates {
            val today = LocalDate.now()
            return of(
                startDate = today.minusMonths(months.toLong()),
                endDate = today,
                strict = false,
                searchPeriod = Period.ofMonths(months),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the previous week.
         *
         * The week boundaries are determined by [weekStartDay]:
         * - If SUNDAY: previous Sunday to Saturday
         * - If MONDAY: previous Monday to Sunday
         *
         * @param weekStartDay the first day of the week (default: SUNDAY)
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun lastWeek(
            weekStartDay: DayOfWeek = DayOfWeek.SUNDAY,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
            val lastWeekStart = thisWeekStart.minusWeeks(1)
            val lastWeekEnd = lastWeekStart.plusDays(6)
            return of(
                startDate = lastWeekStart,
                endDate = lastWeekEnd,
                strict = false,
                searchPeriod = Period.ofWeeks(1),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the current week (week start day to today).
         *
         * The week start is determined by [weekStartDay]:
         * - If SUNDAY: this/previous Sunday to today
         * - If MONDAY: this/previous Monday to today
         *
         * @param weekStartDay the first day of the week (default: SUNDAY)
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun thisWeek(
            weekStartDay: DayOfWeek = DayOfWeek.SUNDAY,
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            val weekStart = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
            return of(
                startDate = weekStart,
                endDate = today,
                strict = false,
                searchPeriod = Period.ofWeeks(1),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the previous month (1st to last day).
         *
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun lastMonth(
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            val firstDayOfLastMonth = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
            val lastDayOfLastMonth = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            return of(
                startDate = firstDayOfLastMonth,
                endDate = lastDayOfLastMonth,
                strict = false,
                searchPeriod = Period.ofMonths(1),
                maxSearchPeriod = maxSearchPeriod,
            )
        }

        /**
         * Creates a [SearchDates] for the current month (1st to today).
         *
         * @param maxSearchPeriod the maximum allowed period (default: 1 month)
         * @return a new [SearchDates] instance
         */
        @JvmStatic
        @JvmOverloads
        fun thisMonth(
            maxSearchPeriod: Period = DEFAULT_MAX_SEARCH_PERIOD,
        ): SearchDates {
            val today = LocalDate.now()
            val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
            return of(
                startDate = firstDayOfMonth,
                endDate = today,
                strict = false,
                searchPeriod = Period.ofMonths(1),
                maxSearchPeriod = maxSearchPeriod,
            )
        }
    }

    // =============================================================================
    // Containment Checks
    // =============================================================================

    /**
     * Checks if the specified [date] is within this search range.
     *
     * Both start and end dates are included (closed interval).
     * Supports the `in` operator for idiomatic Kotlin usage.
     *
     * @param date the date to check
     * @return true if [date] is within [startDate, endDate], false otherwise
     */
    operator fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
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
        return Period.between(startDate, endDate)
    }

    /**
     * Returns the total number of days between start and end dates.
     *
     * @return the total days
     */
    fun daysBetween(): Long {
        return ChronoUnit.DAYS.between(startDate, endDate)
    }

    /**
     * Returns the maximum allowed search days based on [maxSearchPeriod].
     *
     * @return the maximum number of days allowed for search
     */
    fun maxSearchDays(): Long {
        return ChronoUnit.DAYS.between(REFERENCE_DATE, REFERENCE_DATE.plus(maxSearchPeriod))
    }

    /**
     * Checks if the current search range is within the maximum allowed period.
     *
     * @return true if current range does not exceed [maxSearchPeriod], false otherwise
     */
    fun isWithinMaxPeriod(): Boolean {
        return daysBetween() <= maxSearchDays()
    }

    // =============================================================================
    // Conversion
    // =============================================================================

    /**
     * Converts this [SearchDates] to a [LocalDateRange].
     *
     * @return a new [LocalDateRange] with the same start and end dates
     */
    fun toLocalDateRange(): LocalDateRange {
        return LocalDateRange.from(startDate, endDate)
    }

}
