package com.myrealtrip.common.utils.coroutine

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

private val virtualThreadDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

// =============================================================================
// MDC Context Propagation
// =============================================================================

/**
 * MDC 컨텍스트를 전파하는 runBlocking
 *
 * 지정된 코루틴 컨텍스트에 MDC 컨텍스트를 결합하여 실행합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * // 기본 사용
 * runBlockingWithMDC {
 *     val result1 = asyncWithMDC { fetchData1() }
 *     val result2 = asyncWithMDC { fetchData2() }
 *     result1.await() + result2.await()
 * }
 *
 * // 커스텀 dispatcher 사용
 * runBlockingWithMDC(Dispatchers.IO) {
 *     // IO dispatcher에서 실행
 * }
 * ```
 *
 * @param context 코루틴 컨텍스트 (기본값: EmptyCoroutineContext)
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 */
fun <T> runBlockingWithMDC(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(context + MDCContext(), block)

/**
 * MDC 컨텍스트를 전파하는 async
 *
 * 지정된 코루틴 컨텍스트에 MDC 컨텍스트를 결합하여 실행합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     val result1 = asyncWithMDC { fetchData1() }
 *     val result2 = asyncWithMDC { fetchData2() }
 *     result1.await() + result2.await()
 * }
 * ```
 *
 * @param context 추가 코루틴 컨텍스트 (MDCContext와 결합됨)
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Deferred 결과
 */
fun <T> CoroutineScope.asyncWithMDC(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = async(context + MDCContext(), start, block)

/**
 * MDC 컨텍스트를 전파하는 launch
 *
 * 지정된 코루틴 컨텍스트에 MDC 컨텍스트를 결합하여 실행합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     launchWithMDC { sendNotification() }
 *     launchWithMDC { updateAuditLog() }
 * }
 * ```
 *
 * @param context 추가 코루틴 컨텍스트 (MDCContext와 결합됨)
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Job
 */
fun CoroutineScope.launchWithMDC(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context + MDCContext(), start, block)

// =============================================================================
// Virtual Thread + MDC Context Propagation
// =============================================================================

/**
 * Virtual Thread에서 MDC를 전파하는 runBlocking
 *
 * Virtual Thread Dispatcher를 사용하여 블로킹 I/O 작업을 효율적으로 처리합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingOnVirtualThread {
 *     val result1 = asyncWithMDC { blockingApiCall1() }
 *     val result2 = asyncWithMDC { blockingApiCall2() }
 *     result1.await() + result2.await()
 * }
 * ```
 *
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 */
fun <T> runBlockingOnVirtualThread(
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(virtualThreadDispatcher + MDCContext(), block)

/**
 * Virtual Thread에서 MDC를 전파하는 async
 *
 * Virtual Thread Dispatcher를 사용하여 블로킹 I/O 작업을 효율적으로 처리합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     val result1 = asyncOnVirtualThread { blockingApiCall1() }
 *     val result2 = asyncOnVirtualThread { blockingApiCall2() }
 *     result1.await() + result2.await()
 * }
 * ```
 *
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Deferred 결과
 */
fun <T> CoroutineScope.asyncOnVirtualThread(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = async(virtualThreadDispatcher + MDCContext(), start, block)

/**
 * Virtual Thread에서 MDC를 전파하는 launch
 *
 * Virtual Thread Dispatcher를 사용하여 블로킹 I/O 작업을 효율적으로 처리합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     launchOnVirtualThread { sendNotification() }
 *     launchOnVirtualThread { updateAuditLog() }
 * }
 * ```
 *
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Job
 */
fun CoroutineScope.launchOnVirtualThread(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(virtualThreadDispatcher + MDCContext(), start, block)

// =============================================================================
// IO Dispatcher + MDC Context Propagation
// =============================================================================

/**
 * IO Dispatcher에서 MDC를 전파하는 runBlocking
 *
 * Dispatchers.IO를 사용하여 블로킹 I/O 작업을 처리합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingOnIoThread {
 *     val result1 = asyncWithMDC { readFile() }
 *     val result2 = asyncWithMDC { fetchFromDb() }
 *     result1.await() + result2.await()
 * }
 * ```
 *
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 */
fun <T> runBlockingOnIoThread(
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(Dispatchers.IO + MDCContext(), block)

/**
 * IO Dispatcher에서 MDC를 전파하는 async
 *
 * Dispatchers.IO를 사용하여 블로킹 I/O 작업을 처리합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     val result1 = asyncOnIoThread { readFile1() }
 *     val result2 = asyncOnIoThread { readFile2() }
 *     result1.await() + result2.await()
 * }
 * ```
 *
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Deferred 결과
 */
fun <T> CoroutineScope.asyncOnIoThread(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = async(Dispatchers.IO + MDCContext(), start, block)

/**
 * IO Dispatcher에서 MDC를 전파하는 launch
 *
 * Dispatchers.IO를 사용하여 블로킹 I/O 작업을 처리합니다.
 * 부모 코루틴이 취소되면 이 코루틴도 취소됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * runBlockingWithMDC {
 *     launchOnIoThread { writeToFile() }
 *     launchOnIoThread { flushLogs() }
 * }
 * ```
 *
 * @param start 코루틴 시작 옵션
 * @param block 실행할 suspend 블록
 * @return Job
 */
fun CoroutineScope.launchOnIoThread(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(Dispatchers.IO + MDCContext(), start, block)

// =============================================================================
// Retry
// =============================================================================

/**
 * 실패 시 재시도하는 suspend 함수
 *
 * 예외 발생 시 지정된 횟수만큼 재시도합니다.
 * 지수 백오프(exponential backoff)를 지원합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * // 기본 사용 (3회 시도, 500ms 간격)
 * val result = retry { apiClient.call() }
 *
 * // 지수 백오프 사용
 * val result = retry(
 *     maxAttempts = 5,
 *     delay = 100.milliseconds,
 *     backoffMultiplier = 2.0, // 100ms -> 200ms -> 400ms -> 800ms
 * ) {
 *     apiClient.call()
 * }
 *
 * // 특정 예외만 재시도
 * val result = retry(
 *     retryOn = { it is IOException },
 * ) {
 *     apiClient.call()
 * }
 * ```
 *
 * @param maxAttempts 최대 시도 횟수 (기본값: 3)
 * @param delay 재시도 간격 (기본값: 500ms)
 * @param backoffMultiplier 재시도 간격 배수 (기본값: 1.0, 고정 간격)
 * @param retryOn 재시도 조건 (기본값: 모든 예외)
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 * @throws Throwable 마지막 시도에서 발생한 예외 또는 retryOn 조건에 맞지 않는 예외
 */
suspend fun <T> retry(
    maxAttempts: Int = 3,
    delay: Duration = 500.milliseconds,
    backoffMultiplier: Double = 1.0,
    retryOn: (Throwable) -> Boolean = { true },
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1" }

    var currentDelay = delay
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            if (attempt == maxAttempts - 1 || !retryOn(e)) throw e

            logger.warn {
                "Retry ${attempt + 1}/$maxAttempts failed, retrying in ${currentDelay.inWholeMilliseconds}ms: ${e.message}"
            }
            delay(currentDelay)
            currentDelay *= backoffMultiplier
        }
    }

    error("Unreachable")
}

/**
 * 실패 시 재시도하는 블로킹 함수
 *
 * [retry]의 블로킹 버전입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val result = retryBlocking(maxAttempts = 3) {
 *     apiClient.call()
 * }
 * ```
 *
 * @param maxAttempts 최대 시도 횟수 (기본값: 3)
 * @param delay 재시도 간격 (기본값: 500ms)
 * @param backoffMultiplier 재시도 간격 배수 (기본값: 1.0, 고정 간격)
 * @param retryOn 재시도 조건 (기본값: 모든 예외)
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 * @see retry
 */
fun <T> retryBlocking(
    maxAttempts: Int = 3,
    delay: Duration = 500.milliseconds,
    backoffMultiplier: Double = 1.0,
    retryOn: (Throwable) -> Boolean = { true },
    block: suspend () -> T,
): T = runBlockingWithMDC {
    retry(maxAttempts, delay, backoffMultiplier, retryOn, block)
}

// =============================================================================
// Utilities
// =============================================================================

/**
 * 로깅이 포함된 coroutineScope
 *
 * 시작과 종료 시점에 스레드 ID와 함께 로그를 남깁니다.
 * 디버깅 목적으로 사용합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * withLogging("fetchUserData") {
 *     val user = asyncWithMDC { userClient.fetch() }
 *     val orders = asyncWithMDC { orderClient.fetch() }
 *     UserWithOrders(user.await(), orders.await())
 * }
 * ```
 *
 * @param title 로그에 표시할 제목
 * @param block 실행할 suspend 블록
 * @return 블록의 실행 결과
 */
suspend fun <R> withLogging(
    title: String,
    block: suspend CoroutineScope.() -> R,
): R = coroutineScope {
    val startThreadId = Thread.currentThread().threadId()
    logger.debug { "# >>> $title, start thread: $startThreadId" }
    try {
        block()
    } finally {
        val endThreadId = Thread.currentThread().threadId()
        logger.debug { "# <<< $title, end thread: $endThreadId" }
    }
}
