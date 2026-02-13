package com.myrealtrip.domain.holiday.dto

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.KnownException
import java.time.LocalDate

data class HolidayInfo(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
)

data class CreateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)

data class UpdateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)

class HolidayNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND,
    "Holiday not found: $id"
)
