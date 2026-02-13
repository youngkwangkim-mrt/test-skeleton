package com.myrealtrip.domain.holiday.application

import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.dto.UpdateHolidayRequest
import com.myrealtrip.domain.holiday.service.HolidayService
import com.myrealtrip.domain.notification.HolidayNotificationEventFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HolidayCommandApplication(
    private val holidayService: HolidayService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    fun create(request: CreateHolidayRequest): HolidayInfo {
        val createdHoliday = holidayService.create(request)
        val event = HolidayNotificationEventFactory.holidayCreated(
            createdHoliday.id, createdHoliday.holidayDate, createdHoliday.name
        )
        applicationEventPublisher.publishEvent(event)
        return createdHoliday
    }

    fun createAll(requests: List<CreateHolidayRequest>): List<HolidayInfo> {
        val created = holidayService.createAll(requests)
        applicationEventPublisher.publishEvent(
            HolidayNotificationEventFactory.holidayBulkCreated(created.size)
        )
        return created
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        val updated = holidayService.update(id, request)
        applicationEventPublisher.publishEvent(
            HolidayNotificationEventFactory.holidayUpdated(updated.id, updated.holidayDate, updated.name)
        )
        return updated
    }

    fun delete(id: Long) {
        holidayService.delete(id)
        applicationEventPublisher.publishEvent(
            HolidayNotificationEventFactory.holidayDeleted(id)
        )
    }
}
