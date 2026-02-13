package com.myrealtrip.infrastructure.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.infrastructure.redis.RedisConstants.REDIS_PREFIX
import com.myrealtrip.infrastructure.redis.RedisConstants.SEPARATOR
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private const val DEFAULT_WAIT_SECONDS = 3L
private const val DEFAULT_LEASE_SECONDS = 5L
private val DEFAULT_TIME_UNIT = TimeUnit.SECONDS

private val logger = KotlinLogging.logger {}

@Component
class RedisLockAspect(
    private val _redissonClient: RedissonClient
) {

    init {
        RedisLockAspect.redissonClient = _redissonClient
    }

    companion object {
        lateinit var redissonClient: RedissonClient
            private set
    }

}

/**
 * Executes a function within a distributed Redis lock.
 *
 * This provides mutual exclusion across multiple application instances,
 * ensuring that only one instance can execute the protected code at a time.
 *
 * The lock is automatically released after function execution or when the lease time expires,
 * whichever comes first. This prevents deadlocks in case of application failures.
 *
 * @param T The return type of the function
 * @param key The lock key suffix (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 * @param waitTime Maximum time to wait for lock acquisition (default: [DEFAULT_WAIT_SECONDS])
 * @param leaseTime Maximum time to hold the lock (default: [DEFAULT_LEASE_SECONDS])
 * @param timeUnit Time unit for waitTime and leaseTime (default: [TimeUnit.SECONDS])
 * @param function The function to execute while holding the lock
 * @return The result of the function execution
 * @throws IllegalStateException if the lock cannot be acquired within the wait time
 *
 * @sample
 * ```kotlin
 * // Simple lock with default 3s wait, 5s lease
 * val result = redisLock("order:process:${orderId}") {
 *     orderService.processOrder(orderId)
 * }
 *
 * // Custom wait and lease times
 * val payment = redisLock(
 *     key = "payment:${paymentId}",
 *     waitTime = 10,
 *     leaseTime = 30,
 *     timeUnit = TimeUnit.SECONDS
 * ) {
 *     paymentService.processPayment(paymentId)
 * }
 *
 * // Inventory update with lock
 * redisLock("inventory:${productId}") {
 *     val current = inventoryService.getStock(productId)
 *     inventoryService.updateStock(productId, current - quantity)
 * }
 *
 * // Handle lock acquisition failure
 * try {
 *     redisLock("resource:${id}", waitTime = 1) {
 *         // critical section
 *     }
 * } catch (e: IllegalStateException) {
 *     logger.warn { "Resource is busy, try again later" }
 * }
 * ```
 *
 * @see org.redisson.api.RLock for underlying Redisson lock implementation
 */
fun <T> redisLock(
    key: String,
    waitTime: Long = DEFAULT_WAIT_SECONDS,
    leaseTime: Long = DEFAULT_LEASE_SECONDS,
    timeUnit: TimeUnit = DEFAULT_TIME_UNIT,
    function: () -> T
): T {

    val lockKey = "$REDIS_PREFIX$SEPARATOR$key"
    val lock = RedisLockAspect.redissonClient.getLock(lockKey)

    try {
        val acquired = lock.tryLock(waitTime, leaseTime, timeUnit)

        if (!acquired) {
            throw IllegalStateException("Could not acquire redis lock for key: $lockKey within $waitTime $timeUnit")
        }
        logger.debug { "==> redis lock acquired for: $lockKey" }

        return function.invoke()

    } finally {
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
            logger.debug { "==> redis lock released for key: $lockKey" }
        }
    }

}