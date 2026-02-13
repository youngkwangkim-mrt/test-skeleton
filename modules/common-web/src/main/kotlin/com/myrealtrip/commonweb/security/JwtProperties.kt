package com.myrealtrip.commonweb.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.OctetSequenceKey
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties(prefix = "common-web.jwt")
data class JwtProperties(
    val key: String = "",
    val accessTokenExpire: Duration = Duration.ofHours(1),
    val refreshTokenExpire: Duration = Duration.ofDays(14),
) {
    val secretKey: SecretKeySpec
        get() {
            val octetKey = OctetSequenceKey.Builder(key.toByteArray())
                .algorithm(JWSAlgorithm.HS256)
                .build()
            return SecretKeySpec(octetKey.toByteArray(), "HmacSHA256")
        }
}
