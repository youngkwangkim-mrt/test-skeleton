package com.myrealtrip.infrastructure.export

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 타입별 셀 값 변환기
 */
object ValueConverter {

    private val defaultDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val defaultDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val defaultTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val defaultZonedDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")

    /**
     * 값을 셀에 설정
     *
     * 스타일(포맷 포함)은 CellStyleFactory에서 미리 생성하여 전달해야 합니다.
     *
     * @param cell 대상 셀
     * @param value 설정할 값
     * @param format 포맷 문자열 (날짜용, 숫자는 style에 포함)
     * @param style 적용할 셀 스타일 (포맷 포함)
     */
    fun setCellValue(
        cell: Cell,
        value: Any?,
        format: String,
        style: CellStyle? = null,
    ) {
        when (value) {
            null -> cell.setBlank()
            is String -> cell.setCellValue(value)
            is Boolean -> cell.setCellValue(if (value) "Y" else "N")
            is Number -> cell.setCellValue(value.toDouble())
            is LocalDate -> setDateValue(cell, value, format)
            is LocalDateTime -> setDateTimeValue(cell, value, format)
            is LocalTime -> setTimeValue(cell, value, format)
            is ZonedDateTime -> setZonedDateTimeValue(cell, value, format)
            is Enum<*> -> cell.setCellValue(value.name)
            else -> cell.setCellValue(value.toString())
        }

        // 스타일 적용
        if (style != null) {
            cell.cellStyle = style
        }
    }

    private fun setDateValue(cell: Cell, value: LocalDate, format: String) {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            defaultDateFormat
        }
        cell.setCellValue(value.format(formatter))
    }

    private fun setDateTimeValue(cell: Cell, value: LocalDateTime, format: String) {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            defaultDateTimeFormat
        }
        cell.setCellValue(value.format(formatter))
    }

    private fun setTimeValue(cell: Cell, value: LocalTime, format: String) {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            defaultTimeFormat
        }
        cell.setCellValue(value.format(formatter))
    }

    private fun setZonedDateTimeValue(cell: Cell, value: ZonedDateTime, format: String) {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            defaultZonedDateTimeFormat
        }
        cell.setCellValue(value.format(formatter))
    }
}
