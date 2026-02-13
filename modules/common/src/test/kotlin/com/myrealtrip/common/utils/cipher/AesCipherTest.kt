package com.myrealtrip.common.utils.cipher

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@DisplayName("AesCipher")
class AesCipherTest {

    private val key16 = "1234567890123456" // 16 bytes for AES-128
    private val key32 = "12345678901234567890123456789012" // 32 bytes for AES-256
    private val iv16 = "abcdefghijklmnop" // 16 bytes IV
    private val plainText = "Hello, World!"

    @Nested
    @DisplayName("encrypt and decrypt with CBC mode")
    inner class CbcMode {

        @Test
        fun `should encrypt and decrypt roundtrip successfully`(): Unit {
            // given
            val key = key16
            val iv = iv16

            // when
            val encrypted = AesCipher.encrypt(plainText, key, iv)
            val decrypted = AesCipher.decrypt(encrypted, key, iv)

            // then
            assertThat(encrypted).isNotEmpty.isNotEqualTo(plainText)
            assertThat(decrypted).isEqualTo(plainText)
        }

        @Test
        fun `should use zero-filled IV when iv is null`(): Unit {
            // given
            val key = key16

            // when
            val encrypted = AesCipher.encrypt(plainText, key, null)
            val decrypted = AesCipher.decrypt(encrypted, key, null)

            // then
            assertThat(decrypted).isEqualTo(plainText)
        }

        @Test
        fun `should produce different ciphertext with different keys`(): Unit {
            // given
            val key1 = "1111111111111111"
            val key2 = "2222222222222222"

            // when
            val encrypted1 = AesCipher.encrypt(plainText, key1, iv16)
            val encrypted2 = AesCipher.encrypt(plainText, key2, iv16)

            // then
            assertThat(encrypted1).isNotEqualTo(encrypted2)
        }

        @Test
        fun `should produce different ciphertext with different IVs`(): Unit {
            // given
            val iv1 = "aaaaaaaaaaaaaaaa"
            val iv2 = "bbbbbbbbbbbbbbbb"

            // when
            val encrypted1 = AesCipher.encrypt(plainText, key16, iv1)
            val encrypted2 = AesCipher.encrypt(plainText, key16, iv2)

            // then
            assertThat(encrypted1).isNotEqualTo(encrypted2)
        }

        @Test
        fun `should work with AES-256 key`(): Unit {
            // given
            val key = key32

            // when
            val encrypted = AesCipher.encrypt(plainText, key, iv16)
            val decrypted = AesCipher.decrypt(encrypted, key, iv16)

            // then
            assertThat(decrypted).isEqualTo(plainText)
        }
    }

    @Nested
    @DisplayName("encrypt and decrypt with ECB mode")
    inner class EcbMode {

        @Test
        fun `should encrypt and decrypt roundtrip successfully`(): Unit {
            // given
            val key = key16

            // when
            val encrypted = AesCipher.encrypt(plainText, key, null, AesMode.ECB)
            val decrypted = AesCipher.decrypt(encrypted, key, null, AesMode.ECB)

            // then
            assertThat(encrypted).isNotEmpty
            assertThat(decrypted).isEqualTo(plainText)
        }

        @Test
        fun `should produce same ciphertext for same plaintext - ECB characteristic`(): Unit {
            // given
            val key = key16

            // when
            val encrypted1 = AesCipher.encrypt(plainText, key, null, AesMode.ECB)
            val encrypted2 = AesCipher.encrypt(plainText, key, null, AesMode.ECB)

            // then - ECB mode produces deterministic output (same input = same output)
            assertThat(encrypted1).isEqualTo(encrypted2)
        }
    }

