package com.myrealtrip.domain.holiday.repository

import com.myrealtrip.domain.common.querydsl.QuerydslRepositorySupport
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.domain.holiday.entity.Holiday
import com.myrealtrip.domain.holiday.entity.QHoliday.holiday
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class HolidayQueryRepository : QuerydslRepositorySupport(Holiday::class.java) {

    fun fetchPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return applyPagination(
            pageable,
            contentQuery = { queryFactory ->
                queryFactory
                    .selectFrom(holiday)
                    .where(holiday.holidayDate.year().eq(year))
                    .orderBy(holiday.holidayDate.asc())
            },
            countQuery = { queryFactory ->
                queryFactory
                    .select(holiday.count())
                    .from(holiday)
                    .where(holiday.holidayDate.year().eq(year))
            },
        ).map { it.toInfo() }
    }

    fun fetchPageByYearAndMonth(year: Int, month: Int, pageable: Pageable): Page<HolidayInfo> {
        return applyPagination(
            pageable,
            contentQuery = { queryFactory ->
                queryFactory
                    .selectFrom(holiday)
                    .where(
                        holiday.holidayDate.year().eq(year),
                        holiday.holidayDate.month().eq(month),
                    )
                    .orderBy(holiday.holidayDate.asc())
            },
            countQuery = { queryFactory ->
                queryFactory
                    .select(holiday.count())
                    .from(holiday)
                    .where(
                        holiday.holidayDate.year().eq(year),
                        holiday.holidayDate.month().eq(month),
                    )
            },
        ).map { it.toInfo() }
    }
}
