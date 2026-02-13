package com.myrealtrip.infrastructure.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

class HttpLoggingInterceptor(
    private val clientName: String = DEFAULT_CLIENT_NAME,
    private val level: Level = Level.SIMPLE
) : ClientHttpRequestInterceptor {

    enum class Level { SIMPLE, FULL }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val tag = buildTag(request)

        fun log(message: String) = logger.debug { "$tag $message" }

        // Request
        log("$REQUEST_PREFIX ${request.method} ${request.uri}")
        if (level == Level.FULL) {
            request.headers.log(::log)
            if (body.isNotEmpty()) log(body.decodeToString())
        }
        log("$REQUEST_PREFIX END (${body.size}-byte body)")

        // Execute
        val startTime = System.currentTimeMillis()
        val response = BufferingClientHttpResponseWrapper(execution.execute(request, body))
        val elapsedTime = System.currentTimeMillis() - startTime

        // Response
        log("$RESPONSE_PREFIX ${response.statusCode} (${elapsedTime}ms)")
        val responseBody = response.bodyAsString
        if (level == Level.FULL) {
            response.headers.log(::log)
            if (responseBody.isNotEmpty()) log(responseBody)
        }
        log("$RESPONSE_PREFIX END (${responseBody.length}-byte body)")

        return response
    }

    private fun buildTag(request: HttpRequest): String {
        val methodName = HttpExchangeMethodContext.get() ?: inferMethodName(request)
        return "[$clientName#$methodName]"
    }

    private fun inferMethodName(request: HttpRequest): String {
        return request.method.name().lowercase()
    }

    private fun HttpHeaders.log(log: (String) -> Unit) {
        forEach { name, values -> values.forEach { log("$name: $it") } }
    }

    companion object {
        private const val DEFAULT_CLIENT_NAME = "RestClient"
        private const val REQUEST_PREFIX = "--->"
        private const val RESPONSE_PREFIX = "<---"
    }
}

private class BufferingClientHttpResponseWrapper(
    private val delegate: ClientHttpResponse
) : ClientHttpResponse by delegate {

    private val cachedBody: ByteArray by lazy { delegate.body.readAllBytes() }

    val bodyAsString: String get() = cachedBody.decodeToString()

    override fun getBody() = cachedBody.inputStream()
}

private fun ByteArray.decodeToString() = String(this, StandardCharsets.UTF_8)
