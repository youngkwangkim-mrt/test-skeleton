package com.myrealtrip.commonweb.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class IpAddrUtilTest : DescribeSpec({

    afterEach {
        RequestContextHolder.resetRequestAttributes()
    }

    describe("IpAddrUtil") {

        context("serverIp") {
            it("should return a valid IP address or 'unknown'") {
                // when
                val serverIp = IpAddrUtil.serverIp

                // then
                serverIp.shouldNotBeBlank()
                // Should be either a valid IP format or "unknown"
                val isValidIpOrUnknown = serverIp == "unknown" ||
                    serverIp.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
                isValidIpOrUnknown shouldBe true
            }
        }

        context("serverIpLastOctet") {
            it("should return last octet of server IP") {
                // when
                val lastOctet = IpAddrUtil.serverIpLastOctet

                // then
                lastOctet.shouldNotBeBlank()
                // If serverIp is valid IP, lastOctet should be numeric (1-3 digits)
                // If serverIp is "unknown", lastOctet will be "unknown"
                if (IpAddrUtil.serverIp != "unknown") {
                    lastOctet shouldMatch Regex("""\d{1,3}""")
                }
            }
        }

        context("getClientIp without parameter") {
            it("should throw IllegalStateException when no request context is available") {
                // given
                RequestContextHolder.resetRequestAttributes()

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    IpAddrUtil.getClientIp()
                }
                exception.message shouldBe "No request context available"
            }

            it("should return client IP when request context is available") {
                // given
                val mockRequest = MockHttpServletRequest()
                mockRequest.remoteAddr = "192.168.1.100"
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))

                // when
                val result = IpAddrUtil.getClientIp()

                // then
                result shouldBe "192.168.1.100"
            }
        }

        context("getClientIp with request parameter - header priority") {
            it("should return X-Forwarded-For header value first") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "10.0.0.1")
                request.addHeader("Proxy-Client-IP", "10.0.0.2")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.1"
            }

            it("should return Proxy-Client-IP when X-Forwarded-For is not present") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("Proxy-Client-IP", "10.0.0.2")
                request.addHeader("WL-Proxy-Client-IP", "10.0.0.3")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.2"
            }

            it("should return WL-Proxy-Client-IP when higher priority headers are not present") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("WL-Proxy-Client-IP", "10.0.0.3")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.3"
            }

            it("should check all header candidates in order") {
                // given - testing HTTP_X_FORWARDED_FOR
                val request = MockHttpServletRequest()
                request.addHeader("HTTP_X_FORWARDED_FOR", "10.0.0.4")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.4"
            }
        }

        context("getClientIp with request parameter - fallback to remoteAddr") {
            it("should return remoteAddr when no headers are present") {
                // given
                val request = MockHttpServletRequest()
                request.remoteAddr = "192.168.1.100"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "192.168.1.100"
            }

            it("should return remoteAddr when all headers are blank") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "")
                request.addHeader("Proxy-Client-IP", "   ")
                request.remoteAddr = "192.168.1.100"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "192.168.1.100"
            }
        }

        context("getClientIp with request parameter - unknown value handling") {
            it("should skip header with 'unknown' value (case-insensitive)") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "unknown")
                request.addHeader("Proxy-Client-IP", "10.0.0.2")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.2"
            }

            it("should skip header with 'UNKNOWN' value (uppercase)") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "UNKNOWN")
                request.addHeader("Proxy-Client-IP", "Unknown")
                request.addHeader("WL-Proxy-Client-IP", "10.0.0.3")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.3"
            }

            it("should fall back to remoteAddr when all headers are 'unknown'") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "unknown")
                request.addHeader("Proxy-Client-IP", "Unknown")
                request.addHeader("WL-Proxy-Client-IP", "UNKNOWN")
                request.remoteAddr = "192.168.1.100"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "192.168.1.100"
            }
        }

        context("getClientIp with request parameter - IP trimming") {
            it("should trim whitespace from IP address") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "  10.0.0.1  ")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "10.0.0.1"
            }
        }

        context("getClientIp with request parameter - X-Forwarded-For with multiple IPs") {
            it("should extract first IP (original client) from comma-separated X-Forwarded-For") {
                // given - X-Forwarded-For contains: client, proxy1, proxy2
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                // Should return only the first IP (original client)
                result shouldBe "203.0.113.195"
            }

            it("should handle single IP in X-Forwarded-For") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "203.0.113.195")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "203.0.113.195"
            }
        }

        context("getClientIp with request parameter - IPv6 addresses") {
            it("should handle IPv6 address") {
                // given
                val request = MockHttpServletRequest()
                request.addHeader("X-Forwarded-For", "2001:0db8:85a3:0000:0000:8a2e:0370:7334")
                request.remoteAddr = "192.168.1.1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
            }

            it("should handle IPv6 loopback address") {
                // given
                val request = MockHttpServletRequest()
                request.remoteAddr = "0:0:0:0:0:0:0:1"

                // when
                val result = IpAddrUtil.getClientIp(request)

                // then
                result shouldBe "0:0:0:0:0:0:0:1"
            }
        }
    }
})
