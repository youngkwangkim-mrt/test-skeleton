@file:JvmName("NumberExt")

package com.myrealtrip.common.utils.extensions

import kotlin.random.Random

/**
 * Represents valid digit lengths (1 to 10) for random number generation.
 */
enum class DigitLength(val length: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
}

private val POWERS_OF_10 = longArrayOf(
    1L,
    10L,
    100L,
    1_000L,
    10_000L,
    100_000L,
    1_000_000L,
    10_000_000L,
    100_000_000L,
    1_000_000_000L,
    10_000_000_000L,
)

// =============================================================================
// Random Number Functions
// =============================================================================

/**
 * @return 0 or 1 randomly
 */
fun randomZeroOrOne(): Int = Random.nextInt(2)

/**
 * @return 0, 1, or 2 randomly
 */
fun randomZeroToTwo(): Int = Random.nextInt(3)

/**
 * @return random integer between [min] and [max] inclusive
 */
fun randomIntBetween(min: Int, max: Int): Int = Random.nextInt((max - min) + 1) + min

/**
 * @return random long between [min] and [max] inclusive
 */
fun randomLongBetween(min: Long, max: Long): Long = Random.nextLong(min, max + 1)

/**
 * Generates a random number with the specified digit length.
 *
 * @param length digit length (compile-time safe)
 * @return random number with exactly [length] digits
 */
fun randomOfLength(length: DigitLength): Long {
    val minValue = POWERS_OF_10[length.length - 1]
    val maxValue = POWERS_OF_10[length.length] - 1
    return Random.nextLong(minValue, maxValue + 1)
}

// =============================================================================
// Number Formatting Extensions
// =============================================================================

/**
 * Formats the number with comma separators and specified decimal places.
 *
 * @param decimalPlaces the number of decimal places to display
 * @return the formatted string with comma separators
 */
fun Number.commaSeparated(decimalPlaces: Int = 0): String {
    require(decimalPlaces >= 0) { "Decimal places must be non-negative." }
    return "%,.${decimalPlaces}f".format(this.toDouble())
}
