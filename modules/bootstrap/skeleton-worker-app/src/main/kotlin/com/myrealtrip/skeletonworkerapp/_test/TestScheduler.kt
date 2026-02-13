package com.myrealtrip.skeletonworkerapp._test

import com.myrealtrip.common.annoatations.LogTrace
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("local")
@Component
class TestScheduler(
    private val testService: TestService
) {
    
//    @Scheduled(fixedDelay = 5_000)
    @LogTrace
    fun every5Seconds() {
        testService.redissonTest()
    }
    
//    @Scheduled(fixedDelay = 10_000)
    @LogTrace
    fun every10Seconds() {
        testService.asyncTest()
    }
}