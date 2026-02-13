package com.myrealtrip.common.values

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.myrealtrip.common.utils.extensions.maskEmail

/**
 * Represents an email address with validation.
 *
 * This class provides a type-safe way to handle email addresses
 * with format validation and convenient accessors for email parts.
 *
 * Usage:
 * ```kotlin
 * // Create email
 * val email = Email.of("user@example.com")
 *
 * // Access parts
 * email.localPart   // "user"
 * email.domain      // "example.com"
 *
 * // Masking for privacy
 * email.masked()    // "us**@example.com"
 * ```
 *
 * @property value the email address string
 */
@JvmInline
value class Email private constructor(
    @get:JsonValue
    val value: String,
) : Comparable<Email> {

    companion object {
        /**
         * Basic email regex pattern.
         * Validates common email formats without being overly strict.
         */
        private val EMAIL_PATTERN = Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$"
        )

        /**
         * Creates an [Email] from a string value.
         *
         * @param email the email address string
         * @return a new [Email] instance
         * @throws IllegalArgumentException if email format is invalid
         */
        @JvmStatic
        @JsonCreator
        fun of(email: String): Email {
            val trimmed = email.trim().lowercase()
            require(trimmed.isNotBlank()) { "Email cannot be blank" }
            require(EMAIL_PATTERN.matches(trimmed)) {
                "Invalid email format: $email"
            }
            return Email(trimmed)
        }

        /**
         * Creates an [Email] from a string value, returning null if invalid.
         *
         * @param email the email address string
         * @return a new [Email] instance or null if invalid
         */
        @JvmStatic
        fun ofOrNull(email: String?): Email? {
            if (email.isNullOrBlank()) return null
            return runCatching { of(email) }.getOrNull()
        }

        /**
         * Checks if the given string is a valid email format.
         *
         * @param email the email address string to validate
         * @return true if valid, false otherwise
         */
        @JvmStatic
        fun isValid(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return EMAIL_PATTERN.matches(email.trim().lowercase())
        }
    }

    /**
     * Returns the local part of the email (before @).
     */
    val localPart: String
        get() = value.substringBefore('@')

    /**
     * Returns the domain part of the email (after @).
     */
    val domain: String
        get() = value.substringAfter('@')

    /**
     * Returns the top-level domain (e.g., "com", "co.kr").
     */
    val topLevelDomain: String
        get() = domain.substringAfterLast('.')

    /**
     * Returns a masked version of the email for privacy.
     *
     * Examples:
     * - "username@example.com".masked() -> "us******@example.com"
     * - "username@example.com".masked(3) -> "use*****@example.com"
     * - "username@example.com".masked(2, '#') -> "us######@example.com"
     *
     * @param visibleChars number of characters to show at the start (default: 2)
     * @param maskChar character to use for masking (default: '*')
     * @return masked email string
     */
    fun masked(visibleChars: Int = 2, maskChar: Char = '*'): String =
        value.maskEmail(visibleChars, maskChar)

    override fun compareTo(other: Email): Int = value.compareTo(other.value)

    override fun toString(): String = value
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Converts this string to an [Email].
 *
 * @throws IllegalArgumentException if email format is invalid
 */
val String.asEmail: Email
    get() = Email.of(this)

/**
 * Converts this string to an [Email], returning null if invalid.
 */
val String.asEmailOrNull: Email?
    get() = Email.ofOrNull(this)
