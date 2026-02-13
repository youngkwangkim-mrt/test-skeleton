package com.myrealtrip.common.utils.datetime

import com.myrealtrip.common.utils.datetime.DateFormatter.numericToDate
import com.myrealtrip.common.utils.datetime.DateFormatter.numericToDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.numericToTime
import com.myrealtrip.common.utils.datetime.DateFormatter.parseOffset
import com.myrealtrip.common.utils.datetime.DateFormatter.toDate
import com.myrealtrip.common.utils.datetime.DateFormatter.toDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toDayOfWeek
import com.myrealtrip.common.utils.datetime.DateFormatter.toKorean
import com.myrealtrip.common.utils.datetime.DateFormatter.toLocalDate
import com.myrealtrip.common.utils.datetime.DateFormatter.toLocalDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toNumericStr
import com.myrealtrip.common.utils.datetime.DateFormatter.toStr
import com.myrealtrip.common.utils.datetime.DateFormatter.toThreeLetter
import com.myrealtrip.common.utils.datetime.DateFormatter.toTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toTwoLetter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.*
import java.time.format.DateTimeParseException
import javax.xml.datatype.DatatypeFactory

@DisplayName("DateFormatter")
class DateFormatterTest {

    @Nested
    @DisplayName("String to LocalTime parsing")
    inner class StringToLocalTimeParsingTest {

        @Test
        fun `should parse time string with default format`() {
            // given
            val timeStr = "14:30:45"

            // when
            val result = timeStr.toTime()

            // then
            assertThat(result).isEqualTo(LocalTime.of(14, 30, 45))
        }

        @Test
        fun `should parse time string with custom format`() {
            // given
            val timeStr = "02:30 PM"
            val format = "hh:mm a"

            // when
            val result = timeStr.toTime(format)

            // then
            assertThat(result).isEqualTo(LocalTime.of(14, 30, 0))
        }

        @Test
        fun `should parse numeric time strings`() {
            // given
            val sixDigitTime = "143045"
            val fourDigitTime = "1430"

            // when
            val resultSixDigit = sixDigitTime.numericToTime()
            val resultFourDigit = fourDigitTime.numericToTime()

            // then
            assertThat(resultSixDigit).isEqualTo(LocalTime.of(14, 30, 45))
            assertThat(resultFourDigit).isEqualTo(LocalTime.of(14, 30, 0))
        }

        @Test
        fun `should throw exception for invalid time string`() {
            // given
            val invalidTime = "invalid"

            // when & then
            assertThatThrownBy { invalidTime.toTime() }
                .isInstanceOf(DateTimeParseException::class.java)
        }
    }

    @Nested
    @DisplayName("String to LocalDate parsing")
    inner class StringToLocalDateParsingTest {

        @Test
        fun `should parse date string with default format`() {
            // given
            val dateStr = "2025-12-05"

            // when
            val result = dateStr.toDate()

            // then
            assertThat(result).isEqualTo(LocalDate.of(2025, 12, 5))
        }

        @Test
        fun `should parse date string with custom format`() {
            // given
            val dateStr = "05/12/2025"
            val format = "dd/MM/yyyy"

            // when
            val result = dateStr.toDate(format)

            // then
            assertThat(result).isEqualTo(LocalDate.of(2025, 12, 5))
        }

        @Test
        fun `should parse numeric date string`() {
            // given
            val numericDate = "20251205"

            // when
            val result = numericDate.numericToDate()

            // then
            assertThat(result).isEqualTo(LocalDate.of(2025, 12, 5))
        }

        @Test
        fun `should throw exception for invalid date string`() {
            // given
            val invalidDate = "invalid"

            // when & then
            assertThatThrownBy { invalidDate.toDate() }
                .isInstanceOf(DateTimeParseException::class.java)
        }
    }

