package com.myrealtrip.commonweb.security.jwt

import com.myrealtrip.commonweb.security.JwtProperties
import com.myrealtrip.commonweb.security.Role
import com.myrealtrip.commonweb.security.TokenErrorCode
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date

private val logger = KotlinLogging.logger {}

@Component
class JwtManager(
    private val jwtProperties: JwtProperties,
    private val jwtDecoder: JwtDecoder,
) {

    fun generateToken(payload: JwtPayload, expiration: Duration = jwtProperties.accessTokenExpire): String {
        val now = Instant.now()
        val claims = JWTClaimsSet.Builder()
            .issuer(payload.issuer)
            .subject(payload.subject)
            .claim(SCOPE_CLAIM, payload.role.name)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(expiration)))
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.HS256).build()
        val signedJwt = SignedJWT(header, claims)

        try {
            val signer = MACSigner(jwtProperties.key.toByteArray())
            signedJwt.sign(signer)
        } catch (e: Exception) {
            logger.error(e) { "Token encoding failed" }
            throw JwtAuthenticationException(TokenErrorCode.TOKEN_ENCODE_ERROR, cause = e)
        }

        return signedJwt.serialize()
    }

    fun decodeToken(token: String): Jwt {
        return try {
            jwtDecoder.decode(token)
        } catch (e: JwtException) {
            val errorCode = resolveErrorCode(e)
            logger.debug { "Token decode failed: [${errorCode.name}] ${e.message}" }
            throw JwtAuthenticationException(errorCode, cause = e)
        } catch (e: Exception) {
            logger.error(e) { "Unexpected token decode error" }
            throw JwtAuthenticationException(TokenErrorCode.TOKEN_ERROR, cause = e)
        }
    }

    fun extractRole(jwt: Jwt): Role {
        val scope = jwt.getClaimAsString(SCOPE_CLAIM)
            ?: throw JwtAuthenticationException(TokenErrorCode.INVALID_TOKEN, "Missing scope claim")
        return try {
            Role.valueOf(scope)
        } catch (e: IllegalArgumentException) {
            throw JwtAuthenticationException(TokenErrorCode.INVALID_AUTHORIZATION, "Unknown role: $scope")
        }
    }

    private fun resolveErrorCode(e: JwtException): TokenErrorCode {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("expired") -> TokenErrorCode.EXPIRED_TOKEN
            message.contains("signature") -> TokenErrorCode.INVALID_SIGNATURE
            else -> TokenErrorCode.TOKEN_DECODE_ERROR
        }
    }

    companion object {
        private const val SCOPE_CLAIM = "scope"
    }
}
