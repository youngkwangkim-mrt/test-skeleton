package com.myrealtrip.testsupport

import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Utility for measuring test execution time.
 *
 * Usage:
 * ```kotlin
 * // Simple measurement
 * val elapsed = TestTimeRunner.measure("my operation") {
 *     myService.process()
 * }
 *
 * // With loop count
 * val elapsed = TestTimeRunner.measure("my operation", loopCount = 1000) {
 *     myService.process()
 * }
 *
 * // With warm-up iterations
 * val elapsed = TestTimeRunner.measure("my operation", loopCount = 1000, warmUp = 100) {
 *     myService.process()
 * }
 * ```
 */
object TestTimeRunner {

    fun measure(
        testName: String,
        loopCount: Int = 1,
        warmUp: Int = 0,
        block: () -> Any,
    ): Duration {
        repeat(warmUp) { block() }

        val elapsed = measureTime {
            repeat(loopCount) { block() }
        }

        println("==> $testName, elapsed = ${elapsed.inWholeMilliseconds} ms")
        return elapsed
    }

}
