package com.myrealtrip.common.values

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Represents a password with validation.
 *
 * This class provides a type-safe way to handle passwords
 * with format validation and security features.
 *
 * Password requirements:
 * - Minimum 8 characters, maximum 72 characters (bcrypt limit)
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character from: !@#$%^&*()_+-=[]{}|;':\",./<>?
 * - No whitespace allowed
 *
 * Usage:
 * ```kotlin
 * // Create password
 * val password = Password.of("SecurePass123!")
 *
 * // Access properties
 * password.length         // 14
 * password.meetsComplexity()  // true
 *
 * // Masking for security
 * password.masked()       // "********"
 * password.toString()     // "********"
 * ```
 *
 * @property value the raw password string
 */
@JvmInline
value class Password private constructor(
    @get:JsonValue
    val value: String,
) : Comparable<Password> {

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 72 // bcrypt limit
        private const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        private val HAS_UPPERCASE = Regex("[A-Z]")
        private val HAS_LOWERCASE = Regex("[a-z]")
        private val HAS_DIGIT = Regex("\\d")
        private val HAS_SPECIAL = Regex("[${Regex.escape(SPECIAL_CHARS)}]")
        private val HAS_WHITESPACE = Regex("\\s")

        /**
         * Creates a Password instance from a string.
         *
         * @param password the password string to validate
         * @return a validated Password instance
         * @throws IllegalArgumentException if validation fails
         */
        @JvmStatic
        @JsonCreator
        fun of(password: String): Password {
            require(password.isNotBlank()) { "Password cannot be blank" }
            require(password.length >= MIN_LENGTH) {
                "Password must be at least $MIN_LENGTH characters"
            }
            require(password.length <= MAX_LENGTH) {
                "Password must not exceed $MAX_LENGTH characters"
            }
            require(!HAS_WHITESPACE.containsMatchIn(password)) {
                "Password cannot contain whitespace"
            }
            require(HAS_UPPERCASE.containsMatchIn(password)) {
                "Password must contain at least one uppercase letter"
            }
            require(HAS_LOWERCASE.containsMatchIn(password)) {
                "Password must contain at least one lowercase letter"
            }
            require(HAS_DIGIT.containsMatchIn(password)) {
                "Password must contain at least one digit"
            }
            require(HAS_SPECIAL.containsMatchIn(password)) {
                "Password must contain at least one special character from: $SPECIAL_CHARS"
            }
            return Password(password)
        }

        /**
         * Creates a Password instance from a string, returning null if invalid.
         *
         * @param password the password string to validate
         * @return a Password instance or null if validation fails
         */
        @JvmStatic
        fun ofOrNull(password: String?): Password? {
            if (password.isNullOrBlank()) return null
            return runCatching { of(password) }.getOrNull()
        }

        /**
         * Checks if a string is a valid password.
         *
         * @param password the password string to check
         * @return true if valid, false otherwise
         */
        @JvmStatic
        fun isValid(password: String?): Boolean {
            if (password.isNullOrBlank()) return false
            return runCatching { of(password) }.isSuccess
        }
    }

    /**
     * The length of the password.
     */
    val length: Int
        get() = value.length

    /**
     * Returns a masked version of the password for display purposes.
     * Always returns a fixed-length mask for security.
     *
     * @return "********"
     */
    fun masked(): String = "********"

    /**
     * Checks if the password meets complexity requirements.
     * Always returns true since validation happens at creation time.
     *
     * @return true
     */
    fun meetsComplexity(): Boolean = true

    override fun compareTo(other: Password): Int = value.compareTo(other.value)

    /**
     * Returns a masked representation of the password.
     * Never exposes the raw password value.
     *
     * @return "********"
     */
    override fun toString(): String = masked()
}

/**
 * Extension property to convert a String to a Password.
 *
 * @throws IllegalArgumentException if the string is not a valid password
 */
val String.asPassword: Password
    get() = Password.of(this)

/**
 * Extension property to convert a String to a Password or null.
 *
 * @return a Password instance or null if invalid
 */
val String.asPasswordOrNull: Password?
    get() = Password.ofOrNull(this)
