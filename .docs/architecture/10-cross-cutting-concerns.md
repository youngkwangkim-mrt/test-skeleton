---
title: "Cross-Cutting Concerns"
description: "Request tracing, logging, and IP verification through Filters, Interceptors, AOP, and Async configuration"
category: "architecture"
order: 10
last_updated: "2026-02-14"
---

# Cross-Cutting Concerns

## Overview

This document describes the cross-cutting concerns that apply across the entire application. The system implements request tracing, logging, and IP verification through Filters, Interceptors, and Aspect-Oriented Programming (AOP).

**Application Order**:
```
Request → Filter → Interceptor → AOP → Controller → Response
```

## Filter Chain

### 1. ContentCachingFilter (Order: HIGHEST_PRECEDENCE)

ContentCachingFilter caches Request and Response bodies to enable multiple reads for logging purposes.

**Purpose**: Cache Request/Response Body for multiple reads

```kotlin
@WebFilter(filterName = "ContentCachingFilter", urlPatterns = ["/**"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class ContentCachingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        val cachedRequest = CachedBodyHttpServletRequest(request)
        val cachedResponse = ContentCachingResponseWrapper(response)

        try {
            filterChain.doFilter(cachedRequest, cachedResponse)
        } finally {
            cachedResponse.copyBodyToResponse()
        }
    }
}
```

**Role**: Enable multiple reads of Request/Response Body for logging purposes

### 2. AppTraceFilter (Order: HIGHEST_PRECEDENCE + 1)

AppTraceFilter generates a unique trace ID (UUID v7) for each request and configures MDC (Mapped Diagnostic Context) for logging.

**Purpose**: Generate request trace ID (UUID v7) and configure MDC

```kotlin
@WebFilter(filterName = "AppTraceFilter", urlPatterns = ["/**"])
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class AppTraceFilter(private val tracer: Tracer) : OncePerRequestFilter() {

    override fun doFilterInternal(...) {
        val startNanos = System.nanoTime()
        val appTraceId = Uuid.generateV7().toString()  // UUID v7

        setupTraceContext(response, appTraceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            val processTimeMs = (System.nanoTime() - startNanos) / 1_000_000
            logProcessTime(processTimeMs)
            response.setHeader(APP_RESPONSE_TIMESTAMP, System.currentTimeMillis().toString())
            MDC.clear()
        }
    }

    private fun setupTraceContext(response: HttpServletResponse, appTraceId: String) {
        MDC.put(APP_TRACE_ID, appTraceId)
        response.setHeader(APP_TRACE_ID, appTraceId)
        response.setHeader(X_B3_TRACE_ID, currentTraceId())
    }
}
```

**Key Features**:

| Feature | Description |
|---------|-------------|
| **UUID v7** | Time-based sortable trace ID |
| **MDC Setup** | Automatically includes `APP_TRACE_ID` in all logs |
| **Response Headers** | Adds `APP_TRACE_ID`, `X-B3-TRACE-ID`, `APP_RESPONSE_TIMESTAMP` |
| **Processing Time** | Logs warning when processing exceeds 8 seconds |

**Response Headers**:
```http
APP_TRACE_ID: 01932d8e-7890-7abc-9def-123456789abc
X-B3-TRACE-ID: 4bf92f3577b34da6
APP_RESPONSE_TIMESTAMP: 1707897600000
```

## Interceptor Chain

### 1. LogInterceptor

LogInterceptor logs request information including IP, URI, Headers, Parameters, and Body.

**Purpose**: Log request information (IP, URI, Headers, Parameters, Body)

```kotlin
class LogInterceptor(
    private val environmentUtil: EnvironmentUtil,
) : HandlerInterceptor {

    override fun preHandle(...): Boolean {
        if (shouldSkipLogging(handler)) return true
        logRequestInfo(request)
        return true
    }

    private fun shouldSkipLogging(handler: Any): Boolean =
        environmentUtil.isProduction() &&
        handler is HandlerMethod &&
        handler.hasMethodAnnotation(ExcludeRequestLog::class.java)
}
```

**Logged Content**:
```
# ==> REQUEST INFO ::
ServerIp = 192.168.1.10 , ClientIp = 203.0.113.45
RequestURI = POST /api/users
Headers:
  content-type = application/json
  x-b3-traceid = 4bf92f3577b34da6
RequestBody = {"name":"John","email":"john@example.com"}
```

**Exclude Logging**:

Use the `@ExcludeRequestLog` annotation to exclude sensitive endpoints from logging in production.

```kotlin
@ExcludeRequestLog  // Exclude logging in production
@GetMapping("/sensitive-data")
fun getSensitiveData()
```

### 2. LogResponseBodyInterceptor

LogResponseBodyInterceptor logs Response Body when the `@LogResponseBody` annotation is present.

**Purpose**: Log Response Body (annotation-based)

```kotlin
class LogResponseBodyInterceptor : HandlerInterceptor {

    override fun afterCompletion(...) {
        if (handler !is HandlerMethod) return

        handler.getMethodAnnotation(LogResponseBody::class.java)
            ?.let { logResponseBody(it) }
    }
}
```

