package com.myrealtrip.common.annoatations

/**
 * Exclude log trace annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcludeRequestLog
