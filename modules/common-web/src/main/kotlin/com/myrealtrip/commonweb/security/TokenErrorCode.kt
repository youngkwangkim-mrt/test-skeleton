package com.myrealtrip.commonweb.security

import com.myrealtrip.common.codes.ResponseCode

enum class TokenErrorCode(
    override val message: String,
) : ResponseCode {
    NO_TOKEN("토큰이 존재하지 않습니다."),
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    INVALID_SIGNATURE("토큰 서명이 올바르지 않습니다."),
    EXPIRED_TOKEN("만료된 토큰입니다."),
    INVALID_AUTHENTICATION("인증 정보가 올바르지 않습니다."),
    INVALID_AUTHORIZATION("인가 정보가 올바르지 않습니다."),
    TOKEN_ENCODE_ERROR("토큰 인코딩 오류입니다."),
    TOKEN_DECODE_ERROR("토큰 디코딩 오류입니다."),
    TOKEN_ERROR("토큰 오류입니다."),
    ;

    override val status: Int
        get() = 401
}
