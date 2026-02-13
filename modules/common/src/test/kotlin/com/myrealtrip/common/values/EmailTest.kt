package com.myrealtrip.common.values

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@DisplayName("Email")
class EmailTest {

    @Nested
    @DisplayName("Factory Methods")
    inner class FactoryMethods {

        @Test
        fun `should create email from valid string`(): Unit {
            // given
            val emailString = "user@example.com"

            // when
            val email = Email.of(emailString)

            // then
            assertThat(email.value).isEqualTo("user@example.com")
        }

        @Test
        fun `should normalize email to lowercase`(): Unit {
            // given
            val emailString = "User@EXAMPLE.COM"

            // when
            val email = Email.of(emailString)

            // then
            assertThat(email.value).isEqualTo("user@example.com")
        }

        @Test
        fun `should trim whitespace from email`(): Unit {
            // given
            val emailString = "  user@example.com  "

            // when
            val email = Email.of(emailString)

            // then
            assertThat(email.value).isEqualTo("user@example.com")
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "simple@example.com",
            "very.common@example.com",
            "user.name+tag@example.com",
            "user-name@example.com",
            "user_name@example.com",
            "user123@example.com",
            "user@subdomain.example.com",
            "user@example.co.kr",
            "1234567890@example.com",
        ])
        fun `should accept valid email formats`(emailString: String): Unit {
            // when
            val email = Email.of(emailString)

            // then
            assertThat(email.value).isEqualTo(emailString.lowercase())
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            "   ",
            "invalid",
            "@example.com",
            "user@",
            "user@.com",
            "user@example",
            "user@@example.com",
            "user@example..com",
        ])
        fun `should reject invalid email formats`(emailString: String): Unit {
            // when & then
            assertThatThrownBy { Email.of(emailString) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should return email from ofOrNull for valid email`(): Unit {
            // when
            val email = Email.ofOrNull("user@example.com")

            // then
            assertThat(email).isNotNull
            assertThat(email?.value).isEqualTo("user@example.com")
        }

        @Test
        fun `should return null from ofOrNull for invalid email`(): Unit {
            // when
            val email = Email.ofOrNull("invalid")

            // then
            assertThat(email).isNull()
        }

        @Test
        fun `should return null from ofOrNull for null input`(): Unit {
            // when
            val email = Email.ofOrNull(null)

            // then
            assertThat(email).isNull()
        }

        @Test
        fun `should return null from ofOrNull for blank input`(): Unit {
            // when
            val email = Email.ofOrNull("   ")

            // then
            assertThat(email).isNull()
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {

        @Test
        fun `isValid should return true for valid email`(): Unit {
            // when & then
            assertThat(Email.isValid("user@example.com")).isTrue
        }

        @Test
        fun `isValid should return false for invalid email`(): Unit {
            // when & then
            assertThat(Email.isValid("invalid")).isFalse
        }

        @Test
        fun `isValid should return false for null`(): Unit {
            // when & then
            assertThat(Email.isValid(null)).isFalse
        }

        @Test
        fun `isValid should return false for blank`(): Unit {
            // when & then
            assertThat(Email.isValid("   ")).isFalse
        }
    }

    @Nested
    @DisplayName("Properties")
    inner class Properties {

        @Test
        fun `should extract local part`(): Unit {
            // given
            val email = Email.of("user.name@example.com")

            // when & then
            assertThat(email.localPart).isEqualTo("user.name")
        }

        @Test
        fun `should extract domain`(): Unit {
            // given
            val email = Email.of("user@subdomain.example.com")

            // when & then
            assertThat(email.domain).isEqualTo("subdomain.example.com")
        }

        @Test
        fun `should extract top level domain`(): Unit {
            // given
            val email = Email.of("user@example.co.kr")

            // when & then
            assertThat(email.topLevelDomain).isEqualTo("kr")
        }
    }

    @Nested
    @DisplayName("Masking")
    inner class Masking {

        @Test
        fun `should mask email with default settings`(): Unit {
            // given
            val email = Email.of("username@example.com")

            // when
            val masked = email.masked()

            // then
            assertThat(masked).isEqualTo("us******@example.com")
        }

        @Test
        fun `should not mask short local part`(): Unit {
            // given
            val email = Email.of("ab@example.com")

            // when
            val masked = email.masked()

            // then
            assertThat(masked).isEqualTo("ab@example.com")
        }

        @Test
        fun `should not mask single character local part`(): Unit {
            // given
            val email = Email.of("a@example.com")

            // when
            val masked = email.masked()

            // then
            assertThat(masked).isEqualTo("a@example.com")
        }

        @Test
        fun `should mask with custom visible characters`(): Unit {
            // given
            val email = Email.of("username@example.com")

            // when
            val masked = email.masked(visibleChars = 3)

            // then
            assertThat(masked).isEqualTo("use*****@example.com")
        }

        @Test
        fun `should mask with custom mask character`(): Unit {
            // given
            val email = Email.of("username@example.com")

            // when
            val masked = email.masked(visibleChars = 2, maskChar = '#')

            // then
            assertThat(masked).isEqualTo("us######@example.com")
        }
    }

    @Nested
    @DisplayName("Comparison")
    inner class Comparison {

        @Test
        fun `should compare emails alphabetically`(): Unit {
            // given
            val email1 = Email.of("alice@example.com")
            val email2 = Email.of("bob@example.com")

            // when & then
            assertThat(email1 < email2).isTrue
            assertThat(email2 > email1).isTrue
        }

        @Test
        fun `should be equal for same email`(): Unit {
            // given
            val email1 = Email.of("user@example.com")
            val email2 = Email.of("USER@EXAMPLE.COM")

            // when & then
            assertThat(email1).isEqualTo(email2)
        }
    }

    @Nested
    @DisplayName("Extension Functions")
    inner class ExtensionFunctions {

        @Test
        fun `should create email using asEmail extension`(): Unit {
            // when
            val email = "user@example.com".asEmail

            // then
            assertThat(email.value).isEqualTo("user@example.com")
        }

        @Test
        fun `should return email using asEmailOrNull extension`(): Unit {
            // when
            val valid = "user@example.com".asEmailOrNull
            val invalid = "invalid".asEmailOrNull

            // then
            assertThat(valid).isNotNull
            assertThat(invalid).isNull()
        }
    }

    @Nested
    @DisplayName("toString")
    inner class ToStringTest {

        @Test
        fun `toString should return email value`(): Unit {
            // given
            val email = Email.of("user@example.com")

            // when & then
            assertThat(email.toString()).isEqualTo("user@example.com")
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        private val mapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        @Test
        fun `should serialize email to string`(): Unit {
            // given
            val email = Email.of("user@example.com")

            // when
            val json = mapper.writeValueAsString(email)

            // then
            assertThat(json).isEqualTo("\"user@example.com\"")
        }

        @Test
        fun `should deserialize string to email`(): Unit {
            // given
            val json = "\"user@example.com\""

            // when
            val email = mapper.readValue(json, Email::class.java)

            // then
            assertThat(email.value).isEqualTo("user@example.com")
        }

        @Test
        fun `should serialize and deserialize email in object`(): Unit {
            // given
            data class User(val id: Long, val email: Email)
            val user = User(1L, Email.of("user@example.com"))

            // when
            val json = mapper.writeValueAsString(user)
            val deserialized = mapper.readValue(json, User::class.java)

            // then
            assertThat(json).contains("\"email\":\"user@example.com\"")
            assertThat(deserialized.email.value).isEqualTo("user@example.com")
        }
    }
}
