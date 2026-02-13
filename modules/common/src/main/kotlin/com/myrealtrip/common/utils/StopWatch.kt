package com.myrealtrip.common.utils

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.System.Logger.Level

@PublishedApi
internal val stopWatchLogger = KotlinLogging.logger {}

@PublishedApi
internal const val NANOS_PER_MILLI = 1_000_000L

/**
 * Measures and logs the process time of the function.
 * Uses System.nanoTime() for high-precision, low-overhead timing.
 *
 * @param title the title of the process.
 * @param level the log level to use (default: INFO).
 * @param function a function that takes no arguments and returns a value of type [T].
 * @return Pair of (elapsed time in milliseconds, function result)
 */
@JvmOverloads
inline fun <T> stopWatch(
    title: String = "stopWatch",
    level: Level = Level.INFO,
    function: () -> T,
): Pair<Long, T> {
    stopWatchLogger.log(level) { "# >>> $title" }
    val startNanos = System.nanoTime()

    return try {
        val result = function()
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
        stopWatchLogger.log(level) { "# <<< $title , elapsed: ${elapsedMs}ms" }
        elapsedMs to result
    } catch (e: Exception) {
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
        stopWatchLogger.log(level) { "# <<< $title , elapsed: ${elapsedMs}ms (exception)" }
        throw e
    }
}

@PublishedApi
internal inline fun KLogger.log(level: Level, crossinline message: () -> String) {
    when (level) {
        Level.TRACE -> trace { message() }
        Level.DEBUG -> debug { message() }
        Level.INFO -> info { message() }
        Level.WARNING -> warn { message() }
        Level.ERROR -> error { message() }
        Level.ALL, Level.OFF -> {}
    }
}