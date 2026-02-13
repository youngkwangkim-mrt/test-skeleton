package com.myrealtrip.common.utils.coroutine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import org.slf4j.MDC
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class CoroutineUtilsTest : DescribeSpec({

    val traceIdKey = "traceId"

    beforeEach {
        MDC.clear()
    }

    afterEach {
        MDC.clear()
    }

    describe("runBlockingWithMDC") {

        context("MDC 전파") {
            it("should propagate MDC context into coroutine") {
                // given
                val expectedTraceId = "test-trace-id-12345"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay") {
                // given
                val expectedTraceId = "trace-with-delay"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    delay(50)
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("결과 반환") {
            it("should return block result") {
                // given
                val expected = "test result"

                // when
                val result = runBlockingWithMDC { expected }

                // then
                result shouldBe expected
            }

            it("should work with different return types") {
                // when
                val intResult = runBlockingWithMDC { 42 }
                val listResult = runBlockingWithMDC { listOf(1, 2, 3) }
                val nullResult = runBlockingWithMDC<String?> { null }

                // then
                intResult shouldBe 42
                listResult shouldBe listOf(1, 2, 3)
                nullResult shouldBe null
            }
        }

    }

    describe("asyncWithMDC") {

        context("MDC 전파") {
            it("should propagate MDC context into async coroutine") {
                // given
                val expectedTraceId = "async-trace-id"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncWithMDC {
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in async") {
                // given
                val expectedTraceId = "async-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncWithMDC {
                        delay(50)
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("병렬 실행") {
            it("should execute multiple async coroutines in parallel") {
                // given
                val expectedTraceId = "parallel-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val results = runBlockingWithMDC {
                    val deferred1 = asyncWithMDC {
                        delay(50)
                        "result1-${MDC.get(traceIdKey)}"
                    }
                    val deferred2 = asyncWithMDC {
                        delay(50)
                        "result2-${MDC.get(traceIdKey)}"
                    }
                    listOf(deferred1.await(), deferred2.await())
                }

                // then
                results shouldBe listOf("result1-$expectedTraceId", "result2-$expectedTraceId")
            }
        }
    }

    describe("launchWithMDC") {

        context("MDC 전파") {
            it("should propagate MDC context into launch coroutine") {
                // given
                val expectedTraceId = "launch-trace-id"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null
                val latch = CountDownLatch(1)

                // when
                runBlockingWithMDC {
                    launchWithMDC {
                        capturedTraceId = MDC.get(traceIdKey)
                        latch.countDown()
                    }
                }
                latch.await(1, TimeUnit.SECONDS)

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in launch") {
                // given
                val expectedTraceId = "launch-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null

                // when
                runBlockingWithMDC {
                    val job = launchWithMDC {
                        delay(50)
                        capturedTraceId = MDC.get(traceIdKey)
                    }
                    job.join()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("실행 완료") {
            it("should complete launch job") {
                // given
                var executed = false

                // when
                runBlockingWithMDC {
                    val job = launchWithMDC {
                        executed = true
                    }
                    job.join()
                }

                // then
                executed shouldBe true
            }
        }
    }

    describe("withLogging") {

        context("결과 반환") {
            it("should return block result") {
                // given
                val expected = "logged result"

                // when
                val result = runBlockingWithMDC {
                    withLogging("Test Block") { expected }
                }

                // then
                result shouldBe expected
            }

            it("should preserve MDC in withLogging block") {
                // given
                val expectedTraceId = "logging-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    withLogging("MDC Test") {
                        MDC.get(traceIdKey)
                    }
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }
    }

    describe("MDC 격리") {

        it("should not affect outer MDC after coroutine completes") {
            // given
            val outerTraceId = "outer-trace"
            MDC.put(traceIdKey, outerTraceId)

            // when
            runBlockingWithMDC {
                MDC.put(traceIdKey, "inner-trace")
            }

            // then
            MDC.get(traceIdKey) shouldBe outerTraceId
        }

        it("should handle empty MDC") {
            // given
            MDC.clear()

            // when
            val capturedTraceId = runBlockingWithMDC {
                MDC.get(traceIdKey)
            }

            // then
            capturedTraceId shouldBe null
        }
    }

    describe("복합 시나리오") {

        it("should propagate MDC through nested async and launch") {
            // given
            val expectedTraceId = "nested-trace"
            MDC.put(traceIdKey, expectedTraceId)
            val capturedIds = mutableListOf<String?>()

            // when
            runBlockingWithMDC {
                capturedIds.add(MDC.get(traceIdKey))

                val deferred = asyncWithMDC {
                    delay(10)
                    capturedIds.add(MDC.get(traceIdKey))

                    launchWithMDC {
                        delay(10)
                        capturedIds.add(MDC.get(traceIdKey))
                    }.join()

                    MDC.get(traceIdKey)
                }
                deferred.await()
            }

            // then
            capturedIds.forEach { it shouldBe expectedTraceId }
        }

        it("should preserve MDC in multiple parallel coroutines") {
            // given
            val expectedTraceId = "parallel-trace"
            MDC.put(traceIdKey, expectedTraceId)

            // when
            val traceIds = runBlockingWithMDC {
                val deferred1 = asyncWithMDC {
                    delay(10)
                    MDC.get(traceIdKey)
                }
                val deferred2 = asyncWithMDC {
                    delay(10)
                    MDC.get(traceIdKey)
                }
                listOf(deferred1.await(), deferred2.await())
            }

            // then
            traceIds.size shouldBe 2
            traceIds.forEach { it shouldBe expectedTraceId }
        }

        it("should propagate MDC across different dispatchers") {
            // given
            val expectedTraceId = "cross-dispatcher-trace"
            MDC.put(traceIdKey, expectedTraceId)
            val capturedIds = mutableListOf<String?>()

            // when
            runBlockingWithMDC {
                capturedIds.add(MDC.get(traceIdKey))

                val deferred = asyncOnVirtualThread {
                    delay(10)
                    capturedIds.add(MDC.get(traceIdKey))

                    launchOnIoThread {
                        delay(10)
                        capturedIds.add(MDC.get(traceIdKey))
                    }.join()

                    MDC.get(traceIdKey)
                }
                deferred.await()
            }

            // then
            capturedIds.size shouldBe 3
            capturedIds.forEach { it shouldBe expectedTraceId }
        }
    }

    // =========================================================================
    // Virtual Thread + MDC
    // =========================================================================

    describe("runBlockingOnVirtualThread") {

        context("MDC 전파") {
            it("should propagate MDC context into virtual thread coroutine") {
                // given
                val expectedTraceId = "vt-blocking-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingOnVirtualThread {
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay") {
                // given
                val expectedTraceId = "vt-blocking-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingOnVirtualThread {
                    delay(50)
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("결과 반환") {
            it("should return block result") {
                // given
                val expected = "vt-result"

                // when
                val result = runBlockingOnVirtualThread { expected }

                // then
                result shouldBe expected
            }
        }

        context("Virtual Thread 확인") {
            it("should run on virtual thread") {
                // when
                val isVirtual = runBlockingOnVirtualThread {
                    Thread.currentThread().isVirtual
                }

                // then
                isVirtual shouldBe true
            }
        }
    }

    describe("asyncOnVirtualThread") {

        context("MDC 전파") {
            it("should propagate MDC context into async virtual thread coroutine") {
                // given
                val expectedTraceId = "vt-async-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncOnVirtualThread {
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in async") {
                // given
                val expectedTraceId = "vt-async-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncOnVirtualThread {
                        delay(50)
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("병렬 실행") {
            it("should execute multiple async coroutines in parallel with MDC") {
                // given
                val expectedTraceId = "vt-async-parallel-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val results = runBlockingWithMDC {
                    val deferred1 = asyncOnVirtualThread {
                        delay(50)
                        "result1-${MDC.get(traceIdKey)}"
                    }
                    val deferred2 = asyncOnVirtualThread {
                        delay(50)
                        "result2-${MDC.get(traceIdKey)}"
                    }
                    listOf(deferred1.await(), deferred2.await())
                }

                // then
                results shouldBe listOf("result1-$expectedTraceId", "result2-$expectedTraceId")
            }
        }

        context("Virtual Thread 확인") {
            it("should run on virtual thread") {
                // when
                val isVirtual = runBlockingWithMDC {
                    val deferred = asyncOnVirtualThread {
                        Thread.currentThread().isVirtual
                    }
                    deferred.await()
                }

                // then
                isVirtual shouldBe true
            }
        }
    }

    describe("launchOnVirtualThread") {

        context("MDC 전파") {
            it("should propagate MDC context into launch virtual thread coroutine") {
                // given
                val expectedTraceId = "vt-launch-trace"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null

                // when
                runBlockingWithMDC {
                    val job = launchOnVirtualThread {
                        capturedTraceId = MDC.get(traceIdKey)
                    }
                    job.join()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in launch") {
                // given
                val expectedTraceId = "vt-launch-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null

                // when
                runBlockingWithMDC {
                    val job = launchOnVirtualThread {
                        delay(50)
                        capturedTraceId = MDC.get(traceIdKey)
                    }
                    job.join()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("실행 완료") {
            it("should complete launch job") {
                // given
                var executed = false

                // when
                runBlockingWithMDC {
                    val job = launchOnVirtualThread {
                        executed = true
                    }
                    job.join()
                }

                // then
                executed shouldBe true
            }
        }

        context("Virtual Thread 확인") {
            it("should run on virtual thread") {
                // given
                var isVirtual = false

                // when
                runBlockingWithMDC {
                    val job = launchOnVirtualThread {
                        isVirtual = Thread.currentThread().isVirtual
                    }
                    job.join()
                }

                // then
                isVirtual shouldBe true
            }
        }
    }

    // =========================================================================
    // IO Dispatcher + MDC
    // =========================================================================

    describe("runBlockingOnIoThread") {

        context("MDC 전파") {
            it("should propagate MDC context into IO thread coroutine") {
                // given
                val expectedTraceId = "io-blocking-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingOnIoThread {
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay") {
                // given
                val expectedTraceId = "io-blocking-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingOnIoThread {
                    delay(50)
                    MDC.get(traceIdKey)
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("결과 반환") {
            it("should return block result") {
                // given
                val expected = "io-result"

                // when
                val result = runBlockingOnIoThread { expected }

                // then
                result shouldBe expected
            }
        }
    }

    describe("asyncOnIoThread") {

        context("MDC 전파") {
            it("should propagate MDC context into async IO coroutine") {
                // given
                val expectedTraceId = "io-async-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncOnIoThread {
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in async") {
                // given
                val expectedTraceId = "io-async-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val capturedTraceId = runBlockingWithMDC {
                    val deferred = asyncOnIoThread {
                        delay(50)
                        MDC.get(traceIdKey)
                    }
                    deferred.await()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("병렬 실행") {
            it("should execute multiple async coroutines in parallel with MDC") {
                // given
                val expectedTraceId = "io-async-parallel-trace"
                MDC.put(traceIdKey, expectedTraceId)

                // when
                val results = runBlockingWithMDC {
                    val deferred1 = asyncOnIoThread {
                        delay(50)
                        "result1-${MDC.get(traceIdKey)}"
                    }
                    val deferred2 = asyncOnIoThread {
                        delay(50)
                        "result2-${MDC.get(traceIdKey)}"
                    }
                    listOf(deferred1.await(), deferred2.await())
                }

                // then
                results shouldBe listOf("result1-$expectedTraceId", "result2-$expectedTraceId")
            }
        }
    }

    describe("launchOnIoThread") {

        context("MDC 전파") {
            it("should propagate MDC context into launch IO coroutine") {
                // given
                val expectedTraceId = "io-launch-trace"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null

                // when
                runBlockingWithMDC {
                    val job = launchOnIoThread {
                        capturedTraceId = MDC.get(traceIdKey)
                    }
                    job.join()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }

            it("should preserve MDC after delay in launch") {
                // given
                val expectedTraceId = "io-launch-delay-trace"
                MDC.put(traceIdKey, expectedTraceId)
                var capturedTraceId: String? = null

                // when
                runBlockingWithMDC {
                    val job = launchOnIoThread {
                        delay(50)
                        capturedTraceId = MDC.get(traceIdKey)
                    }
                    job.join()
                }

                // then
                capturedTraceId shouldBe expectedTraceId
            }
        }

        context("실행 완료") {
            it("should complete launch job") {
                // given
                var executed = false

                // when
                runBlockingWithMDC {
                    val job = launchOnIoThread {
                        executed = true
                    }
                    job.join()
                }

                // then
                executed shouldBe true
            }
        }
    }

    // =========================================================================
    // Retry
    // =========================================================================

    describe("retry") {

        context("성공 케이스") {
            it("should succeed on first attempt without retry") {
                // given
                val attemptCount = AtomicInteger(0)

                // when
                val result = runBlockingWithMDC {
                    retry(maxAttempts = 3, delay = 10.milliseconds) {
                        attemptCount.incrementAndGet()
                        "success"
                    }
                }

                // then
                result shouldBe "success"
                attemptCount.get() shouldBe 1
            }

            it("should succeed after intermediate failures") {
                // given
                val attemptCount = AtomicInteger(0)

                // when
                val result = runBlockingWithMDC {
                    retry(maxAttempts = 3, delay = 10.milliseconds) {
                        val attempt = attemptCount.incrementAndGet()
                        if (attempt < 3) throw IOException("attempt $attempt failed")
                        "success-on-third"
                    }
                }

                // then
                result shouldBe "success-on-third"
                attemptCount.get() shouldBe 3
            }
        }

        context("실패 케이스") {
            it("should throw last exception when all attempts exhausted") {
                // given
                val attemptCount = AtomicInteger(0)

                // when & then
                val exception = shouldThrow<IOException> {
                    runBlockingWithMDC {
                        retry(maxAttempts = 3, delay = 10.milliseconds) {
                            attemptCount.incrementAndGet()
                            throw IOException("always fails")
                        }
                    }
                }

                exception.message shouldBe "always fails"
                attemptCount.get() shouldBe 3
            }

            it("should throw IllegalArgumentException when maxAttempts less than 1") {
                // when & then
                shouldThrow<IllegalArgumentException> {
                    runBlockingWithMDC {
                        retry(maxAttempts = 0, delay = 10.milliseconds) {
                            "unreachable"
                        }
                    }
                }
            }
        }

        context("재시도 횟수") {
            it("should retry exact maxAttempts times") {
                // given
                val attemptCount = AtomicInteger(0)

                // when & then
                shouldThrow<IOException> {
                    runBlockingWithMDC {
                        retry(maxAttempts = 5, delay = 10.milliseconds) {
                            attemptCount.incrementAndGet()
                            throw IOException("fail")
                        }
                    }
                }

                attemptCount.get() shouldBe 5
            }
        }

        context("retryOn 조건") {
            it("should not retry when retryOn returns false") {
                // given
                val attemptCount = AtomicInteger(0)

                // when & then
                shouldThrow<IllegalStateException> {
                    runBlockingWithMDC {
                        retry(
                            maxAttempts = 3,
                            delay = 10.milliseconds,
                            retryOn = { it is IOException },
                        ) {
                            attemptCount.incrementAndGet()
                            throw IllegalStateException("not retryable")
                        }
                    }
                }

                attemptCount.get() shouldBe 1
            }
        }

        context("지수 백오프") {
            it("should apply exponential backoff and retry correct number of times") {
                // given
                val attemptCount = AtomicInteger(0)

                // when & then
                shouldThrow<IOException> {
                    runBlockingWithMDC {
                        retry(
                            maxAttempts = 4,
                            delay = 10.milliseconds,
                            backoffMultiplier = 2.0,
                        ) {
                            attemptCount.incrementAndGet()
                            throw IOException("backoff fail")
                        }
                    }
                }

                attemptCount.get() shouldBe 4
            }
        }
    }

    describe("retryBlocking") {

        context("결과 반환") {
            it("should return result") {
                // given
                val attemptCount = AtomicInteger(0)

                // when
                val result = retryBlocking(maxAttempts = 3, delay = 10.milliseconds) {
                    val attempt = attemptCount.incrementAndGet()
                    if (attempt < 2) throw IOException("attempt $attempt")
                    "blocking-success"
                }

                // then
                result shouldBe "blocking-success"
                attemptCount.get() shouldBe 2
            }
        }

        context("MDC 전파") {
            it("should propagate MDC through retries") {
                // given
                val expectedTraceId = "retry-mdc-trace"
                MDC.put(traceIdKey, expectedTraceId)
                val capturedTraceIds = mutableListOf<String?>()
                val attemptCount = AtomicInteger(0)

                // when
                val result = retryBlocking(maxAttempts = 3, delay = 10.milliseconds) {
                    capturedTraceIds.add(MDC.get(traceIdKey))
                    val attempt = attemptCount.incrementAndGet()
                    if (attempt < 3) throw IOException("attempt $attempt")
                    "mdc-retry-success"
                }

                // then
                result shouldBe "mdc-retry-success"
                capturedTraceIds.size shouldBe 3
                capturedTraceIds.forEach { it shouldBe expectedTraceId }
            }
        }

        context("실패 케이스") {
            it("should throw when all attempts fail") {
                // given
                val attemptCount = AtomicInteger(0)

                // when & then
                val exception = shouldThrow<IOException> {
                    retryBlocking(maxAttempts = 3, delay = 10.milliseconds) {
                        attemptCount.incrementAndGet()
                        throw IOException("blocking always fails")
                    }
                }

                exception.message shouldBe "blocking always fails"
                attemptCount.get() shouldBe 3
            }
        }
    }
})
