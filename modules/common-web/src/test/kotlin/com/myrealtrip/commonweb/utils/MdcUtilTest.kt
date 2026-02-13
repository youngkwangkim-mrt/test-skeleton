package com.myrealtrip.commonweb.utils

import com.myrealtrip.common.TraceHeader.APP_TRACE_ID
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeEmpty
import org.slf4j.MDC

class MdcUtilTest : DescribeSpec({

    afterEach {
        MDC.clear()
    }

    describe("MdcUtil") {

        context("getAppTraceId") {
            it("should return MDC value when APP_TRACE_ID is present") {
                // given
                val expectedTraceId = "test-trace-id-12345"
                MDC.put(APP_TRACE_ID, expectedTraceId)

                // when
                val result = MdcUtil.getAppTraceId()

                // then
                result shouldBe expectedTraceId
            }

            it("should return UUID when APP_TRACE_ID is not present in MDC") {
                // given
                MDC.clear()

                // when
                val result = MdcUtil.getAppTraceId()

                // then
                result.shouldNotBeEmpty()
                // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
                result shouldMatch Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            }

            it("should return different UUIDs on multiple calls when MDC is empty") {
                // given
                MDC.clear()

                // when
                val result1 = MdcUtil.getAppTraceId()
                val result2 = MdcUtil.getAppTraceId()

                // then
                result1 shouldNotBe result2
            }
        }

        context("getTraceId") {
            it("should return MDC value when traceId is present") {
                // given
                val expectedTraceId = "span-trace-id-67890"
                MDC.put("traceId", expectedTraceId)

                // when
                val result = MdcUtil.getTraceId()

                // then
                result shouldBe expectedTraceId
            }

            it("should return empty string when traceId is not present in MDC") {
                // given
                MDC.clear()

                // when
                val result = MdcUtil.getTraceId()

                // then
                result.shouldBeEmpty()
            }
        }

        context("MDC isolation") {
            it("should handle different MDC keys independently") {
                // given
                val appTraceId = "app-trace-123"
                val traceId = "trace-456"
                MDC.put(APP_TRACE_ID, appTraceId)
                MDC.put("traceId", traceId)

                // when
                val appResult = MdcUtil.getAppTraceId()
                val traceResult = MdcUtil.getTraceId()

                // then
                appResult shouldBe appTraceId
                traceResult shouldBe traceId
            }

            it("should return correct fallback when only one key is present") {
                // given
                MDC.put("traceId", "only-trace-id")
                // APP_TRACE_ID is not set

                // when
                val appResult = MdcUtil.getAppTraceId()
                val traceResult = MdcUtil.getTraceId()

                // then
                appResult shouldMatch Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                traceResult shouldBe "only-trace-id"
            }
        }
    }
})
