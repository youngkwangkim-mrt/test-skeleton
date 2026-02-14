package com.myrealtrip.commonweb.security.jwt

import com.myrealtrip.common.TraceHeader
import com.myrealtrip.commonweb.security.TokenErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class JwtAuthManager(
    private val jwtManager: JwtManager,
) : AuthenticationManager {

    override fun authenticate(authentication: Authentication): Authentication {
        val bearer = authentication as? BearerTokenAuthenticationToken
            ?: throw JwtAuthenticationException(TokenErrorCode.INVALID_AUTHENTICATION)

        val jwt = jwtManager.decodeToken(bearer.token)
        val role = jwtManager.extractRole(jwt)

        MDC.put(TraceHeader.APP_ACCESS_ID, jwt.subject)
        logger.debug { "Authenticated: subject=${jwt.subject}, role=${role.name}" }

        return JwtAuthenticationToken(jwt, role.getAuthorities())
    }
}
