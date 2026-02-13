package com.myrealtrip.common.values

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber
import com.myrealtrip.common.utils.extensions.maskDigits

/**
 * Represents a validated phone number with formatting and parsing capabilities.
 *
 * This class provides a type-safe way to handle phone numbers using Google's libphonenumber.
 * Internally stores the phone number in E.164 format for consistency.
 *
 * Usage:
 * ```kotlin
 * // Create from various formats
 * val phone1 = PhoneNumber.of("+821012345678")
 * val phone2 = PhoneNumber.of("010-1234-5678", "KR")
 * val phone3 = PhoneNumber.of("(02) 1234-5678", "KR")
 *
 * // Format in different styles
 * phone1.toE164()           // "+821012345678"
 * phone1.toNational()       // "010-1234-5678"
 * phone1.toInternational()  // "+82 10-1234-5678"
 *
 * // Extract information
 * phone1.countryCode        // 82
 * phone1.nationalNumber     // "1012345678"
 * phone1.regionCode         // "KR"
 *
 * // Masking for privacy (toString() also returns masked value)
 * phone1.masked()               // "***-****-5678" (national format)
 * phone1.maskedInternational()  // "+82 **-****-5678"
 * phone1.toString()             // "***-****-5678"
 * ```
 *
 * @property value the phone number in E.164 format
 */
