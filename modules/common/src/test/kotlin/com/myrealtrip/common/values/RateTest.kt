package com.myrealtrip.common.values

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.math.BigDecimal

@DisplayName("Rate")
class RateTest {

    @Nested
    @DisplayName("Factory Methods")
    inner class FactoryMethods {

        @Test
        fun `should create rate from decimal value`(): Unit {
            // given
            val decimal = BigDecimal("0.15")

            // when
            val rate = Rate.of(decimal)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.1500"))
        }

        @Test
        fun `should create rate from double value`(): Unit {
            // given
            val decimal = 0.15

            // when
            val rate = Rate.of(decimal)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should create rate from string value`(): Unit {
            // given
            val decimal = "0.15"

            // when
            val rate = Rate.of(decimal)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should create rate from percentage`(): Unit {
            // given
            val percent = 15

            // when
            val rate = Rate.ofPercent(percent)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should create rate from percentage with decimals`(): Unit {
            // given
            val percent = BigDecimal("15.1234")

            // when
            val rate = Rate.ofPercent(percent)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.1512"))
        }

        @Test
        fun `should create rate with custom scale`(): Unit {
            // given
            val decimal = BigDecimal("0.123456")

            // when
            val rate = Rate.of(decimal, scale = 6)

            // then
            assertThat(rate.value.scale()).isEqualTo(6)
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.123456"))
        }

        @Test
        fun `should throw exception when decimal is negative`(): Unit {
            // when & then
            assertThatThrownBy { Rate.of(BigDecimal("-0.1")) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Rate must be between 0 and 1")
        }

        @Test
        fun `should throw exception when decimal is greater than 1`(): Unit {
            // when & then
            assertThatThrownBy { Rate.of(BigDecimal("1.1")) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Rate must be between 0 and 1")
        }

        @Test
        fun `should throw exception when percent is negative`(): Unit {
            // when & then
            assertThatThrownBy { Rate.ofPercent(-10) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Percent must be between 0 and 100")
        }

        @Test
        fun `should throw exception when percent is greater than 100`(): Unit {
            // when & then
            assertThatThrownBy { Rate.ofPercent(101) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Percent must be between 0 and 100")
        }

        @Test
        fun `should create rate using extension function`(): Unit {
            // when
            val rate = 15.percent

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should create rate using asRate extension`(): Unit {
            // when
            val rate = 0.15.asRate

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }
    }

    @Nested
    @DisplayName("Conversion Methods")
    inner class ConversionMethods {

        @Test
        fun `should convert to percent`(): Unit {
            // given
            val rate = Rate.of(BigDecimal("0.15"))

            // when
            val percent = rate.toPercent()

            // then
            assertThat(percent).isEqualByComparingTo(BigDecimal("15"))
        }

        @Test
        fun `should convert to percent with custom scale`(): Unit {
            // given
            val rate = Rate.of(BigDecimal("0.1512"), scale = 4)

            // when
            val percent = rate.toPercent(scale = 2)

            // then
            assertThat(percent).isEqualByComparingTo(BigDecimal("15.12"))
        }

        @Test
        fun `should convert to decimal`(): Unit {
            // given
            val rate = Rate.ofPercent(15)

            // when
            val decimal = rate.toDecimal()

            // then
            assertThat(decimal).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should convert to percent string`(): Unit {
            // given
            val rate = Rate.ofPercent(15)

            // when
            val percentString = rate.toPercentString()

            // then
            assertThat(percentString).isEqualTo("15%")
        }

        @Test
        fun `should convert to percent string with decimals`(): Unit {
            // given
            val rate = Rate.ofPercent(BigDecimal("15.50"))

            // when
            val percentString = rate.toPercentString(scale = 2)

            // then
            assertThat(percentString).isEqualTo("15.50%")
        }
    }

    @Nested
    @DisplayName("Calculation Methods")
    inner class CalculationMethods {

        @Test
        fun `should apply rate to BigDecimal amount`(): Unit {
            // given
            val rate = Rate.ofPercent(15)
            val amount = BigDecimal("10000")

            // when
            val result = rate.applyTo(amount)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("1500"))
        }

        @Test
        fun `should apply rate to Long amount`(): Unit {
            // given
            val rate = Rate.ofPercent(15)
            val amount = 10000L

            // when
            val result = rate.applyTo(amount)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("1500"))
        }

        @Test
        fun `should apply rate to Int amount`(): Unit {
            // given
            val rate = Rate.ofPercent(15)
            val amount = 10000

            // when
            val result = rate.applyTo(amount)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("1500"))
        }

        @Test
        fun `should apply rate with custom scale`(): Unit {
            // given
            val rate = Rate.ofPercent(BigDecimal("15.5"))
            val amount = BigDecimal("10000.00")

            // when
            val result = rate.applyTo(amount, scale = 2)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("1550.00"))
            assertThat(result.scale()).isEqualTo(2)
        }

        @Test
        fun `should calculate remainder after applying rate`(): Unit {
            // given
            val rate = Rate.ofPercent(15)
            val amount = BigDecimal("10000")

            // when
            val remainder = rate.remainderOf(amount)

            // then
            assertThat(remainder).isEqualByComparingTo(BigDecimal("8500"))
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    inner class ArithmeticOperations {

        @Test
        fun `should add two rates`(): Unit {
            // given
            val rate1 = Rate.ofPercent(10)
            val rate2 = Rate.ofPercent(5)

            // when
            val result = rate1 + rate2

            // then
            assertThat(result.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should cap sum at 100 percent`(): Unit {
            // given
            val rate1 = Rate.ofPercent(60)
            val rate2 = Rate.ofPercent(50)

            // when
            val result = rate1 + rate2

            // then
            assertThat(result).isEqualTo(Rate.ONE)
        }

        @Test
        fun `should subtract two rates`(): Unit {
            // given
            val rate1 = Rate.ofPercent(15)
            val rate2 = Rate.ofPercent(5)

            // when
            val result = rate1 - rate2

            // then
            assertThat(result.value).isEqualByComparingTo(BigDecimal("0.10"))
        }

        @Test
        fun `should floor difference at zero`(): Unit {
            // given
            val rate1 = Rate.ofPercent(5)
            val rate2 = Rate.ofPercent(10)

            // when
            val result = rate1 - rate2

            // then
            assertThat(result).isEqualTo(Rate.ZERO)
        }

        @Test
        fun `should multiply rate by scalar`(): Unit {
            // given
            val rate = Rate.ofPercent(10)

            // when
            val result = rate * 2

            // then
            assertThat(result.value).isEqualByComparingTo(BigDecimal("0.20"))
        }

        @Test
        fun `should cap multiplication at 100 percent`(): Unit {
            // given
            val rate = Rate.ofPercent(60)

            // when
            val result = rate * 2

            // then
            assertThat(result).isEqualTo(Rate.ONE)
        }

        @Test
        fun `should divide rate by scalar`(): Unit {
            // given
            val rate = Rate.ofPercent(20)

            // when
            val result = rate / 2

            // then
            assertThat(result.value).isEqualByComparingTo(BigDecimal("0.10"))
        }

        @Test
        fun `should throw exception when dividing by zero`(): Unit {
            // given
            val rate = Rate.ofPercent(10)

            // when & then
            assertThatThrownBy { rate / 0 }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot divide by zero")
        }
    }

    @Nested
    @DisplayName("Comparison & Utility Methods")
    inner class ComparisonAndUtilityMethods {

        @Test
        fun `should compare rates`(): Unit {
            // given
            val rate1 = Rate.ofPercent(10)
            val rate2 = Rate.ofPercent(20)
            val rate3 = Rate.ofPercent(10)

            // when & then
            assertThat(rate1 < rate2).isTrue
            assertThat(rate2 > rate1).isTrue
            assertThat(rate1.compareTo(rate3)).isEqualTo(0)
        }

        @Test
        fun `should check if rate is zero`(): Unit {
            // when & then
            assertThat(Rate.ZERO.isZero()).isTrue
            assertThat(Rate.ofPercent(10).isZero()).isFalse
        }

        @Test
        fun `should check if rate is positive`(): Unit {
            // when & then
            assertThat(Rate.ofPercent(10).isPositive()).isTrue
            assertThat(Rate.ZERO.isPositive()).isFalse
        }

        @Test
        fun `should change scale with withScale`(): Unit {
            // given
            val rate = Rate.of(BigDecimal("0.1234"), scale = 4)

            // when
            val result = rate.withScale(2)

            // then
            assertThat(result.value.scale()).isEqualTo(2)
            assertThat(result.value).isEqualByComparingTo(BigDecimal("0.12"))
        }
    }

    @Nested
    @DisplayName("Constants")
    inner class Constants {

        @Test
        fun `should have correct constant values`(): Unit {
            // when & then
            assertThat(Rate.ZERO.value).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(Rate.ONE.value).isEqualByComparingTo(BigDecimal.ONE)
            assertThat(Rate.FIVE_PERCENT.value).isEqualByComparingTo(BigDecimal("0.05"))
            assertThat(Rate.TEN_PERCENT.value).isEqualByComparingTo(BigDecimal("0.10"))
            assertThat(Rate.HALF.value).isEqualByComparingTo(BigDecimal("0.50"))
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        private val mapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        @Test
        fun `should serialize rate to decimal value`(): Unit {
            // given
            val rate = Rate.ofPercent(15)

            // when
            val json = mapper.writeValueAsString(rate)

            // then
            assertThat(json).isEqualTo("0.1500")
        }

        @Test
        fun `should deserialize decimal value to rate`(): Unit {
            // given
            val json = "0.15"

            // when
            val rate = mapper.readValue(json, Rate::class.java)

            // then
            assertThat(rate.value).isEqualByComparingTo(BigDecimal("0.15"))
        }

        @Test
        fun `should serialize and deserialize rate in object`(): Unit {
            // given
            data class Product(val name: String, val discountRate: Rate)
            val product = Product("Test Product", Rate.ofPercent(20))

            // when
            val json = mapper.writeValueAsString(product)
            val deserialized = mapper.readValue(json, Product::class.java)

            // then
            assertThat(json).contains("\"discountRate\":0.2000")
            assertThat(deserialized.discountRate.value).isEqualByComparingTo(BigDecimal("0.20"))
        }
    }

}