**Usage**:

```kotlin
@LogResponseBody  // INFO level, max 1000 chars
@GetMapping("/users")
fun getUsers()

@LogResponseBody(logLevel = Level.DEBUG, maxLength = 5000)
@GetMapping("/debug")
fun debugEndpoint()
```

## AOP Aspects

### 1. LogTraceAspect (Order: 2)

LogTraceAspect provides method execution tracing and call hierarchy visualization.

**Purpose**: Method execution tracing and call hierarchy visualization

```kotlin
@Aspect
@Order(2)
class LogTraceAspect(private val logTrace: LogTrace) {

    @Around("com.myrealtrip.commonweb.aop.logtrace.TracePointcuts.all()")
    fun traceMethod(joinPoint: ProceedingJoinPoint): Any? {
        val status = logTrace.begin(joinPoint.signature.toShortString())

        return try {
            joinPoint.proceed().also { logTrace.end(status) }
        } catch (e: Exception) {
            logTrace.exception(status, e)
            throw e
        }
    }
}
```

**Pointcut**:

The aspect applies to Controller, Facade, and Application layers.

```kotlin
@Pointcut("execution(* com.myrealtrip..api..*Controller.*(..))")
fun controllerPointcut()

@Pointcut("execution(* com.myrealtrip..facade..*Facade.*(..))")
fun facadePointcut()

@Pointcut("execution(* com.myrealtrip..application..*Application.*(..))")
fun applicationPointcut()
```

**Log Output**:

The aspect produces hierarchical log output showing method call depth and execution time.

```
[01932d8e] |-->UserController.getUser(..)
[01932d8e] |   |-->UserFacade.findById(..)
[01932d8e] |   |   |-->UserQueryApplication.findById(..)
[01932d8e] |   |   |<--UserQueryApplication.findById(..) time=15ms
[01932d8e] |   |<--UserFacade.findById(..) time=18ms
[01932d8e] |<--UserController.getUser(..) time=25ms
```

### 2. CheckIpAspect (Order: 3)

CheckIpAspect implements IP whitelist-based access control.

**Purpose**: IP whitelist-based access control

```kotlin
@Aspect
@Order(3)
class CheckIpAspect {

    @Before("checkIpPointcut()")
    fun checkClientIp(joinPoint: JoinPoint) {
        val annotation = findAnnotation(joinPoint) ?: return
        val clientIp = IpAddrUtil.getClientIp()
        val allowedPatterns = IpWhitelist.LOCAL_IPS + annotation.allowedIps

        if (!IpWhitelist.isWhitelisted(clientIp, allowedPatterns)) {
            throw UnauthorizedIpException(clientIp)
        }
    }
}
```

**Usage**:

Apply the `@CheckIp` annotation at class or method level.

```kotlin
// Class level
@CheckIp(allowedIps = ["192.168.1.0/24", "10.0.0.1"])
@RestController
class AdminController

// Method level (takes precedence)
@CheckIp(allowedIps = ["203.0.113.0/24"])
@PostMapping("/admin/users")
fun createUser()
```

**IP Patterns**:

| Type | Example | Description |
|------|---------|-------------|
| Local | `127.0.0.1`, `localhost` | Always allowed |
| Exact | `192.168.1.100` | Exact match |
| Wildcard | `192.168.1.*` | Range match |
| CIDR | `192.168.1.0/24` | Subnet match |

## Async Configuration

### AsyncConfig

AsyncConfig preserves MDC context during asynchronous method execution using Virtual Threads.

**Purpose**: Preserve MDC context during asynchronous method execution

```kotlin
@Configuration
class AsyncConfig(
    private val contextPropagatingTaskDecorator: ContextPropagatingTaskDecorator
) : AsyncConfigurer {

    override fun getAsyncExecutor(): Executor? {
        return asyncVirtualThreadTaskExecutor()
    }

    private fun asyncVirtualThreadTaskExecutor(): AsyncTaskExecutor {
        val adapter = TaskExecutorAdapter(VirtualThreadTaskExecutor("async-vt-"))
        adapter.setTaskDecorator(contextPropagatingTaskDecorator)
        return adapter
    }
}
```

**Features**:
- Uses Java 21+ Virtual Threads
- Automatic MDC context propagation
- Thread Prefix: `async-vt-`

### MdcTaskDecorator

MdcTaskDecorator copies and restores MDC context for asynchronous tasks.

```kotlin
class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val callerContext = MDC.getCopyOfContextMap()

        return Runnable {
            val previous = MDC.getCopyOfContextMap()
            try {
                setOrClear(callerContext)
                runnable.run()
            } finally {
                setOrClear(previous)
            }
        }
    }
}
```

**Usage**:

MdcTaskDecorator automatically propagates MDC context to `@Async` methods.

```kotlin
@Service
class NotificationService {

    @Async  // MDC automatically propagated
    fun sendEmail(to: String, subject: String) {
        logger.info { "Sending email to: $to" }  // APP_TRACE_ID preserved
        emailClient.send(to, subject)
    }
}
```

## Request Lifecycle

### Complete Flow

The following diagram shows the complete request processing flow through all cross-cutting concerns:

