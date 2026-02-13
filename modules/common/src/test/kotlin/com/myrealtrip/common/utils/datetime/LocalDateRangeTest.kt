package com.myrealtrip.common.utils.datetime

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

@DisplayName("LocalDateRange")
class LocalDateRangeTest {

    @Nested
    @DisplayName("Creation validation")
    inner class CreationValidationTest {

        @Test
        fun `should throw exception when start is after end`(): Unit {
            // given
            val start = LocalDate.of(2025, 12, 31)
            val end = LocalDate.of(2025, 1, 1)

            // when & then
            assertThatThrownBy { LocalDateRange.from(start, end) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("start must be before or equal to end")
        }

        @Test
        fun `should create range when start equals end`(): Unit {
            // given
            val date = LocalDate.of(2025, 6, 15)

            // when
            val range = LocalDateRange.from(date, date)

            // then
            assertThat(range.start).isEqualTo(date)
            assertThat(range.end).isEqualTo(date)
        }

        @Test
        fun `should create range with invoke operator`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)

            // when
            val range = LocalDateRange(start, end)

            // then
            assertThat(range.start).isEqualTo(start)
            assertThat(range.end).isEqualTo(end)
        }
    }

    @Nested
    @DisplayName("Boundary containment")
    inner class BoundaryContainmentTest {

        @Test
        fun `should include start and end dates in range (closed interval)`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)
            val range = LocalDateRange.from(start, end)

            // when & then
            assertThat(start in range).isTrue()
            assertThat(end in range).isTrue()
        }

        @Test
        fun `should exclude dates outside range boundaries`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)
            val range = LocalDateRange.from(start, end)

            // when & then
            assertThat(start.minusDays(1) in range).isFalse()
            assertThat(end.plusDays(1) in range).isFalse()
        }

        @Test
        fun `should contain single date when start equals end`(): Unit {
            // given
            val date = LocalDate.of(2025, 6, 15)
            val range = LocalDateRange.from(date, date)

            // when & then
            assertThat(date in range).isTrue()
            assertThat(date.minusDays(1) in range).isFalse()
            assertThat(date.plusDays(1) in range).isFalse()
        }
    }

    @Nested
    @DisplayName("Overlaps edge cases")
    inner class OverlapsEdgeCasesTest {

        @Test
        fun `should overlap when ranges touch at single point`(): Unit {
            // given
            val range1 = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 30)
            )
            val range2 = LocalDateRange.from(
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
            assertThat(range2.overlaps(range1)).isTrue()
        }

        @Test
        fun `should not overlap when ranges are adjacent but not touching`(): Unit {
            // given
            val range1 = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 29)
            )
            val range2 = LocalDateRange.from(
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isFalse()
            assertThat(range2.overlaps(range1)).isFalse()
        }

        @Test
        fun `should overlap when one range contains another`(): Unit {
            // given
            val outer = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
            )
            val inner = LocalDateRange.from(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 9, 30)
            )

            // when & then
            assertThat(outer.overlaps(inner)).isTrue()
            assertThat(inner.overlaps(outer)).isTrue()
        }

        @Test
        fun `should overlap with itself`(): Unit {
            // given
            val range = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
            )

            // when & then
            assertThat(range.overlaps(range)).isTrue()
        }
    }

    @Nested
    @DisplayName("Intersect edge cases")
    inner class IntersectEdgeCasesTest {

        @Test
        fun `should return single point intersection when ranges touch at boundary`(): Unit {
            // given
            val range1 = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 30)
            )
            val range2 = LocalDateRange.from(
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
            )

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNotNull
            assertThat(intersection!!.start).isEqualTo(LocalDate.of(2025, 6, 30))
            assertThat(intersection.end).isEqualTo(LocalDate.of(2025, 6, 30))
            assertThat(intersection.daysBetween()).isEqualTo(0L)
        }

        @Test
        fun `should return null when ranges do not overlap`(): Unit {
            // given
            val range1 = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 29)
            )
            val range2 = LocalDateRange.from(
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
            )

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNull()
        }

        @Test
        fun `should return inner range when one contains another`(): Unit {
            // given
            val outer = LocalDateRange.from(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
            )
            val inner = LocalDateRange.from(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 9, 30)
            )

            // when
            val intersection = outer.intersect(inner)

            // then
            assertThat(intersection).isEqualTo(inner)
        }
    }

    @Nested
    @DisplayName("Duration calculations")
    inner class DurationCalculationsTest {

        @Test
        fun `should return zero days when start equals end`(): Unit {
            // given
            val date = LocalDate.of(2025, 6, 15)
            val range = LocalDateRange.from(date, date)

            // when & then
            assertThat(range.daysBetween()).isEqualTo(0L)
            assertThat(range.period()).isEqualTo(Period.ZERO)
        }

        @Test
        fun `should calculate correct days for year span`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)
            val range = LocalDateRange.from(start, end)

            // when
            val days = range.daysBetween()

            // then
            assertThat(days).isEqualTo(364L)
        }

        @Test
        fun `should handle leap year correctly`(): Unit {
            // given - 2024 is a leap year
            val start = LocalDate.of(2024, 1, 1)
            val end = LocalDate.of(2024, 12, 31)
            val range = LocalDateRange.from(start, end)

            // when
            val days = range.daysBetween()

            // then
            assertThat(days).isEqualTo(365L)
        }

        @Test
        fun `should calculate correct months between dates`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 15)
            val end = LocalDate.of(2025, 4, 15)
            val range = LocalDateRange.from(start, end)

            // when
            val months = range.monthsBetween()

            // then
            assertThat(months).isEqualTo(3L)
        }

        @Test
        fun `should truncate partial months`(): Unit {
            // given
            val start = LocalDate.of(2025, 1, 15)
            val end = LocalDate.of(2025, 4, 14)
            val range = LocalDateRange.from(start, end)

            // when
            val months = range.monthsBetween()

            // then
            assertThat(months).isEqualTo(2L)
        }
    }
}
