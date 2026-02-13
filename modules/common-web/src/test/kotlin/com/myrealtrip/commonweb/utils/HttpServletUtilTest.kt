package com.myrealtrip.commonweb.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.ByteArrayInputStream

class HttpServletUtilTest : DescribeSpec({

    afterEach {
        RequestContextHolder.resetRequestAttributes()
    }

    describe("HttpServletUtil") {

        context("getRequest") {
            it("should throw IllegalStateException when no request context is available") {
                // given
                RequestContextHolder.resetRequestAttributes()

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    HttpServletUtil.getRequest()
                }
                exception.message shouldBe "No request context available"
            }

            it("should return HttpServletRequest when context is available") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.requestURI = "/test/path"
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequest()

                // then
                result.requestURI shouldBe "/test/path"
            }
        }

        context("getResponse") {
            it("should throw IllegalStateException when no request context is available") {
                // given
                RequestContextHolder.resetRequestAttributes()

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    HttpServletUtil.getResponse()
                }
                exception.message shouldBe "No request context available"
            }

            it("should throw IllegalStateException when response is null") {
                // given
                val mockRequest = MockHttpServletRequest()
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    HttpServletUtil.getResponse()
                }
                exception.message shouldBe "No response available"
            }

            it("should return HttpServletResponse when context and response are available") {
                // given
                val mockRequest = MockHttpServletRequest()
                val mockResponse = MockHttpServletResponse()
                mockResponse.status = 200
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest, mockResponse))

                // when
                val result = HttpServletUtil.getResponse()

                // then
                result.status shouldBe 200
            }
        }

        context("getRequestParams") {
            it("should return formatted request parameters") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.addParameter("name", "John")
                mockRequest.addParameter("age", "30")
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestParams()

                // then
                result shouldContain "name=[John]"
                result shouldContain "age=[30]"
            }

            it("should return empty string when no parameters exist") {
                // given
                val mockRequest = MockHttpServletRequest()
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestParams()

                // then
                result shouldBe ""
            }

            it("should handle multiple values for same parameter") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.addParameter("tags", "a", "b", "c")
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestParams()

                // then
                result shouldContain "tags=[a, b, c]"
            }
        }

        context("getRequestBody") {
            it("should return content type and size for multipart form data") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = MediaType.MULTIPART_FORM_DATA_VALUE
                mockRequest.setContent(ByteArray(1024))
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldContain MediaType.MULTIPART_FORM_DATA_VALUE
                result shouldContain "1024 bytes"
            }

            it("should parse JSON request body") {
                // given
                val jsonBody = """{"name":"John","age":30}"""
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = MediaType.APPLICATION_JSON_VALUE
                mockRequest.setContent(jsonBody.toByteArray())
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldContain "name"
                result shouldContain "John"
                result shouldContain "age"
            }

            it("should read plain text request body") {
                // given
                val textBody = "Hello, World!"
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = MediaType.TEXT_PLAIN_VALUE
                mockRequest.setContent(textBody.toByteArray())
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldBe textBody
            }

            it("should handle empty request body") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = MediaType.APPLICATION_JSON_VALUE
                mockRequest.setContent(ByteArray(0))
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldBe ""
            }
        }

        context("getRequestBody with maxLength") {
            it("should truncate request body when exceeding maxLength") {
                // given
                val longBody = "A".repeat(1000)
                val mockRequest = createMockRequestWithInputStream(longBody, MediaType.TEXT_PLAIN_VALUE)
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody(100)

                // then
                result.length shouldBe 100
                result shouldBe "A".repeat(100)
            }

            it("should return full body when shorter than maxLength") {
                // given
                val shortBody = "Short text"
                val mockRequest = createMockRequestWithInputStream(shortBody, MediaType.TEXT_PLAIN_VALUE)
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody(1000)

                // then
                result shouldBe shortBody
            }

            it("should return full body when maxLength is -1 (unlimited)") {
                // given
                val body = "A".repeat(5000)
                val mockRequest = createMockRequestWithInputStream(body, MediaType.TEXT_PLAIN_VALUE)
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody(-1)

                // then
                result.length shouldBe 5000
            }
        }

        context("getResponseBody") {
            it("should return empty string when response is not ContentCachingResponseWrapper") {
                // given
                val mockRequest = MockHttpServletRequest()
                val mockResponse = MockHttpServletResponse()
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest, mockResponse))

                // when
                val result = HttpServletUtil.getResponseBody()

                // then
                result shouldBe ""
            }

            it("should return response body from ContentCachingResponseWrapper") {
                // given
                val mockRequest = MockHttpServletRequest()
                val mockResponse = MockHttpServletResponse()
                val cachingResponse = ContentCachingResponseWrapper(mockResponse)
                cachingResponse.writer.write("Response content")
                cachingResponse.writer.flush()

                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest, cachingResponse))

                // when
                val result = HttpServletUtil.getResponseBody()

                // then
                result shouldBe "Response content"
            }

            it("should truncate response body when maxLength is specified") {
                // given
                val mockRequest = MockHttpServletRequest()
                val mockResponse = MockHttpServletResponse()
                val cachingResponse = ContentCachingResponseWrapper(mockResponse)
                cachingResponse.writer.write("A".repeat(1000))
                cachingResponse.writer.flush()

                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest, cachingResponse))

                // when
                val result = HttpServletUtil.getResponseBody(100)

                // then
                result.length shouldBe 100
            }

            it("should return empty string when response body is empty") {
                // given
                val mockRequest = MockHttpServletRequest()
                val mockResponse = MockHttpServletResponse()
                val cachingResponse = ContentCachingResponseWrapper(mockResponse)

                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest, cachingResponse))

                // when
                val result = HttpServletUtil.getResponseBody()

                // then
                result shouldBe ""
            }
        }

        context("content type handling") {
            it("should handle application/json with charset") {
                // given
                val jsonBody = """{"key":"value"}"""
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = "application/json;charset=UTF-8"
                mockRequest.setContent(jsonBody.toByteArray())
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldContain "key"
                result shouldContain "value"
            }

            it("should handle multipart/form-data with boundary") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.contentType = "multipart/form-data; boundary=----WebKitFormBoundary"
                mockRequest.setContent(ByteArray(2048))
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = HttpServletUtil.getRequestBody()

                // then
                result shouldContain "multipart/form-data"
                result shouldContain "2048 bytes"
            }
        }
    }
})

private fun createMockRequestWithInputStream(content: String, contentType: String): HttpServletRequest {
    val inputStream = ByteArrayInputStream(content.toByteArray())
    return object : MockHttpServletRequest() {
        init {
            this.contentType = contentType
        }

        override fun getInputStream(): ServletInputStream {
            return object : ServletInputStream() {
                override fun read(): Int = inputStream.read()
                override fun isFinished(): Boolean = inputStream.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(listener: ReadListener?) {}
            }
        }
    }
}
