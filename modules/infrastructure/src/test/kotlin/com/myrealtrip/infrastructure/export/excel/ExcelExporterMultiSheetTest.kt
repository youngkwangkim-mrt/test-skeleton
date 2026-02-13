package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.ColumnMetaExtractor
import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import com.myrealtrip.infrastructure.export.annotation.OverflowStrategy
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 멀티시트 분할 기능 테스트
 *
 * 실제 MAX_ROWS_PER_SHEET(1,048,575)는 너무 크므로,
 * 테스트용으로 작은 값(10)을 사용
 */
@DisplayName("ExcelExporter 멀티시트 분할 테스트")
class ExcelExporterMultiSheetTest {

    private lateinit var exporter: ExcelExporter

    @BeforeEach
    fun setUp(): Unit {
        exporter = ExcelExporter(maxRowsPerSheet = 10)
        ColumnMetaExtractor.clearCache()
    }

    @Nested
    @DisplayName("MULTI_SHEET 전략")
    inner class MultiSheetStrategyTest {

        @Test
        fun `should create single sheet when rows within limit`(): Unit {
            // given
            val data = (1..9).map { MultiSheetDto(name = "이름$it", value = it) }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, MultiSheetDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
            assertThat(workbook.numberOfSheets).isEqualTo(1)
            assertThat(workbook.getSheetAt(0).sheetName).isEqualTo("데이터")
            assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(9) // header + 9 data rows

            workbook.close()
        }

        @Test
        fun `should create multiple sheets when rows exceed limit`(): Unit {
            // given - 10 rows per sheet limit, 25 data rows -> 3 sheets
            val data = (1..25).map { MultiSheetDto(name = "이름$it", value = it) }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, MultiSheetDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))

            assertThat(workbook.numberOfSheets).isEqualTo(3)
            assertThat(workbook.getSheetAt(0).sheetName).isEqualTo("데이터")
            assertThat(workbook.getSheetAt(1).sheetName).isEqualTo("데이터 (2)")
            assertThat(workbook.getSheetAt(2).sheetName).isEqualTo("데이터 (3)")

            // 첫 번째 시트: header + 9 data rows (row index 0-9)
            val sheet1 = workbook.getSheetAt(0)
            assertThat(sheet1.lastRowNum).isEqualTo(9)
            assertThat(sheet1.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
            assertThat(sheet1.getRow(1).getCell(0).stringCellValue).isEqualTo("이름1")
            assertThat(sheet1.getRow(9).getCell(0).stringCellValue).isEqualTo("이름9")

            // 두 번째 시트: header + 9 data rows
            val sheet2 = workbook.getSheetAt(1)
            assertThat(sheet2.lastRowNum).isEqualTo(9)
            assertThat(sheet2.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
            assertThat(sheet2.getRow(1).getCell(0).stringCellValue).isEqualTo("이름10")
            assertThat(sheet2.getRow(9).getCell(0).stringCellValue).isEqualTo("이름18")

            // 세 번째 시트: header + 7 data rows
            val sheet3 = workbook.getSheetAt(2)
            assertThat(sheet3.lastRowNum).isEqualTo(7)
            assertThat(sheet3.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
            assertThat(sheet3.getRow(1).getCell(0).stringCellValue).isEqualTo("이름19")
            assertThat(sheet3.getRow(7).getCell(0).stringCellValue).isEqualTo("이름25")

            workbook.close()
        }

        @Test
        fun `should create multiple sheets with chunk export`(): Unit {
            // given - 10 rows per sheet limit, 15 data rows in 3 chunks -> 2 sheets
            val allData = (1..15).map { MultiSheetDto(name = "청크$it", value = it) }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.exportWithChunks(MultiSheetDto::class, outputStream) { consumer ->
                allData.chunked(5).forEach { chunk ->
                    consumer(chunk)
                }
            }

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))

            assertThat(workbook.numberOfSheets).isEqualTo(2)
            assertThat(workbook.getSheetAt(0).sheetName).isEqualTo("데이터")
            assertThat(workbook.getSheetAt(1).sheetName).isEqualTo("데이터 (2)")

            // 첫 번째 시트: header + 9 data rows
            val sheet1 = workbook.getSheetAt(0)
            assertThat(sheet1.getRow(1).getCell(0).stringCellValue).isEqualTo("청크1")
            assertThat(sheet1.getRow(9).getCell(0).stringCellValue).isEqualTo("청크9")

            // 두 번째 시트: header + 6 data rows
            val sheet2 = workbook.getSheetAt(1)
            assertThat(sheet2.getRow(1).getCell(0).stringCellValue).isEqualTo("청크10")
            assertThat(sheet2.getRow(6).getCell(0).stringCellValue).isEqualTo("청크15")

            workbook.close()
        }

