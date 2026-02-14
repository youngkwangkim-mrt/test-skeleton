---
title: "Caching Strategy"
description: "Two-tier cache architecture (L1 Caffeine + L2 Redis) and cache management strategy"
category: "architecture"
order: 9
last_updated: "2026-02-14"
---

# Caching Strategy

## Overview

This document describes the two-tier cache architecture that combines local in-memory caching with distributed caching. The architecture provides both high performance through L1 Caffeine cache and scalability through L2 Redis cache.

## Architecture

The system implements a two-tier cache hierarchy:

- **L1 Cache (Caffeine)**: Provides ultra-fast local in-memory lookups
- **L2 Cache (Redis)**: Enables data sharing across multiple application instances

```
Request → L1 (Caffeine) → L2 (Redis) → Database
            ↓ hit          ↓ hit        ↓ miss
         Response       Response     Response + Cache
```

## L1 Cache: Caffeine

### Configuration

The Caffeine cache manager configures a local in-memory cache with automatic eviction policies.

**Location**: `modules/infrastructure/src/main/kotlin/com/myrealtrip/infrastructure/cache/CacheConfig.kt`

```kotlin
@Bean
@Primary
fun caffeineCacheManager(): CacheManager {
    return CaffeineCacheManager().apply {
        setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(200)                           // Max 200 entries
                .expireAfterWrite(Duration.ofMinutes(30))   // 30min TTL
                .expireAfterAccess(Duration.ofMinutes(10))  // 10min idle
                .recordStats()
                .scheduler(Scheduler.systemScheduler())
        )
    }
}
```

### Usage

Apply the `@Cacheable` annotation to cache method results in the local cache.

```kotlin
@Service
@Transactional(readOnly = true)
class UserQueryApplication(
    private val userService: UserService,
) {
    @Cacheable("app-cache-default")
    fun findById(id: Long): UserInfo = userService.findById(id)
}
```

## L2 Cache: Redis

### Cache Names

The system defines four cache tiers with different Time-To-Live (TTL) and MaxIdle values.

| Cache Name | TTL | MaxIdle | Use Case |
|-----------|-----|---------|---------|
| `app-cache-default` | 30min | 10min | Default queries |
| `app-cache-shortLived` | 10min | 5min | Real-time data |
| `app-cache-midLived` | 1hour | 20min | Semi-static data |
| `app-cache-longLived` | 24hour | 4hour | Code tables |

### Redis Utilities

#### redisGet: Retrieve or Compute

The `redisGet` function retrieves cached values or computes and caches new values atomically.

```kotlin
fun <T> redisGet(
    key: String,
    ttl: Duration = Duration.ofMinutes(5),
    bypass: Boolean = false,
    force: Boolean = false,
    cacheNullOrEmpty: Boolean = false,
    function: () -> T
): T
```

**Usage Examples**:

```kotlin
// Basic usage
val users = redisGet("users:all") {
    userRepository.findAll()
}

// Custom TTL
val flight = redisGet(key = "flight:${id}", ttl = Duration.ofHours(1)) {
    flightService.findById(id)
}

// Cache null/empty results (prevent repeated DB queries)
val result = redisGet(
    key = "search:${query}",
    cacheNullOrEmpty = true,
    ttl = Duration.ofMinutes(1)
) {
    searchService.search(query)
}
```

**Operation Flow**:

1. When `bypass` is true, the function ignores the cache
2. When `force` is true, the function forces cache refresh
3. On cache hit, the function returns the Redis value
4. On cache miss, the function executes the provided function and saves the result asynchronously
5. On Redis failure, the function returns the computed result only (graceful degradation)

#### redisSetAsync: Asynchronous Save

The `redisSetAsync` function computes a value and saves it to Redis asynchronously.

```kotlin
val report = redisSetAsync(
    key = "report:${month}",
    ttl = Duration.ofHours(24)
) {
    reportService.generate(month)
}
```

#### redisUnlinkAsync: Asynchronous Delete

The `redisUnlinkAsync` function removes a single key from Redis without blocking.

```kotlin
userService.deleteUser(userId)
redisUnlinkAsync("user:${userId}")
```

#### redisUnlinkByPatternAsync: Pattern-based Delete

The `redisUnlinkByPatternAsync` function removes all keys matching a pattern.

```kotlin
// Remove all user sessions
redisUnlinkByPatternAsync("session:user:*")

// Remove specific airline flight cache
redisUnlinkByPatternAsync("flight:${airlineCode}:*")
```

## Redis Distributed Lock

### redisLock

The `redisLock` function acquires a distributed lock before executing critical sections.

```kotlin
fun <T> redisLock(
    key: String,
    waitTime: Long = 3,
    leaseTime: Long = 5,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    function: () -> T
): T
```

**Usage Examples**:

```kotlin
// Basic usage
val result = redisLock("order:process:${orderId}") {
    orderService.processOrder(orderId)
}

// Inventory update (concurrency control)
redisLock("inventory:${productId}") {
    val stock = inventoryService.getStock(productId)
    inventoryService.updateStock(productId, stock - quantity)
}

// Handle lock acquisition failure
try {
    redisLock("resource:${id}", waitTime = 1) {
        // critical section
    }
} catch (e: IllegalStateException) {
    logger.warn { "Resource is busy" }
}
```

## Cache Key Naming

### Structure

Follow this structure when naming cache keys:

```
{prefix}:{category}:{identifier}[:{sub-category}]
```

The system automatically adds the `glory:app:` prefix to all keys.

