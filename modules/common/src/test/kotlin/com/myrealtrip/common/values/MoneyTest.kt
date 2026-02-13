package com.myrealtrip.common.values

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.math.BigDecimal
import java.util.Currency

@DisplayName("Money")
class MoneyTest {

    @Nested
    @DisplayName("Factory Methods")
    inner class FactoryMethods {

        @Test
        fun `should create money from BigDecimal and currency code`(): Unit {
            // given
            val amount = BigDecimal("10000")

            // when
            val money = Money.of(amount, "KRW")

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("10000"))
            assertThat(money.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should create money from Long and currency code`(): Unit {
            // when
            val money = Money.of(10000L, "KRW")

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("10000"))
            assertThat(money.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should create money from Double and currency code`(): Unit {
            // when
            val money = Money.of(99.99, "USD")

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("99.99"))
            assertThat(money.currencyCode).isEqualTo("USD")
        }

        @Test
        fun `should create money from String and currency code`(): Unit {
            // when
            val money = Money.of("123.45", "EUR")

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("123.45"))
            assertThat(money.currencyCode).isEqualTo("EUR")
        }

        @Test
        fun `should create money with Currency object`(): Unit {
            // given
            val currency = Currency.getInstance("USD")

            // when
            val money = Money.of(BigDecimal("50.00"), currency)

            // then
            assertThat(money.currency).isEqualTo(currency)
            assertThat(money.currencyCode).isEqualTo("USD")
        }

        @Test
        fun `should create zero money`(): Unit {
            // when
            val zero = Money.zero("KRW")

            // then
            assertThat(zero.isZero()).isTrue
            assertThat(zero.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should apply correct scale for currency`(): Unit {
            // when
            val krw = Money.of(1000, "KRW")  // KRW has 0 decimal places
            val usd = Money.of(100, "USD")   // USD has 2 decimal places

            // then
            assertThat(krw.scale).isEqualTo(0)
            assertThat(usd.scale).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Convenience Factory Methods")
    inner class ConvenienceFactoryMethods {

        @Test
        fun `should create KRW money`(): Unit {
            // when
            val money = Money.krw(10000)

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("10000"))
            assertThat(money.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should create USD money`(): Unit {
            // when
            val money = Money.usd(99.99)

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("99.99"))
            assertThat(money.currencyCode).isEqualTo("USD")
        }

        @Test
        fun `should create EUR money`(): Unit {
            // when
            val money = Money.eur(50.00)

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("50.00"))
            assertThat(money.currencyCode).isEqualTo("EUR")
        }

