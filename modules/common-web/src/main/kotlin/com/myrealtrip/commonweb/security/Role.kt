package com.myrealtrip.commonweb.security

import org.springframework.security.core.authority.SimpleGrantedAuthority

enum class Role {
    SUPER,
    ADMIN,
    USER,
    ;

    fun getAuthorities(): List<SimpleGrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_$name"))
}