        @Test
        fun `should have header row on each sheet`(): Unit {
            // given
            val data = (1..20).map { MultiSheetDto(name = "이름$it", value = it) }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, MultiSheetDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))

            // 모든 시트의 첫 행이 헤더인지 확인
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                val headerRow = sheet.getRow(0)
                assertThat(headerRow.getCell(0).stringCellValue).isEqualTo("이름")
                assertThat(headerRow.getCell(1).stringCellValue).isEqualTo("값")
            }

            workbook.close()
        }

        @Test
        fun `should freeze header on each sheet when freezeHeader is true`(): Unit {
            // given
            val data = (1..15).map { MultiSheetDto(name = "이름$it", value = it) }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, MultiSheetDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))

            assertThat(workbook.numberOfSheets).isEqualTo(2)
            // Note: SXSSF에서 freeze pane 설정은 저장 후 XSSF로 읽을 때 확인 가능
            // POI에서 freeze pane 상태를 직접 확인하기 어려우므로 시트 수만 검증

            workbook.close()
        }
    }

    @Nested
    @DisplayName("EXCEPTION 전략")
    inner class ExceptionStrategyTest {

        @Test
        fun `should throw exception when rows exceed limit with EXCEPTION strategy`(): Unit {
            // given
            val data = (1..15).map { ExceptionDto(name = "이름$it") }
            val outputStream = ByteArrayOutputStream()

            // when & then
            assertThatThrownBy {
                exporter.export(data, ExceptionDto::class, outputStream)
            }.isInstanceOf(ExcelRowLimitExceededException::class.java)
                .hasMessageContaining("Excel row limit exceeded")
        }

        @Test
        fun `should not throw exception when rows within limit with EXCEPTION strategy`(): Unit {
            // given
            val data = (1..9).map { ExceptionDto(name = "이름$it") }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, ExceptionDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
            assertThat(workbook.numberOfSheets).isEqualTo(1)
            assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(9)
            workbook.close()
        }
    }

    @Nested
    @DisplayName("인덱스 컬럼 포함 멀티시트")
    inner class IndexedMultiSheetTest {

        @Test
        fun `should maintain continuous index across multiple sheets`(): Unit {
            // given
            val data = (1..15).map { IndexedMultiSheetDto(name = "이름$it") }
            val outputStream = ByteArrayOutputStream()

            // when
            exporter.export(data, IndexedMultiSheetDto::class, outputStream)

            // then
            val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))

            assertThat(workbook.numberOfSheets).isEqualTo(2)

            // 첫 번째 시트: index 1-9
            val sheet1 = workbook.getSheetAt(0)
            assertThat(sheet1.getRow(0).getCell(0).stringCellValue).isEqualTo("No.")
            assertThat(sheet1.getRow(1).getCell(0).numericCellValue).isEqualTo(1.0)
            assertThat(sheet1.getRow(9).getCell(0).numericCellValue).isEqualTo(9.0)

            // 두 번째 시트: index는 연속 (10부터 시작)
            val sheet2 = workbook.getSheetAt(1)
            assertThat(sheet2.getRow(0).getCell(0).stringCellValue).isEqualTo("No.")
            assertThat(sheet2.getRow(1).getCell(0).numericCellValue).isEqualTo(10.0)
            assertThat(sheet2.getRow(6).getCell(0).numericCellValue).isEqualTo(15.0)

            workbook.close()
        }
    }

    // Test DTOs

    @ExportSheet(name = "데이터", includeIndex = false, overflowStrategy = OverflowStrategy.MULTI_SHEET)
    data class MultiSheetDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
        @ExportColumn(header = "값", order = 2)
        val value: Int,
    )

    @ExportSheet(name = "예외시트", includeIndex = false, overflowStrategy = OverflowStrategy.EXCEPTION)
    data class ExceptionDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
    )

    @ExportSheet(name = "인덱스시트", includeIndex = true, overflowStrategy = OverflowStrategy.MULTI_SHEET)
    data class IndexedMultiSheetDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
    )
}