        @Test
        fun `should create JPY money`(): Unit {
            // when
            val money = Money.jpy(1000)

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("1000"))
            assertThat(money.currencyCode).isEqualTo("JPY")
        }

        @Test
        fun `should create money using extension functions`(): Unit {
            // when
            val krw = 10000.krw
            val usd = 99.99.usd
            val eur = 50.00.eur
            val jpy = 1000.jpy

            // then
            assertThat(krw.currencyCode).isEqualTo("KRW")
            assertThat(usd.currencyCode).isEqualTo("USD")
            assertThat(eur.currencyCode).isEqualTo("EUR")
            assertThat(jpy.currencyCode).isEqualTo("JPY")
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations - Same Currency")
    inner class ArithmeticOperationsSameCurrency {

        @Test
        fun `should add two money values`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(5000)

            // when
            val result = money1 + money2

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("15000"))
            assertThat(result.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should subtract two money values`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(3000)

            // when
            val result = money1 - money2

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("7000"))
        }

        @Test
        fun `should multiply money by int scalar`(): Unit {
            // given
            val money = Money.krw(1000)

            // when
            val result = money * 3

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("3000"))
        }

        @Test
        fun `should multiply money by long scalar`(): Unit {
            // given
            val money = Money.krw(1000)

            // when
            val result = money * 3L

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("3000"))
        }

        @Test
        fun `should multiply money by BigDecimal scalar`(): Unit {
            // given
            val money = Money.usd(100.00)

            // when
            val result = money * BigDecimal("1.5")

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("150.00"))
        }

        @Test
        fun `should divide money by int scalar`(): Unit {
            // given
            val money = Money.krw(9000)

            // when
            val result = money / 3

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("3000"))
        }

        @Test
        fun `should divide money by BigDecimal scalar`(): Unit {
            // given
            val money = Money.usd(100.00)

            // when
            val result = money / BigDecimal("4")

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("25.00"))
        }

        @Test
        fun `should negate money`(): Unit {
            // given
            val money = Money.krw(1000)

            // when
            val result = -money

            // then
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("-1000"))
        }

        @Test
        fun `should throw exception when dividing by zero`(): Unit {
            // given
            val money = Money.krw(1000)

            // when & then
            assertThatThrownBy { money / 0 }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot divide by zero")
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations - Different Currency")
    inner class ArithmeticOperationsDifferentCurrency {

        @Test
        fun `should throw exception when adding different currencies`(): Unit {
            // given
            val krw = Money.krw(10000)
            val usd = Money.usd(100.00)

            // when & then
            assertThatThrownBy { krw + usd }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Currency mismatch")
        }

        @Test
        fun `should throw exception when subtracting different currencies`(): Unit {
            // given
            val krw = Money.krw(10000)
            val usd = Money.usd(100.00)

            // when & then
            assertThatThrownBy { krw - usd }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Currency mismatch")
        }
    }

    @Nested
    @DisplayName("Rate Operations")
    inner class RateOperations {

        @Test
        fun `should multiply money by rate`(): Unit {
            // given
            val price = Money.krw(10000)
            val discountRate = Rate.ofPercent(15)

            // when
            val discount = price * discountRate

            // then
            assertThat(discount.amount).isEqualByComparingTo(BigDecimal("1500"))
        }

        @Test
        fun `should apply rate to money`(): Unit {
            // given
            val price = Money.usd(100.00)
            val taxRate = Rate.ofPercent(10)

            // when
            val tax = price.applyRate(taxRate)

            // then
            assertThat(tax.amount).isEqualByComparingTo(BigDecimal("10.00"))
        }

        @Test
        fun `should calculate remainder after rate`(): Unit {
            // given
            val price = Money.krw(10000)
            val discountRate = Rate.ofPercent(20)

            // when
            val afterDiscount = price.remainderAfterRate(discountRate)

            // then
            assertThat(afterDiscount.amount).isEqualByComparingTo(BigDecimal("8000"))
        }

        @Test
        fun `should add rate to money`(): Unit {
            // given
            val price = Money.usd(100.00)
            val taxRate = Rate.ofPercent(10)

            // when
            val priceWithTax = price.addRate(taxRate)

            // then
            assertThat(priceWithTax.amount).isEqualByComparingTo(BigDecimal("110.00"))
        }
    }

    @Nested
    @DisplayName("Comparison Methods")
    inner class ComparisonMethods {

        @Test
        fun `should compare money values`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(5000)
            val money3 = Money.krw(10000)

            // when & then
            assertThat(money1 > money2).isTrue
            assertThat(money2 < money1).isTrue
            assertThat(money1.compareTo(money3)).isEqualTo(0)
        }

        @Test
        fun `should check if greater than`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(5000)

            // when & then
            assertThat(money1.isGreaterThan(money2)).isTrue
            assertThat(money2.isGreaterThan(money1)).isFalse
        }

        @Test
        fun `should check if less than`(): Unit {
            // given
            val money1 = Money.krw(5000)
            val money2 = Money.krw(10000)

            // when & then
            assertThat(money1.isLessThan(money2)).isTrue
            assertThat(money2.isLessThan(money1)).isFalse
        }

        @Test
        fun `should throw exception when comparing different currencies`(): Unit {
            // given
            val krw = Money.krw(10000)
            val usd = Money.usd(100.00)

            // when & then
            assertThatThrownBy { krw.compareTo(usd) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Currency mismatch")
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    inner class UtilityMethods {

        @Test
        fun `should check if zero`(): Unit {
            // when & then
            assertThat(Money.zero("KRW").isZero()).isTrue
            assertThat(Money.krw(1000).isZero()).isFalse
        }

        @Test
        fun `should check if positive`(): Unit {
            // when & then
            assertThat(Money.krw(1000).isPositive()).isTrue
            assertThat(Money.krw(0).isPositive()).isFalse
            assertThat((-Money.krw(1000)).isPositive()).isFalse
        }

        @Test
        fun `should check if negative`(): Unit {
            // when & then
            assertThat((-Money.krw(1000)).isNegative()).isTrue
            assertThat(Money.krw(0).isNegative()).isFalse
            assertThat(Money.krw(1000).isNegative()).isFalse
        }

        @Test
        fun `should return absolute value`(): Unit {
            // given
            val negative = -Money.krw(1000)

            // when
            val abs = negative.abs()

            // then
            assertThat(abs.amount).isEqualByComparingTo(BigDecimal("1000"))
        }

        @Test
        fun `should return minimum`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(5000)

            // when
            val min = money1.min(money2)

            // then
            assertThat(min).isEqualTo(money2)
        }

        @Test
        fun `should return maximum`(): Unit {
            // given
            val money1 = Money.krw(10000)
            val money2 = Money.krw(5000)

            // when
            val max = money1.max(money2)

            // then
            assertThat(max).isEqualTo(money1)
        }

        @Test
        fun `should check same currency`(): Unit {
            // given
            val krw1 = Money.krw(10000)
            val krw2 = Money.krw(5000)
            val usd = Money.usd(100.00)

            // when & then
            assertThat(krw1.isSameCurrency(krw2)).isTrue
            assertThat(krw1.isSameCurrency(usd)).isFalse
        }
    }

    @Nested
    @DisplayName("Formatting")
    inner class Formatting {

        @Test
        fun `should format with currency symbol`(): Unit {
            // given
            val krw = Money.krw(10000)

            // when
            val formatted = krw.format()

            // then
            assertThat(formatted).contains("10,000")
        }

        @Test
        fun `should format with currency code`(): Unit {
            // given
            val krw = Money.krw(10000)

            // when
            val formatted = krw.formatWithCode()

            // then
            assertThat(formatted).isEqualTo("10,000 KRW")
        }

        @Test
        fun `should format USD with decimals`(): Unit {
            // given
            val usd = Money.usd(1234.56)

            // when
            val formatted = usd.formatWithCode()

            // then
            assertThat(formatted).isEqualTo("1,234.56 USD")
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        private val mapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        @Test
        fun `should serialize money to JSON`(): Unit {
            // given
            val money = Money.krw(10000)

            // when
            val json = mapper.writeValueAsString(money)

            // then
            assertThat(json).contains("\"amount\"")
            assertThat(json).contains("\"currency\":\"KRW\"")
        }

        @Test
        fun `should deserialize JSON to money`(): Unit {
            // given
            val json = """{"amount":10000,"currency":"KRW"}"""

            // when
            val money = mapper.readValue(json, Money::class.java)

            // then
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("10000"))
            assertThat(money.currencyCode).isEqualTo("KRW")
        }

        @Test
        fun `should serialize and deserialize money in object`(): Unit {
            // given
            data class Order(val id: Long, val totalPrice: Money)
            val order = Order(1L, Money.usd(99.99))

            // when
            val json = mapper.writeValueAsString(order)
            val deserialized = mapper.readValue(json, Order::class.java)

            // then
            assertThat(deserialized.totalPrice.amount).isEqualByComparingTo(BigDecimal("99.99"))
            assertThat(deserialized.totalPrice.currencyCode).isEqualTo("USD")
        }
    }

    @Nested
    @DisplayName("Currency Constants")
    inner class CurrencyConstants {

        @Test
        fun `should have correct currency constants`(): Unit {
            // when & then
            assertThat(Money.KRW.currencyCode).isEqualTo("KRW")
            assertThat(Money.USD.currencyCode).isEqualTo("USD")
            assertThat(Money.EUR.currencyCode).isEqualTo("EUR")
            assertThat(Money.JPY.currencyCode).isEqualTo("JPY")
            assertThat(Money.CNY.currencyCode).isEqualTo("CNY")
            assertThat(Money.GBP.currencyCode).isEqualTo("GBP")
        }
    }
}
