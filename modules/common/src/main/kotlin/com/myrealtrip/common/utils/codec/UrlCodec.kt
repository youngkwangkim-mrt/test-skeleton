package com.myrealtrip.common.utils.codec

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

/**
 * URL encoding/decoding utilities.
 */
object UrlCodec {

    private val DEFAULT_CHARSET: Charset = UTF_8

    /**
     * Encodes a string using URL encoding.
     *
     * @param value the string to encode
     * @param charset the charset to use (default: UTF-8)
     * @return URL-encoded string
     */
    @JvmStatic
    @JvmOverloads
    fun encode(value: String, charset: Charset = DEFAULT_CHARSET): String =
        URLEncoder.encode(value, charset)

    /**
     * Decodes a URL-encoded string.
     *
     * @param value the URL-encoded string to decode
     * @param charset the charset to use (default: UTF-8)
     * @return decoded string
     */
    @JvmStatic
    @JvmOverloads
    fun decode(value: String, charset: Charset = DEFAULT_CHARSET): String =
        URLDecoder.decode(value, charset)

    /**
     * Encodes and then decodes a string to normalize URL-unsafe characters.
     *
     * This is useful for sanitizing strings that may contain characters
     * that need URL encoding.
     *
     * @param value the string to normalize
     * @param charset the charset to use (default: UTF-8)
     * @return normalized string
     */
    @JvmStatic
    @JvmOverloads
    fun normalize(value: String, charset: Charset = DEFAULT_CHARSET): String =
        decode(encode(value, charset), charset)
}
