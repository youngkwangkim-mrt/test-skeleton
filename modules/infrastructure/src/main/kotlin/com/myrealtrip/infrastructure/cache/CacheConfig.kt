package com.myrealtrip.infrastructure.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import com.github.benmanes.caffeine.cache.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.redisson.spring.cache.RedissonSpringCacheManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration
import org.redisson.spring.cache.CacheConfig as RedissonCacheConfig

private val logger = KotlinLogging.logger {}

@Configuration
class CacheConfig {

    @Bean
    @Primary
    fun caffeineCacheManager(): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .maximumSize(200)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .evictionListener(RemovalListener { key, _, cause ->
                        logger.debug { "# ==> Key $key was evicted ($cause)" }
                    })
                    .recordStats()
                    .scheduler(Scheduler.systemScheduler())
            )
        }
    }

    @Bean
    @Qualifier("redisCacheManager")
    fun redisCacheManager(redissonClient: RedissonClient): CacheManager {
        // CacheConfig(ttl, maxIdleTime) - in milliseconds
        val config = hashMapOf(
            DEFAULT to RedissonCacheConfig(
                Duration.ofMinutes(30).toMillis(),  // ttl
                Duration.ofMinutes(10).toMillis()   // maxIdleTime
            ),
            SHORT_LIVED to RedissonCacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(5).toMillis()
            ),
            MID_LIVED to RedissonCacheConfig(
                Duration.ofHours(1).toMillis(),
                Duration.ofMinutes(20).toMillis()
            ),
            LONG_LIVED to RedissonCacheConfig(
                Duration.ofHours(24).toMillis(),
                Duration.ofHours(4).toMillis()
            ),
        )

        return RedissonSpringCacheManager(redissonClient, config)
    }

    companion object {
        private const val PREFIX = "app-cache-"
        const val DEFAULT = "${PREFIX}default"
        const val SHORT_LIVED = "${PREFIX}shortLived"
        const val MID_LIVED = "${PREFIX}midLived"
        const val LONG_LIVED = "${PREFIX}longLived"
    }
}
