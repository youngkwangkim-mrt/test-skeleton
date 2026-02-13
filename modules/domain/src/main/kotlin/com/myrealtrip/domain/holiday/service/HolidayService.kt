package com.myrealtrip.domain.holiday.service

import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.dto.HolidayNotFoundException
import com.myrealtrip.domain.holiday.dto.UpdateHolidayRequest
import com.myrealtrip.domain.holiday.entity.Holiday
import com.myrealtrip.domain.holiday.repository.HolidayJpaRepository
import com.myrealtrip.domain.holiday.repository.HolidayQueryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class HolidayService(
    private val holidayJpaRepository: HolidayJpaRepository,
    private val holidayQueryRepository: HolidayQueryRepository,
) {

    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return holidayQueryRepository.fetchPageByYear(year, pageable)
    }

    fun findPageByYearAndMonth(year: Int, month: Int, pageable: Pageable): Page<HolidayInfo> {
        return holidayQueryRepository.fetchPageByYearAndMonth(year, month, pageable)
    }

    fun findByDate(year: Int, month: Int, day: Int): List<HolidayInfo> {
        val date = LocalDate.of(year, month, day)
        return holidayJpaRepository.findByHolidayDate(date)
            .map { it.toInfo() }
    }

    fun findById(id: Long): HolidayInfo {
        return holidayJpaRepository.findById(id)
            .map { it.toInfo() }
            .orElseThrow { HolidayNotFoundException(id) }
    }

    fun create(request: CreateHolidayRequest): HolidayInfo {
        val entity = Holiday.create(
            holidayDate = request.holidayDate,
            name = request.name,
        )
        return holidayJpaRepository.save(entity).toInfo()
    }

    fun createAll(requests: List<CreateHolidayRequest>): List<HolidayInfo> {
        val entities = requests.map {
            Holiday.create(
                holidayDate = it.holidayDate,
                name = it.name,
            )
        }
        return holidayJpaRepository.saveAll(entities).map { it.toInfo() }
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        val entity = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }

        entity.update(
            holidayDate = request.holidayDate,
            name = request.name,
        )
        return entity.toInfo()
    }

    fun delete(id: Long) {
        if (!holidayJpaRepository.existsById(id)) {
            throw HolidayNotFoundException(id)
        }
        holidayJpaRepository.deleteById(id)
    }
}
