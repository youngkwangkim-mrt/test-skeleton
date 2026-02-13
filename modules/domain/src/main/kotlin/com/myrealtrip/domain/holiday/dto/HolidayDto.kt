package com.myrealtrip.domain.holiday.dto

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.KnownException
import com.myrealtrip.domain.holiday.entity.Holiday
import java.time.LocalDate

data class HolidayInfo(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(entity: Holiday) = HolidayInfo(
            id = entity.id!!,
            holidayDate = entity.holidayDate,
            name = entity.name,
        )
    }
}

data class CreateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)

data class UpdateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)

