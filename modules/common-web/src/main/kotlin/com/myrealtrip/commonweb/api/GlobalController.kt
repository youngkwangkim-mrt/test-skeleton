package com.myrealtrip.commonweb.api

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.annoatations.LogResponseBody
import com.myrealtrip.common.utils.extensions.extractLocalDateTime
import com.myrealtrip.commonweb.response.resource.ApiResource
import com.myrealtrip.commonweb.utils.EnvironmentUtil
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.System.Logger.Level
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/_global")
class GlobalController(
    private val environmentUtil: EnvironmentUtil
) {

    @RequestMapping("/health", method = [RequestMethod.GET, RequestMethod.POST])
    fun health(): String = "UP"

    @GetMapping("/profiles")
    fun profiles(): String = environmentUtil.activeProfiles()
        .joinToString(separator = ", ") { it }
        .ifEmpty { "No active profiles" }
        .also { logger.info { "Active profiles: $it" } }

    @GetMapping("/app-trace-id/{appTraceId}")
    @LogResponseBody(logLevel = Level.DEBUG)
    @OptIn(ExperimentalUuidApi::class)
    fun parseAppTraceId(@PathVariable appTraceId: String): ResponseEntity<ApiResource<Any>> {
        val uuid = Uuid.parse(appTraceId)
        val generatedAt = uuid.extractLocalDateTime()

        val result = mapOf(
            "appTraceId" to appTraceId,
            "generatedAt" to generatedAt
        )
        logger.debug { "==> parseAppTraceId result = $result " }
        return ApiResource.of(result)
    }

    init {
        logger.info { "# ==> ${this.javaClass.simpleName} initialized" }
    }
}