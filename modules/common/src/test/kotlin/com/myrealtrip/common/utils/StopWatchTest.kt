package com.myrealtrip.common.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.lang.Thread.sleep

class StopWatchTest : DescribeSpec({

    describe("stopWatch") {

        context("with title parameter") {
            it("should return Pair of elapsed time and function result") {
                // given
                val expectedResult = "test result"

                // when
                val (elapsedMs, result) = stopWatch("Test Function") { expectedResult }

                // then
                result shouldBe expectedResult
                elapsedMs shouldBeGreaterThanOrEqual 0
            }

            it("should measure execution time accurately") {
                // given
                val delayMillis = 100L

                // when
                val (elapsedMs, result) = stopWatch("Long Running Test") {
                    sleep(delayMillis)
                    "completed"
                }

                // then
                result shouldBe "completed"
                elapsedMs shouldBeGreaterThanOrEqual delayMillis
                elapsedMs shouldBeLessThan delayMillis + 100 // Allow 100ms tolerance
            }

            it("should propagate exception while still measuring time") {
                // given
                val expectedException = RuntimeException("Test exception")

                // when & then
                val exception = shouldThrow<RuntimeException> {
                    stopWatch("Exception Test") {
                        sleep(50)
                        throw expectedException
                    }
                }
                exception shouldBe expectedException
            }

            it("should handle null return values") {
                // when
                val (elapsedMs, result) = stopWatch<String?>("Null Return Test") { null }

                // then
                result shouldBe null
                elapsedMs shouldBeGreaterThanOrEqual 0
            }

            it("should work with different return types") {
                // when
                val (_, intResult) = stopWatch("Int Test") { 42 }
                val (_, listResult) = stopWatch("List Test") { listOf(1, 2, 3) }
                val (_, mapResult) = stopWatch("Map Test") { mapOf("key" to "value") }

                // then
                intResult shouldBe 42
                listResult shouldBe listOf(1, 2, 3)
                mapResult shouldBe mapOf("key" to "value")
            }
        }

        context("without title parameter") {
            it("should return Pair using default title") {
                // given
                val expectedResult = "result without title"

                // when
                val (elapsedMs, result) = stopWatch { expectedResult }

                // then
                result shouldBe expectedResult
                elapsedMs shouldBeGreaterThanOrEqual 0
            }

            it("should propagate exceptions without title") {
                // given
                val expectedException = IllegalArgumentException("Invalid argument")

                // when & then
                val exception = shouldThrow<IllegalArgumentException> {
                    stopWatch<Nothing> { throw expectedException }
                }
                exception shouldBe expectedException
            }
        }

        context("elapsed time accuracy") {
            it("should return elapsed time close to actual execution time") {
                // given
                val delays = listOf(10L, 50L, 100L)

                // when & then
                delays.forEach { delay ->
                    val (elapsedMs, _) = stopWatch("Delay $delay") {
                        sleep(delay)
                        "done"
                    }
                    elapsedMs shouldBeGreaterThanOrEqual delay
                    elapsedMs shouldBeLessThan delay + 50 // 50ms tolerance
                }
            }

            it("should return near-zero elapsed time for fast functions") {
                // when
                val (elapsedMs, result) = stopWatch("Fast Function") { 1 + 1 }

                // then
                result shouldBe 2
                elapsedMs shouldBeLessThan 10 // Should be nearly instant
            }
        }

        context("concurrent execution") {
            it("should handle multiple sequential executions with independent timing") {
                // given
                val functions = (1..5).map { index ->
                    {
                        sleep(10)
                        "Result $index"
                    }
                }

                // when
                val results = functions.mapIndexed { index, func ->
                    stopWatch("Sequential Test $index") { func() }
                }

                // then
                results.forEachIndexed { index, (elapsedMs, result) ->
                    result shouldBe "Result ${index + 1}"
                    elapsedMs shouldBeGreaterThanOrEqual 10
                }
            }
        }
    }
})