```kotlin
// Input: "user:123"
// Actual: "glory:app:user:123"
```

**Examples**:

```kotlin
"glory:app:user:${userId}"              // Single entity
"glory:app:users:all"                   // Collection
"glory:app:search:flight:${query}"      // Search results
"glory:app:stats:daily:${date}"         // Statistics
```

## Cache Strategy Selection

### L1 Cache (Caffeine)

**Use L1 cache when**:
- The application runs in a single instance environment
- Ultra-fast lookup performance is required
- Inter-instance synchronization is not needed

**Advantages**: Nanosecond-level lookup, no network overhead

**Disadvantages**: Inter-instance inconsistency, memory limit of 200 entries

### L2 Cache (Redis)

**Use L2 cache when**:
- The application runs in a multi-instance environment
- Data sharing across instances is required
- Large data volumes need caching

**Advantages**: Consistency maintained, minimal capacity constraints

**Disadvantages**: Network latency in milliseconds

### Two-tier Cache

**Use two-tier cache when**:
- Both maximum performance and consistency are required
- Shared data experiences very high read frequency

**Advantages**: Best performance on L1 hit, consistency via L2

**Disadvantages**: Increased complexity, complex invalidation logic

## Cache Invalidation

### Single Key

Invalidate a single cache entry using `@CacheEvict` and `redisUnlinkAsync`.

```kotlin
@CacheEvict("app-cache-default", key = "#id")
fun update(id: Long, request: UpdateRequest): Info {
    val updated = service.update(id, request)
    redisUnlinkAsync("resource:${id}")
    return updated
}
```

### Pattern Matching

Invalidate multiple cache entries matching a pattern.

```kotlin
@CacheEvict("app-cache-default", allEntries = true)
fun deleteAll() {
    service.deleteAll()
    redisUnlinkByPatternAsync("resources:*")
}
```

### Time-based Auto Invalidation

Configure automatic cache expiration using TTL.

```kotlin
val data = redisGet(
    key = "stats:hourly:${hour}",
    ttl = Duration.ofHours(1)  // Auto-delete after 1 hour
) {
    statsService.calculate(hour)
}
```

## Performance Optimization

### 1. Set Appropriate TTL

Match the TTL to the data's update frequency.

```kotlin
// TTL matching data characteristics
redisGet("realtime-data", ttl = Duration.ofMinutes(1))
redisGet("static-config", ttl = Duration.ofHours(24))
```

### 2. Cache Null and Empty Results

Cache empty results to prevent repeated database queries.

```kotlin
// Cache empty results to reduce DB load
redisGet(
    key = "search:${query}",
    cacheNullOrEmpty = true,
    ttl = Duration.ofMinutes(1)
) {
    searchService.search(query)
}
```

### 3. Minimize Distributed Locks

Use distributed locks only for write operations requiring concurrency control.

```kotlin
// Use locks only for write operations
redisLock("user:update:${id}") {
    userRepository.updateBalance(id, amount)
}
```

### 4. Warm Up Cache

Pre-populate frequently accessed data during application startup.

```kotlin
@EventListener(ApplicationReadyEvent::class)
fun warmUpCache() {
    val activeUsers = userService.findAllActive()
    activeUsers.forEach { user ->
        redisSetAsync("user:${user.id}") { user }
    }
}
```

## Monitoring

### Caffeine Statistics

Retrieve and log cache statistics to monitor cache effectiveness.

```kotlin
val cache = cacheManager.getCache("app-cache-default")
val nativeCache = cache.nativeCache as Cache<Any, Any>
val stats = nativeCache.stats()

logger.info {
    """
    Hit Rate: ${stats.hitRate()}
    Miss Rate: ${stats.missRate()}
    Eviction Count: ${stats.evictionCount()}
    """.trimIndent()
}
```

## Troubleshooting

### Cache Inconsistency

**Problem**: Redis deletion is missing after cache eviction.

```kotlin
// Problem: Missing Redis deletion
@CacheEvict("app-cache-default", key = "#id")
fun update(id: Long): Info {
    val updated = service.update(id)
    // Redis deletion missing!
    return updated
}
```

**Solution**: Invalidate all cache layers.

```kotlin
// Solution: Invalidate all layers
@CacheEvict("app-cache-default", key = "#id")
fun update(id: Long): Info {
    val updated = service.update(id)
    redisUnlinkAsync("resource:${id}")  // Delete from Redis too
    return updated
}
```

### Redis Connection Failure

The `redisGet` function provides graceful degradation when Redis fails.

```kotlin
// redisGet provides graceful degradation on Redis failure
val data = redisGet("key") {
    database.query()  // Query DB directly on Redis failure
}
```

## Summary

| Layer | Use Case | TTL | Advantages | Disadvantages |
|-------|---------|-----|-----------|---------------|
| **L1 (Caffeine)** | Local high-speed | 10~30min | Ultra-fast | Inter-instance inconsistency |
| **L2 (Redis)** | Distributed sharing | 10min~24hour | Consistency | Network latency |
| **Distributed Lock** | Concurrency control | 3~30sec | Data integrity | Performance overhead |

**Best Practices**:
1. Use L1 Caffeine for high read frequency data
2. Use L2 Redis when inter-instance sharing is needed
3. Use distributed locks for concurrent writes
4. Cache null and empty results to reduce database load
5. Optimize memory with appropriate TTL settings

## Related Documents

- [Cross-Cutting Concerns](10-cross-cutting-concerns.md)
- [Infrastructure Services](11-infrastructure-services.md)
