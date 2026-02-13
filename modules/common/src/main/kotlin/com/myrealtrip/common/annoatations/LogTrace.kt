package com.myrealtrip.common.annoatations

import java.lang.System.Logger.Level

/**
 * Log trace annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogTrace(
    /**
     * Log level
     */
    val logLevel: Level = Level.DEBUG
)