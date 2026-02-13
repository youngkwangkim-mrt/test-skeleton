package com.myrealtrip.common.utils

import java.io.Serializable

/**
 * Represents a four-tuple of values.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @property first the first value.
 * @property second the second value.
 * @property third the third value.
 * @property fourth the fourth value.
 * @see kotlin.Pair
 */
data class Quartet<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
) : Serializable {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * Represents a five-tuple of values.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @param E type of the fifth value.
 * @property first the first value.
 * @property second the second value.
 * @property third the third value.
 * @property fourth the fourth value.
 * @property fifth the fifth value.
 * @see kotlin.Pair
 */
data class Quintet<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) : Serializable {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

/**
 * Creates a tuple of type [Pair] from this and that.
 * @param that the second value.
 */
infix fun <A, B> A.and(that: B): Pair<A, B> = Pair(this, that)

/**
 * Creates a tuple of type [Triple] from [Pair] and that.
 * @param that the third value.
 */
infix fun <A, B, C> Pair<A, B>.and(that: C): Triple<A, B, C> = Triple(this.first, this.second, that)

/**
 * Creates a tuple of type [Quartet] from [Triple] and that.
 * @param that the fourth value.
 */
infix fun <A, B, C, D> Triple<A, B, C>.and(that: D): Quartet<A, B, C, D> =
    Quartet(this.first, this.second, this.third, that)

/**
 * Creates a tuple of type [Quintet] from [Quartet] and that.
 * @param that the fifth value.
 */
infix fun <A, B, C, D, E> Quartet<A, B, C, D>.and(that: E): Quintet<A, B, C, D, E> =
    Quintet(this.first, this.second, this.third, this.fourth, that)

/**
 * Converts [Quartet] tuple to a list.
 */
fun <T> Quartet<T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth)

/**
 * Converts [Quintet] tuple to a list.
 */
fun <T> Quintet<T, T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth, fifth)