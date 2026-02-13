package com.myrealtrip.common

object TraceHeader {
    const val APP_REQUEST_TIMESTAMP = "X-ZZ-Req-Ts"
    const val APP_RESPONSE_TIMESTAMP = "X-ZZ-Res-Ts"
    const val APP_TRACE_ID = "X-ZZ-TraceId"
    const val APP_ACCESS_ID = "X-ZZ-AccessId"
    const val X_B3_TRACE_ID = "X-B3-TraceId"
    const val X_B3_SPAN_ID = "X-B3-SpanId"
}

const val HEALTH_CHECK_PATH = "/_global/health"

val FILTER_EXCLUDE_PATH = arrayOf(
    HEALTH_CHECK_PATH,
    "/css/",
    "/js/",
    "/img/",
    "/images/",
    "/error/",
    "/v3/api-docs",
    "/swagger-ui",
    "/download/",
    ".ico",
    "/.well-known",
)

val INTERCEPTOR_EXCLUDE_PATH = listOf(
    HEALTH_CHECK_PATH,
    "/css/**", "/js/**", "/img/**", "/images/**", "/error/**", "/download/**", "/common/file**",
    "/swagger-ui/**", "/v3/api-docs/**", "/*.ico",
    "/.well-known/**",
)
