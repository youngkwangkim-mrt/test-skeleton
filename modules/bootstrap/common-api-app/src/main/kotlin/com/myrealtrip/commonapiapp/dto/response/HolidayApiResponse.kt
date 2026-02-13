package com.myrealtrip.commonapiapp.dto.response

import com.myrealtrip.domain.holiday.dto.HolidayInfo
import java.time.LocalDate

data class HolidayDto(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(holiday: HolidayInfo) = HolidayDto(
            id = holiday.id,
            holidayDate = holiday.holidayDate,
            name = holiday.name,
        )
    }
}

data class HolidaysResponse(
    val holidays: List<HolidayItem>,
) {
    companion object {
        fun from(holidays: List<HolidayInfo>) = HolidaysResponse(
            holidays = holidays.map { HolidayItem(it.holidayDate, it.name) }
        )
    }
}

data class HolidayItem(
    val date: LocalDate,
    val name: String,
)
