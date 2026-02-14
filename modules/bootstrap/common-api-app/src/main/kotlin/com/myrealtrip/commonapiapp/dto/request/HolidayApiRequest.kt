package com.myrealtrip.commonapiapp.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateHolidayApiRequest(
    @field:NotNull
    val holidayDate: LocalDate,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)

data class UpdateHolidayApiRequest(
    @field:NotNull
    val holidayDate: LocalDate,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)

data class BulkCreateHolidayApiRequest(
    @field:NotEmpty
    @field:Valid
    val holidays: List<CreateHolidayApiRequest>,
)
