package com.myrealtrip.skeletonapiapp._test

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.infrastructure.cache.CacheConfig.Companion.DEFAULT
import com.myrealtrip.infrastructure.redis.redisGet
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class TestService {

    @Cacheable("testCache")
    fun localCacheTest(key: String): String {
        logger.info { "did not hit local cache" }
        return "localCacheTest"
    }

    @Cacheable(
        cacheManager = "redisCacheManager",
        cacheNames = [DEFAULT],
        key = "#key"
    )
    fun redisCacheableTest(key: String): String {
        logger.info { "did not hit cache" }
        return "redisCacheTest"
    }

    fun redissonCacheTest(key: String): String = redisGet(
        key = key
    ) {
        logger.info { "did not hit redisson cache" }
        "redissonCacheTest"
    }

    @Async
    fun asyncTest() {
        Thread.sleep(500)
        logger.info { "async test executed" }
    }
}