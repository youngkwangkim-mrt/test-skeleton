package com.myrealtrip.commonweb.security.jwt

import com.myrealtrip.commonweb.security.Role

data class JwtPayload(
    val issuer: String,
    val subject: String,
    val role: Role,
)
