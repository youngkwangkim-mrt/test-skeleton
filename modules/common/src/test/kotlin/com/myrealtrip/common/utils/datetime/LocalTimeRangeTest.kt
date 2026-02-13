package com.myrealtrip.common.utils.datetime

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalTime

@DisplayName("LocalTimeRange")
class LocalTimeRangeTest {

    @Nested
    @DisplayName("Creation validation")
    inner class CreationValidationTest {

        @Test
        fun `should throw exception when start is after end`(): Unit {
            // given
            val start = LocalTime.of(18, 0)
            val end = LocalTime.of(9, 0)

            // when & then
            assertThatThrownBy { LocalTimeRange.from(start, end) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("start must be before or equal to end")
        }

        @Test
        fun `should create range when start equals end`(): Unit {
            // given
            val time = LocalTime.of(12, 30, 45)

            // when
            val range = LocalTimeRange.from(time, time)

            // then
            assertThat(range.start).isEqualTo(time)
            assertThat(range.end).isEqualTo(time)
        }

        @Test
        fun `should handle midnight as start`(): Unit {
            // given
            val start = LocalTime.MIDNIGHT
            val end = LocalTime.of(8, 0)

            // when
            val range = LocalTimeRange.from(start, end)

            // then
            assertThat(range.start).isEqualTo(LocalTime.MIDNIGHT)
            assertThat(range.hoursBetween()).isEqualTo(8L)
        }

        @Test
        fun `should handle max time boundary`(): Unit {
            // given
            val start = LocalTime.of(23, 0)
            val end = LocalTime.MAX // 23:59:59.999999999

            // when
            val range = LocalTimeRange.from(start, end)

            // then
            assertThat(range.end).isEqualTo(LocalTime.MAX)
        }
    }

    @Nested
    @DisplayName("Boundary containment")
    inner class BoundaryContainmentTest {

        @Test
        fun `should include start and end times in range (closed interval)`(): Unit {
            // given
            val start = LocalTime.of(9, 0, 0, 0)
            val end = LocalTime.of(18, 0, 0, 0)
            val range = LocalTimeRange.from(start, end)

            // when & then
            assertThat(start in range).isTrue()
            assertThat(end in range).isTrue()
        }

        @Test
        fun `should exclude times one nanosecond outside boundaries`(): Unit {
            // given
            val start = LocalTime.of(9, 0, 0, 0)
            val end = LocalTime.of(18, 0, 0, 0)
            val range = LocalTimeRange.from(start, end)

            // when & then
            assertThat(start.minusNanos(1) in range).isFalse()
            assertThat(end.plusNanos(1) in range).isFalse()
        }

        @Test
        fun `should contain single instant when start equals end`(): Unit {
            // given
            val time = LocalTime.of(12, 30)
            val range = LocalTimeRange.from(time, time)

            // when & then
            assertThat(time in range).isTrue()
            assertThat(time.minusNanos(1) in range).isFalse()
            assertThat(time.plusNanos(1) in range).isFalse()
        }
    }

    @Nested
    @DisplayName("Overlaps edge cases")
    inner class OverlapsEdgeCasesTest {

        @Test
        fun `should overlap when ranges touch at single instant`(): Unit {
            // given
            val range1 = LocalTimeRange.from(LocalTime.of(9, 0), LocalTime.of(12, 0))
            val range2 = LocalTimeRange.from(LocalTime.of(12, 0), LocalTime.of(18, 0))

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
        }

        @Test
        fun `should not overlap when ranges are one nanosecond apart`(): Unit {
            // given
            val range1 = LocalTimeRange.from(
                LocalTime.of(9, 0, 0, 0),
                LocalTime.of(12, 0, 0, 0)
            )
            val range2 = LocalTimeRange.from(
                LocalTime.of(12, 0, 0, 1),
                LocalTime.of(18, 0, 0, 0)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isFalse()
        }

        @Test
        fun `should overlap with itself`(): Unit {
            // given
            val range = LocalTimeRange.from(LocalTime.of(9, 0), LocalTime.of(18, 0))

            // when & then
            assertThat(range.overlaps(range)).isTrue()
        }

        @Test
        fun `should detect partial overlap`(): Unit {
            // given
            val range1 = LocalTimeRange.from(LocalTime.of(9, 0), LocalTime.of(14, 0))
            val range2 = LocalTimeRange.from(LocalTime.of(12, 0), LocalTime.of(18, 0))

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
            assertThat(range2.overlaps(range1)).isTrue()
        }
    }

    @Nested
    @DisplayName("Intersect edge cases")
    inner class IntersectEdgeCasesTest {

        @Test
        fun `should return single instant intersection when ranges touch`(): Unit {
            // given
            val touchPoint = LocalTime.of(12, 0)
            val range1 = LocalTimeRange.from(LocalTime.of(9, 0), touchPoint)
            val range2 = LocalTimeRange.from(touchPoint, LocalTime.of(18, 0))

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNotNull
            assertThat(intersection!!.start).isEqualTo(touchPoint)
            assertThat(intersection.end).isEqualTo(touchPoint)
            assertThat(intersection.duration()).isEqualTo(Duration.ZERO)
        }

        @Test
        fun `should return null when ranges do not overlap`(): Unit {
            // given
            val range1 = LocalTimeRange.from(LocalTime.of(9, 0), LocalTime.of(11, 59))
            val range2 = LocalTimeRange.from(LocalTime.of(12, 0), LocalTime.of(18, 0))

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNull()
        }

        @Test
        fun `should return inner range when one contains another`(): Unit {
            // given
            val outer = LocalTimeRange.from(LocalTime.of(8, 0), LocalTime.of(20, 0))
            val inner = LocalTimeRange.from(LocalTime.of(10, 0), LocalTime.of(16, 0))

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
        fun `should return zero duration when start equals end`(): Unit {
            // given
            val time = LocalTime.of(12, 30)
            val range = LocalTimeRange.from(time, time)

            // when & then
            assertThat(range.duration()).isEqualTo(Duration.ZERO)
            assertThat(range.hoursBetween()).isEqualTo(0L)
            assertThat(range.minutesBetween()).isEqualTo(0L)
            assertThat(range.secondsBetween()).isEqualTo(0L)
        }

        @Test
        fun `should calculate correct hours for work day`(): Unit {
            // given
            val start = LocalTime.of(9, 0)
            val end = LocalTime.of(18, 0)
            val range = LocalTimeRange.from(start, end)

            // when
            val hours = range.hoursBetween()

            // then
            assertThat(hours).isEqualTo(9L)
        }

        @Test
        fun `should truncate partial hours`(): Unit {
            // given
            val start = LocalTime.of(9, 0)
            val end = LocalTime.of(11, 59, 59)
            val range = LocalTimeRange.from(start, end)

            // when
            val hours = range.hoursBetween()

            // then
            assertThat(hours).isEqualTo(2L) // not 3
        }

        @Test
        fun `should calculate correct minutes for partial hour`(): Unit {
            // given
            val start = LocalTime.of(10, 15)
            val end = LocalTime.of(10, 45)
            val range = LocalTimeRange.from(start, end)

            // when
            val minutes = range.minutesBetween()

            // then
            assertThat(minutes).isEqualTo(30L)
        }

        @Test
        fun `should calculate full day duration`(): Unit {
            // given
            val start = LocalTime.MIDNIGHT
            val end = LocalTime.of(23, 59, 59)
            val range = LocalTimeRange.from(start, end)

            // when
            val hours = range.hoursBetween()
            val minutes = range.minutesBetween()

            // then
            assertThat(hours).isEqualTo(23L)
            assertThat(minutes).isEqualTo(23 * 60 + 59)
        }
    }
}
