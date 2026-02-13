@file:JvmName("DateTimeExt")

package com.myrealtrip.common.utils.extensions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * @return true if this date is today
 */
fun LocalDate.isToday(): Boolean = equals(LocalDate.now())

/**
 * @return true if this date is yesterday
 */
fun LocalDate.isYesterday(): Boolean = equals(LocalDate.now().minusDays(1))

/**
 * @return true if this date is tomorrow
 */
fun LocalDate.isTomorrow(): Boolean = equals(LocalDate.now().plusDays(1))

/**
 * @return true if this date is in the past
 */
fun LocalDate.isPast(): Boolean = this < LocalDate.now()

/**
 * @return true if this date is in the future
 */
fun LocalDate.isFuture(): Boolean = this > LocalDate.now()

/**
 * @param targetDate the date to compare with (default: today)
 * @return age in years
 */
@JvmOverloads
fun LocalDate.getAge(targetDate: LocalDate = LocalDate.now()): Int {
    return Period.between(this, targetDate).years
}

/**
 * 한국 나이를 계산합니다.
 * 한국 나이 = 기준년도 - 출생년도 + 1
 *
 * @param targetDate 기준 날짜 (기본값: 오늘)
 * @return 한국 나이 (세는 나이)
 */
@JvmOverloads
fun LocalDate.getKoreanAge(targetDate: LocalDate = LocalDate.now()): Int {
    return targetDate.year - this.year + 1
}

private val KST = ZoneId.of("Asia/Seoul")

/**
 * UTC 기준 LocalDateTime을 KST LocalDateTime으로 변환합니다.
 */
fun LocalDateTime.kst(): LocalDateTime = this.atZone(ZoneId.of("UTC")).withZoneSameInstant(KST).toLocalDateTime()

/**
 * UTC 기준 ZonedDateTime을 KST ZonedDateTime으로 변환합니다.
 */
fun ZonedDateTime.kst(): ZonedDateTime = this.withZoneSameInstant(KST)