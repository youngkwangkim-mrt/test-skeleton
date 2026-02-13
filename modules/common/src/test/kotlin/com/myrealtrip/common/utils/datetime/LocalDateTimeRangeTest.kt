package com.myrealtrip.common.utils.datetime

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

@DisplayName("LocalDateTimeRange")
class LocalDateTimeRangeTest {

    @Nested
    @DisplayName("Creation validation")
    inner class CreationValidationTest {

        @Test
        fun `should throw exception when start is after end`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 12, 31, 23, 59)
            val end = LocalDateTime.of(2025, 1, 1, 0, 0)

            // when & then
            assertThatThrownBy { LocalDateTimeRange.from(start, end) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("start must be before or equal to end")
        }

        @Test
        fun `should throw exception when start is one second after end`(): Unit {
            // given
            val end = LocalDateTime.of(2025, 6, 15, 12, 0, 0)
            val start = end.plusSeconds(1)

            // when & then
            assertThatThrownBy { LocalDateTimeRange.from(start, end) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should create range when start equals end`(): Unit {
            // given
            val dateTime = LocalDateTime.of(2025, 6, 15, 12, 30, 45)

            // when
            val range = LocalDateTimeRange.from(dateTime, dateTime)

            // then
            assertThat(range.start).isEqualTo(dateTime)
            assertThat(range.end).isEqualTo(dateTime)
        }
    }

    @Nested
    @DisplayName("Boundary containment with nanosecond precision")
    inner class BoundaryContainmentTest {

        @Test
        fun `should include start and end datetimes in range (closed interval)`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 1, 9, 0, 0, 0)
            val end = LocalDateTime.of(2025, 1, 1, 18, 0, 0, 0)
            val range = LocalDateTimeRange.from(start, end)

            // when & then
            assertThat(start in range).isTrue()
            assertThat(end in range).isTrue()
        }

        @Test
        fun `should exclude datetimes one nanosecond outside boundaries`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 1, 9, 0, 0, 0)
            val end = LocalDateTime.of(2025, 1, 1, 18, 0, 0, 0)
            val range = LocalDateTimeRange.from(start, end)

            // when & then
            assertThat(start.minusNanos(1) in range).isFalse()
            assertThat(end.plusNanos(1) in range).isFalse()
        }

        @Test
        fun `should handle midnight boundary correctly`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 1, 23, 59, 59)
            val end = LocalDateTime.of(2025, 1, 2, 0, 0, 1)
            val range = LocalDateTimeRange.from(start, end)
            val midnight = LocalDateTime.of(2025, 1, 2, 0, 0, 0)

            // when & then
            assertThat(midnight in range).isTrue()
        }
    }

    @Nested
    @DisplayName("Overlaps with time precision")
    inner class OverlapsEdgeCasesTest {

        @Test
        fun `should overlap when ranges touch at single instant`(): Unit {
            // given
            val range1 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0)
            )
            val range2 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                LocalDateTime.of(2025, 1, 1, 18, 0)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
        }

        @Test
        fun `should not overlap when ranges are one nanosecond apart`(): Unit {
            // given
            val range1 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 9, 0, 0, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0, 0, 0)
            )
            val range2 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 12, 0, 0, 1),
                LocalDateTime.of(2025, 1, 1, 18, 0, 0, 0)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isFalse()
        }

        @Test
        fun `should overlap across day boundary`(): Unit {
            // given
            val range1 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 20, 0),
                LocalDateTime.of(2025, 1, 2, 4, 0)
            )
            val range2 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 2, 0, 0),
                LocalDateTime.of(2025, 1, 2, 8, 0)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
        }
    }

    @Nested
    @DisplayName("Intersect with time precision")
    inner class IntersectEdgeCasesTest {

        @Test
        fun `should return single instant intersection when ranges touch`(): Unit {
            // given
            val touchPoint = LocalDateTime.of(2025, 1, 1, 12, 0)
            val range1 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                touchPoint
            )
            val range2 = LocalDateTimeRange.from(
                touchPoint,
                LocalDateTime.of(2025, 1, 1, 18, 0)
            )

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNotNull
            assertThat(intersection!!.start).isEqualTo(touchPoint)
            assertThat(intersection.end).isEqualTo(touchPoint)
            assertThat(intersection.duration()).isEqualTo(Duration.ZERO)
        }

        @Test
        fun `should return correct overlap period`(): Unit {
            // given
            val range1 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 14, 0)
            )
            val range2 = LocalDateTimeRange.from(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                LocalDateTime.of(2025, 1, 1, 18, 0)
            )

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNotNull
            assertThat(intersection!!.start).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0))
            assertThat(intersection.end).isEqualTo(LocalDateTime.of(2025, 1, 1, 14, 0))
            assertThat(intersection.hoursBetween()).isEqualTo(2L)
        }
    }

    @Nested
    @DisplayName("Duration calculations")
    inner class DurationCalculationsTest {

        @Test
        fun `should return zero duration when start equals end`(): Unit {
            // given
            val dateTime = LocalDateTime.of(2025, 6, 15, 12, 30)
            val range = LocalDateTimeRange.from(dateTime, dateTime)

            // when & then
            assertThat(range.duration()).isEqualTo(Duration.ZERO)
            assertThat(range.daysBetween()).isEqualTo(0L)
            assertThat(range.hoursBetween()).isEqualTo(0L)
            assertThat(range.minutesBetween()).isEqualTo(0L)
            assertThat(range.secondsBetween()).isEqualTo(0L)
        }

        @Test
        fun `should truncate partial hours`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 1, 10, 0)
            val end = LocalDateTime.of(2025, 1, 1, 12, 59)
            val range = LocalDateTimeRange.from(start, end)

            // when
            val hours = range.hoursBetween()

            // then
            assertThat(hours).isEqualTo(2L) // not 3
        }

        @Test
        fun `should calculate correct minutes for partial hour`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 1, 10, 15)
            val end = LocalDateTime.of(2025, 1, 1, 10, 45)
            val range = LocalDateTimeRange.from(start, end)

            // when
            val minutes = range.minutesBetween()

            // then
            assertThat(minutes).isEqualTo(30L)
        }

        @Test
        fun `should calculate days across month boundary`(): Unit {
            // given
            val start = LocalDateTime.of(2025, 1, 31, 0, 0)
            val end = LocalDateTime.of(2025, 2, 2, 0, 0)
            val range = LocalDateTimeRange.from(start, end)

            // when
            val days = range.daysBetween()

            // then
            assertThat(days).isEqualTo(2L)
        }
    }
}
