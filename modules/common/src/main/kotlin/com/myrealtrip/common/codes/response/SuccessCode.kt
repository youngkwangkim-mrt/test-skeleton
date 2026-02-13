package com.myrealtrip.common.codes.response

import com.myrealtrip.common.codes.ResponseCode

enum class SuccessCode(
    override val status: Int,
    override val message: String,
) : ResponseCode {
    //
    SUCCESS(200, "성공"),
    CREATED(201, "생성 성공"),
    ACCEPTED(202, "접수 성공"),
}