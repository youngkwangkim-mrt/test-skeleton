package com.myrealtrip.testsupport

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.slf4j.MDC
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base class for integration tests using `embed` profile (embedded database).
 *
 * These tests run first in the execution order for fastest feedback.
 */
@SpringBootTest
@ActiveProfiles("embed")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class IntegratedEmbedTestSupport {

    protected val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @OptIn(ExperimentalUuidApi::class)
    @BeforeEach
    fun setUpMdc() {
        MDC.put("X-ZZ-TraceId", Uuid.generateV7().toString())
    }

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }
}
