package com.myrealtrip.domain.holiday.application

import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.dto.UpdateHolidayRequest
import com.myrealtrip.domain.holiday.service.HolidayService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HolidayCommandApplication(
    private val holidayService: HolidayService,
) {

    fun create(request: CreateHolidayRequest): HolidayInfo {
        return holidayService.create(request)
    }

    fun createAll(requests: List<CreateHolidayRequest>): List<HolidayInfo> {
        return holidayService.createAll(requests)
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        return holidayService.update(id, request)
    }

    fun delete(id: Long) {
        holidayService.delete(id)
    }
}
