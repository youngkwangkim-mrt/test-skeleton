package com.myrealtrip.common.codes.response

import com.myrealtrip.common.codes.ResponseCode

enum class ErrorCode(
    override val status: Int,
    override val message: String,
) : ResponseCode {
    //
    UNAUTHORIZED(401, "인증이 필요합니다."),
    UNAUTHORIZED_IP(401, "허용되지 않은 IP 입니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    NOT_FOUND(404, "요청한 자원을 찾을 수 없습니다."),

    //
    INVALID_ARGUMENT(400, "요청 인자가 올바르지 않습니다."),
    NOT_READABLE(400, "요청 메시지가 올바르지 않습니다."),

    //
    ILLEGAL_ARGUMENT(406, "인자가 올바르지 않습니다."),
    ILLEGAL_STATE(406, "상태가 올바르지 않습니다."),
    DATA_NOT_FOUND(406, "요청한 데이터가 없습니다."),
    UNSUPPORTED_OPERATION(406, "지원하지 않는 기능입니다."),

    //
    DB_ACCESS_ERROR(406, "데이터베이스 접근 오류입니다."),

    //
    CALL_RESPONSE_ERROR(406, "상태가 올바르지 않습니다.") /* 외부 오류 */,

    //
    SERVER_ERROR(500, "일시적인 오류입니다. 잠시 후 다시 시도해주세요."),
}
