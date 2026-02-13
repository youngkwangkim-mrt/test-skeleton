package com.myrealtrip.common.utils.extensions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NumberExtTest : FunSpec({

    context("randomZeroOrOne") {
        test("should return 0 or 1") {
            repeat(100) {
                val result = randomZeroOrOne()
                result shouldBeInRange 0..1
            }
        }
    }

    context("randomZeroToTwo") {
        test("should return 0, 1, or 2") {
            repeat(100) {
                val result = randomZeroToTwo()
                result shouldBeInRange 0..2
            }
        }
    }

    context("randomIntBetween") {
        test("should return value within specified range") {
            repeat(100) {
                val result = randomIntBetween(10, 20)
                result shouldBeInRange 10..20
            }
        }

        test("should return exact value when min equals max") {
            val result = randomIntBetween(5, 5)
            result shouldBe 5
        }

        test("should work with negative numbers") {
            repeat(100) {
                val result = randomIntBetween(-10, -5)
                result shouldBeInRange -10..-5
            }
        }
    }

    context("randomLongBetween") {
        test("should return value within specified range") {
            repeat(100) {
                val result = randomLongBetween(100L, 200L)
                result shouldBeInRange 100L..200L
            }
        }

        test("should return exact value when min equals max") {
            val result = randomLongBetween(50L, 50L)
            result shouldBe 50L
        }

        test("should work with negative numbers") {
            repeat(100) {
                val result = randomLongBetween(-100L, -50L)
                result shouldBeInRange -100L..-50L
            }
        }

        test("should work with large numbers") {
            repeat(100) {
                val result = randomLongBetween(1_000_000_000L, 2_000_000_000L)
                result shouldBeInRange 1_000_000_000L..2_000_000_000L
            }
        }
    }

    context("randomOfLength") {
        test("should return number with correct length for DigitLength.ONE") {
            repeat(50) {
                val result = randomOfLength(DigitLength.ONE)
                result shouldBeInRange 1L..9L
            }
        }

        test("should return number with correct length for DigitLength.FIVE") {
            repeat(50) {
                val result = randomOfLength(DigitLength.FIVE)
                result shouldBeInRange 10000L..99999L
            }
        }

        test("should return number with correct length for DigitLength.TEN") {
            repeat(50) {
                val result = randomOfLength(DigitLength.TEN)
                result shouldBeInRange 1000000000L..9999999999L
            }
        }
    }

    context("commaSeparated") {
        test("should format integer with comma separators") {
            1000.commaSeparated() shouldBe "1,000"
            1000000.commaSeparated() shouldBe "1,000,000"
            123456789.commaSeparated() shouldBe "123,456,789"
        }

        test("should format with decimal places") {
            1234.56.commaSeparated(2) shouldBe "1,234.56"
            1000.commaSeparated(2) shouldBe "1,000.00"
            1234567.89.commaSeparated(1) shouldBe "1,234,567.9"
        }

        test("should handle small numbers") {
            0.commaSeparated() shouldBe "0"
            999.commaSeparated() shouldBe "999"
            100.commaSeparated(2) shouldBe "100.00"
        }

        test("should handle negative numbers") {
            (-1000).commaSeparated() shouldBe "-1,000"
            (-1234.56).commaSeparated(2) shouldBe "-1,234.56"
        }

        test("should throw exception for negative decimal places") {
            val exception = shouldThrow<IllegalArgumentException> {
                1000.commaSeparated(-1)
            }
            exception.message shouldContain "Decimal places must be non-negative"
        }
    }
})
