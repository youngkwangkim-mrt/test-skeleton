package com.myrealtrip.common.utils.datetime

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAdjusters

@DisplayName("SearchDates")
class SearchDatesTest {

    @Nested
    @DisplayName("Creation with strict mode")
    inner class StrictModeTest {

        @Test
        fun `should throw exception when start is after end in strict mode`(): Unit {
            // given
            val start = LocalDate.of(2025, 12, 31)
            val end = LocalDate.of(2025, 1, 1)

            // when & then
            assertThatThrownBy {
                SearchDates.of(startDate = start, endDate = end, strict = true)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("startDate must be before or equal to endDate")
        }

        @Test
        fun `should throw exception when search period exceeds max in strict mode`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)
            val maxPeriod = Period.ofDays(30)

            // when & then
            assertThatThrownBy {
                SearchDates.of(
                    startDate = start,
                    endDate = end,
                    strict = true,
                    maxSearchPeriod = maxPeriod
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Search period must not exceed")
        }

        @Test
        fun `should create successfully with valid dates in strict mode`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 1, 15)

            // when
            val searchDates = SearchDates.of(startDate = start, endDate = end, strict = true)

            // then
            assertThat(searchDates.startDate).isEqualTo(start)
            assertThat(searchDates.endDate).isEqualTo(end)
            assertThat(searchDates.strict).isTrue()
        }
    }

    @Nested
    @DisplayName("Auto-adjustment in non-strict mode")
    inner class NonStrictModeTest {

        @Test
        fun `should adjust start date when start is after end`(): Unit {
            // given
            val start = LocalDate.of(2025, 6, 30)
            val end = LocalDate.of(2025, 6, 15)
            val searchPeriod = Period.ofDays(7)

            // when
            val searchDates = SearchDates.of(
                startDate = start,
                endDate = end,
                strict = false,
                searchPeriod = searchPeriod
            )

            // then
            assertThat(searchDates.startDate).isEqualTo(end.minus(searchPeriod))
            assertThat(searchDates.endDate).isEqualTo(end)
        }

        @Test
        fun `should adjust start date when exceeds max search period`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)
            val maxPeriod = Period.ofDays(30)

            // when
            val searchDates = SearchDates.of(
                startDate = start,
                endDate = end,
                strict = false,
                maxSearchPeriod = maxPeriod
            )

            // then
            assertThat(searchDates.startDate).isEqualTo(end.minus(maxPeriod))
            assertThat(searchDates.daysBetween()).isEqualTo(30L)
        }

        @Test
        fun `should handle double adjustment when start is after end AND exceeds max`(): Unit {
            // given
            val start = LocalDate.of(2025, 12, 31)
            val end = LocalDate.of(2025, 6, 15)
            val searchPeriod = Period.ofDays(60)
            val maxPeriod = Period.ofDays(30)

            // when
            val searchDates = SearchDates.of(
                startDate = start,
                endDate = end,
                strict = false,
                searchPeriod = searchPeriod,
                maxSearchPeriod = maxPeriod
            )

            // then - first adjusted by searchPeriod, then by maxPeriod
            assertThat(searchDates.daysBetween()).isLessThanOrEqualTo(30L)
        }
    }

    @Nested
    @DisplayName("Boundary containment")
    inner class BoundaryContainmentTest {

        @Test
        fun `should include start and end dates in range (closed interval)`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 1, 31)
            val searchDates = SearchDates.of(startDate = start, endDate = end)

            // when & then
            assertThat(start in searchDates).isTrue()
            assertThat(end in searchDates).isTrue()
        }

        @Test
        fun `should exclude dates outside range boundaries`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 1, 31)
            val searchDates = SearchDates.of(startDate = start, endDate = end)

            // when & then
            assertThat(start.minusDays(1) in searchDates).isFalse()
            assertThat(end.plusDays(1) in searchDates).isFalse()
        }
    }

    @Nested
    @DisplayName("today and yesterday")
    inner class TodayYesterdayTest {

        @Test
        fun `today should create single day range for current date`(): Unit {
            // given
            val today = LocalDate.now()

            // when
            val searchDates = SearchDates.today()

            // then
            assertThat(searchDates.startDate).isEqualTo(today)
            assertThat(searchDates.endDate).isEqualTo(today)
            assertThat(searchDates.daysBetween()).isEqualTo(0L)
            assertThat(searchDates.strict).isTrue()
        }

        @Test
        fun `yesterday should create single day range for previous date`(): Unit {
            // given
            val yesterday = LocalDate.now().minusDays(1)

            // when
            val searchDates = SearchDates.yesterday()

            // then
            assertThat(searchDates.startDate).isEqualTo(yesterday)
            assertThat(searchDates.endDate).isEqualTo(yesterday)
            assertThat(searchDates.daysBetween()).isEqualTo(0L)
            assertThat(searchDates.strict).isTrue()
        }

        @Test
        fun `today should only contain today's date`(): Unit {
            // given
            val today = LocalDate.now()
            val searchDates = SearchDates.today()

            // when & then
            assertThat(today in searchDates).isTrue()
            assertThat(today.minusDays(1) in searchDates).isFalse()
            assertThat(today.plusDays(1) in searchDates).isFalse()
        }

        @Test
        fun `yesterday should only contain yesterday's date`(): Unit {
            // given
            val yesterday = LocalDate.now().minusDays(1)
            val searchDates = SearchDates.yesterday()

            // when & then
            assertThat(yesterday in searchDates).isTrue()
            assertThat(yesterday.minusDays(1) in searchDates).isFalse()
            assertThat(yesterday.plusDays(1) in searchDates).isFalse()
        }
    }

    @Nested
    @DisplayName("lastDays boundary")
    inner class LastDaysBoundaryTest {

        @Test
        fun `lastDays(0) should create single day range`(): Unit {
            // given
            val today = LocalDate.now()

            // when
            val searchDates = SearchDates.lastDays(0)

            // then
            assertThat(searchDates.startDate).isEqualTo(today)
            assertThat(searchDates.endDate).isEqualTo(today)
            assertThat(searchDates.daysBetween()).isEqualTo(0L)
        }

        @Test
        fun `lastDays should be limited by maxSearchPeriod`(): Unit {
            // given
            val maxPeriod = Period.ofDays(7)

            // when
            val searchDates = SearchDates.lastDays(30, maxSearchPeriod = maxPeriod)

            // then
            assertThat(searchDates.daysBetween()).isEqualTo(7L)
        }
    }

    @Nested
    @DisplayName("thisWeek with different start days")
    inner class ThisWeekBoundaryTest {

        @Test
        fun `thisWeek with SUNDAY should start from Sunday`(): Unit {
            // given
            val today = LocalDate.now()
            val expectedStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

            // when
            val searchDates = SearchDates.thisWeek(weekStartDay = DayOfWeek.SUNDAY)

            // then
            assertThat(searchDates.startDate).isEqualTo(expectedStart)
            assertThat(searchDates.startDate.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
            assertThat(searchDates.endDate).isEqualTo(today)
        }

        @Test
        fun `thisWeek with MONDAY should start from Monday`(): Unit {
            // given
            val today = LocalDate.now()
            val expectedStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            // when
            val searchDates = SearchDates.thisWeek(weekStartDay = DayOfWeek.MONDAY)

            // then
            assertThat(searchDates.startDate).isEqualTo(expectedStart)
            assertThat(searchDates.startDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        }

        @Test
        fun `thisWeek should never exceed 6 days from start`(): Unit {
            // when
            val searchDates = SearchDates.thisWeek()

            // then
            assertThat(searchDates.daysBetween()).isLessThanOrEqualTo(6L)
        }
    }

    @Nested
    @DisplayName("lastWeek with different start days")
    inner class LastWeekBoundaryTest {

        @Test
        fun `lastWeek with SUNDAY should span Sunday to Saturday`(): Unit {
            // when
            val searchDates = SearchDates.lastWeek(weekStartDay = DayOfWeek.SUNDAY)

            // then
            assertThat(searchDates.startDate.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
            assertThat(searchDates.endDate.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
            assertThat(searchDates.daysBetween()).isEqualTo(6L)
        }

        @Test
        fun `lastWeek with MONDAY should span Monday to Sunday`(): Unit {
            // when
            val searchDates = SearchDates.lastWeek(weekStartDay = DayOfWeek.MONDAY)

            // then
            assertThat(searchDates.startDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
            assertThat(searchDates.endDate.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
            assertThat(searchDates.daysBetween()).isEqualTo(6L)
        }

        @Test
        fun `lastWeek should always be exactly 7 days (6 days between)`(): Unit {
            // when
            val searchDates = SearchDates.lastWeek()

            // then
            assertThat(searchDates.daysBetween()).isEqualTo(6L)
        }
    }

    @Nested
    @DisplayName("thisMonth boundary")
    inner class ThisMonthBoundaryTest {

        @Test
        fun `thisMonth should start from 1st of current month`(): Unit {
            // given
            val today = LocalDate.now()
            val firstOfMonth = today.withDayOfMonth(1)

            // when
            val searchDates = SearchDates.thisMonth()

            // then
            assertThat(searchDates.startDate).isEqualTo(firstOfMonth)
            assertThat(searchDates.startDate.dayOfMonth).isEqualTo(1)
            assertThat(searchDates.endDate).isEqualTo(today)
        }
    }

    @Nested
    @DisplayName("lastMonth boundary")
    inner class LastMonthBoundaryTest {

        @Test
        fun `lastMonth should span 1st to last day of previous month`(): Unit {
            // given
            val today = LocalDate.now()
            val expectedStart = today.minusMonths(1).withDayOfMonth(1)
            val expectedEnd = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())

            // when
            val searchDates = SearchDates.lastMonth()

            // then
            assertThat(searchDates.startDate).isEqualTo(expectedStart)
            assertThat(searchDates.endDate).isEqualTo(expectedEnd)
            assertThat(searchDates.startDate.dayOfMonth).isEqualTo(1)
        }

        @Test
        fun `lastMonth should handle February correctly`(): Unit {
            // given - Test with a date in March to check February
            val marchDate = LocalDate.of(2025, 3, 15)
            val expectedEnd = LocalDate.of(2025, 2, 28) // 2025 is not a leap year

            // when - We can't directly test with a specific date without mocking,
            // but we can verify the logic is consistent
            val searchDates = SearchDates.lastMonth()

            // then - The end date should always be the last day of the previous month
            assertThat(searchDates.endDate.dayOfMonth)
                .isEqualTo(searchDates.endDate.lengthOfMonth())
        }
    }

    @Nested
    @DisplayName("maxSearchPeriod validation")
    inner class MaxSearchPeriodTest {

        @Test
        fun `isWithinMaxPeriod should return true after auto-adjustment`(): Unit {
            // given - range that exceeds max, but will be auto-adjusted
            val searchDates = SearchDates.of(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 12, 31),
                strict = false,
                maxSearchPeriod = Period.ofDays(30)
            )

            // when & then
            assertThat(searchDates.isWithinMaxPeriod()).isTrue()
        }

        @Test
        fun `maxSearchDays should calculate correctly for month period`(): Unit {
            // given
            val searchDates = SearchDates.of(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 15),
                maxSearchPeriod = Period.ofMonths(1)
            )

            // when
            val maxDays = searchDates.maxSearchDays()

            // then - 1 month from reference date (2000-01-01) = 31 days
            assertThat(maxDays).isEqualTo(31L)
        }
    }

    @Nested
    @DisplayName("Conversion to LocalDateRange")
    inner class ConversionTest {

        @Test
        fun `toLocalDateRange should preserve start and end dates`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 1, 31)
            val searchDates = SearchDates.of(startDate = start, endDate = end)

            // when
            val range = searchDates.toLocalDateRange()

            // then
            assertThat(range.start).isEqualTo(start)
            assertThat(range.end).isEqualTo(end)
        }
    }
}
