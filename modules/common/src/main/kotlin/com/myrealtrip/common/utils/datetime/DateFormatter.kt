package com.myrealtrip.common.utils.datetime

import java.time.*
import java.time.format.DateTimeFormatter
import javax.xml.datatype.XMLGregorianCalendar

object DateFormatter {

    // ========================================
    // Format Constants - Standard ISO-like
    // ========================================

    const val TIME_FORMAT = "HH:mm:ss"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATE_FORMAT_UTC = "yyyy-MM-dd'Z'"
    const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val ZONED_DT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'"

    // ========================================
    // Format Constants - Numeric (no separators)
    // ========================================

    const val NUMERIC_TIME_NO_SECONDS = "HHmm"
    const val NUMERIC_TIME = "HHmmss"
    const val NUMERIC_DATE = "yyyyMMdd"
    const val NUMERIC_DATE_TIME = "yyyyMMddHHmmss"

    // ========================================
    // Format Constants - Korean
    // ========================================

    const val KOREAN_TIME = "H시 m분 s초"
    const val KOREAN_TIME_NO_SECONDS = "H시 m분"
    const val KOREAN_DATE = "yyyy년 M월 d일"
    const val KOREAN_DT = "yyyy년 M월 d일 H시 m분 s초"
    const val KOREAN_DT_NO_SECONDS = "yyyy년 M월 d일 H시 m분"
    const val KOREAN_ZONED_DT_NO_SECONDS = "yyyy년 M월 d일 H시 m분 VV (XXX)"

    // ========================================
    // Private Helper
    // ========================================

    private fun formatter(format: String): DateTimeFormatter = DateTimeFormatter.ofPattern(format)

    // ========================================
    // String Parsing - LocalTime
    // ========================================

    /**
     * @return [LocalTime] from the provided time string and [format]
     */
    @JvmStatic
    @JvmOverloads
    fun String.toTime(format: String = TIME_FORMAT): LocalTime {
        return LocalTime.parse(this, formatter(format))
    }

    /**
     * @return [LocalTime] from the provided numeric time string
     */
    @JvmStatic
    fun String.numericToTime(): LocalTime {
        val effectiveFormat = if (this.length == 4) NUMERIC_TIME_NO_SECONDS else NUMERIC_TIME
        return this.toTime(effectiveFormat)
    }

    // ========================================
    // String Parsing - LocalDate
    // ========================================

    /**
     * @return [LocalDate] from the provided date string and [format]
     */
    @JvmStatic
    @JvmOverloads
    fun String.toDate(format: String = DATE_FORMAT): LocalDate {
        return LocalDate.parse(this, formatter(format))
    }

    /**
     * @return [LocalDate] from the provided numeric date string
     */
    @JvmStatic
    fun String.numericToDate(): LocalDate {
        return this.toDate(NUMERIC_DATE)
    }

    // ========================================
    // String Parsing - LocalDateTime
    // ========================================

    /**
     * @return [LocalDateTime] from the provided datetime string and [format]
     */
    @JvmStatic
    @JvmOverloads
    fun String.toDateTime(format: String = DATE_TIME_FORMAT): LocalDateTime {
        return LocalDateTime.parse(this, formatter(format))
    }

    /**
     * @return [LocalDateTime] from the provided numeric datetime string
     */
    @JvmStatic
    fun String.numericToDateTime(): LocalDateTime {
        val paddedValue = when (this.length) {
            8 -> this + "000000"
            10 -> this + "0000"
            12 -> this + "00"
            else -> this
        }
        return paddedValue.toDateTime(NUMERIC_DATE_TIME)
    }

    // ========================================
    // Formatting - LocalTime to String
    // ========================================

    /**
     * @return the provided [time] as a string in the given [format]
     */
    @JvmStatic
    @JvmOverloads
    fun LocalTime.toStr(format: String = TIME_FORMAT): String {
        return this.format(formatter(format))
    }

    /**
     * @return the provided [time] as a string in numeric format without seconds
     */
    @JvmStatic
    fun LocalTime.toNumericStr(): String {
        return this.toStr(NUMERIC_TIME_NO_SECONDS)
    }

    /**
     * @return the provided [time] as a string in Korean format without seconds
     * If minute is 0, returns without minute part (e.g., "10시")
     */
    @JvmStatic
    fun LocalTime.toKorean(): String {
        return if (this.minute == 0) this.toStr("H시") else this.toStr(KOREAN_TIME_NO_SECONDS)
    }

    // ========================================
    // Formatting - LocalDate to String
    // ========================================

    /**
     * @return the provided [date] as a string in the given [format]
     */
    @JvmStatic
    @JvmOverloads
    fun LocalDate.toStr(format: String = DATE_FORMAT): String {
        return this.format(formatter(format))
    }

    /**
     * @return the provided [date] as a string in numeric format
     */
    @JvmStatic
    fun LocalDate.toNumericStr(): String {
        return this.toStr(NUMERIC_DATE)
    }

    /**
     * @return the provided [date] as a string in Korean format
     */
    @JvmStatic
    fun LocalDate.toKorean(): String {
        return this.toStr(KOREAN_DATE)
    }

    // ========================================
    // Formatting - LocalDateTime to String
    // ========================================

