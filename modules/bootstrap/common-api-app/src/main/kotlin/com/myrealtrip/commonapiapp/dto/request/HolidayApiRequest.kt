package com.myrealtrip.commonapiapp.dto.request

import java.time.LocalDate

data class CreateHolidayApiRequest(
    val holidayDate: LocalDate,
    val name: String,
)

data class UpdateHolidayApiRequest(
    val holidayDate: LocalDate,
    val name: String,
)

data class BulkCreateHolidayApiRequest(
    val holidays: List<CreateHolidayApiRequest>,
)
