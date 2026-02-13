package com.myrealtrip.commonapiapp.config

import com.myrealtrip.domain.holiday.dto.CreateHolidayRequest
import com.myrealtrip.domain.holiday.application.HolidayCommandApplication
import com.myrealtrip.domain.holiday.application.HolidayQueryApplication
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@Profile("local")
class HolidayDataInitializer(
    private val holidayQueryApplication: HolidayQueryApplication,
    private val holidayCommandApplication: HolidayCommandApplication,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (holidayQueryApplication.findPageByYear(2026, PageRequest.of(0, 1)).totalElements > 0) {
            log.info("Holiday data already exists, skipping initialization")
            return
        }

        val holidays = listOf(
            // 2026년 설날
            CreateHolidayRequest(LocalDate.of(2026, 2, 16), "설날"),
            CreateHolidayRequest(LocalDate.of(2026, 2, 17), "설날"),
            CreateHolidayRequest(LocalDate.of(2026, 2, 18), "설날"),

            // 삼일절
            CreateHolidayRequest(LocalDate.of(2026, 3, 1), "삼일절"),
            CreateHolidayRequest(LocalDate.of(2026, 3, 2), "삼일절 휴일"),

            // 어린이날, 부처님 오신 날
            CreateHolidayRequest(LocalDate.of(2026, 5, 5), "어린이날"),
            CreateHolidayRequest(LocalDate.of(2026, 5, 24), "부처님 오신 날"),
            CreateHolidayRequest(LocalDate.of(2026, 5, 25), "부처님 오신 날 휴일"),

            // 6월
            CreateHolidayRequest(LocalDate.of(2026, 6, 3), "제9회 전국동시지방선거"),
            CreateHolidayRequest(LocalDate.of(2026, 6, 6), "현충일"),

            // 제헌절
            CreateHolidayRequest(LocalDate.of(2026, 7, 17), "제헌절"),

            // 광복절
            CreateHolidayRequest(LocalDate.of(2026, 8, 15), "광복절"),
            CreateHolidayRequest(LocalDate.of(2026, 8, 17), "광복절 휴일"),

            // 추석
            CreateHolidayRequest(LocalDate.of(2026, 9, 24), "추석"),
            CreateHolidayRequest(LocalDate.of(2026, 9, 25), "추석"),
            CreateHolidayRequest(LocalDate.of(2026, 9, 26), "추석"),

            // 개천절, 한글날
            CreateHolidayRequest(LocalDate.of(2026, 10, 3), "개천절"),
            CreateHolidayRequest(LocalDate.of(2026, 10, 5), "개천절 휴일"),
            CreateHolidayRequest(LocalDate.of(2026, 10, 9), "한글날"),

            // 성탄절
            CreateHolidayRequest(LocalDate.of(2026, 12, 25), "성탄절"),
        )

        holidayCommandApplication.createAll(holidays)
        log.info("Initialized {} holiday records for 2026", holidays.size)
    }
}
