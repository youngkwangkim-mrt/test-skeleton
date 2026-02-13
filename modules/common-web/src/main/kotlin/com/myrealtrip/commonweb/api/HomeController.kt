package com.myrealtrip.commonweb.api

import com.myrealtrip.common.utils.extensions.kst
import com.myrealtrip.commonweb.utils.EnvironmentUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@RestController
class HomeController(
    private val environmentUtil: EnvironmentUtil,
    @param:Value("\${spring.application.name:}")
    private var name: String,
    @param:Value("\${spring.application.version:0.0.0}")
    private var version: String
) {

    @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
    fun home() =
        "application name = $name , version = $version , profile = ${environmentUtil.activeProfile()}, now = ${LocalDateTime.now().kst()}"

    init {
        logger.info { "# ==> ${this.javaClass.simpleName} initialized" }
    }
}