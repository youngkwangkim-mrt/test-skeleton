package com.myrealtrip.common.utils.datetime

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@DisplayName("ZonedDateTimeRange")
class ZonedDateTimeRangeTest {

    private val seoulZone = ZoneId.of("Asia/Seoul")
    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")

    @Nested
    @DisplayName("Creation validation")
    inner class CreationValidationTest {

        @Test
        fun `should throw exception when start is after end`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 12, 31, 23, 59, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, seoulZone)

            // when & then
            assertThatThrownBy { ZonedDateTimeRange.from(start, end) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("start must be before or equal to end")
        }

        @Test
        fun `should create range when start equals end`(): Unit {
            // given
            val dateTime = ZonedDateTime.of(2025, 6, 15, 12, 30, 0, 0, seoulZone)

            // when
            val range = ZonedDateTimeRange.from(dateTime, dateTime)

            // then
            assertThat(range.start).isEqualTo(dateTime)
            assertThat(range.end).isEqualTo(dateTime)
        }

        @Test
        fun `should handle different time zones correctly`(): Unit {
            // given - Same instant in different zones
            val seoulTime = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val utcTime = seoulTime.withZoneSameInstant(utcZone) // 09:00 UTC

            // when
            val range = ZonedDateTimeRange.from(utcTime, seoulTime)

            // then - Same instant, so duration is zero
            assertThat(range.duration()).isEqualTo(Duration.ZERO)
        }
    }

    @Nested
    @DisplayName("Boundary containment with timezone")
    inner class BoundaryContainmentTest {

        @Test
        fun `should include start and end datetimes in range (closed interval)`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when & then
            assertThat(start in range).isTrue()
            assertThat(end in range).isTrue()
        }

        @Test
        fun `should exclude datetime one nanosecond before start`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when & then
            assertThat(start.minusNanos(1) in range).isFalse()
        }

        @Test
        fun `should check containment based on instant, not local time`(): Unit {
            // given - Range in Seoul time
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when - Check with same instant in UTC
            val checkTimeUtc = ZonedDateTime.of(2025, 1, 1, 3, 0, 0, 0, utcZone) // 12:00 Seoul

            // then
            assertThat(checkTimeUtc in range).isTrue()
        }
    }

    @Nested
    @DisplayName("Overlaps with timezone")
    inner class OverlapsEdgeCasesTest {

        @Test
        fun `should overlap when ranges touch at single instant`(): Unit {
            // given
            val touchPoint = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, seoulZone)
            val range1 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone),
                touchPoint
            )
            val range2 = ZonedDateTimeRange.from(
                touchPoint,
                ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isTrue()
        }

        @Test
        fun `should detect overlap across different time zones`(): Unit {
            // given
            val seoulRange = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone),
                ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            )
            // UTC 00:00-09:00 = Seoul 09:00-18:00
            val utcRange = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, utcZone),
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, utcZone)
            )

            // when & then
            assertThat(seoulRange.overlaps(utcRange)).isTrue()
        }

        @Test
        fun `should not overlap when ranges are one nanosecond apart`(): Unit {
            // given
            val range1 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone),
                ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, seoulZone)
            )
            val range2 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 1, seoulZone),
                ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            )

            // when & then
            assertThat(range1.overlaps(range2)).isFalse()
        }
    }

    @Nested
    @DisplayName("Intersect with timezone")
    inner class IntersectEdgeCasesTest {

        @Test
        fun `should return single instant intersection when ranges touch`(): Unit {
            // given
            val touchPoint = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, seoulZone)
            val range1 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone),
                touchPoint
            )
            val range2 = ZonedDateTimeRange.from(
                touchPoint,
                ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
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
        fun `should return null when ranges do not overlap`(): Unit {
            // given
            val range1 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone),
                ZonedDateTime.of(2025, 1, 1, 11, 59, 0, 0, seoulZone)
            )
            val range2 = ZonedDateTimeRange.from(
                ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, seoulZone),
                ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            )

            // when
            val intersection = range1.intersect(range2)

            // then
            assertThat(intersection).isNull()
        }
    }

    @Nested
    @DisplayName("Duration calculations")
    inner class DurationCalculationsTest {

        @Test
        fun `should return zero duration when start equals end`(): Unit {
            // given
            val dateTime = ZonedDateTime.of(2025, 6, 15, 12, 30, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(dateTime, dateTime)

            // when & then
            assertThat(range.duration()).isEqualTo(Duration.ZERO)
            assertThat(range.daysBetween()).isEqualTo(0L)
            assertThat(range.hoursBetween()).isEqualTo(0L)
        }

        @Test
        fun `should calculate correct duration for work day`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when
            val hours = range.hoursBetween()

            // then
            assertThat(hours).isEqualTo(9L)
        }

        @Test
        fun `should handle DST transition correctly`(): Unit {
            // given - Using a timezone with DST
            // Note: This test verifies the behavior, actual DST dates vary by year
            val beforeDst = ZonedDateTime.of(2025, 3, 9, 1, 0, 0, 0, newYorkZone)
            val afterDst = ZonedDateTime.of(2025, 3, 9, 3, 0, 0, 0, newYorkZone)
            val range = ZonedDateTimeRange.from(beforeDst, afterDst)

            // when
            val hours = range.hoursBetween()

            // then - During spring forward, 2:00 AM becomes 3:00 AM
            // So 1:00 to 3:00 is only 1 hour (not 2)
            assertThat(hours).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("Zone operations")
    inner class ZoneOperationsTest {

        @Test
        fun `timeOffset should return zero when same zone`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when
            val offset = range.timeOffset()

            // then
            assertThat(offset).isEqualTo(ZoneOffset.ofTotalSeconds(0))
        }

        @Test
        fun `toLocalDateTimeRange should strip timezone info`(): Unit {
            // given
            val start = ZonedDateTime.of(2025, 1, 1, 9, 0, 0, 0, seoulZone)
            val end = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, seoulZone)
            val range = ZonedDateTimeRange.from(start, end)

            // when
            val localRange = range.toLocalDateTimeRange()

            // then
            assertThat(localRange.start).isEqualTo(start.toLocalDateTime())
            assertThat(localRange.end).isEqualTo(end.toLocalDateTime())
        }
    }
}
