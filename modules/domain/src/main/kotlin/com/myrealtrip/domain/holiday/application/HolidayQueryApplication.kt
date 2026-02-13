package com.myrealtrip.domain.holiday.application

import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.service.HolidayService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class HolidayQueryApplication(
    private val holidayService: HolidayService,
) {

    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return holidayService.findPageByYear(year, pageable)
    }

    fun findPageByYearAndMonth(year: Int, month: Int, pageable: Pageable): Page<HolidayInfo> {
        return holidayService.findPageByYearAndMonth(year, month, pageable)
    }

    fun findByDate(year: Int, month: Int, day: Int): List<HolidayInfo> {
        return holidayService.findByDate(year, month, day)
    }

    fun findById(id: Long): HolidayInfo {
        return holidayService.findById(id)
    }
}
