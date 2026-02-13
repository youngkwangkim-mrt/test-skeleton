package com.myrealtrip.testsupport

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger {}

/**
 * Base class for external endpoint testing using RestClient.
 *
 * Usage:
 * ```kotlin
 * class ExternalApiTest : EndPointTestSupport("https://api.external.com") {
 *
 *     @Test
 *     fun `should call external API`() {
 *         val response = callGet("/v1/data")
 *         assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
 *     }
 * }
 * ```
 */
abstract class EndPointTestSupport(
    baseUrl: String,
    contentType: String = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
) {

    protected val jsonMapper: JsonMapper = JsonMapper.builder().build()

    protected val client: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, contentType)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    protected fun callGet(uri: String): ResponseEntity<String> =
        client.get()
            .uri(uri)
            .retrieve()
            .toEntity<String>()
            .also { log.info { "==> response = ${it.body}" } }

    protected fun callPost(uri: String, paramMap: MultiValueMap<String, String>): ResponseEntity<String> =
        client.post()
            .uri(uri)
            .body(paramMap)
            .retrieve()
            .toEntity<String>()
            .also { log.info { "==> response = ${it.body}" } }
}
