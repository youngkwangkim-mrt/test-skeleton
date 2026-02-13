package com.myrealtrip.skeletonapiapp._test

import com.myrealtrip.infrastructure.export.DataExporter
import com.myrealtrip.infrastructure.export.ExportFormat
import com.myrealtrip.infrastructure.export.annotation.*
import com.myrealtrip.infrastructure.export.csv.CsvExporter
import com.myrealtrip.infrastructure.export.excel.ExcelExporter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Profile("local")
@RestController
@RequestMapping("/_test/export")
class TestExcelController(
    private val excelExporter: ExcelExporter,
    private val csvExporter: CsvExporter,
) {

    @GetMapping("/download")
    fun download(
        @RequestParam(defaultValue = "100") rowCount: Int,
        @RequestParam(defaultValue = "EXCEL") format: ExportFormat,
        response: HttpServletResponse,
    ) {
        val filename = "test-export.${format.extension}"
        val validatedRowCount = rowCount.coerceIn(MIN_ROW_COUNT, MAX_ROW_COUNT)
        val data = generateDummyData(validatedRowCount)

        response.contentType = format.contentType
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; $filename"
        )

        getExporter(format).export(data, DummyExportDto::class, response.outputStream)
    }

    private fun getExporter(format: ExportFormat): DataExporter = when (format) {
        ExportFormat.EXCEL -> excelExporter
        ExportFormat.CSV -> csvExporter
        ExportFormat.CSV_EXCEL -> CsvExporter(includeBom = true)
    }

    private fun generateDummyData(count: Int): List<DummyExportDto> =
        (1..count).map { createDummyDto(it) }

    private fun createDummyDto(index: Int) = DummyExportDto(
        id = index.toLong(),
        name = "User $index",
        email = "user$index@example.com",
        amount = BigDecimal.valueOf(index * 1000L),
        registeredDate = LocalDate.now().minusDays(index.toLong()),
        lastLoginAt = LocalDateTime.now().minusHours(index.toLong()),
        isActive = index % 2 == 0,
    )

    companion object {
        private const val MIN_ROW_COUNT = 1
        private const val MAX_ROW_COUNT = 1_050_000
    }

    @ExportSheet(name = "테스트데이터", overflowStrategy = OverflowStrategy.MULTI_SHEET)
    data class DummyExportDto(
        @ExportColumn(
            header = "ID",
            order = 1,
            width = 10,
            headerStyle = ExportCellStyle(
                preset = ExportStylePreset.HEADER_DEFAULT,
            ),
            bodyStyle = ExportCellStyle(
                preset = ExportStylePreset.BODY_DEFAULT,
            )
        )
        val id: Long,
        @ExportColumn(header = "이름", order = 2, width = 15)
        val name: String,
        @ExportColumn(header = "이메일", order = 3, width = 25)
        val email: String,
        @ExportColumn(
            header = "금액", order = 4, width = 15, format = "#,##0",
            bodyStyle = ExportCellStyle(
                preset = ExportStylePreset.BODY_CURRENCY,
                font = ExportFont.CONSOLAS,
            )
        )
        val amount: BigDecimal,
        @ExportColumn(header = "등록일", order = 5, width = 15)
        val registeredDate: LocalDate,
        @ExportColumn(header = "마지막 로그인", order = 6, width = 20, format = "yyyy-MM-dd HH:mm")
        val lastLoginAt: LocalDateTime,
        @ExportColumn(
            header = "활성여부", order = 7, width = 10,
            bodyStyle = ExportCellStyle(
                preset = ExportStylePreset.BODY_WARNING,
            )
        )
        val isActive: Boolean,
    )
}
