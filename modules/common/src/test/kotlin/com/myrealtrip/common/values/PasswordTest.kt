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

@DisplayName("Password")
class PasswordTest {

    @Nested
    @DisplayName("Factory Methods")
    inner class FactoryMethods {

        @Test
        fun `should create password from valid string`(): Unit {
            // given
            val passwordString = "SecurePass123!"

            // when
            val password = Password.of(passwordString)

            // then
            assertThat(password.value).isEqualTo("SecurePass123!")
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "Abcdef1!",
                "MyP@ssw0rd",
                "Complex!Pass9",
                "Test123!@#"
            ]
        )
        fun `should accept valid password formats`(passwordString: String): Unit {
            // when
            val password = Password.of(passwordString)

            // then
            assertThat(password.value).isEqualTo(passwordString)
        }

        @Test
        fun `should reject blank password`(): Unit {
            // when & then
            assertThatThrownBy { Password.of("   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password cannot be blank")
        }

        @Test
        fun `should reject short password`(): Unit {
            // given
            val shortPassword = "Ab1!"

            // when & then
            assertThatThrownBy { Password.of(shortPassword) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must be at least 8 characters")
        }

        @Test
        fun `should reject password exceeding max length`(): Unit {
            // given
            val longPassword = "A".repeat(73) + "1!"

            // when & then
            assertThatThrownBy { Password.of(longPassword) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must not exceed 72 characters")
        }

        @Test
        fun `should reject password without uppercase`(): Unit {
            // given
            val passwordString = "abcdefg1!"

            // when & then
            assertThatThrownBy { Password.of(passwordString) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must contain at least one uppercase letter")
        }

        @Test
        fun `should reject password without lowercase`(): Unit {
            // given
            val passwordString = "ABCDEFG1!"

            // when & then
            assertThatThrownBy { Password.of(passwordString) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must contain at least one lowercase letter")
        }

        @Test
        fun `should reject password without digit`(): Unit {
            // given
            val passwordString = "Abcdefgh!"

            // when & then
            assertThatThrownBy { Password.of(passwordString) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must contain at least one digit")
        }

        @Test
        fun `should reject password without special character`(): Unit {
            // given
            val passwordString = "Abcdefg1"

            // when & then
            assertThatThrownBy { Password.of(passwordString) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password must contain at least one special character")
        }

        @Test
        fun `should reject password with whitespace`(): Unit {
            // given
            val passwordString = "Abcde fg1!"

            // when & then
            assertThatThrownBy { Password.of(passwordString) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password cannot contain whitespace")
        }

        @Test
        fun `should return password from ofOrNull for valid password`(): Unit {
            // when
            val password = Password.ofOrNull("SecurePass123!")

            // then
            assertThat(password).isNotNull
            assertThat(password?.value).isEqualTo("SecurePass123!")
        }

        @Test
        fun `should return null from ofOrNull for invalid password`(): Unit {
            // when
            val password = Password.ofOrNull("invalid")

            // then
            assertThat(password).isNull()
        }

        @Test
        fun `should return null from ofOrNull for null input`(): Unit {
            // when
            val password = Password.ofOrNull(null)

            // then
            assertThat(password).isNull()
        }

        @Test
        fun `should return null from ofOrNull for blank input`(): Unit {
            // when
            val password = Password.ofOrNull("   ")

            // then
            assertThat(password).isNull()
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {

        @Test
        fun `isValid should return true for valid password`(): Unit {
            assertThat(Password.isValid("SecurePass123!")).isTrue
        }

        @Test
        fun `isValid should return false for invalid password`(): Unit {
            assertThat(Password.isValid("invalid")).isFalse
        }

        @Test
        fun `isValid should return false for null`(): Unit {
            assertThat(Password.isValid(null)).isFalse
        }

        @Test
        fun `isValid should return false for blank`(): Unit {
            assertThat(Password.isValid("   ")).isFalse
        }
    }

    @Nested
    @DisplayName("Properties")
    inner class Properties {

        @Test
        fun `should return correct length`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val length = password.length

            // then
            assertThat(length).isEqualTo(14)
        }

        @Test
        fun `should return true for meetsComplexity`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val meetsComplexity = password.meetsComplexity()

            // then
            assertThat(meetsComplexity).isTrue
        }
    }

    @Nested
    @DisplayName("Masking")
    inner class Masking {

        @Test
        fun `should return fixed mask string`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val masked = password.masked()

            // then
            assertThat(masked).isEqualTo("********")
        }

        @Test
        fun `should not expose password length in mask`(): Unit {
            // given
            val shortPassword = Password.of("Short1!a")
            val longPassword = Password.of("VeryLongPassword123!")

            // when
            val shortMasked = shortPassword.masked()
            val longMasked = longPassword.masked()

            // then
            assertThat(shortMasked).isEqualTo(longMasked)
            assertThat(shortMasked).isEqualTo("********")
        }
    }

    @Nested
    @DisplayName("Comparison")
    inner class Comparison {

        @Test
        fun `should compare passwords alphabetically`(): Unit {
            // given
            val password1 = Password.of("APassword1!")
            val password2 = Password.of("BPassword1!")

            // when
            val comparison = password1.compareTo(password2)

            // then
            assertThat(comparison).isLessThan(0)
        }

        @Test
        fun `should be equal for same password`(): Unit {
            // given
            val password1 = Password.of("SecurePass123!")
            val password2 = Password.of("SecurePass123!")

            // when
            val comparison = password1.compareTo(password2)

            // then
            assertThat(comparison).isEqualTo(0)
            assertThat(password1).isEqualTo(password2)
        }
    }

    @Nested
    @DisplayName("Extension Functions")
    inner class ExtensionFunctions {

        @Test
        fun `should create password using asPassword extension`(): Unit {
            // given
            val passwordString = "SecurePass123!"

            // when
            val password = passwordString.asPassword

            // then
            assertThat(password.value).isEqualTo("SecurePass123!")
        }

        @Test
        fun `should return password using asPasswordOrNull extension`(): Unit {
            // given
            val validPasswordString = "SecurePass123!"
            val invalidPasswordString = "invalid"

            // when
            val validPassword = validPasswordString.asPasswordOrNull
            val invalidPassword = invalidPasswordString.asPasswordOrNull

            // then
            assertThat(validPassword).isNotNull
            assertThat(validPassword?.value).isEqualTo("SecurePass123!")
            assertThat(invalidPassword).isNull()
        }
    }

    @Nested
    @DisplayName("toString")
    inner class ToStringTest {

        @Test
        fun `toString should return masked value`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val result = password.toString()

            // then
            assertThat(result).isEqualTo("********")
        }

        @Test
        fun `toString should not expose actual password`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val result = password.toString()

            // then
            assertThat(result).doesNotContain("SecurePass123!")
            assertThat(result).isEqualTo("********")
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        private val mapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        @Test
        fun `should serialize password to string`(): Unit {
            // given
            val password = Password.of("SecurePass123!")

            // when
            val json = mapper.writeValueAsString(password)

            // then
            assertThat(json).isEqualTo("\"SecurePass123!\"")
        }

        @Test
        fun `should deserialize string to password`(): Unit {
            // given
            val json = "\"SecurePass123!\""

            // when
            val password = mapper.readValue(json, Password::class.java)

            // then
            assertThat(password.value).isEqualTo("SecurePass123!")
        }

        @Test
        fun `should serialize and deserialize password in object`(): Unit {
            // given
            data class UserCredential(val id: Long, val password: Password)

            val credential = UserCredential(1L, Password.of("SecurePass123!"))

            // when
            val json = mapper.writeValueAsString(credential)
            val deserialized = mapper.readValue(json, UserCredential::class.java)

            // then
            assertThat(deserialized.id).isEqualTo(1L)
            assertThat(deserialized.password.value).isEqualTo("SecurePass123!")
        }
    }
}
