package com.myrealtrip.commonapiapp.api

import com.myrealtrip.commonapiapp.dto.request.BulkCreateHolidayApiRequest
import com.myrealtrip.commonapiapp.dto.request.CreateHolidayApiRequest
import com.myrealtrip.commonapiapp.dto.request.UpdateHolidayApiRequest
import com.myrealtrip.commonapiapp.dto.response.HolidayDto
import com.myrealtrip.commonapiapp.dto.response.HolidaysResponse
import com.myrealtrip.commonapiapp.facade.HolidayFacade
import com.myrealtrip.commonweb.response.resource.ApiResource
import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.dto.UpdateHolidayRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/holidays")
class HolidayController(
    private val holidayFacade: HolidayFacade,
) {

    @GetMapping("/{year}")
    fun getByYear(
        @PathVariable year: Int,
        pageable: Pageable = Pageable.ofSize(10),
    ): ResponseEntity<ApiResource<List<HolidayDto>>> {
        return ApiResource.ofPage(holidayFacade.findPageByYear(year, pageable))
    }

    @GetMapping("/{year}/{month}")
    fun getByYearAndMonth(
        @PathVariable year: Int,
        @PathVariable month: Int,
        pageable: Pageable = Pageable.ofSize(10),
    ): ResponseEntity<ApiResource<List<HolidayDto>>> {
        return ApiResource.ofPage(holidayFacade.findPageByYearAndMonth(year, month, pageable))
    }

    @GetMapping("/{year}/{month}/{day}")
    fun getByDate(
        @PathVariable year: Int,
        @PathVariable month: Int,
        @PathVariable day: Int,
    ): ResponseEntity<ApiResource<HolidaysResponse>> {
        return ApiResource.success(holidayFacade.findByDate(year, month, day))
    }

    @PostMapping
    fun create(@RequestBody request: CreateHolidayApiRequest): ResponseEntity<ApiResource<HolidayDto>> {
        val holiday = holidayFacade.create(
            CreateHolidayRequest(
                holidayDate = request.holidayDate,
                name = request.name,
            )
        )
        return ApiResource.success(holiday)
    }

    @PostMapping("/bulk")
    fun createBulk(@RequestBody request: BulkCreateHolidayApiRequest): ResponseEntity<ApiResource<List<HolidayDto>>> {
        val requests = request.holidays.map {
            CreateHolidayRequest(
                holidayDate = it.holidayDate,
                name = it.name,
            )
        }
        return ApiResource.success(holidayFacade.createAll(requests))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateHolidayApiRequest,
    ): ResponseEntity<ApiResource<HolidayDto>> {
        val holiday = holidayFacade.update(
            id,
            UpdateHolidayRequest(
                holidayDate = request.holidayDate,
                name = request.name,
            )
        )
        return ApiResource.success(holiday)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
        holidayFacade.delete(id)
        return ApiResource.success()
    }
}
