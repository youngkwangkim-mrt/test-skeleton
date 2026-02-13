package com.myrealtrip.common.utils.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DateTimeExtTest : FunSpec({

    val today = LocalDate.now()

    context("isToday") {
        test("should return true only for today's date") {
            today.isToday() shouldBe true
            today.minusDays(1).isToday() shouldBe false
            today.plusDays(1).isToday() shouldBe false
        }
    }

    context("isYesterday") {
        test("should return true only for yesterday's date") {
            today.minusDays(1).isYesterday() shouldBe true
            today.isYesterday() shouldBe false
            today.minusDays(2).isYesterday() shouldBe false
        }
    }

    context("isTomorrow") {
        test("should return true only for tomorrow's date") {
            today.plusDays(1).isTomorrow() shouldBe true
            today.isTomorrow() shouldBe false
            today.plusDays(2).isTomorrow() shouldBe false
        }
    }

    context("isPast") {
        test("should return true for past dates, false for today and future") {
            today.minusDays(1).isPast() shouldBe true
            today.isPast() shouldBe false
            today.plusDays(1).isPast() shouldBe false
        }

        test("should return true for distant past dates") {
            LocalDate.of(1900, 1, 1).isPast() shouldBe true
            LocalDate.MIN.isPast() shouldBe true
        }
    }

    context("isFuture") {
        test("should return true for future dates, false for today and past") {
            today.plusDays(1).isFuture() shouldBe true
            today.isFuture() shouldBe false
            today.minusDays(1).isFuture() shouldBe false
        }

        test("should return true for distant future dates") {
            LocalDate.of(2999, 12, 31).isFuture() shouldBe true
            LocalDate.MAX.isFuture() shouldBe true
        }
    }

    context("getAge") {
        context("standard age calculation") {
            data class AgeTestCase(
                val description: String,
                val birthDate: LocalDate,
                val targetDate: LocalDate,
                val expectedAge: Int,
            )

            withData(
                nameFn = { it.description },
                AgeTestCase(
                    description = "exact birthday - turns 35",
                    birthDate = LocalDate.of(1990, 5, 15),
                    targetDate = LocalDate.of(2025, 5, 15),
                    expectedAge = 35,
                ),
                AgeTestCase(
                    description = "day after birthday - already 35",
                    birthDate = LocalDate.of(1990, 5, 15),
                    targetDate = LocalDate.of(2025, 5, 16),
                    expectedAge = 35,
                ),
                AgeTestCase(
                    description = "day before birthday - still 34",
                    birthDate = LocalDate.of(1990, 5, 15),
                    targetDate = LocalDate.of(2025, 5, 14),
                    expectedAge = 34,
                ),
                AgeTestCase(
                    description = "same year as birth - age 0",
                    birthDate = LocalDate.of(2025, 1, 1),
                    targetDate = LocalDate.of(2025, 12, 31),
                    expectedAge = 0,
                ),
                AgeTestCase(
                    description = "exactly 1 year old",
                    birthDate = LocalDate.of(2025, 6, 1),
                    targetDate = LocalDate.of(2026, 6, 1),
                    expectedAge = 1,
                ),
            ) { (_, birthDate, targetDate, expectedAge) ->
                birthDate.getAge(targetDate) shouldBe expectedAge
            }
        }

        context("year boundary") {
            test("should calculate age correctly across year boundary (Dec 31 to Jan 1)") {
                val birthDate = LocalDate.of(2000, 12, 31)

                birthDate.getAge(LocalDate.of(2001, 1, 1)) shouldBe 0
                birthDate.getAge(LocalDate.of(2001, 12, 30)) shouldBe 0
                birthDate.getAge(LocalDate.of(2001, 12, 31)) shouldBe 1
            }

            test("should calculate age correctly for January 1st birthday") {
                val birthDate = LocalDate.of(2000, 1, 1)

                birthDate.getAge(LocalDate.of(2000, 12, 31)) shouldBe 0
                birthDate.getAge(LocalDate.of(2001, 1, 1)) shouldBe 1
            }
        }

        context("leap year birthday (Feb 29)") {
            val leapYearBirthday = LocalDate.of(2000, 2, 29)

            test("should turn 1 on Feb 28 in non-leap year") {
                leapYearBirthday.getAge(LocalDate.of(2001, 2, 28)) shouldBe 0
                leapYearBirthday.getAge(LocalDate.of(2001, 3, 1)) shouldBe 1
            }

            test("should turn age on Feb 29 in leap year") {
                leapYearBirthday.getAge(LocalDate.of(2004, 2, 28)) shouldBe 3
                leapYearBirthday.getAge(LocalDate.of(2004, 2, 29)) shouldBe 4
            }
        }

        context("default parameter (current date)") {
            test("should use current date when targetDate not specified") {
                val birthDate = today.minusYears(30)
                birthDate.getAge() shouldBe 30
            }

            test("should return 0 for birth date earlier this year") {
                val targetDate = LocalDate.of(2025, 6, 15)
                val birthDateThisYear = LocalDate.of(2025, 1, 1)
                birthDateThisYear.getAge(targetDate) shouldBe 0
            }
        }
    }

    context("getKoreanAge") {
        context("한국 나이 계산 (세는 나이)") {
            data class KoreanAgeTestCase(
                val description: String,
                val birthDate: LocalDate,
                val targetDate: LocalDate,
                val expectedAge: Int,
            )

            withData(
                nameFn = { it.description },
                KoreanAgeTestCase(
                    description = "태어난 해 - 1살",
                    birthDate = LocalDate.of(2025, 12, 31),
                    targetDate = LocalDate.of(2025, 12, 31),
                    expectedAge = 1,
                ),
                KoreanAgeTestCase(
                    description = "새해 첫날 - 2살 (1월 1일에 한 살 추가)",
                    birthDate = LocalDate.of(2025, 12, 31),
                    targetDate = LocalDate.of(2026, 1, 1),
                    expectedAge = 2,
                ),
                KoreanAgeTestCase(
                    description = "1990년생 2025년 기준 - 36살",
                    birthDate = LocalDate.of(1990, 5, 15),
                    targetDate = LocalDate.of(2025, 1, 1),
                    expectedAge = 36,
                ),
                KoreanAgeTestCase(
                    description = "생일 전후 상관없이 동일 (생일 전)",
                    birthDate = LocalDate.of(1990, 12, 25),
                    targetDate = LocalDate.of(2025, 6, 1),
                    expectedAge = 36,
                ),
                KoreanAgeTestCase(
                    description = "생일 전후 상관없이 동일 (생일 후)",
                    birthDate = LocalDate.of(1990, 1, 1),
                    targetDate = LocalDate.of(2025, 6, 1),
                    expectedAge = 36,
                ),
            ) { (_, birthDate, targetDate, expectedAge) ->
                birthDate.getKoreanAge(targetDate) shouldBe expectedAge
            }
        }

        context("만 나이와 한국 나이 비교") {
            test("생일 전에는 한국 나이가 만 나이보다 2살 많음") {
                val birthDate = LocalDate.of(1990, 12, 25)
                val targetDate = LocalDate.of(2025, 6, 1)

                val internationalAge = birthDate.getAge(targetDate)
                val koreanAge = birthDate.getKoreanAge(targetDate)

                internationalAge shouldBe 34
                koreanAge shouldBe 36
                koreanAge - internationalAge shouldBe 2
            }

            test("생일 당일/후에는 한국 나이가 만 나이보다 1살 많음") {
                val birthDate = LocalDate.of(1990, 5, 15)
                val targetDate = LocalDate.of(2025, 5, 15)

                val internationalAge = birthDate.getAge(targetDate)
                val koreanAge = birthDate.getKoreanAge(targetDate)

                internationalAge shouldBe 35
                koreanAge shouldBe 36
                koreanAge - internationalAge shouldBe 1
            }
        }

        context("default parameter (current date)") {
            test("should use current date when targetDate not specified") {
                val birthDate = today.minusYears(30)
                birthDate.getKoreanAge() shouldBe 31
            }
        }
    }
})