@JvmInline
value class PhoneNumber private constructor(
    @get:JsonValue
    val value: String,
) : Comparable<PhoneNumber> {

    companion object {
        private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

        /** Default region code for parsing phone numbers without country code */
        const val DEFAULT_REGION: String = "KR"

        // =============================================================================
        // Factory Methods
        // =============================================================================

        /**
         * Creates a [PhoneNumber] from a string value with explicit region.
         *
         * @param phoneNumber the phone number string (can be in any recognizable format)
         * @param defaultRegion the default region code (ISO 3166-1 alpha-2) for parsing numbers without country code
         * @return a new [PhoneNumber] instance
         * @throws IllegalArgumentException if the phone number is invalid
         */
        @JvmStatic
        @JvmOverloads
        fun of(phoneNumber: String, defaultRegion: String = DEFAULT_REGION): PhoneNumber {
            val parsed = parseAndValidate(phoneNumber, defaultRegion)
            return PhoneNumber(phoneUtil.format(parsed, PhoneNumberFormat.E164))
        }

        /**
         * Creates a [PhoneNumber] from E.164 format string.
         * Used for JSON deserialization. Accepts phone numbers from any country.
         *
         * @param e164 the phone number in E.164 format (e.g., "+821012345678", "+14155551234")
         * @return a new [PhoneNumber] instance
         * @throws IllegalArgumentException if the phone number is invalid
         */
        @JvmStatic
        @JsonCreator
        fun ofE164(e164: String): PhoneNumber {
            require(e164.trim().startsWith("+")) {
                "E.164 format must start with '+': $e164"
            }
            return of(e164)
        }

        /**
         * Creates a [PhoneNumber] from a string value, returning null if invalid.
         *
         * @param phoneNumber the phone number string
         * @param defaultRegion the default region code for parsing
         * @return a new [PhoneNumber] instance or null if invalid
         */
        @JvmStatic
        @JvmOverloads
        fun ofOrNull(phoneNumber: String?, defaultRegion: String = DEFAULT_REGION): PhoneNumber? {
            if (phoneNumber.isNullOrBlank()) return null
            return runCatching { of(phoneNumber, defaultRegion) }.getOrNull()
        }

        // =============================================================================
        // Validation Methods
        // =============================================================================

        /**
         * Checks if the given string is a valid phone number.
         *
         * @param phoneNumber the phone number string to validate
         * @param defaultRegion the default region code for parsing
         * @return true if valid, false otherwise
         */
        @JvmStatic
        @JvmOverloads
        fun isValid(phoneNumber: String?, defaultRegion: String = DEFAULT_REGION): Boolean {
            return parseOrNull(phoneNumber, defaultRegion) != null
        }

        /**
         * Checks if the given string is a valid mobile phone number.
         *
         * @param phoneNumber the phone number string to validate
         * @param defaultRegion the default region code for parsing
         * @return true if valid mobile number, false otherwise
         */
        @JvmStatic
        @JvmOverloads
        fun isValidMobile(phoneNumber: String?, defaultRegion: String = DEFAULT_REGION): Boolean {
            val parsed = parseOrNull(phoneNumber, defaultRegion) ?: return false
            return phoneUtil.getNumberType(parsed) == PhoneNumberUtil.PhoneNumberType.MOBILE
        }

        // =============================================================================
        // Private Helpers
        // =============================================================================

        /**
         * Parses and validates a phone number, throwing on failure.
         */
        private fun parseAndValidate(phoneNumber: String, region: String): Phonenumber.PhoneNumber {
            require(phoneNumber.isNotBlank()) { "Phone number cannot be blank" }

            return parseOrNull(phoneNumber, region)
                ?: throw IllegalArgumentException("Invalid phone number: $phoneNumber")
        }

        /**
         * Parses and validates a phone number, returning null on failure.
         */
        private fun parseOrNull(phoneNumber: String?, region: String): Phonenumber.PhoneNumber? {
            if (phoneNumber.isNullOrBlank()) return null
            return runCatching {
                phoneUtil.parse(phoneNumber.trim(), region)
                    .takeIf { phoneUtil.isValidNumber(it) }
            }.getOrNull()
        }
    }

    // =============================================================================
    // Properties
    // =============================================================================

    /**
     * Returns the country calling code (e.g., 82 for Korea, 1 for US).
     */
    val countryCode: Int
        get() = parsedNumber.countryCode

    /**
     * Returns the national number without country code.
     */
    val nationalNumber: String
        get() = parsedNumber.nationalNumber.toString()

    /**
     * Returns the region code (ISO 3166-1 alpha-2) for this phone number.
     */
    val regionCode: String
        get() = phoneUtil.getRegionCodeForNumber(parsedNumber) ?: ""

    /**
     * Returns the phone number type (MOBILE, FIXED_LINE, etc.).
     */
    val numberType: PhoneNumberUtil.PhoneNumberType
        get() = phoneUtil.getNumberType(parsedNumber)

    /**
     * Returns true if this is a mobile phone number.
     */
    val isMobile: Boolean
        get() = numberType == PhoneNumberUtil.PhoneNumberType.MOBILE

    /**
     * Returns true if this is a fixed line (landline) phone number.
     */
    val isFixedLine: Boolean
        get() = numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE

    private val parsedNumber: Phonenumber.PhoneNumber
        get() = phoneUtil.parse(value, null)

    // =============================================================================
    // Formatting Methods
    // =============================================================================

    /**
     * Returns the phone number in E.164 format (e.g., "+821012345678").
     * This is the stored value.
     */
    fun toE164(): String = value

    /**
     * Returns the phone number in national format (e.g., "010-1234-5678" for Korea).
     */
    fun toNational(): String = phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL)

    /**
     * Returns the phone number in international format (e.g., "+82 10-1234-5678").
     */
    fun toInternational(): String = phoneUtil.format(parsedNumber, PhoneNumberFormat.INTERNATIONAL)

    /**
     * Returns the phone number in RFC 3966 format for tel: URIs (e.g., "tel:+82-10-1234-5678").
     */
    fun toRfc3966(): String = phoneUtil.format(parsedNumber, PhoneNumberFormat.RFC3966)

    /**
     * Returns a masked version of the phone number in national format.
     *
     * Examples:
     * - "+821012345678".masked() -> "***-****-5678"
     * - "+821012345678".masked(2) -> "***-****-**78"
     *
     * @param visibleDigits number of digits to show at the end (default: 4)
     * @param maskChar character to use for masking (default: '*')
     * @return masked phone number in national format
     */
    fun masked(visibleDigits: Int = 4, maskChar: Char = '*'): String =
        toNational().maskDigits(visibleDigits, maskChar)

    /**
     * Returns a masked version of the phone number in international format.
     * Preserves country code and masks digits except the last N.
     *
     * Examples:
     * - "+821012345678".maskedInternational() -> "+82 **-****-5678" (Korean mobile)
     * - "+14155551234".maskedInternational() -> "+1 ***-***-1234" (US)
     *
     * @param visibleDigits number of digits to show at the end (default: 4)
     * @param maskChar character to use for masking (default: '*')
     * @return masked phone number in international format with country code visible
     */
    fun maskedInternational(visibleDigits: Int = 4, maskChar: Char = '*'): String {
        val international = toInternational()
        val countryCodePrefix = "+$countryCode "
        val nationalPart = international.removePrefix(countryCodePrefix)
        return countryCodePrefix + nationalPart.maskDigits(visibleDigits, maskChar)
    }

    // =============================================================================
    // Comparison & Utility Methods
    // =============================================================================

    override fun compareTo(other: PhoneNumber): Int = value.compareTo(other.value)

    /**
     * Checks if this phone number belongs to the given region.
     *
     * @param regionCode ISO 3166-1 alpha-2 region code
     * @return true if the phone number belongs to the region
     */
    fun isRegion(regionCode: String): Boolean = this.regionCode.equals(regionCode, ignoreCase = true)

    override fun toString(): String = masked()
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Converts this string to a [PhoneNumber] using default region (KR).
 *
 * @throws IllegalArgumentException if phone number format is invalid
 */
val String.asPhoneNumber: PhoneNumber
    get() = PhoneNumber.of(this)

/**
 * Converts this string to a [PhoneNumber], returning null if invalid.
 * Uses default region (KR).
 */
val String.asPhoneNumberOrNull: PhoneNumber?
    get() = PhoneNumber.ofOrNull(this)

/**
 * Converts this string to a [PhoneNumber] with explicit region.
 *
 * @param region ISO 3166-1 alpha-2 region code
 * @throws IllegalArgumentException if phone number format is invalid
 */
fun String.asPhoneNumber(region: String): PhoneNumber =
    PhoneNumber.of(this, region)

/**
 * Converts this string to a [PhoneNumber], returning null if invalid.
 *
 * @param region ISO 3166-1 alpha-2 region code
 */
fun String.asPhoneNumberOrNull(region: String): PhoneNumber? =
    PhoneNumber.ofOrNull(this, region)
