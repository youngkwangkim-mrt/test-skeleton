@file:JvmName("StringExt")

package com.myrealtrip.common.utils.extensions

/**
 * Returns the string itself if it's not null or blank; otherwise, returns the [default] value.
 */
@JvmOverloads
fun String?.ifNullOrBlank(default: String = ""): String = if (isNullOrBlank()) default else this

/**
 * Returns the string with all spaces removed.
 */
fun String.removeAllSpaces(): String = replace(" ", "")
