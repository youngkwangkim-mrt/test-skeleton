package com.myrealtrip.commonweb.security

import com.myrealtrip.commonweb.filters.ContentCachingFilter
import com.myrealtrip.commonweb.security.jwt.JwtAccessDeniedHandler
import com.myrealtrip.commonweb.security.jwt.JwtAuthManager
import com.myrealtrip.commonweb.security.jwt.JwtAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.HeaderWriterFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthManager: JwtAuthManager,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterAfter(ContentCachingFilter(), HeaderWriterFilter::class.java)
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt -> jwt.authenticationManager(jwtAuthManager) }
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
            }
            .build()
    }

    @Bean
    fun roleHierarchy(): RoleHierarchy {
        val hierarchy = """
        ROLE_SUPER > ROLE_ADMIN > ROLE_USER
        """.trimIndent()
        return RoleHierarchyImpl.fromHierarchy(hierarchy)
    }
}