```
1. ContentCachingFilter
   → Cache Request/Response Body

2. AppTraceFilter
   → Generate UUID v7, setup MDC, start time measurement

3. LogInterceptor.preHandle()
   → Log request information

4. CheckIpAspect (if @CheckIp)
   → Verify IP address

5. LogTraceAspect - begin
   → Log method entry

6. Controller → Facade → Application → Service
   → Business logic execution

7. LogTraceAspect - end
   → Log method exit (execution time)

8. LogResponseBodyInterceptor (if @LogResponseBody)
   → Log response body

9. AppTraceFilter - finally
   → Log processing time, set response headers, clear MDC

10. ContentCachingFilter - finally
    → Copy response body
```

### Log Output Example

The system produces structured log output at each processing stage:

```
# 1. Request start
2026-02-14 10:30:00.000 [http-nio-8080-exec-1] TRACE AppTraceFilter - REQ START

# 2. Request information
2026-02-14 10:30:00.001 [http-nio-8080-exec-1] INFO LogInterceptor -
# ==> REQUEST INFO ::
ServerIp = 192.168.1.10 , ClientIp = 203.0.113.45
RequestURI = POST /api/users
RequestBody = {"name":"John"}

# 3. Method tracing
2026-02-14 10:30:00.005 [http-nio-8080-exec-1] INFO [01932d8e] |-->UserController.create(..)
2026-02-14 10:30:00.006 [http-nio-8080-exec-1] INFO [01932d8e] |   |-->UserFacade.create(..)
2026-02-14 10:30:00.020 [http-nio-8080-exec-1] INFO [01932d8e] |   |<--UserFacade.create(..) time=14ms
2026-02-14 10:30:00.021 [http-nio-8080-exec-1] INFO [01932d8e] |<--UserController.create(..) time=16ms

# 4. Processing time
2026-02-14 10:30:00.025 [http-nio-8080-exec-1] DEBUG AppTraceFilter - Process time: 25ms

# 5. Request end
2026-02-14 10:30:00.026 [http-nio-8080-exec-1] TRACE AppTraceFilter - REQ END
```

## Configuration Order

The following table shows the execution order of cross-cutting concern components:

| Order | Component | Role |
|-------|-----------|------|
| `HIGHEST_PRECEDENCE` | ContentCachingFilter | Body caching |
| `HIGHEST_PRECEDENCE + 1` | AppTraceFilter | Trace ID, MDC |
| 2 | LogTraceAspect | Method tracing |
| 3 | CheckIpAspect | IP verification |

## Performance Considerations

### Logging Levels

Configure logging levels differently for development and production environments.

**Development**:
```yaml
logging:
  level:
    com.myrealtrip.commonweb.filters: DEBUG
    com.myrealtrip.commonweb.interceptors: INFO
```

**Production**:
```yaml
logging:
  level:
    com.myrealtrip.commonweb.filters: INFO
    com.myrealtrip.commonweb.interceptors: WARN
```

### Exclude Logging

Use annotations to exclude logging for sensitive or high-traffic endpoints.

```kotlin
// Sensitive endpoints
@ExcludeRequestLog
@GetMapping("/users/{id}/password")
fun getPassword()

// Log response body only when needed
@LogResponseBody(logLevel = Level.DEBUG)
@GetMapping("/debug/state")
fun debugState()
```

## Troubleshooting

### MDC Lost in Async

**Problem**: MDC context is not available in asynchronous methods.

```kotlin
// Problem: No MDC in @Async
@Async
fun sendNotification() {
    logger.info { "Send" }  // No APP_TRACE_ID
}
```

**Solution**: The MdcTaskDecorator automatically propagates MDC context when configured in AsyncConfig.

### IP Verification Failed

**Problem**: The system only checks the proxy IP address instead of the real client IP.

```kotlin
// Problem: Only checking proxy IP
val clientIp = request.remoteAddr
```

**Solution**: Use IpAddrUtil which considers X-Forwarded-For headers.

```kotlin
// Solution: Use IpAddrUtil (considers X-Forwarded-For)
val clientIp = IpAddrUtil.getClientIp(request)
```

## Summary

| Layer | Component | Role | Applied At |
|-------|-----------|------|-----------|
| Filter | ContentCachingFilter | Body caching | First |
| Filter | AppTraceFilter | Trace ID, MDC | Second |
| Interceptor | LogInterceptor | Request logging | Before controller |
| Interceptor | LogResponseBodyInterceptor | Response logging | After controller |
| AOP | LogTraceAspect | Method tracing | Around methods |
| AOP | CheckIpAspect | IP verification | Before methods |
| Async | MdcTaskDecorator | MDC propagation | On @Async |

**Best Practices**:
1. Use `@ExcludeRequestLog` for sensitive data endpoints
2. Configure WARN or higher logging levels in production
3. Apply IP verification only to admin APIs
4. Rely on automatic MDC propagation through MdcTaskDecorator
5. Use response body logging for debugging purposes only

## Related Documents

- [Caching Strategy](09-caching-strategy.md)
- [Infrastructure Services](11-infrastructure-services.md)
