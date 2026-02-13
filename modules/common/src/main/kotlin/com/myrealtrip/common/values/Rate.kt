package com.myrealtrip.common.values

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a rate (ratio) value between 0 and 1.
 *
 * This class provides a type-safe way to handle percentage calculations.
 * Internally stores values as decimals (0-1), but supports creation from percentages (0-100).
 *
 * Usage:
 * ```kotlin
 * // Create from decimal (0-1)
 * val rate = Rate.of(0.15)           // 15%
 *
 * // Create from percentage (0-100)
 * val rate = Rate.ofPercent(15)      // 15%
 *
 * // Apply to amount
 * val discount = rate.applyTo(10000) // 1500
 *
 * // Convert to percentage
 * rate.toPercent()                   // 15.0000
 * rate.toPercentString()             // "15%"
 *
 * // Arithmetic operations
 * val combined = rate1 + rate2
 * val doubled = rate * 2
 * ```
 *
 * @property value the decimal value (0-1 range)
 */
@JvmInline
value class Rate private constructor(
    @get:JsonValue
    val value: BigDecimal,
) : Comparable<Rate> {

    companion object {
        /** Default scale for rate calculations */
        const val DEFAULT_SCALE: Int = 4

        /** Default rounding mode */
        val DEFAULT_ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP

        private val HUNDRED = BigDecimal(100)

        // =============================================================================
        // Common Constants
        // =============================================================================

        /** Rate of 0% */
        val ZERO: Rate = Rate(BigDecimal.ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE))

        /** Rate of 100% */
        val ONE: Rate = Rate(BigDecimal.ONE.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE))

        /** Rate of 5% */
        val FIVE_PERCENT: Rate = ofPercent(5)

        /** Rate of 10% */
        val TEN_PERCENT: Rate = ofPercent(10)

        /** Rate of 50% */
        val HALF: Rate = ofPercent(50)

        // =============================================================================
        // Factory Methods
        // =============================================================================

        /**
         * Creates a [Rate] from a decimal value (0-1 range).
         * Used for JSON deserialization.
         *
         * @param decimal the decimal value (e.g., 0.15 for 15%)
         * @return a new [Rate] instance with default scale
         * @throws IllegalArgumentException if decimal is not between 0 and 1
         */
        @JvmStatic
        @JsonCreator
        fun of(decimal: BigDecimal): Rate = of(decimal, DEFAULT_SCALE)

        /**
         * Creates a [Rate] from a decimal value (0-1 range).
         *
         * @param decimal the decimal value (e.g., 0.15 for 15%)
         * @param scale the number of decimal places (default: 4)
         * @return a new [Rate] instance
         * @throws IllegalArgumentException if decimal is not between 0 and 1
         */
        @JvmStatic
        fun of(decimal: BigDecimal, scale: Int): Rate {
            require(decimal >= BigDecimal.ZERO && decimal <= BigDecimal.ONE) {
                "Rate must be between 0 and 1, but was: $decimal"
            }
            return Rate(decimal.setScale(scale, DEFAULT_ROUNDING_MODE))
        }

        /**
         * Creates a [Rate] from a decimal value (0-1 range).
         *
         * @param decimal the decimal value as String (e.g., "0.15" for 15%)
         * @param scale the number of decimal places (default: 4)
         * @return a new [Rate] instance
         */
        @JvmStatic
        @JvmOverloads
        fun of(decimal: String, scale: Int = DEFAULT_SCALE): Rate {
            return of(BigDecimal(decimal), scale)
        }

        /**
         * Creates a [Rate] from a decimal value (0-1 range).
         *
         * @param decimal the decimal value (e.g., 0.15 for 15%)
         * @param scale the number of decimal places (default: 4)
         * @return a new [Rate] instance
         */
        @JvmStatic
        @JvmOverloads
        fun of(decimal: Double, scale: Int = DEFAULT_SCALE): Rate {
            return of(BigDecimal.valueOf(decimal), scale)
        }

        /**
         * Creates a [Rate] from a percentage value (0-100 range).
         *
         * @param percent the percentage value (e.g., 15 for 15%)
         * @param scale the number of decimal places for the resulting decimal (default: 4)
         * @return a new [Rate] instance
         * @throws IllegalArgumentException if percent is not between 0 and 100
         */
        @JvmStatic
        @JvmOverloads
        fun ofPercent(percent: BigDecimal, scale: Int = DEFAULT_SCALE): Rate {
            require(percent >= BigDecimal.ZERO && percent <= HUNDRED) {
                "Percent must be between 0 and 100, but was: $percent"
            }
            val decimal = percent.divide(HUNDRED, scale + 2, DEFAULT_ROUNDING_MODE)
            return Rate(decimal.setScale(scale, DEFAULT_ROUNDING_MODE))
        }

        /**
         * Creates a [Rate] from a percentage value (0-100 range).
         *
         * @param percent the percentage value (e.g., 15 for 15%)
         * @param scale the number of decimal places for the resulting decimal (default: 4)
         * @return a new [Rate] instance
         */
        @JvmStatic
        @JvmOverloads
        fun ofPercent(percent: Number, scale: Int = DEFAULT_SCALE): Rate {
            return ofPercent(BigDecimal(percent.toString()), scale)
        }

        /**
         * Creates a [Rate] from a percentage value (0-100 range).
         *
         * @param percent the percentage value as String (e.g., "15.5" for 15.5%)
         * @param scale the number of decimal places for the resulting decimal (default: 4)
         * @return a new [Rate] instance
         */
        @JvmStatic
        @JvmOverloads
        fun ofPercent(percent: String, scale: Int = DEFAULT_SCALE): Rate {
            return ofPercent(BigDecimal(percent), scale)
        }
    }

    // =============================================================================
    // Conversion Methods
    // =============================================================================

    /**
     * Returns the rate as a percentage value (0-100).
     *
     * @param scale the number of decimal places (default: same as internal scale)
     * @return the percentage value
     */
    fun toPercent(scale: Int = value.scale()): BigDecimal {
        return value.multiply(HUNDRED).setScale(scale, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Returns the rate as a decimal value (0-1).
     *
     * @return the decimal value
     */
    fun toDecimal(): BigDecimal = value

    /**
     * Returns a formatted percentage string (e.g., "15%", "15.5%").
     *
     * @param scale the number of decimal places for the percentage (default: 0)
     * @return the formatted percentage string
     */
    @JvmOverloads
    fun toPercentString(scale: Int = 0): String {
        val percent = toPercent(scale)
        return if (scale == 0) {
            "${percent.toInt()}%"
        } else {
            "$percent%"
        }
    }

    // =============================================================================
    // Calculation Methods
    // =============================================================================

    /**
     * Applies this rate to the given amount.
     *
     * @param amount the amount to apply the rate to
     * @param scale the number of decimal places for the result (default: same as amount)
     * @return the calculated amount (amount * rate)
     */
    @JvmOverloads
    fun applyTo(amount: BigDecimal, scale: Int = amount.scale()): BigDecimal {
        return amount.multiply(value).setScale(scale, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Applies this rate to the given amount.
     *
     * @param amount the amount to apply the rate to
     * @return the calculated amount (amount * rate)
     */
    fun applyTo(amount: Long): BigDecimal {
        return applyTo(BigDecimal.valueOf(amount), DEFAULT_SCALE)
    }

    /**
     * Applies this rate to the given amount.
     *
     * @param amount the amount to apply the rate to
     * @return the calculated amount (amount * rate)
     */
    fun applyTo(amount: Int): BigDecimal {
        return applyTo(BigDecimal.valueOf(amount.toLong()), DEFAULT_SCALE)
    }

    /**
     * Calculates the remaining amount after applying this rate.
     *
     * @param amount the original amount
     * @param scale the number of decimal places for the result (default: same as amount)
     * @return the remaining amount (amount * (1 - rate))
     */
    @JvmOverloads
    fun remainderOf(amount: BigDecimal, scale: Int = amount.scale()): BigDecimal {
        return amount.subtract(applyTo(amount, scale)).setScale(scale, DEFAULT_ROUNDING_MODE)
    }

    // =============================================================================
    // Arithmetic Operations
    // =============================================================================

    /**
     * Adds two rates.
     *
     * @param other the rate to add
     * @return the sum of the rates (capped at 1.0)
     */
    operator fun plus(other: Rate): Rate {
        val sum = value.add(other.value)
        if (sum > BigDecimal.ONE) return ONE
        val resultScale = maxOf(value.scale(), other.value.scale())
        return Rate(sum.setScale(resultScale, DEFAULT_ROUNDING_MODE))
    }

    /**
     * Subtracts a rate from this rate.
     *
     * @param other the rate to subtract
     * @return the difference (floored at 0.0)
     */
    operator fun minus(other: Rate): Rate {
        val diff = value.subtract(other.value)
        if (diff < BigDecimal.ZERO) return ZERO
        val resultScale = maxOf(value.scale(), other.value.scale())
        return Rate(diff.setScale(resultScale, DEFAULT_ROUNDING_MODE))
    }

    /**
     * Multiplies this rate by a scalar.
     *
     * @param scalar the multiplier
     * @return the scaled rate (capped at 1.0)
     */
    operator fun times(scalar: Int): Rate {
        val product = value.multiply(BigDecimal.valueOf(scalar.toLong()))
        if (product > BigDecimal.ONE) return ONE
        return Rate(product.setScale(value.scale(), DEFAULT_ROUNDING_MODE))
    }

    /**
     * Multiplies this rate by a scalar.
     *
     * @param scalar the multiplier
     * @return the scaled rate (capped at 1.0)
     */
    operator fun times(scalar: BigDecimal): Rate {
        val product = value.multiply(scalar)
        if (product > BigDecimal.ONE) return ONE
        return Rate(product.setScale(value.scale(), DEFAULT_ROUNDING_MODE))
    }

    /**
     * Divides this rate by a scalar.
     *
     * @param scalar the divisor
     * @return the divided rate
     * @throws IllegalArgumentException if scalar is zero
     */
    operator fun div(scalar: Int): Rate {
        require(scalar != 0) { "Cannot divide by zero" }
        return Rate(value.divide(BigDecimal.valueOf(scalar.toLong()), value.scale(), DEFAULT_ROUNDING_MODE))
    }

    // =============================================================================
    // Comparison & Utility Methods
    // =============================================================================

    override fun compareTo(other: Rate): Int = value.compareTo(other.value)

    /**
     * Checks if this rate is zero.
     */
    fun isZero(): Boolean = value.compareTo(BigDecimal.ZERO) == 0

    /**
     * Checks if this rate is positive (greater than zero).
     */
    fun isPositive(): Boolean = value > BigDecimal.ZERO

    /**
     * Returns a new rate with the specified scale.
     *
     * @param scale the number of decimal places
     * @return a new [Rate] with the specified scale
     */
    fun withScale(scale: Int): Rate {
        return Rate(value.setScale(scale, DEFAULT_ROUNDING_MODE))
    }

    override fun toString(): String = "Rate($value)"
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Creates a [Rate] from this number as a percentage.
 *
 * Usage:
 * ```kotlin
 * val rate = 15.percent  // Rate of 15%
 * ```
 */
val Number.percent: Rate
    get() = Rate.ofPercent(this)

/**
 * Creates a [Rate] from this number as a decimal.
 *
 * Usage:
 * ```kotlin
 * val rate = 0.15.asRate  // Rate of 15%
 * ```
 */
val Double.asRate: Rate
    get() = Rate.of(this)
