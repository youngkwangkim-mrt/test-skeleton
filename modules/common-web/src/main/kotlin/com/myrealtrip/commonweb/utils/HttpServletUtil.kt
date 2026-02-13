package com.myrealtrip.commonweb.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.ContentCachingResponseWrapper
import tools.jackson.databind.json.JsonMapper
import java.io.InputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val logger = KotlinLogging.logger {}

/**
 * HttpServlet utility class
 */
object HttpServletUtil {

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    /**
     * Get HttpServletRequest from current request context
     *
     * @return HttpServletRequest
     * @throws IllegalStateException if no request context is available
     */
    fun getRequest(): HttpServletRequest {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw IllegalStateException("No request context available")
        return attributes.request
    }

    /**
     * Get HttpServletResponse from current request context
     *
     * @return HttpServletResponse
     * @throws IllegalStateException if no request context is available
     */
    fun getResponse(): HttpServletResponse {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw IllegalStateException("No request context available")
        return attributes.response
            ?: throw IllegalStateException("No response available")
    }

    /**
     * Get request parameters as string
     *
     * @return request parameters as string
     */
    fun getRequestParams(): String {
        return getRequest().parameterMap
            .entries
            .joinToString(" ") { (key, value) -> "$key=${value.contentToString()}" }
    }

    /**
     * Read request body of application/json, application/xml
     *
     * @return request body
     */
    fun getRequestBody(): String {
        val request = getRequest()
        val contentType = request.contentType
        val contentLength = request.contentLength

        if (isContentTypeOf(request, MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return "$contentType :: $contentLength bytes"
        }

        return try {
            request.inputStream.use { inputStream ->
                when {
                    isContentTypeOf(request, MediaType.APPLICATION_JSON_VALUE) -> {
                        jsonMapper.readTree(inputStream)?.toString() ?: ""
                    }
                    // isContentTypeOf(request, MediaType.TEXT_XML_VALUE) ||
                    // isContentTypeOf(request, MediaType.APPLICATION_XML_VALUE) -> {
                    //     parseXmlToString(inputStream)
                    // }
                    else -> {
                        inputStream.readAllBytes().toString(Charsets.UTF_8)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn { "Failed to parse request body :: ${e.message}" }
            "$contentType :: $contentLength bytes"
        }
    }

    /**
     * Read request body with maximum length
     *
     * @param maxLength Maximum length of the request body (-1 for unlimited)
     * @return request body with maximum length
     */
    fun getRequestBody(maxLength: Int): String {
        val inputStream = getRequest().inputStream
        val requestBodyBuilder = StringBuilder()
        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (maxLength != -1) {
                val lengthToAppend = minOf(bytesRead, maxLength - requestBodyBuilder.length)
                requestBodyBuilder.append(String(buffer, 0, lengthToAppend, Charsets.UTF_8))
                if (requestBodyBuilder.length >= maxLength) {
                    break
                }
            } else {
                requestBodyBuilder.append(String(buffer, 0, bytesRead, Charsets.UTF_8))
            }
        }

        return requestBodyBuilder.toString()
    }

    /**
     * Read response body
     *
     * @return response body as string
     */
    fun getResponseBody(): String = getResponseBody(-1)

    /**
     * Read response body with maximum length
     *
     * @param maxLength Maximum length of the response body (-1 for unlimited)
     * @return response body with maximum length
     */
    fun getResponseBody(maxLength: Int): String {
        val wrapper = getResponse() as? ContentCachingResponseWrapper ?: return ""
        wrapper.characterEncoding = Charsets.UTF_8.name()

        val buf = wrapper.contentAsByteArray
        val length = if (maxLength == -1) buf.size else minOf(buf.size, maxLength)

        return if (length > 0) {
            String(buf, 0, length, Charsets.UTF_8)
        } else {
            ""
        }
    }

    /**
     * Check if the request content type matches the expected type
     *
     * @param request HttpServletRequest
     * @param expectedType expected content type
     * @return true if content type matches
     */
    private fun isContentTypeOf(request: HttpServletRequest, expectedType: String): Boolean {
        return request.contentType?.startsWith(expectedType) == true
    }

    /**
     * Parse XML input stream to string
     *
     * @param inputStream XML input stream
     * @return XML as formatted string
     */
    @Suppress("unused")
    private fun parseXmlToString(inputStream: InputStream): String {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")

        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

}
