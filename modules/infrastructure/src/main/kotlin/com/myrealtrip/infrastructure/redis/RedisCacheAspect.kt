package com.myrealtrip.infrastructure.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.infrastructure.redis.RedisCacheAspect.Companion.redissonClient
import com.myrealtrip.infrastructure.redis.RedisConstants.REDIS_PREFIX
import com.myrealtrip.infrastructure.redis.RedisConstants.SEPARATOR
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration

private val DEFAULT_TTL_MINUTES = Duration.ofMinutes(5L)

private val logger = KotlinLogging.logger {}

@Component
class RedisCacheAspect(
    private val _redissonClient: RedissonClient
) {

    init {
        RedisCacheAspect.redissonClient = _redissonClient
    }

    companion object {
        lateinit var redissonClient: RedissonClient
            private set
    }

}

/**
 * Retrieves a value from Redis cache or computes and caches it if not present.
 *
 * Special behaviors:
 * - By default, null values and empty collections are not cached to avoid cache pollution
 * - Set [cacheNullOrEmpty] to true to cache null and empty collections (useful for preventing repeated DB queries)
 * - Supports bypass mode for cache-disabled scenarios
 * - Supports force mode to refresh cache unconditionally
 * - Handles failures gracefully by falling back to direct function execution
 *
 * @param T The type of the cached value
 * @param key The cache key suffix (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 * @param ttl Time-to-live for the cached value (default: [DEFAULT_TTL_MINUTES])
 * @param bypass If true, skips cache and directly executes the function
 * @param force If true, refreshes cache by executing function and storing result
 * @param cacheNullOrEmpty If true, caches null values and empty collections (default: false)
 * @param function The function to execute if cache miss occurs
 * @return The cached value or the result of the function execution
 *
 * @sample
 * ```kotlin
 * // Simple cache with default 5-minute TTL
 * val users = redisGet("users:all") {
 *     userRepository.findAll()
 * }
 *
 * // Custom TTL of 1 hour
 * val flight = redisGet(
 *     key = "flight:${flightId}",
 *     ttl = Duration.ofHours(1)
 * ) {
 *     flightService.findById(flightId)
 * }
 *
 * // Force refresh cache
 * val freshData = redisGet(
 *     key = "stats:daily",
 *     force = true
 * ) {
 *     statsService.calculateDailyStats()
 * }
 *
 * // Bypass cache for testing
 * val directResult = redisGet(
 *     key = "data:test",
 *     bypass = true
 * ) {
 *     dataService.getData()
 * }
 *
 * // Cache null or empty results to prevent repeated DB queries
 * val maybeEmpty = redisGet(
 *     key = "search:${query}",
 *     cacheNullOrEmpty = true,
 *     ttl = Duration.ofMinutes(1)
 * ) {
 *     searchService.search(query)
 * }
 * ```
 */
fun <T> redisGet(
    key: String,
    ttl: Duration = DEFAULT_TTL_MINUTES,
    bypass: Boolean = false,
    force: Boolean = false,
    cacheNullOrEmpty: Boolean = false,
    function: () -> T
): T {
    if (bypass) {
        logger.debug { "==> redisGet bypass" }
        return function.invoke()
    }

    if (force) {
        logger.debug { "==> redisGet force" }
        return redisSetAsync(key, ttl, cacheNullOrEmpty, function)
    }

    val cacheKey = buildCacheKey(key)
    return runCatching {
        val cachedValue = redissonClient.getBucket<T>(cacheKey).get()

        // Cache hit - return if valid
        if (cachedValue != null) {
            if (!cacheNullOrEmpty && cachedValue is Collection<*> && cachedValue.isEmpty()) {
                logger.debug { "==> redisGet empty collection found, re-fetching: $cacheKey" }
            } else {
                logger.debug { "==> redisGet hit: $cacheKey" }
                return@runCatching cachedValue
            }
        }

        // Cache miss - delegate to redisSetAsync
        redisSetAsync(key, ttl, cacheNullOrEmpty, function)

    }.getOrElse { ex ->
        logger.warn(ex) { "==> redisGet failed for: $cacheKey" }
        function.invoke()
    }
}

/**
 * Executes a function and asynchronously stores its result in Redis cache.
 *
 * By default, null values and empty collections are not cached to maintain cache quality.
 * Set [cacheNullOrEmpty] to true to cache these values.
 * If the cache operation fails, the function still returns the computed result successfully.
 *
 * @param T The type of the value to cache
 * @param key The cache key suffix (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 * @param ttl Time-to-live for the cached value (default: [DEFAULT_TTL_MINUTES])
 * @param cacheNullOrEmpty If true, caches null values and empty collections (default: false)
 * @param function The function to execute and cache the result
 * @return The result of the function execution
 *
 * @sample
 * ```kotlin
 * // Compute and cache asynchronously
 * val report = redisSetAsync(
 *     key = "report:monthly:${month}",
 *     ttl = Duration.ofHours(24)
 * ) {
 *     reportService.generateMonthlyReport(month)
 * }
 *
 * // Update cache after data modification
 * val updatedUser = redisSetAsync("user:${userId}") {
 *     userService.updateUser(userId, userData)
 * }
 *
 * // Cache even if result is null or empty
 * val searchResult = redisSetAsync(
 *     key = "search:${query}",
 *     cacheNullOrEmpty = true
 * ) {
 *     searchService.search(query)
 * }
 * ```
 *
 * @see redisGet for cache retrieval with automatic caching
 */
