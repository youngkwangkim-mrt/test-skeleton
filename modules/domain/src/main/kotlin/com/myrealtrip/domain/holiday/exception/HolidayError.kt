package com.myrealtrip.domain.holiday.exception

import com.myrealtrip.common.codes.ResponseCode

enum class HolidayError(
    override val message: String,
) : ResponseCode {
    HOLIDAY_NOT_FOUND("공휴일을 찾을 수 없습니다."),
    HOLIDAY_ALREADY_EXISTS("이미 등록된 공휴일입니다."),
    INVALID_HOLIDAY_DATE("공휴일 날짜가 올바르지 않습니다."),
    ;

    override val status: Int
        get() = 406

}
