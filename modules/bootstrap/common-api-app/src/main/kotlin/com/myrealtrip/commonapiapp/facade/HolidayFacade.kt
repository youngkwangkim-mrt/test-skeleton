package com.myrealtrip.commonapiapp.facade

import com.myrealtrip.commonapiapp.dto.response.HolidayDto
import com.myrealtrip.commonapiapp.dto.response.HolidaysResponse
import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.dto.UpdateHolidayRequest
import com.myrealtrip.domain.holiday.application.HolidayCommandApplication
import com.myrealtrip.domain.holiday.application.HolidayQueryApplication
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class HolidayFacade(
    private val holidayQueryApplication: HolidayQueryApplication,
    private val holidayCommandApplication: HolidayCommandApplication,
) {

    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayDto> {
        return holidayQueryApplication.findPageByYear(year, pageable)
            .map { HolidayDto.from(it) }
    }

    fun findPageByYearAndMonth(year: Int, month: Int, pageable: Pageable): Page<HolidayDto> {
        return holidayQueryApplication.findPageByYearAndMonth(year, month, pageable)
            .map { HolidayDto.from(it) }
    }

    fun findByDate(year: Int, month: Int, day: Int): HolidaysResponse {
        val holidays = holidayQueryApplication.findByDate(year, month, day)
        return HolidaysResponse.from(holidays)
    }

    fun create(request: CreateHolidayRequest): HolidayDto {
        val holiday = holidayCommandApplication.create(request)
        return HolidayDto.from(holiday)
    }

    fun createAll(requests: List<CreateHolidayRequest>): List<HolidayDto> {
        val holidays = holidayCommandApplication.createAll(requests)
        return holidays.map { HolidayDto.from(it) }
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayDto {
        val holiday = holidayCommandApplication.update(id, request)
        return HolidayDto.from(holiday)
    }

    fun delete(id: Long) {
        holidayCommandApplication.delete(id)
    }
}