fun <T> redisSetAsync(
    key: String,
    ttl: Duration = DEFAULT_TTL_MINUTES,
    cacheNullOrEmpty: Boolean = false,
    function: () -> T
): T {
    val result = function.invoke()

    if (!cacheNullOrEmpty) {
        if (result == null) {
            return result
        }
        if (result is Collection<*> && result.isEmpty()) {
            return result
        }
    }

    val cacheKey = buildCacheKey(key)
    return runCatching {
        val bucket = redissonClient.getBucket<T>(cacheKey)
        val rFuture = bucket.setAsync(result, ttl)
        rFuture.thenAccept {
            logger.debug { "==> redisSetAsync: $cacheKey" }
        }
        result

    }.getOrElse { ex ->
        logger.warn(ex) { "redisSetAsync failed for: $cacheKey" }
        result
    }
}

/**
 * Asynchronously removes a key from Redis cache using the unlink command.
 *
 * @param key The cache key suffix to remove (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 *
 * @sample
 * ```kotlin
 * // Remove user from cache after deletion
 * userService.deleteUser(userId)
 * redisUnlinkAsync("user:${userId}")
 *
 * // Invalidate cache after update
 * flightService.updateFlight(flightId)
 * redisUnlinkAsync("flight:${flightId}")
 * ```
 *
 * @see redisDelete for synchronous deletion with result confirmation
 */
fun redisUnlinkAsync(key: String, skipPrefix: Boolean = false) {
    val cacheKey = if (skipPrefix) key else buildCacheKey(key)

    try {
        val rFuture = redissonClient.getBucket<Any>(cacheKey).unlinkAsync()
        rFuture.thenAccept { deleted ->
            if (deleted) {
                logger.debug { "==> redisUnlinkAsync: $cacheKey" }
            } else {
                logger.debug { "==> redisUnlinkAsync not found: $cacheKey" }
            }
        }
    } catch (ex: Exception) {
        logger.warn(ex) { "redisUnlinkAsync failed for: $cacheKey" }
    }
}

/**
 * Synchronously deletes a key from Redis cache and returns the operation result.
 *
 * @param key The cache key suffix to delete (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 * @return true if the key was deleted, false if the key didn't exist or deletion failed
 *
 * @sample
 * ```kotlin
 * // Delete with confirmation
 * val wasDeleted = redisDelete("session:${sessionId}")
 * if (wasDeleted) {
 *     logger.info { "Session cache cleared for: $sessionId" }
 * }
 *
 * // Conditional logic based on deletion
 * if (!redisDelete("temp:data:${id}")) {
 *     logger.warn { "Temporary data was already removed or never existed" }
 * }
 * ```
 *
 * @see redisUnlinkAsync for asynchronous deletion without waiting
 */
fun redisDelete(key: String, skipPrefix: Boolean = false): Boolean {
    val cacheKey = if (skipPrefix) key else buildCacheKey(key)

    return try {
        val deleted = redissonClient.getBucket<Any>(cacheKey).delete()
        if (deleted) {
            logger.debug { "==> redisDelete: $cacheKey" }
        } else {
            logger.debug { "==> redisDelete not found: $cacheKey" }
        }
        deleted
    } catch (ex: Exception) {
        logger.warn(ex) { "redisDelete failed for: $cacheKey" }
        false
    }
}

/**
 * Asynchronously removes all keys matching a pattern from Redis cache.
 *
 * WARNING: Be careful with patterns as they can potentially delete many keys.
 * Always test patterns in a non-production environment first.
 *
 * @param pattern The pattern to match keys (will be prefixed with "[REDIS_PREFIX]+[SEPARATOR]")
 *                Supports Redis glob patterns: * (any chars), ? (single char), [...] (char set)
 *
 * @sample
 * ```kotlin
 * // Remove all user sessions
 * redisUnlinkByPatternAsync("session:user:*")
 *
 * // Remove all cached flights for a specific airline
 * redisUnlinkByPatternAsync("flight:${airlineCode}:*")
 *
 * // Clear all temporary data with specific prefix
 * redisUnlinkByPatternAsync("temp:import:2024-*")
 *
 * // Remove all cached search results
 * redisUnlinkByPatternAsync("search:flight:*:results")
 * ```
 *
 * @see redisDelete for single key deletion
 * @see redisUnlinkAsync for single key async deletion
 */
fun redisUnlinkByPatternAsync(pattern: String, skipPrefix: Boolean = false) {
    val fullPattern = if (skipPrefix) pattern else buildCacheKey(pattern)

    try {
        val rFuture = redissonClient.keys.unlinkByPatternAsync(fullPattern)
        rFuture.thenAccept { deletedCount ->
            logger.debug { "==> redisUnlinkByPatternAsync: $deletedCount keys matching pattern: $fullPattern" }
        }
    } catch (ex: Exception) {
        logger.warn(ex) { "redisUnlinkByPatternAsync failed for: $fullPattern" }
    }
}

/**
 * Builds a standardized cache key by adding the application prefix and converting to lowercase.
 *
 * All cache keys in the system are prefixed with "[REDIS_PREFIX]+[SEPARATOR]" to namespace them and avoid
 * conflicts with other applications using the same Redis instance. Keys are also converted
 * to lowercase for consistency.
 *
 * @param key The raw cache key suffix
 * @return The fully qualified cache key in format "[REDIS_PREFIX]+[SEPARATOR]+key" (lowercase)
 */
private fun buildCacheKey(key: String): String = "$REDIS_PREFIX$SEPARATOR$key".lowercase()
