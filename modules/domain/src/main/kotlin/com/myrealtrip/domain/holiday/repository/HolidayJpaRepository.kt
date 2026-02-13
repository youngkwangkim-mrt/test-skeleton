package com.myrealtrip.domain.holiday.repository

import com.myrealtrip.domain.holiday.entity.Holiday
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface HolidayJpaRepository : JpaRepository<Holiday, Long> {

    fun findByHolidayDate(holidayDate: LocalDate): List<Holiday>

    @Query(
        """
        select h
          from Holiday h
         where year(h.holidayDate) = :year
         order by h.holidayDate
        """
    )
    fun findByYear(year: Int): List<Holiday>

    @Query(
        """
        select h
          from Holiday h
         where year(h.holidayDate) = :year
           and month(h.holidayDate) = :month
         order by h.holidayDate
        """
    )
    fun findByYearAndMonth(year: Int, month: Int): List<Holiday>

    fun existsByHolidayDateAndName(holidayDate: LocalDate, name: String): Boolean
}
