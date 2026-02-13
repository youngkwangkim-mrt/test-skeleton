package com.myrealtrip.common.values

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@DisplayName("PhoneNumber")
class PhoneNumberTest {

    @Nested
    @DisplayName("Factory Methods")
    inner class FactoryMethods {

        @Test
        fun `should create phone number from E164 format`(): Unit {
            // given
            val phoneString = "+821012345678"

            // when
            val phone = PhoneNumber.of(phoneString)

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should create phone number from national format with region`(): Unit {
            // given
            val phoneString = "010-1234-5678"

            // when
            val phone = PhoneNumber.of(phoneString, "KR")

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should create phone number from national format without dashes`(): Unit {
            // given
            val phoneString = "01012345678"

            // when
            val phone = PhoneNumber.of(phoneString, "KR")

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should trim whitespace from phone number`(): Unit {
            // given
            val phoneString = "  +821012345678  "

            // when
            val phone = PhoneNumber.of(phoneString)

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @ParameterizedTest
        @CsvSource(
            "+821012345678, KR",
            "+14155551234, US",
            "+442071234567, GB",
            "+81312345678, JP",
        )
        fun `should accept valid international phone numbers`(phoneString: String, expectedRegion: String): Unit {
            // when
            val phone = PhoneNumber.of(phoneString)

            // then
            assertThat(phone.regionCode).isEqualTo(expectedRegion)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "010-1234-5678",
            "01012345678",
            "02-1234-5678",
            "031-123-4567",
        ])
        fun `should accept valid Korean phone numbers`(phoneString: String): Unit {
            // when
            val phone = PhoneNumber.of(phoneString, "KR")

            // then
            assertThat(phone.countryCode).isEqualTo(82)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            "   ",
            "invalid",
            "123",
            "abcdefghij",
        ])
        fun `should reject invalid phone numbers`(phoneString: String): Unit {
            // when & then
            assertThatThrownBy { PhoneNumber.of(phoneString) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should return phone from ofOrNull for valid number`(): Unit {
            // when
            val phone = PhoneNumber.ofOrNull("+821012345678")

            // then
            assertThat(phone).isNotNull
            assertThat(phone?.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should return null from ofOrNull for invalid number`(): Unit {
            // when
            val phone = PhoneNumber.ofOrNull("invalid")

            // then
            assertThat(phone).isNull()
        }

        @Test
        fun `should return null from ofOrNull for null input`(): Unit {
            // when
            val phone = PhoneNumber.ofOrNull(null)

            // then
            assertThat(phone).isNull()
        }

        @Test
        fun `should return null from ofOrNull for blank input`(): Unit {
            // when
            val phone = PhoneNumber.ofOrNull("   ")

            // then
            assertThat(phone).isNull()
        }

        @Test
        fun `should create phone number using ofE164`(): Unit {
            // when
            val phone = PhoneNumber.ofE164("+821012345678")

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @ParameterizedTest
        @CsvSource(
            "+821012345678, KR, 82",
            "+14155551234, US, 1",
            "+442071234567, GB, 44",
            "+81312345678, JP, 81",
        )
        fun `ofE164 should accept phone numbers from any country`(
            e164: String,
            expectedRegion: String,
            expectedCountryCode: Int,
        ): Unit {
            // when
            val phone = PhoneNumber.ofE164(e164)

            // then
            assertThat(phone.value).isEqualTo(e164)
            assertThat(phone.regionCode).isEqualTo(expectedRegion)
            assertThat(phone.countryCode).isEqualTo(expectedCountryCode)
        }

        @Test
        fun `should reject ofE164 without plus sign`(): Unit {
            // when & then
            assertThatThrownBy { PhoneNumber.ofE164("821012345678") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("E.164 format must start with '+'")
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {

        @Test
        fun `isValid should return true for valid phone number`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValid("+821012345678")).isTrue
            assertThat(PhoneNumber.isValid("010-1234-5678", "KR")).isTrue
        }

        @Test
        fun `isValid should return false for invalid phone number`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValid("invalid")).isFalse
            assertThat(PhoneNumber.isValid("123")).isFalse
        }

        @Test
        fun `isValid should return false for null`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValid(null)).isFalse
        }

        @Test
        fun `isValid should return false for blank`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValid("   ")).isFalse
        }

        @Test
        fun `isValidMobile should return true for mobile numbers`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValidMobile("+821012345678")).isTrue
            assertThat(PhoneNumber.isValidMobile("010-1234-5678", "KR")).isTrue
        }

        @Test
        fun `isValidMobile should return false for landline numbers`(): Unit {
            // when & then
            assertThat(PhoneNumber.isValidMobile("+8221234567")).isFalse
            assertThat(PhoneNumber.isValidMobile("02-123-4567", "KR")).isFalse
        }
    }

    @Nested
    @DisplayName("Properties")
    inner class Properties {

        @Test
        fun `should extract country code`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.countryCode).isEqualTo(82)
        }

        @Test
        fun `should extract national number`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.nationalNumber).isEqualTo("1012345678")
        }

        @Test
        fun `should extract region code`(): Unit {
            // given
            val krPhone = PhoneNumber.of("+821012345678")
            val usPhone = PhoneNumber.of("+14155551234")

            // when & then
            assertThat(krPhone.regionCode).isEqualTo("KR")
            assertThat(usPhone.regionCode).isEqualTo("US")
        }

        @Test
        fun `should detect mobile number type`(): Unit {
            // given
            val mobile = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(mobile.isMobile).isTrue
            assertThat(mobile.isFixedLine).isFalse
            assertThat(mobile.numberType).isEqualTo(PhoneNumberUtil.PhoneNumberType.MOBILE)
        }

        @Test
        fun `should detect fixed line number type`(): Unit {
            // given
            val landline = PhoneNumber.of("+8221234567")

            // when & then
            assertThat(landline.isFixedLine).isTrue
            assertThat(landline.isMobile).isFalse
        }
    }

    @Nested
    @DisplayName("Formatting")
    inner class Formatting {

        @Test
        fun `should format to E164`(): Unit {
            // given
            val phone = PhoneNumber.of("010-1234-5678", "KR")

            // when & then
            assertThat(phone.toE164()).isEqualTo("+821012345678")
        }

        @Test
        fun `should format to national`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.toNational()).isEqualTo("010-1234-5678")
        }

        @Test
        fun `should format to international`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.toInternational()).isEqualTo("+82 10-1234-5678")
        }

        @Test
        fun `should format to RFC3966`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.toRfc3966()).isEqualTo("tel:+82-10-1234-5678")
        }

        @Test
        fun `should format US phone number correctly`(): Unit {
            // given
            val phone = PhoneNumber.of("+14155551234")

            // when & then
            assertThat(phone.toNational()).isEqualTo("(415) 555-1234")
            assertThat(phone.toInternational()).isEqualTo("+1 415-555-1234")
        }
    }

    @Nested
    @DisplayName("Masking")
    inner class Masking {

        @Test
        fun `should mask Korean mobile number in national format by default`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val masked = phone.masked()

            // then
            assertThat(masked).isEqualTo("***-****-5678")
        }

        @Test
        fun `should mask with custom character`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val masked = phone.masked(maskChar = '#')

            // then
            assertThat(masked).isEqualTo("###-####-5678")
        }

        @Test
        fun `should mask US phone number in national format by default`(): Unit {
            // given
            val phone = PhoneNumber.of("+14155551234")

            // when
            val masked = phone.masked()

            // then
            assertThat(masked).isEqualTo("(***) ***-1234")
        }

        @Test
        fun `should mask with custom visible digits`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val masked = phone.masked(visibleDigits = 2)

            // then
            assertThat(masked).isEqualTo("***-****-**78")
        }

        @Test
        fun `should mask international format preserving country code`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val masked = phone.maskedInternational()

            // then
            assertThat(masked).isEqualTo("+82 **-****-5678")
        }

        @Test
        fun `should mask US phone number in international format`(): Unit {
            // given
            val phone = PhoneNumber.of("+14155551234")

            // when
            val masked = phone.maskedInternational()

            // then
            assertThat(masked).isEqualTo("+1 ***-***-1234")
        }

        @Test
        fun `should mask international format with custom character`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val masked = phone.maskedInternational(maskChar = '#')

            // then
            assertThat(masked).isEqualTo("+82 ##-####-5678")
        }
    }

    @Nested
    @DisplayName("Region Checks")
    inner class RegionChecks {

        @Test
        fun `isRegion should return true for matching region`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.isRegion("KR")).isTrue
            assertThat(phone.isRegion("kr")).isTrue
        }

        @Test
        fun `isRegion should return false for non-matching region`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.isRegion("US")).isFalse
        }
    }

    @Nested
    @DisplayName("Comparison")
    inner class Comparison {

        @Test
        fun `should compare phone numbers`(): Unit {
            // given
            val phone1 = PhoneNumber.of("+821012345678")
            val phone2 = PhoneNumber.of("+821087654321")

            // when & then
            assertThat(phone1 < phone2).isTrue
        }

        @Test
        fun `should be equal for same phone number in different formats`(): Unit {
            // given
            val phone1 = PhoneNumber.of("+821012345678")
            val phone2 = PhoneNumber.of("010-1234-5678", "KR")

            // when & then
            assertThat(phone1).isEqualTo(phone2)
        }
    }

    @Nested
    @DisplayName("Extension Functions")
    inner class ExtensionFunctions {

        @Test
        fun `should create phone number using asPhoneNumber property extension`(): Unit {
            // when - default region (KR)
            val phone = "+821012345678".asPhoneNumber

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should return phone number using asPhoneNumberOrNull property extension`(): Unit {
            // when - default region (KR)
            val valid = "+821012345678".asPhoneNumberOrNull
            val invalid = "invalid".asPhoneNumberOrNull

            // then
            assertThat(valid).isNotNull
            assertThat(invalid).isNull()
        }

        @Test
        fun `should create phone number using asPhoneNumber function with explicit region`(): Unit {
            // when
            val krPhone = "010-1234-5678".asPhoneNumber("KR")
            val usPhone = "(415) 555-1234".asPhoneNumber("US")

            // then
            assertThat(krPhone.value).isEqualTo("+821012345678")
            assertThat(usPhone.value).isEqualTo("+14155551234")
        }

        @Test
        fun `asPhoneNumber should work for E164 format ignoring region parameter`(): Unit {
            // given - E.164 format already contains country code
            val e164 = "+821012341234"

            // when - region parameter is ignored because +82 already specifies Korea
            val withDefault = e164.asPhoneNumber
            val withKR = e164.asPhoneNumber("KR")
            val withUS = e164.asPhoneNumber("US")  // region ignored

            // then - all should produce the same result
            assertThat(withDefault.value).isEqualTo("+821012341234")
            assertThat(withKR.value).isEqualTo("+821012341234")
            assertThat(withUS.value).isEqualTo("+821012341234")
            assertThat(withDefault.regionCode).isEqualTo("KR")
            assertThat(withKR.regionCode).isEqualTo("KR")
            assertThat(withUS.regionCode).isEqualTo("KR")
        }

        @Test
        fun `should return phone number using asPhoneNumberOrNull function with explicit region`(): Unit {
            // when
            val valid = "010-1234-5678".asPhoneNumberOrNull("KR")
            val invalid = "invalid".asPhoneNumberOrNull

            // then
            assertThat(valid).isNotNull
            assertThat(invalid).isNull()
        }
    }

    @Nested
    @DisplayName("toString")
    inner class ToStringTest {

        @Test
        fun `toString should return masked national format for privacy`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when & then
            assertThat(phone.toString()).isEqualTo("***-****-5678")
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        private val mapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        @Test
        fun `should serialize phone number to string`(): Unit {
            // given
            val phone = PhoneNumber.of("+821012345678")

            // when
            val json = mapper.writeValueAsString(phone)

            // then
            assertThat(json).isEqualTo("\"+821012345678\"")
        }

        @Test
        fun `should deserialize string to phone number`(): Unit {
            // given
            val json = "\"+821012345678\""

            // when
            val phone = mapper.readValue(json, PhoneNumber::class.java)

            // then
            assertThat(phone.value).isEqualTo("+821012345678")
        }

        @Test
        fun `should serialize and deserialize phone number in object`(): Unit {
            // given
            data class Contact(val name: String, val phone: PhoneNumber)
            val contact = Contact("John", PhoneNumber.of("+821012345678"))

            // when
            val json = mapper.writeValueAsString(contact)
            val deserialized = mapper.readValue(json, Contact::class.java)

            // then
            assertThat(json).contains("\"phone\":\"+821012345678\"")
            assertThat(deserialized.phone.value).isEqualTo("+821012345678")
        }
    }
}
