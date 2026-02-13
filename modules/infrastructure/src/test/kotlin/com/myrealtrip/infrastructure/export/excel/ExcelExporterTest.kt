package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.ColumnMetaExtractor
import com.myrealtrip.infrastructure.export.annotation.ExportAlignment
import com.myrealtrip.infrastructure.export.annotation.ExportCellStyle
import com.myrealtrip.infrastructure.export.annotation.ExportColor
import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import com.myrealtrip.infrastructure.export.annotation.OverflowStrategy
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ExcelExporterTest {

    private lateinit var exporter: ExcelExporter

    @BeforeEach
    fun setUp(): Unit {
        exporter = ExcelExporter()
        ColumnMetaExtractor.clearCache()
    }

    @Test
    fun `should export simple data to excel`(): Unit {
        // given
        val data = listOf(
            SimpleDto(name = "홍길동", age = 30),
            SimpleDto(name = "김철수", age = 25),
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        assertThat(sheet.sheetName).isEqualTo("Sheet1")
        assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
        assertThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("나이")
        assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("홍길동")
        assertThat(sheet.getRow(1).getCell(1).numericCellValue).isEqualTo(30.0)
        assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("김철수")
        assertThat(sheet.getRow(2).getCell(1).numericCellValue).isEqualTo(25.0)

        workbook.close()
    }

    @Test
    fun `should export with custom sheet name`(): Unit {
        // given
        val data = listOf(CustomSheetDto(value = "test"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, CustomSheetDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        assertThat(workbook.getSheetAt(0).sheetName).isEqualTo("커스텀시트")
        workbook.close()
    }

    @Test
    fun `should export with index column when includeIndex is true`(): Unit {
        // given
        val data = listOf(
            IndexedDto(name = "첫번째"),
            IndexedDto(name = "두번째"),
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, IndexedDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("No.")
        assertThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("이름")
        assertThat(sheet.getRow(1).getCell(0).numericCellValue).isEqualTo(1.0)
        assertThat(sheet.getRow(2).getCell(0).numericCellValue).isEqualTo(2.0)

        workbook.close()
    }

    @Test
    fun `should export various types correctly`(): Unit {
        // given
        val data = listOf(
            VariousTypesDto(
                text = "텍스트",
                number = 1000,
                decimal = BigDecimal("1234.56"),
                date = LocalDate.of(2025, 1, 17),
                dateTime = LocalDateTime.of(2025, 1, 17, 14, 30),
                flag = true,
                status = OrderStatus.COMPLETED,
            )
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, VariousTypesDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val row = workbook.getSheetAt(0).getRow(1)

        assertThat(row.getCell(0).stringCellValue).isEqualTo("텍스트")
        assertThat(row.getCell(1).numericCellValue).isEqualTo(1000.0)
        assertThat(row.getCell(2).numericCellValue).isEqualTo(1234.56)
        assertThat(row.getCell(3).stringCellValue).isEqualTo("2025-01-17")
        assertThat(row.getCell(4).stringCellValue).isEqualTo("2025-01-17 14:30")
        assertThat(row.getCell(5).stringCellValue).isEqualTo("Y")
        assertThat(row.getCell(6).stringCellValue).isEqualTo("COMPLETED")

        workbook.close()
    }

    @Test
    fun `should export with chunks`(): Unit {
        // given
        val allData = (1..100).map { SimpleDto(name = "이름$it", age = it) }
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.exportWithChunks(SimpleDto::class, outputStream) { consumer ->
            allData.chunked(30).forEach { chunk ->
                consumer(chunk)
            }
        }

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        assertThat(sheet.lastRowNum).isEqualTo(100) // header + 100 data rows
        assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("이름1")
        assertThat(sheet.getRow(100).getCell(0).stringCellValue).isEqualTo("이름100")

        workbook.close()
    }

    @Test
    fun `should sort columns by order`(): Unit {
        // given
        val data = listOf(OrderedDto(third = "C", first = "A", second = "B"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, OrderedDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val headerRow = workbook.getSheetAt(0).getRow(0)

        assertThat(headerRow.getCell(0).stringCellValue).isEqualTo("첫번째")
        assertThat(headerRow.getCell(1).stringCellValue).isEqualTo("두번째")
        assertThat(headerRow.getCell(2).stringCellValue).isEqualTo("세번째")

        workbook.close()
    }

    @Test
    fun `should apply default header style with bold grey and center alignment`(): Unit {
        // given
        val data = listOf(SimpleDto(name = "테스트", age = 20))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val headerRow = workbook.getSheetAt(0).getRow(0)
        val headerStyle = headerRow.getCell(0).cellStyle

        assertThat(headerStyle.fillForegroundColor).isEqualTo(ExportColor.GREY_25.index)
        assertThat(headerStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
        assertThat(headerStyle.alignment).isEqualTo(HorizontalAlignment.CENTER)

        val font = workbook.getFontAt(headerStyle.fontIndex)
        assertThat(font.bold).isTrue

        workbook.close()
    }

    @Test
    fun `should apply custom header style`(): Unit {
        // given
        val data = listOf(StyledDto(name = "테스트", amount = BigDecimal("1000")))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, StyledDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val headerRow = workbook.getSheetAt(0).getRow(0)

        // 첫 번째 컬럼: 커스텀 헤더 스타일 (파란 배경, 흰색 글자)
        val nameHeaderStyle = headerRow.getCell(0).cellStyle
        assertThat(nameHeaderStyle.fillForegroundColor).isEqualTo(ExportColor.LIGHT_BLUE.index)
        assertThat(nameHeaderStyle.alignment).isEqualTo(HorizontalAlignment.CENTER)

        val nameHeaderFont = workbook.getFontAt(nameHeaderStyle.fontIndex)
        assertThat(nameHeaderFont.bold).isTrue
        assertThat(nameHeaderFont.color).isEqualTo(ExportColor.WHITE.index)

        workbook.close()
    }

    @Test
    fun `should apply custom body style`(): Unit {
        // given
        val data = listOf(StyledDto(name = "테스트", amount = BigDecimal("1000")))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, StyledDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val dataRow = workbook.getSheetAt(0).getRow(1)

        // 두 번째 컬럼(amount): 오른쪽 정렬 바디 스타일
        val amountBodyStyle = dataRow.getCell(1).cellStyle
        assertThat(amountBodyStyle.alignment).isEqualTo(HorizontalAlignment.RIGHT)

        workbook.close()
    }

    @Test
    fun `should apply format with body style`(): Unit {
        // given
        val data = listOf(FormattedStyleDto(price = BigDecimal("12345.67")))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, FormattedStyleDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val dataRow = workbook.getSheetAt(0).getRow(1)

        val priceStyle = dataRow.getCell(0).cellStyle
        assertThat(priceStyle.alignment).isEqualTo(HorizontalAlignment.RIGHT)

        val dataFormatString = workbook.createDataFormat().getFormat(priceStyle.dataFormat)
        assertThat(dataFormatString).isEqualTo("#,##0")

        workbook.close()
    }

    // Test DTOs

    data class SimpleDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
        @ExportColumn(header = "나이", order = 2)
        val age: Int,
    )

    @ExportSheet(name = "커스텀시트")
    data class CustomSheetDto(
        @ExportColumn(header = "값", order = 1)
        val value: String,
    )

    @ExportSheet(includeIndex = true)
    data class IndexedDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
    )

    data class VariousTypesDto(
        @ExportColumn(header = "텍스트", order = 1)
        val text: String,
        @ExportColumn(header = "숫자", order = 2)
        val number: Int,
        @ExportColumn(header = "소수", order = 3)
        val decimal: BigDecimal,
        @ExportColumn(header = "날짜", order = 4)
        val date: LocalDate,
        @ExportColumn(header = "일시", order = 5, format = "yyyy-MM-dd HH:mm")
        val dateTime: LocalDateTime,
        @ExportColumn(header = "플래그", order = 6)
        val flag: Boolean,
        @ExportColumn(header = "상태", order = 7)
        val status: OrderStatus,
    )

    data class OrderedDto(
        @ExportColumn(header = "세번째", order = 3)
        val third: String,
        @ExportColumn(header = "첫번째", order = 1)
        val first: String,
        @ExportColumn(header = "두번째", order = 2)
        val second: String,
    )

    enum class OrderStatus {
        PENDING, COMPLETED, CANCELLED
    }

    data class StyledDto(
        @ExportColumn(
            header = "이름",
            order = 1,
            headerStyle = ExportCellStyle(
                bold = true,
                fontColor = ExportColor.WHITE,
                bgColor = ExportColor.LIGHT_BLUE,
                alignment = ExportAlignment.CENTER,
            ),
        )
        val name: String,
        @ExportColumn(
            header = "금액",
            order = 2,
            bodyStyle = ExportCellStyle(alignment = ExportAlignment.RIGHT),
        )
        val amount: BigDecimal,
    )

    data class FormattedStyleDto(
        @ExportColumn(
            header = "가격",
            order = 1,
            format = "#,##0",
            bodyStyle = ExportCellStyle(alignment = ExportAlignment.RIGHT),
        )
        val price: BigDecimal,
    )

    @Nested
    inner class OverflowStrategyTest {

        @Test
        fun `should throw ExcelRowLimitExceededException when EXCEPTION strategy and exceeds max rows`(): Unit {
            // given
            val rowCount = ExcelExporter.MAX_ROWS_PER_SHEET + 10
            val data = (1..rowCount).map { ExceptionStrategyDto(name = "이름$it") }
            val outputStream = ByteArrayOutputStream()

            // when & then
            assertThatThrownBy {
                exporter.export(data, ExceptionStrategyDto::class, outputStream)
            }.isInstanceOf(ExcelRowLimitExceededException::class.java)
                .hasMessageContaining("Excel row limit exceeded")
                .hasMessageContaining("${ExcelExporter.MAX_ROWS_PER_SHEET}")
        }

        @Test
        fun `should throw UnsupportedOperationException when CSV_FALLBACK strategy`(): Unit {
            // given
            val rowCount = ExcelExporter.MAX_ROWS_PER_SHEET + 10
            val data = (1..rowCount).map { CsvFallbackStrategyDto(name = "이름$it") }
            val outputStream = ByteArrayOutputStream()

            // when & then
            assertThatThrownBy {
                exporter.export(data, CsvFallbackStrategyDto::class, outputStream)
            }.isInstanceOf(UnsupportedOperationException::class.java)
                .hasMessageContaining("CSV_FALLBACK is not implemented")
        }

        @Test
        fun `should use MULTI_SHEET as default overflow strategy`(): Unit {
            // given
            val data = listOf(SimpleDto(name = "테스트", age = 20))
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, SimpleDto::class, outputStream)

            // then - default strategy should not throw exception
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
            assertThat(workbook.numberOfSheets).isEqualTo(1)
            workbook.close()
        }

        @Test
        fun `should export with EXCEPTION strategy when rows within limit`(): Unit {
            // given
            val data = listOf(
                ExceptionStrategyDto(name = "첫번째"),
                ExceptionStrategyDto(name = "두번째"),
            )
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, ExceptionStrategyDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
            assertThat(workbook.numberOfSheets).isEqualTo(1)
            assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(2)
            workbook.close()
        }
    }

    // OverflowStrategy Test DTOs

    @ExportSheet(name = "예외전략", overflowStrategy = OverflowStrategy.EXCEPTION)
    data class ExceptionStrategyDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
    )

    @ExportSheet(name = "CSV대체", overflowStrategy = OverflowStrategy.CSV_FALLBACK)
    data class CsvFallbackStrategyDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
    )
}
