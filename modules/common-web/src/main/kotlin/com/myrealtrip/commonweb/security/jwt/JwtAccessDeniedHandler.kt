package com.myrealtrip.commonweb.security.jwt

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.commonweb.response.resource.ApiResource
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

@Component
class JwtAccessDeniedHandler(
    private val jsonMapper: JsonMapper,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        logger.debug { "Access denied: ${accessDeniedException.message}" }
        val apiResource = ApiResource.of(
            code = ErrorCode.FORBIDDEN,
            data = ErrorCode.FORBIDDEN.message,
        )
        response.status = ErrorCode.FORBIDDEN.status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.writer, apiResource.body)
    }
}
