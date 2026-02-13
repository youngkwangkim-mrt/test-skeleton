package com.myrealtrip.skeletonapiapp._test

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.commonweb.response.resource.ApiResource
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val logger = KotlinLogging.logger {}

@Profile("local")
@RestController
@RequestMapping("/_test")
class TestController(
    private val testService: TestService,
    private val restClient: RestClient,
) {

    @GetMapping("/caches/local")
    fun localCacheTest(): ResponseEntity<ApiResource<String>> {
        testService.localCacheTest("local-cache-annotation-test")
        return ApiResource.success()
    }

    @GetMapping("/caches/redis")
    fun redisCacheableTest(): ResponseEntity<ApiResource<String>> {
        testService.redisCacheableTest("redis-cache-annotation-test")
        return ApiResource.success()
    }

    @GetMapping("/caches/redisson")
    fun redissonCacheTest(): ResponseEntity<ApiResource<String>> {
        testService.redissonCacheTest("redisson-cache-test")
        return ApiResource.success()
    }

    @GetMapping("/async")
    fun asyncTest(): ResponseEntity<ApiResource<String>> {
        testService.asyncTest()
        return ApiResource.success()
    }

    @GetMapping("/external")
    fun external(): ResponseEntity<ApiResource<String>> {
        val response = restClient.get()
            .uri("http://localhost:8081")
            .retrieve()
            .body<String>()!!

        return ApiResource.success(response)
    }

}