    /**
     * @return the provided [dateTime] as a string in the given [format]
     */
    @JvmStatic
    @JvmOverloads
    fun LocalDateTime.toStr(format: String = DATE_TIME_FORMAT): String {
        return this.format(formatter(format))
    }

    /**
     * @return the provided [dateTime] as a string in numeric format
     */
    @JvmStatic
    fun LocalDateTime.toNumericStr(): String {
        return this.toStr(NUMERIC_DATE_TIME)
    }

    /**
     * @return the provided [dateTime] as a string in Korean format without seconds
     * If minute is 0, returns without minute part (e.g., "2025년 12월 3일 10시")
     */
    @JvmStatic
    fun LocalDateTime.toKorean(): String {
        return if (this.minute == 0) this.toStr("yyyy년 M월 d일 H시") else this.toStr(KOREAN_DT_NO_SECONDS)
    }

    // ========================================
    // Formatting - ZonedDateTime to String
    // ========================================

    /**
     * @return the provided [zonedDateTime] as a string in the given [format]
     */
    @JvmStatic
    @JvmOverloads
    fun ZonedDateTime.toStr(format: String = ZONED_DT_FORMAT): String {
        return this.format(formatter(format))
    }

    /**
     * @return the provided [zonedDateTime] as a string in Korean format without seconds
     */
    @JvmStatic
    fun ZonedDateTime.toKorean(): String {
        return this.toStr(KOREAN_ZONED_DT_NO_SECONDS)
    }

    // ========================================
    // DayOfWeek Conversions - String to DayOfWeek
    // ========================================

    /**
     * @return [DayOfWeek] or null if the input is invalid
     */
    @JvmStatic
    fun String.toDayOfWeek(): DayOfWeek? {
        return when (this.trim().uppercase()) {
            "MO", "MON", "월", "월요일" -> DayOfWeek.MONDAY
            "TU", "TUE", "화", "화요일" -> DayOfWeek.TUESDAY
            "WE", "WED", "수", "수요일" -> DayOfWeek.WEDNESDAY
            "TH", "THU", "목", "목요일" -> DayOfWeek.THURSDAY
            "FR", "FRI", "금", "금요일" -> DayOfWeek.FRIDAY
            "SA", "SAT", "토", "토요일" -> DayOfWeek.SATURDAY
            "SU", "SUN", "일", "일요일" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    // ========================================
    // DayOfWeek Conversions - DayOfWeek to String
    // ========================================

    /**
     * @return the two-letter representation of the [DayOfWeek]
     */
    @JvmStatic
    fun DayOfWeek.toTwoLetter(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "MO"
            DayOfWeek.TUESDAY -> "TU"
            DayOfWeek.WEDNESDAY -> "WE"
            DayOfWeek.THURSDAY -> "TH"
            DayOfWeek.FRIDAY -> "FR"
            DayOfWeek.SATURDAY -> "SA"
            DayOfWeek.SUNDAY -> "SU"
        }
    }

    /**
     * @return the three-letter representation of the [DayOfWeek]
     */
    @JvmStatic
    fun DayOfWeek.toThreeLetter(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "MON"
            DayOfWeek.TUESDAY -> "TUE"
            DayOfWeek.WEDNESDAY -> "WED"
            DayOfWeek.THURSDAY -> "THU"
            DayOfWeek.FRIDAY -> "FRI"
            DayOfWeek.SATURDAY -> "SAT"
            DayOfWeek.SUNDAY -> "SUN"
        }
    }

    /**
     * @param short whether to return the short form (e.g., "월") or full form (e.g., "월요일")
     * @return the Korean representation of the [DayOfWeek]
     */
    fun DayOfWeek.toKorean(short: Boolean = false): String {
        return when (this) {
            DayOfWeek.MONDAY -> if (short) "월" else "월요일"
            DayOfWeek.TUESDAY -> if (short) "화" else "화요일"
            DayOfWeek.WEDNESDAY -> if (short) "수" else "수요일"
            DayOfWeek.THURSDAY -> if (short) "목" else "목요일"
            DayOfWeek.FRIDAY -> if (short) "금" else "금요일"
            DayOfWeek.SATURDAY -> if (short) "토" else "토요일"
            DayOfWeek.SUNDAY -> if (short) "일" else "일요일"
        }
    }

    // ========================================
    // ZonedDateTime Utilities
    // ========================================

    /**
     * @return the GMT offset representation of the [ZonedDateTime] with the provided [prefix] and [suffix]
     */
    @JvmStatic
    fun ZonedDateTime.parseOffset(prefix: String = "GMT", suffix: String = ""): String {
        return "${prefix}${this.offset}${suffix}"
    }

    // ========================================
    // XMLGregorianCalendar Conversions
    // ========================================

    /**
     * @return [LocalDateTime] from the provided [XMLGregorianCalendar]
     */
    @JvmStatic
    fun XMLGregorianCalendar.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.of(year, month, day, hour, minute, second)
    }

    /**
     * @return [LocalDate] from the provided [XMLGregorianCalendar]
     */
    @JvmStatic
    fun XMLGregorianCalendar.toLocalDate(): LocalDate {
        return LocalDate.of(year, month, day)
    }

}
