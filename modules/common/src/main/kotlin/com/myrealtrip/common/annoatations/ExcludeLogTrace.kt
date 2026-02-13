package com.myrealtrip.common.annoatations

/**
 * Exclude log trace annotation
 *
 * Methods annotated with this will be excluded from log tracing.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcludeLogTrace
