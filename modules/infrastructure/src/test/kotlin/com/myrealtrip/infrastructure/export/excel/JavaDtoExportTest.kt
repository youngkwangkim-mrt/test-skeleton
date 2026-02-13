package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.ColumnMetaExtractor
import com.myrealtrip.infrastructure.export.JavaExportDto
import com.myrealtrip.infrastructure.export.JavaRecordDto
import com.myrealtrip.infrastructure.export.JavaStyledRecordDto
import com.myrealtrip.infrastructure.export.annotation.ExportColor
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JavaDtoExportTest {

    private lateinit var exporter: ExcelExporter

    @BeforeEach
    fun setUp(): Unit {
        exporter = ExcelExporter()
        ColumnMetaExtractor.clearCache()
    }

    @Test
    fun `should export Java DTO to excel`(): Unit {
        // given
        val data = listOf(
            JavaExportDto("홍길동", 30),
            JavaExportDto("김철수", 25),
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, JavaExportDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        assertThat(sheet.sheetName).isEqualTo("자바DTO")
        assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
        assertThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("나이")
        assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("홍길동")
        assertThat(sheet.getRow(1).getCell(1).numericCellValue).isEqualTo(30.0)

        workbook.close()
    }

    @Test
    fun `should extract column metas from Java class`(): Unit {
        // when
        val metas = ColumnMetaExtractor.extractColumnMetas(JavaExportDto::class)

        // then
        assertThat(metas).isNotEmpty
        assertThat(metas).hasSize(2)
    }

    @Test
    fun `should export Java record to excel`(): Unit {
        // given
        val data = listOf(
            JavaRecordDto("노트북", 1500000, 10, true),
            JavaRecordDto("마우스", 35000, 100, true),
            JavaRecordDto("키보드", 89000, 0, false),
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, JavaRecordDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        // sheet name
        assertThat(sheet.sheetName).isEqualTo("자바레코드")

        // headers
        assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("상품명")
        assertThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("가격")
        assertThat(sheet.getRow(0).getCell(2).stringCellValue).isEqualTo("재고")
        assertThat(sheet.getRow(0).getCell(3).stringCellValue).isEqualTo("판매중")

        // data row 1
        assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("노트북")
        assertThat(sheet.getRow(1).getCell(1).numericCellValue).isEqualTo(1500000.0)
        assertThat(sheet.getRow(1).getCell(2).numericCellValue).isEqualTo(10.0)
        assertThat(sheet.getRow(1).getCell(3).stringCellValue).isEqualTo("Y")

        // data row 3 (out of stock)
        assertThat(sheet.getRow(3).getCell(0).stringCellValue).isEqualTo("키보드")
        assertThat(sheet.getRow(3).getCell(2).numericCellValue).isEqualTo(0.0)
        assertThat(sheet.getRow(3).getCell(3).stringCellValue).isEqualTo("N")

        workbook.close()
    }

    @Test
    fun `should extract column metas from Java record`(): Unit {
        // when
        val metas = ColumnMetaExtractor.extractColumnMetas(JavaRecordDto::class)

        // then
        assertThat(metas).hasSize(4)
        assertThat(metas.map { it.header }).containsExactly("상품명", "가격", "재고", "판매중")
    }

    @Test
    fun `should apply format to Java record fields`(): Unit {
        // given
        val data = listOf(JavaRecordDto("테스트상품", 1234567, 50, true))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, JavaRecordDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)
        val priceCell = sheet.getRow(1).getCell(1)

        // verify numeric value is set
        assertThat(priceCell.numericCellValue).isEqualTo(1234567.0)

        workbook.close()
    }

    @Test
    fun `should apply custom header style to Java record`(): Unit {
        // given
        val data = listOf(JavaStyledRecordDto("노트북", 1500000, "판매중"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, JavaStyledRecordDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        // header row - 상품명 (LIGHT_BLUE background, CENTER alignment)
        val productNameHeader = sheet.getRow(0).getCell(0)
        assertThat(productNameHeader.cellStyle.fillForegroundColor).isEqualTo(ExportColor.LIGHT_BLUE.index)
        assertThat(productNameHeader.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
        assertThat(productNameHeader.cellStyle.alignment).isEqualTo(HorizontalAlignment.CENTER)

        // header row - 가격 (LIGHT_GREEN background)
        val priceHeader = sheet.getRow(0).getCell(1)
        assertThat(priceHeader.cellStyle.fillForegroundColor).isEqualTo(ExportColor.LIGHT_GREEN.index)

        workbook.close()
    }

    @Test
    fun `should apply custom body style to Java record`(): Unit {
        // given
        val data = listOf(JavaStyledRecordDto("노트북", 1500000, "판매중"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, JavaStyledRecordDto::class, outputStream)

        // then
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheetAt(0)

        // body row - 가격 (RIGHT alignment)
        val priceCell = sheet.getRow(1).getCell(1)
        assertThat(priceCell.cellStyle.alignment).isEqualTo(HorizontalAlignment.RIGHT)

        workbook.close()
    }
}