    @Nested
    @DisplayName("encrypt and decrypt with SecretKey API")
    inner class SecretKeyApi {

        @Test
        fun `should encrypt and decrypt with SecretKey and IvParameterSpec`(): Unit {
            // given
            val secretKey = SecretKeySpec(key16.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(iv16.toByteArray(Charsets.UTF_8))

            // when
            val encrypted = AesCipher.encrypt(plainText, secretKey, ivSpec)
            val decrypted = AesCipher.decrypt(encrypted, secretKey, ivSpec)

            // then
            assertThat(decrypted).isEqualTo(plainText)
        }

        @Test
        fun `should encrypt and decrypt with SecretKey in ECB mode`(): Unit {
            // given
            val secretKey = SecretKeySpec(key16.toByteArray(Charsets.UTF_8), "AES")

            // when
            val encrypted = AesCipher.encrypt(plainText, secretKey, null, AesMode.ECB)
            val decrypted = AesCipher.decrypt(encrypted, secretKey, null, AesMode.ECB)

            // then
            assertThat(decrypted).isEqualTo(plainText)
        }
    }

    @Nested
    @DisplayName("getSecretKey")
    inner class GetSecretKey {

        @Test
        fun `should generate 16-byte key with MD5`(): Unit {
            // given
            val keySource = "mySecretKey"

            // when
            val secretKey = AesCipher.getSecretKey(keySource, "MD5")

            // then
            assertThat(secretKey.algorithm).isEqualTo("AES")
            assertThat(secretKey.encoded).hasSize(16)
        }

        @Test
        fun `should generate 32-byte key with SHA-256`(): Unit {
            // given
            val keySource = "mySecretKey"

            // when
            val secretKey = AesCipher.getSecretKey(keySource, "SHA-256")

            // then
            assertThat(secretKey.algorithm).isEqualTo("AES")
            assertThat(secretKey.encoded).hasSize(32)
        }

        @Test
        fun `should produce consistent key for same input`(): Unit {
            // given
            val keySource = "mySecretKey"

            // when
            val key1 = AesCipher.getSecretKey(keySource, "SHA-256")
            val key2 = AesCipher.getSecretKey(keySource, "SHA-256")

            // then
            assertThat(key1.encoded).isEqualTo(key2.encoded)
        }

        @Test
        fun `should throw exception for invalid digest algorithm`(): Unit {
            // given
            val keySource = "mySecretKey"
            val invalidAlgorithm = "INVALID-ALGORITHM"

            // when & then
            assertThatThrownBy { AesCipher.getSecretKey(keySource, invalidAlgorithm) }
                .isInstanceOf(NoSuchAlgorithmException::class.java)
        }
    }

    @Nested
    @DisplayName("getInitialVector")
    inner class GetInitialVector {

        @Test
        fun `should generate 16-byte IV with MD5`(): Unit {
            // given
            val ivSource = "myInitialVector"

            // when
            val ivSpec = AesCipher.getInitialVector(ivSource, "MD5")

            // then
            assertThat(ivSpec.iv).hasSize(16)
        }

        @Test
        fun `should produce consistent IV for same input`(): Unit {
            // given
            val ivSource = "myInitialVector"

            // when
            val iv1 = AesCipher.getInitialVector(ivSource, "MD5")
            val iv2 = AesCipher.getInitialVector(ivSource, "MD5")

            // then
            assertThat(iv1.iv).isEqualTo(iv2.iv)
        }

        @Test
        fun `should throw exception for invalid digest algorithm`(): Unit {
            // given
            val ivSource = "myInitialVector"
            val invalidAlgorithm = "INVALID-ALGORITHM"

            // when & then
            assertThatThrownBy { AesCipher.getInitialVector(ivSource, invalidAlgorithm) }
                .isInstanceOf(NoSuchAlgorithmException::class.java)
        }
    }

    @Nested
    @DisplayName("encryption with hashed key and IV")
    inner class HashedKeyAndIv {

        @Test
        fun `should encrypt and decrypt using generated key and IV`(): Unit {
            // given
            val keySource = "anyLengthKeySource"
            val ivSource = "anyLengthIvSource"
            val secretKey = AesCipher.getSecretKey(keySource, "MD5")
            val ivSpec = AesCipher.getInitialVector(ivSource, "MD5")

            // when
            val encrypted = AesCipher.encrypt(plainText, secretKey, ivSpec)
            val decrypted = AesCipher.decrypt(encrypted, secretKey, ivSpec)

            // then
            assertThat(decrypted).isEqualTo(plainText)
        }
    }

    @Nested
    @DisplayName("error handling")
    inner class ErrorHandling {

        @Test
        fun `should throw exception for invalid key length`(): Unit {
            // given
            val invalidKey = "short"

            // when & then
            assertThatThrownBy { AesCipher.encrypt(plainText, invalidKey, iv16) }
                .isInstanceOf(GeneralSecurityException::class.java)
        }

        @Test
        fun `should throw exception for invalid ciphertext`(): Unit {
            // given
            val invalidCipherText = "not-valid-base64!!!"

            // when & then
            assertThatThrownBy { AesCipher.decrypt(invalidCipherText, key16, iv16) }
                .isInstanceOf(Exception::class.java)
        }

        @Test
        fun `should throw exception when decrypting with wrong key`(): Unit {
            // given
            val encrypted = AesCipher.encrypt(plainText, key16, iv16)
            val wrongKey = "wrongkey12345678"

            // when & then
            assertThatThrownBy { AesCipher.decrypt(encrypted, wrongKey, iv16) }
                .isInstanceOf(GeneralSecurityException::class.java)
        }
    }

    @Nested
    @DisplayName("various input types")
    inner class VariousInputTypes {

        @Test
        fun `should handle empty string`(): Unit {
            // given
            val emptyText = ""

            // when
            val encrypted = AesCipher.encrypt(emptyText, key16, iv16)
            val decrypted = AesCipher.decrypt(encrypted, key16, iv16)

            // then
            assertThat(decrypted).isEqualTo(emptyText)
        }

        @Test
        fun `should handle long text`(): Unit {
            // given
            val longText = "A".repeat(10000)

            // when
            val encrypted = AesCipher.encrypt(longText, key16, iv16)
            val decrypted = AesCipher.decrypt(encrypted, key16, iv16)

            // then
            assertThat(decrypted).isEqualTo(longText)
        }

        @Test
        fun `should handle unicode characters`(): Unit {
            // given
            val unicodeText = "한글 테스트 日本語 中文 🎉"

            // when
            val encrypted = AesCipher.encrypt(unicodeText, key16, iv16)
            val decrypted = AesCipher.decrypt(encrypted, key16, iv16)

            // then
            assertThat(decrypted).isEqualTo(unicodeText)
        }

        @Test
        fun `should handle special characters`(): Unit {
            // given
            val specialText = "!@#\$%^&*()_+-={}[]|\\:\";<>?,./~`"

            // when
            val encrypted = AesCipher.encrypt(specialText, key16, iv16)
            val decrypted = AesCipher.decrypt(encrypted, key16, iv16)

            // then
            assertThat(decrypted).isEqualTo(specialText)
        }
    }

    @Nested
    @DisplayName("AesMode sealed class")
    inner class AesModeTest {

        @Test
        fun `should have correct transformation values`(): Unit {
            // when & then
            assertThat(AesMode.ECB.transformation).isEqualTo("AES/ECB/PKCS5Padding")
            assertThat(AesMode.CBC.transformation).isEqualTo("AES/CBC/PKCS5Padding")
        }
    }
}
