package com.myrealtrip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import java.util.TimeZone

@SpringBootApplication
@EnableCaching
@EnableAsync
class CommonApiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<CommonApiApplication>(*args)
}
