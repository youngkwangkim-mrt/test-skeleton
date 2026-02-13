package com.myrealtrip.common.annoatations

/**
 * Allowed ip addresses annotation
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckIp(
    /**
     * Allowed ip addresses.
     * `*` is allowed for all ip addresses.
     */
    val allowedIps: Array<String> = [""]
)
