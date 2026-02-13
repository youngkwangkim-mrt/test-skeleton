package com.myrealtrip.common.values

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Represents a monetary value with currency.
 *
 * This class provides a type-safe way to handle monetary calculations
 * with support for multiple currencies. Operations between different
 * currencies are not allowed and will throw an exception.
 *
 * Usage:
 * ```kotlin
 * // Create money
 * val price = Money.of(10000, "KRW")
 * val usd = Money.of(99.99, Currency.getInstance("USD"))
 *
 * // Arithmetic operations (same currency only)
 * val total = price1 + price2
 * val change = price1 - price2
 *
 * // Scalar operations
 * val doubled = price * 2
 * val half = price / 2
 *
 * // Apply rate (discount, tax, etc.)
 * val discount = price * Rate.ofPercent(15)
 *
 * // Formatting
 * price.format()  // "₩10,000" or "10,000 KRW"
 * ```
 *
 * @property amount the monetary amount
 * @property currency the currency
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {

    companion object {
        /** Default scale for monetary calculations */
        const val DEFAULT_SCALE: Int = 2

        /** Default rounding mode */
        val DEFAULT_ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP

        // =============================================================================
        // Common Currency Constants
        // =============================================================================

        val KRW: Currency = Currency.getInstance("KRW")
        val USD: Currency = Currency.getInstance("USD")
        val EUR: Currency = Currency.getInstance("EUR")
        val JPY: Currency = Currency.getInstance("JPY")
        val CNY: Currency = Currency.getInstance("CNY")
        val GBP: Currency = Currency.getInstance("GBP")

        // =============================================================================
        // Factory Methods
        // =============================================================================

        /**
         * Creates [Money] from amount and currency code.
         *
         * @param amount the monetary amount
         * @param currencyCode ISO 4217 currency code (e.g., "KRW", "USD")
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: BigDecimal, currencyCode: String): Money {
            return of(amount, Currency.getInstance(currencyCode))
        }

        /**
         * Creates [Money] from amount and currency.
         *
         * @param amount the monetary amount
         * @param currency the currency
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: BigDecimal, currency: Currency): Money {
            val scale = currency.defaultFractionDigits.coerceAtLeast(0)
            return Money(amount.setScale(scale, DEFAULT_ROUNDING_MODE), currency)
        }

        /**
         * Creates [Money] from amount and currency code.
         *
         * @param amount the monetary amount
         * @param currencyCode ISO 4217 currency code (e.g., "KRW", "USD")
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: Long, currencyCode: String): Money {
            return of(BigDecimal.valueOf(amount), currencyCode)
        }

        /**
         * Creates [Money] from amount and currency.
         *
         * @param amount the monetary amount
         * @param currency the currency
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: Long, currency: Currency): Money {
            return of(BigDecimal.valueOf(amount), currency)
        }

        /**
         * Creates [Money] from amount and currency code.
         *
         * @param amount the monetary amount
         * @param currencyCode ISO 4217 currency code (e.g., "KRW", "USD")
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: Double, currencyCode: String): Money {
            return of(BigDecimal.valueOf(amount), currencyCode)
        }

        /**
         * Creates [Money] from amount and currency.
         *
         * @param amount the monetary amount
         * @param currency the currency
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: Double, currency: Currency): Money {
            return of(BigDecimal.valueOf(amount), currency)
        }

        /**
         * Creates [Money] from amount string and currency code.
         *
         * @param amount the monetary amount as string
         * @param currencyCode ISO 4217 currency code (e.g., "KRW", "USD")
         * @return a new [Money] instance
         */
        @JvmStatic
        fun of(amount: String, currencyCode: String): Money {
            return of(BigDecimal(amount), currencyCode)
        }

        /**
         * Creates [Money] with zero amount for the given currency.
         *
         * @param currencyCode ISO 4217 currency code
         * @return a new [Money] instance with zero amount
         */
        @JvmStatic
        fun zero(currencyCode: String): Money {
            return of(BigDecimal.ZERO, currencyCode)
        }

        /**
         * Creates [Money] with zero amount for the given currency.
         *
         * @param currency the currency
         * @return a new [Money] instance with zero amount
         */
        @JvmStatic
        fun zero(currency: Currency): Money {
            return of(BigDecimal.ZERO, currency)
        }

        /**
         * Creates [Money] from JSON.
         */
        @JvmStatic
        @JsonCreator
        fun fromJson(
            @JsonProperty("amount") amount: BigDecimal,
            @JsonProperty("currency") currencyCode: String,
        ): Money {
            return of(amount, currencyCode)
        }

        // =============================================================================
        // Convenience Factory Methods for Common Currencies
        // =============================================================================

        /** Creates [Money] in Korean Won */
        @JvmStatic
        fun krw(amount: Long): Money = of(amount, KRW)

        /** Creates [Money] in Korean Won */
        @JvmStatic
        fun krw(amount: BigDecimal): Money = of(amount, KRW)

        /** Creates [Money] in US Dollars */
        @JvmStatic
        fun usd(amount: Double): Money = of(amount, USD)

        /** Creates [Money] in US Dollars */
        @JvmStatic
        fun usd(amount: BigDecimal): Money = of(amount, USD)

        /** Creates [Money] in Euros */
        @JvmStatic
        fun eur(amount: Double): Money = of(amount, EUR)

        /** Creates [Money] in Euros */
        @JvmStatic
        fun eur(amount: BigDecimal): Money = of(amount, EUR)

        /** Creates [Money] in Japanese Yen */
        @JvmStatic
        fun jpy(amount: Long): Money = of(amount, JPY)

        /** Creates [Money] in Japanese Yen */
        @JvmStatic
        fun jpy(amount: BigDecimal): Money = of(amount, JPY)
    }

    init {
        require(amount.scale() >= 0) { "Amount scale must be non-negative" }
    }

    // =============================================================================
    // Properties
    // =============================================================================

    /** Currency code (e.g., "KRW", "USD") */
    @get:JsonProperty("currency")
    val currencyCode: String
        get() = currency.currencyCode

    /** Currency symbol (e.g., "₩", "$") */
    val currencySymbol: String
        get() = currency.symbol

    /** Number of decimal places for this currency */
    val scale: Int
        get() = amount.scale()

    // =============================================================================
    // Arithmetic Operations
    // =============================================================================

    /**
     * Adds two money values.
     *
     * @param other the money to add
     * @return the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.add(other.amount), currency)
    }

    /**
     * Subtracts money from this value.
     *
     * @param other the money to subtract
     * @return the difference
     * @throws IllegalArgumentException if currencies don't match
     */
    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.subtract(other.amount), currency)
    }

    /**
     * Multiplies this money by a scalar.
     *
     * @param multiplier the multiplier
     * @return the product
     */
    operator fun times(multiplier: Int): Money {
        return Money(
            amount.multiply(BigDecimal.valueOf(multiplier.toLong()))
                .setScale(scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Multiplies this money by a scalar.
     *
     * @param multiplier the multiplier
     * @return the product
     */
    operator fun times(multiplier: Long): Money {
        return Money(
            amount.multiply(BigDecimal.valueOf(multiplier))
                .setScale(scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Multiplies this money by a scalar.
     *
     * @param multiplier the multiplier
     * @return the product
     */
    operator fun times(multiplier: BigDecimal): Money {
        return Money(
            amount.multiply(multiplier).setScale(scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Multiplies this money by a rate.
     * Useful for calculating discounts, taxes, etc.
     *
     * @param rate the rate to apply
     * @return the calculated amount
     */
    operator fun times(rate: Rate): Money {
        return Money(
            amount.multiply(rate.toDecimal()).setScale(scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Divides this money by a scalar.
     *
     * @param divisor the divisor
     * @return the quotient
     * @throws IllegalArgumentException if divisor is zero
     */
    operator fun div(divisor: Int): Money {
        require(divisor != 0) { "Cannot divide by zero" }
        return Money(
            amount.divide(BigDecimal.valueOf(divisor.toLong()), scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Divides this money by a scalar.
     *
     * @param divisor the divisor
     * @return the quotient
     * @throws IllegalArgumentException if divisor is zero
     */
    operator fun div(divisor: BigDecimal): Money {
        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Cannot divide by zero" }
        return Money(
            amount.divide(divisor, scale, DEFAULT_ROUNDING_MODE),
            currency,
        )
    }

    /**
     * Returns the negation of this money.
     *
     * @return the negated money
     */
    operator fun unaryMinus(): Money {
        return Money(amount.negate(), currency)
    }

    // =============================================================================
    // Comparison & Utility Methods
    // =============================================================================

    /**
     * Compares this money to another.
     *
     * @param other the money to compare to
     * @return comparison result
     * @throws IllegalArgumentException if currencies don't match
     */
    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    /**
     * Checks if this money is greater than another.
     */
    fun isGreaterThan(other: Money): Boolean {
        requireSameCurrency(other)
        return amount > other.amount
    }

    /**
     * Checks if this money is less than another.
     */
    fun isLessThan(other: Money): Boolean {
        requireSameCurrency(other)
        return amount < other.amount
    }

    /**
     * Checks if this money is zero.
     */
    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    /**
     * Checks if this money is positive.
     */
    fun isPositive(): Boolean = amount > BigDecimal.ZERO

    /**
     * Checks if this money is negative.
     */
    fun isNegative(): Boolean = amount < BigDecimal.ZERO

    /**
     * Returns the absolute value of this money.
     */
    fun abs(): Money = if (isNegative()) -this else this

    /**
     * Returns the minimum of this money and another.
     */
    fun min(other: Money): Money {
        requireSameCurrency(other)
        return if (this <= other) this else other
    }

    /**
     * Returns the maximum of this money and another.
     */
    fun max(other: Money): Money {
        requireSameCurrency(other)
        return if (this >= other) this else other
    }

    /**
     * Checks if this money has the same currency as another.
     */
    fun isSameCurrency(other: Money): Boolean = currency == other.currency

    // =============================================================================
    // Calculation with Rate
    // =============================================================================

    /**
     * Calculates the portion of this money by applying a rate.
     * Equivalent to `this * rate`.
     *
     * @param rate the rate to apply
     * @return the calculated portion
     */
    fun applyRate(rate: Rate): Money = this * rate

    /**
     * Calculates the remainder after applying a rate.
     * Equivalent to `this - (this * rate)`.
     *
     * @param rate the rate to apply
     * @return the remainder
     */
    fun remainderAfterRate(rate: Rate): Money = this - applyRate(rate)

    /**
     * Adds a percentage to this money.
     * Useful for adding tax, etc.
     *
     * @param rate the rate to add
     * @return the original amount plus the rate portion
     */
    fun addRate(rate: Rate): Money = this + applyRate(rate)

    // =============================================================================
    // Formatting
    // =============================================================================

    /**
     * Formats this money as a string with currency symbol.
     *
     * @return formatted string (e.g., "₩10,000", "$99.99")
     */
    fun format(): String {
        val formatted = String.format("%,.${scale}f", amount)
        return when (currency) {
            KRW, JPY, CNY -> "$currencySymbol$formatted"
            else -> "$currencySymbol$formatted"
        }
    }

    /**
     * Formats this money as a string with currency code.
     *
     * @return formatted string (e.g., "10,000 KRW", "99.99 USD")
     */
    fun formatWithCode(): String {
        val formatted = String.format("%,.${scale}f", amount)
        return "$formatted $currencyCode"
    }

    override fun toString(): String = "Money($amount $currencyCode)"

    // =============================================================================
    // Private Methods
    // =============================================================================

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: cannot operate on $currencyCode and ${other.currencyCode}"
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Creates [Money] in Korean Won from this number.
 *
 * Usage:
 * ```kotlin
 * val price = 10000.krw
 * ```
 */
val Long.krw: Money
    get() = Money.krw(this)

/**
 * Creates [Money] in Korean Won from this number.
 */
val Int.krw: Money
    get() = Money.krw(this.toLong())

/**
 * Creates [Money] in US Dollars from this number.
 *
 * Usage:
 * ```kotlin
 * val price = 99.99.usd
 * ```
 */
val Double.usd: Money
    get() = Money.usd(this)

/**
 * Creates [Money] in Euros from this number.
 */
val Double.eur: Money
    get() = Money.eur(this)

/**
 * Creates [Money] in Japanese Yen from this number.
 */
val Long.jpy: Money
    get() = Money.jpy(this)

/**
 * Creates [Money] in Japanese Yen from this number.
 */
val Int.jpy: Money
    get() = Money.jpy(this.toLong())
