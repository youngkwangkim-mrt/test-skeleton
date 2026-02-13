package com.myrealtrip.commonweb.aop.logtrace

import org.aspectj.lang.annotation.Pointcut

/**
 * Pointcut definitions for log tracing.
 *
 * Traces:
 * - All `*Controller` classes
 * - All `*Service` classes
 * - Methods annotated with `@LogTrace`
 *
 * Excludes methods annotated with `@ExcludeLogTrace`.
 */
class TracePointcuts {

    @Pointcut("@annotation(com.myrealtrip.common.annoatations.ExcludeLogTrace)")
    fun excludeLogTraceAnnotation() = Unit

    @Pointcut("@annotation(com.myrealtrip.common.annoatations.LogTrace)")
    fun logTraceAnnotation() = Unit

    @Pointcut("execution(* com.myrealtrip..*Controller.*(..))")
    fun allController() = Unit

    @Pointcut("execution(* com.myrealtrip..*Service.*(..))")
    fun allService() = Unit

    @Pointcut("execution(* com.myrealtrip..*Repository.*(..))")
    fun allRepository() = Unit

    @Pointcut("(allController() || allService() || logTraceAnnotation()) && !excludeLogTraceAnnotation()")
    fun all() = Unit

}
