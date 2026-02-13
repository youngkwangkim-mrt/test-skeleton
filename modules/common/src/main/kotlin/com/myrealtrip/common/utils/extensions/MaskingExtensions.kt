@file:JvmName("MaskingExt")

package com.myrealtrip.common.utils.extensions

private const val MASK_CHAR = '*'
private const val UNKNOWN_NAME_MASK = "***"
private const val DEFAULT_VISIBLE_SUFFIX = 4

private fun Char.isHangul(): Boolean =
    this in '\uAC00'..'\uD7A3' || this in '\u3131'..'\u318E'

/**
 * Masks characters between [start] and [end] indices.
 *
 * @param start starting index to mask (inclusive, default: 0)
 * @param end ending index to mask (exclusive, default: length - 4)
 * @param maskChar character to use for masking (default: '*')
 * @return masked string
 *
 * Examples:
 * - "1234567890".mask() -> "******7890"
 * - "1234567890".mask(2, 6) -> "12****7890"
 * - "1234".mask() -> "****"
 */
@JvmOverloads
fun String.mask(start: Int = 0, end: Int = length - DEFAULT_VISIBLE_SUFFIX, maskChar: Char = MASK_CHAR): String {
    if (length <= DEFAULT_VISIBLE_SUFFIX) return buildString(DEFAULT_VISIBLE_SUFFIX) {
        repeat(DEFAULT_VISIBLE_SUFFIX) { append(maskChar) }
    }

    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(safeStart, length)

    if (safeStart >= safeEnd) return this

    return buildString(length) {
        append(this@mask, 0, safeStart)
        repeat(safeEnd - safeStart) { append(maskChar) }
        append(this@mask, safeEnd, this@mask.length)
    }
}

/**
 * Masks a name by detecting the script and delegating to the appropriate masking strategy.
 *
 * @param maskChar character to use for masking (default: '*')
 * @return masked name
 *
 * Examples:
 * - "홍길동".maskName() -> "홍*동"
 * - "John Hong".maskName() -> "**** Hong"
 * - "John".maskName() -> "Jo**"
 */
@JvmOverloads
fun String.maskName(maskChar: Char = MASK_CHAR): String {
    if (isBlank()) return UNKNOWN_NAME_MASK

    return if (any { it.isHangul() }) {
        maskKoreanName(maskChar)
    } else {
        maskEnglishName(maskChar)
    }
}

/**
 * Masks a Korean name by hiding middle characters, showing first and last character.
 *
 * @param maskChar character to use for masking
 * @return masked Korean name
 *
 * Examples:
 * - "홍길동".maskKoreanName() -> "홍*동"
 * - "홍길동수".maskKoreanName() -> "홍**수"
 * - "AB".maskKoreanName() -> "A*"
 * - "A".maskKoreanName() -> "***"
 */
private fun String.maskKoreanName(maskChar: Char): String = when (length) {
    0, 1 -> UNKNOWN_NAME_MASK
    2 -> "${first()}$maskChar"
    else -> buildString(length) {
        append(this@maskKoreanName.first())
        repeat(this@maskKoreanName.length - 2) { append(maskChar) }
        append(this@maskKoreanName.last())
    }
}

/**
 * Masks an English name by hiding first/middle names, showing only the last name.
 * For single-word names, shows the first two characters and masks the rest.
 *
 * @param maskChar character to use for masking
 * @return masked English name
 *
 * Examples:
 * - "John Hong".maskEnglishName() -> "**** Hong"
 * - "John Michael Hong".maskEnglishName() -> "**** ******* Hong"
 * - "John".maskEnglishName() -> "Jo**"
 * - "Jo".maskEnglishName() -> "J*"
 * - "J".maskEnglishName() -> "***"
 */
private fun String.maskEnglishName(maskChar: Char): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }

    if (parts.size < 2) {
        val word = parts[0]
        return when (word.length) {
            1 -> UNKNOWN_NAME_MASK
            2 -> "${word[0]}$maskChar"
            else -> buildString(word.length) {
                append(word, 0, 2)
                repeat(word.length - 2) { append(maskChar) }
            }
        }
    }

    val lastName = parts.last()
    val maskedFirstNames = parts.dropLast(1).joinToString(" ") { part ->
        "$maskChar".repeat(part.length)
    }
    return "$maskedFirstNames $lastName"
}

/**
 * Masks digits while preserving non-digit characters (spaces, dashes, etc.).
 *
 * @param visibleDigits number of digits to show at the end (default: 4)
 * @param maskChar character to use for masking (default: '*')
 * @return masked string with format preserved
 *
 * Examples:
 * - "+82 10-1234-5678".maskDigits() -> "+** **-****-5678"
 * - "010-1234-5678".maskDigits(4) -> "***-****-5678"
 * - "1234-5678-9012-3456".maskDigits(4) -> "****-****-****-3456"
 */
@JvmOverloads
fun String.maskDigits(visibleDigits: Int = 4, maskChar: Char = MASK_CHAR): String {
    val digitCount = count { it.isDigit() }
    if (digitCount <= visibleDigits) return this

    val digitsToMask = digitCount - visibleDigits

    return buildString(length) {
        var maskedCount = 0
        for (char in this@maskDigits) {
            if (char.isDigit() && maskedCount < digitsToMask) {
                maskedCount++
                append(maskChar)
            } else {
                append(char)
            }
        }
    }
}

/**
 * Masks an email address by hiding part of the local part.
 *
 * @param visibleChars number of characters to show at start of local part (default: 2)
 * @param maskChar character to use for masking (default: '*')
 * @return masked email
 *
 * Examples:
 * - "user@example.com".maskEmail() -> "us**@example.com"
 * - "username@domain.co.kr".maskEmail() -> "us******@domain.co.kr"
 * - "ab@example.com".maskEmail() -> "ab@example.com" (too short to mask)
 */
@JvmOverloads
fun String.maskEmail(visibleChars: Int = 2, maskChar: Char = MASK_CHAR): String {
    val atIndex = indexOf('@')
    if (atIndex <= 0 || atIndex >= length - 1) return this

    val localPart = substring(0, atIndex)
    val domainPart = substring(atIndex + 1)

    if (localPart.length <= visibleChars) return this

    return buildString(length) {
        append(localPart, 0, visibleChars)
        repeat(localPart.length - visibleChars) { append(maskChar) }
        append('@')
        append(domainPart)
    }
}
