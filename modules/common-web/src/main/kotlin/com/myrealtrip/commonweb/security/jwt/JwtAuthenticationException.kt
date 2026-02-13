package com.myrealtrip.commonweb.security.jwt

import com.myrealtrip.commonweb.security.TokenErrorCode
import org.springframework.security.core.AuthenticationException

class JwtAuthenticationException(
    val errorCode: TokenErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
) : AuthenticationException(message, cause ?: Exception(message))