    @Nested
    @DisplayName("String to LocalDateTime parsing")
    inner class StringToLocalDateTimeParsingTest {

        @Test
        fun `should parse datetime string with default format`() {
            // given
            val dateTimeStr = "2025-12-05T14:30:45"

            // when
            val result = dateTimeStr.toDateTime()

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 30, 45))
        }

        @Test
        fun `should parse datetime string with custom format`() {
            // given
            val dateTimeStr = "05/12/2025 14:30:45"
            val format = "dd/MM/yyyy HH:mm:ss"

            // when
            val result = dateTimeStr.toDateTime(format)

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 30, 45))
        }

        @Test
        fun `should parse numeric datetime strings with various lengths`() {
            // given
            val fullNumeric = "20251205143045"
            val dateOnly = "20251205"
            val dateWithHourMin = "2025120514"
            val dateWithHour = "202512051430"

            // when
            val resultFull = fullNumeric.numericToDateTime()
            val resultDateOnly = dateOnly.numericToDateTime()
            val resultDateWithHourMin = dateWithHourMin.numericToDateTime()
            val resultDateWithHour = dateWithHour.numericToDateTime()

            // then
            assertThat(resultFull).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 30, 45))
            assertThat(resultDateOnly).isEqualTo(LocalDateTime.of(2025, 12, 5, 0, 0, 0))
            assertThat(resultDateWithHourMin).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 0, 0))
            assertThat(resultDateWithHour).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 30, 0))
        }

        @Test
        fun `should throw exception for invalid datetime string`() {
            // given
            val invalidDateTime = "invalid"

            // when & then
            assertThatThrownBy { invalidDateTime.toDateTime() }
                .isInstanceOf(DateTimeParseException::class.java)
        }
    }

    @Nested
    @DisplayName("LocalTime to String formatting")
    inner class LocalTimeToStringFormattingTest {

        @Test
        fun `should format time with default and custom formats`() {
            // given
            val time = LocalTime.of(14, 30, 45)

            // when
            val defaultResult = time.toStr()
            val customResult = time.toStr("HH:mm")

            // then
            assertThat(defaultResult).isEqualTo("14:30:45")
            assertThat(customResult).isEqualTo("14:30")
        }

        @Test
        fun `should format time to numeric string`() {
            // given
            val time = LocalTime.of(14, 30, 45)

            // when
            val result = time.toNumericStr()

            // then
            assertThat(result).isEqualTo("1430")
        }

        @Test
        fun `should format time to Korean with and without minute`() {
            // given
            val timeWithMinute = LocalTime.of(14, 30)
            val timeWithoutMinute = LocalTime.of(10, 0)

            // when
            val resultWithMinute = timeWithMinute.toKorean()
            val resultWithoutMinute = timeWithoutMinute.toKorean()

            // then
            assertThat(resultWithMinute).isEqualTo("14시 30분")
            assertThat(resultWithoutMinute).isEqualTo("10시")
        }
    }

    @Nested
    @DisplayName("LocalDate to String formatting")
    inner class LocalDateToStringFormattingTest {

        @Test
        fun `should format date with default and custom formats`() {
            // given
            val date = LocalDate.of(2025, 12, 5)

            // when
            val defaultResult = date.toStr()
            val customResult = date.toStr("dd/MM/yyyy")

            // then
            assertThat(defaultResult).isEqualTo("2025-12-05")
            assertThat(customResult).isEqualTo("05/12/2025")
        }

        @Test
        fun `should format date to numeric string`() {
            // given
            val date = LocalDate.of(2025, 12, 5)

            // when
            val result = date.toNumericStr()

            // then
            assertThat(result).isEqualTo("20251205")
        }

        @Test
        fun `should format date to Korean`() {
            // given
            val date = LocalDate.of(2025, 12, 5)

            // when
            val result = date.toKorean()

            // then
            assertThat(result).isEqualTo("2025년 12월 5일")
        }
    }

    @Nested
    @DisplayName("LocalDateTime to String formatting")
    inner class LocalDateTimeToStringFormattingTest {

        @Test
        fun `should format datetime with default and custom formats`() {
            // given
            val dateTime = LocalDateTime.of(2025, 12, 5, 14, 30, 45)

            // when
            val defaultResult = dateTime.toStr()
            val customResult = dateTime.toStr("dd/MM/yyyy HH:mm")

            // then
            assertThat(defaultResult).isEqualTo("2025-12-05T14:30:45")
            assertThat(customResult).isEqualTo("05/12/2025 14:30")
        }

        @Test
        fun `should format datetime to numeric string`() {
            // given
            val dateTime = LocalDateTime.of(2025, 12, 5, 14, 30, 45)

            // when
            val result = dateTime.toNumericStr()

            // then
            assertThat(result).isEqualTo("20251205143045")
        }

        @Test
        fun `should format datetime to Korean with and without minute`() {
            // given
            val dateTimeWithMinute = LocalDateTime.of(2025, 12, 5, 14, 30, 45)
            val dateTimeWithoutMinute = LocalDateTime.of(2025, 12, 5, 10, 0, 0)

            // when
            val resultWithMinute = dateTimeWithMinute.toKorean()
            val resultWithoutMinute = dateTimeWithoutMinute.toKorean()

            // then
            assertThat(resultWithMinute).isEqualTo("2025년 12월 5일 14시 30분")
            assertThat(resultWithoutMinute).isEqualTo("2025년 12월 5일 10시")
        }
    }

    @Nested
    @DisplayName("ZonedDateTime to String formatting")
    inner class ZonedDateTimeToStringFormattingTest {

        @Test
        fun `should format zoned datetime with default format`() {
            // given
            val zonedDateTime = ZonedDateTime.of(2025, 12, 5, 14, 30, 45, 0, ZoneId.of("Asia/Seoul"))

            // when
            val result = zonedDateTime.toStr()

            // then
            assertThat(result).isEqualTo("2025-12-05T14:30:45+09:00[Asia/Seoul]")
        }

        @Test
        fun `should format zoned datetime with custom format`() {
            // given
            val zonedDateTime = ZonedDateTime.of(2025, 12, 5, 14, 30, 45, 0, ZoneId.of("Asia/Seoul"))

            // when
            val result = zonedDateTime.toStr("yyyy-MM-dd HH:mm:ss VV")

            // then
            assertThat(result).isEqualTo("2025-12-05 14:30:45 Asia/Seoul")
        }

        @Test
        fun `should format zoned datetime to Korean`() {
            // given
            val zonedDateTime = ZonedDateTime.of(2025, 12, 5, 14, 30, 45, 0, ZoneId.of("Asia/Seoul"))

            // when
            val result = zonedDateTime.toKorean()

            // then
            assertThat(result).isEqualTo("2025년 12월 5일 14시 30분 Asia/Seoul (+09:00)")
        }

        @Test
        fun `should parse offset with custom prefix and suffix`() {
            // given
            val zonedDateTime = ZonedDateTime.of(2025, 12, 5, 14, 30, 45, 0, ZoneId.of("Asia/Seoul"))

            // when
            val defaultResult = zonedDateTime.parseOffset()
            val customResult = zonedDateTime.parseOffset(prefix = "UTC", suffix = " hours")

            // then
            assertThat(defaultResult).isEqualTo("GMT+09:00")
            assertThat(customResult).isEqualTo("UTC+09:00 hours")
        }
    }

    @Nested
    @DisplayName("String to DayOfWeek conversion")
    inner class StringToDayOfWeekConversionTest {

        @Test
        fun `should convert two-letter English abbreviations to DayOfWeek`() {
            // given & when & then
            assertThat("MO".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat("TU".toDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY)
            assertThat("WE".toDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY)
            assertThat("TH".toDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY)
            assertThat("FR".toDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY)
            assertThat("SA".toDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY)
            assertThat("SU".toDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)
        }

        @Test
        fun `should convert three-letter English abbreviations to DayOfWeek`() {
            // given & when & then
            assertThat("MON".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat("TUE".toDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY)
            assertThat("WED".toDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY)
            assertThat("THU".toDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY)
            assertThat("FRI".toDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY)
            assertThat("SAT".toDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY)
            assertThat("SUN".toDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)
        }

        @Test
        fun `should convert short Korean names to DayOfWeek`() {
            // given & when & then
            assertThat("월".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat("화".toDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY)
            assertThat("수".toDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY)
            assertThat("목".toDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY)
            assertThat("금".toDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY)
            assertThat("토".toDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY)
            assertThat("일".toDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)
        }

        @Test
        fun `should convert full Korean names to DayOfWeek`() {
            // given & when & then
            assertThat("월요일".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat("화요일".toDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY)
            assertThat("수요일".toDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY)
            assertThat("목요일".toDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY)
            assertThat("금요일".toDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY)
            assertThat("토요일".toDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY)
            assertThat("일요일".toDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)
        }

        @Test
        fun `should handle case insensitivity and whitespace`() {
            // given & when & then
            assertThat("mo".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat("Mon".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
            assertThat(" MO ".toDayOfWeek()).isEqualTo(DayOfWeek.MONDAY)
        }

        @Test
        fun `should return null for invalid input`() {
            // given & when & then
            assertThat("invalid".toDayOfWeek()).isNull()
            assertThat("".toDayOfWeek()).isNull()
            assertThat("MONDAY".toDayOfWeek()).isNull()
        }
    }

    @Nested
    @DisplayName("DayOfWeek to String conversion")
    inner class DayOfWeekToStringConversionTest {

        @Test
        fun `should convert DayOfWeek to two-letter representation`() {
            // given & when & then
            assertThat(DayOfWeek.MONDAY.toTwoLetter()).isEqualTo("MO")
            assertThat(DayOfWeek.TUESDAY.toTwoLetter()).isEqualTo("TU")
            assertThat(DayOfWeek.WEDNESDAY.toTwoLetter()).isEqualTo("WE")
            assertThat(DayOfWeek.THURSDAY.toTwoLetter()).isEqualTo("TH")
            assertThat(DayOfWeek.FRIDAY.toTwoLetter()).isEqualTo("FR")
            assertThat(DayOfWeek.SATURDAY.toTwoLetter()).isEqualTo("SA")
            assertThat(DayOfWeek.SUNDAY.toTwoLetter()).isEqualTo("SU")
        }

        @Test
        fun `should convert DayOfWeek to three-letter representation`() {
            // given & when & then
            assertThat(DayOfWeek.MONDAY.toThreeLetter()).isEqualTo("MON")
            assertThat(DayOfWeek.TUESDAY.toThreeLetter()).isEqualTo("TUE")
            assertThat(DayOfWeek.WEDNESDAY.toThreeLetter()).isEqualTo("WED")
            assertThat(DayOfWeek.THURSDAY.toThreeLetter()).isEqualTo("THU")
            assertThat(DayOfWeek.FRIDAY.toThreeLetter()).isEqualTo("FRI")
            assertThat(DayOfWeek.SATURDAY.toThreeLetter()).isEqualTo("SAT")
            assertThat(DayOfWeek.SUNDAY.toThreeLetter()).isEqualTo("SUN")
        }

        @Test
        fun `should convert DayOfWeek to short Korean representation`() {
            // given & when & then
            assertThat(DayOfWeek.MONDAY.toKorean(short = true)).isEqualTo("월")
            assertThat(DayOfWeek.TUESDAY.toKorean(short = true)).isEqualTo("화")
            assertThat(DayOfWeek.WEDNESDAY.toKorean(short = true)).isEqualTo("수")
            assertThat(DayOfWeek.THURSDAY.toKorean(short = true)).isEqualTo("목")
            assertThat(DayOfWeek.FRIDAY.toKorean(short = true)).isEqualTo("금")
            assertThat(DayOfWeek.SATURDAY.toKorean(short = true)).isEqualTo("토")
            assertThat(DayOfWeek.SUNDAY.toKorean(short = true)).isEqualTo("일")
        }

        @Test
        fun `should convert DayOfWeek to full Korean representation`() {
            // given & when & then
            assertThat(DayOfWeek.MONDAY.toKorean()).isEqualTo("월요일")
            assertThat(DayOfWeek.TUESDAY.toKorean()).isEqualTo("화요일")
            assertThat(DayOfWeek.WEDNESDAY.toKorean()).isEqualTo("수요일")
            assertThat(DayOfWeek.THURSDAY.toKorean()).isEqualTo("목요일")
            assertThat(DayOfWeek.FRIDAY.toKorean()).isEqualTo("금요일")
            assertThat(DayOfWeek.SATURDAY.toKorean()).isEqualTo("토요일")
            assertThat(DayOfWeek.SUNDAY.toKorean()).isEqualTo("일요일")
        }
    }

    @Nested
    @DisplayName("XMLGregorianCalendar conversion")
    inner class XMLGregorianCalendarConversionTest {

        private val datatypeFactory = DatatypeFactory.newInstance()

        @Test
        fun `should convert XMLGregorianCalendar to LocalDateTime`() {
            // given
            val xmlCalendar = datatypeFactory.newXMLGregorianCalendar(2025, 12, 5, 14, 30, 45, 0, 0)

            // when
            val result = xmlCalendar.toLocalDateTime()

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2025, 12, 5, 14, 30, 45))
        }

        @Test
        fun `should convert XMLGregorianCalendar to LocalDate`() {
            // given
            val xmlCalendar = datatypeFactory.newXMLGregorianCalendar(2025, 12, 5, 14, 30, 45, 0, 0)

            // when
            val result = xmlCalendar.toLocalDate()

            // then
            assertThat(result).isEqualTo(LocalDate.of(2025, 12, 5))
        }
    }

    @Nested
    @DisplayName("Format constants verification")
    inner class FormatConstantsTest {

        @Test
        fun `should have correct format constant values`() {
            // given & when & then
            assertThat(DateFormatter.TIME_FORMAT).isEqualTo("HH:mm:ss")
            assertThat(DateFormatter.DATE_FORMAT).isEqualTo("yyyy-MM-dd")
            assertThat(DateFormatter.DATE_FORMAT_UTC).isEqualTo("yyyy-MM-dd'Z'")
            assertThat(DateFormatter.DATE_TIME_FORMAT).isEqualTo("yyyy-MM-dd'T'HH:mm:ss")
            assertThat(DateFormatter.ZONED_DT_FORMAT).isEqualTo("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
        }

        @Test
        fun `should have correct numeric format constant values`() {
            // given & when & then
            assertThat(DateFormatter.NUMERIC_TIME_NO_SECONDS).isEqualTo("HHmm")
            assertThat(DateFormatter.NUMERIC_TIME).isEqualTo("HHmmss")
            assertThat(DateFormatter.NUMERIC_DATE).isEqualTo("yyyyMMdd")
            assertThat(DateFormatter.NUMERIC_DATE_TIME).isEqualTo("yyyyMMddHHmmss")
        }

        @Test
        fun `should have correct Korean format constant values`() {
            // given & when & then
            assertThat(DateFormatter.KOREAN_TIME).isEqualTo("H시 m분 s초")
            assertThat(DateFormatter.KOREAN_TIME_NO_SECONDS).isEqualTo("H시 m분")
            assertThat(DateFormatter.KOREAN_DATE).isEqualTo("yyyy년 M월 d일")
            assertThat(DateFormatter.KOREAN_DT).isEqualTo("yyyy년 M월 d일 H시 m분 s초")
            assertThat(DateFormatter.KOREAN_DT_NO_SECONDS).isEqualTo("yyyy년 M월 d일 H시 m분")
            assertThat(DateFormatter.KOREAN_ZONED_DT_NO_SECONDS).isEqualTo("yyyy년 M월 d일 H시 m분 VV (XXX)")
        }
    }
}
