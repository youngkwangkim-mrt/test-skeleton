package com.myrealtrip.common.utils.cipher

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES encryption mode.
 *
 * @property transformation the JCE transformation string
 */
sealed class AesMode(val transformation: String) {
    /** ECB mode - does not require IV. Less secure, use only when necessary. */
    data object ECB : AesMode("AES/ECB/PKCS5Padding")

    /** CBC mode - requires IV. Recommended for most use cases. */
    data object CBC : AesMode("AES/CBC/PKCS5Padding")
}

/**
 * AES encryption/decryption utility.
 *
 * Supports AES-128, AES-192, and AES-256 depending on key size.
 * Output is Base64-encoded.
 */
object AesCipher {

    private const val AES = "AES"
    private val DEFAULT_CHARSET: Charset = UTF_8
    private const val IV_SIZE = 16

    /**
     * Encrypts plaintext using AES.
     *
     * @param input plaintext to encrypt
     * @param key encryption key (16/24/32 bytes for AES-128/192/256)
     * @param iv initialization vector (16 bytes, null uses zero-filled IV)
     * @param mode encryption mode (default: CBC)
     * @return Base64-encoded ciphertext
     * @throws GeneralSecurityException if encryption fails
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun encrypt(
        input: String,
        key: String,
        iv: String? = null,
        mode: AesMode = AesMode.CBC,
    ): String {
        val secretKey = SecretKeySpec(key.toByteArray(DEFAULT_CHARSET), AES)
        val ivParameterSpec = createIvParameterSpec(iv)
        return encrypt(input, secretKey, ivParameterSpec, mode)
    }

    /**
     * Encrypts plaintext using AES with [SecretKey].
     *
     * @param input plaintext to encrypt
     * @param key secret key
     * @param iv initialization vector (null for ECB mode)
     * @param mode encryption mode (default: CBC)
     * @return Base64-encoded ciphertext
     * @throws GeneralSecurityException if encryption fails
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun encrypt(
        input: String,
        key: SecretKey,
        iv: IvParameterSpec?,
        mode: AesMode = AesMode.CBC,
    ): String {
        val cipher = Cipher.getInstance(mode.transformation)
        when (mode) {
            AesMode.ECB -> cipher.init(Cipher.ENCRYPT_MODE, key)
            AesMode.CBC -> cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        }
        val cipherText = cipher.doFinal(input.toByteArray(DEFAULT_CHARSET))
        return Base64.getEncoder().encodeToString(cipherText)
    }

    /**
     * Decrypts AES-encrypted ciphertext.
     *
     * @param cipherText Base64-encoded ciphertext
     * @param key decryption key (must match encryption key)
     * @param iv initialization vector (must match encryption IV)
     * @param mode encryption mode (default: CBC)
     * @return decrypted plaintext
     * @throws GeneralSecurityException if decryption fails
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun decrypt(
        cipherText: String,
        key: String,
        iv: String? = null,
        mode: AesMode = AesMode.CBC,
    ): String {
        val secretKey = SecretKeySpec(key.toByteArray(DEFAULT_CHARSET), AES)
        val ivParameterSpec = createIvParameterSpec(iv)
        return decrypt(cipherText, secretKey, ivParameterSpec, mode)
    }

    /**
     * Decrypts AES-encrypted ciphertext with [SecretKey].
     *
     * @param cipherText Base64-encoded ciphertext
     * @param key secret key
     * @param iv initialization vector (null for ECB mode)
     * @param mode encryption mode (default: CBC)
     * @return decrypted plaintext
     * @throws GeneralSecurityException if decryption fails
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun decrypt(
        cipherText: String,
        key: SecretKey,
        iv: IvParameterSpec?,
        mode: AesMode = AesMode.CBC,
    ): String {
        val cipher = Cipher.getInstance(mode.transformation)
        when (mode) {
            AesMode.ECB -> cipher.init(Cipher.DECRYPT_MODE, key)
            AesMode.CBC -> cipher.init(Cipher.DECRYPT_MODE, key, iv)
        }
        val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText))
        return String(plainText, DEFAULT_CHARSET)
    }

    /**
     * Generates a secret key by hashing the input key.
     *
     * @param key source key string
     * @param digestAlgorithm hash algorithm (e.g., "MD5" for 128-bit, "SHA-256" for 256-bit)
     * @return AES secret key
     * @throws NoSuchAlgorithmException if digest algorithm is not available
     */
    @JvmStatic
    @Throws(NoSuchAlgorithmException::class)
    fun getSecretKey(key: String, digestAlgorithm: String): SecretKeySpec {
        val digest = MessageDigest.getInstance(digestAlgorithm).digest(key.toByteArray(DEFAULT_CHARSET))
        return SecretKeySpec(digest, AES)
    }

    /**
     * Generates an initialization vector by hashing the input.
     *
     * @param iv source IV string
     * @param digestAlgorithm hash algorithm (use "MD5" for 16-byte IV)
     * @return initialization vector
     * @throws NoSuchAlgorithmException if digest algorithm is not available
     */
    @JvmStatic
    @Throws(NoSuchAlgorithmException::class)
    fun getInitialVector(iv: String, digestAlgorithm: String): IvParameterSpec {
        val digest = MessageDigest.getInstance(digestAlgorithm).digest(iv.toByteArray(DEFAULT_CHARSET))
        return IvParameterSpec(digest)
    }

    // ========== Private Helpers ==========

    private fun createIvParameterSpec(iv: String?): IvParameterSpec =
        if (iv.isNullOrEmpty()) {
            IvParameterSpec(ByteArray(IV_SIZE))
        } else {
            IvParameterSpec(iv.toByteArray(DEFAULT_CHARSET))
        }

}