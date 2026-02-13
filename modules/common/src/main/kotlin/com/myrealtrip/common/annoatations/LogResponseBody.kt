package com.myrealtrip.common.annoatations

import java.lang.System.Logger.Level

/**
 * Log response body annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogResponseBody(
    /**
     * Whether to log response body
     */
    val value: Boolean = true,
    /**
     * Maximum length of response body to log
     */
    val maxLength: Int = 2_000,
    /**
     * Whether to print all response body
     * If true, it will print the entire response body without truncation
     */
    val printAll: Boolean = false,
    /**
     * Log level
     */
    val logLevel: Level = Level.INFO
)
