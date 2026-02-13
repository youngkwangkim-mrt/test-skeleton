package com.myrealtrip.infrastructure.export

import com.myrealtrip.infrastructure.export.annotation.*
import com.myrealtrip.infrastructure.export.csv.CsvExporter
import com.myrealtrip.infrastructure.export.excel.ExcelExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * 실제 Excel/CSV 파일 생성 테스트
 *
 * 생성된 파일은 build/test-output 디렉토리에서 확인 가능
 */
class ExportFileGeneratorTest {

    private lateinit var excelExporter: ExcelExporter
    private lateinit var csvExporter: CsvExporter
    private lateinit var csvBomExporter: CsvExporter

    private val outputDir = File("build/test-output")

    @BeforeEach
    fun setUp() {
        excelExporter = ExcelExporter()
        csvExporter = CsvExporter()
        csvBomExporter = CsvExporter(includeBom = true)
        ColumnMetaExtractor.clearCache()

        // 출력 디렉토리 정리 및 생성
        if (outputDir.exists()) {
            outputDir.listFiles()?.forEach { it.delete() }
        }
        outputDir.mkdirs()
    }

    @Test
    fun `generate sample Excel file`() {
        // given
        val rowCount = 100
        val data = generateSampleData(rowCount)
        val file = File(outputDir, "sample_orders.xlsx")

        // when
        val elapsed = measureTimeMillis {
            FileOutputStream(file).use { fos ->
                excelExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // then
        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(0)
        println("✅ Excel 파일 생성: ${file.name} | ${rowCount}건 | ${file.length() / 1024}KB | ${elapsed}ms")
    }

    @Test
    fun `generate sample CSV file`() {
        // given
        val rowCount = 100
        val data = generateSampleData(rowCount)
        val file = File(outputDir, "sample_orders.csv")

        // when
        val elapsed = measureTimeMillis {
            FileOutputStream(file).use { fos ->
                csvExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // then
        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(0)
        println("✅ CSV 파일 생성: ${file.name} | ${rowCount}건 | ${file.length() / 1024}KB | ${elapsed}ms")
    }

    @Test
    fun `generate CSV file with BOM for Excel`() {
        // given
        val rowCount = 100
        val data = generateSampleData(rowCount)
        val file = File(outputDir, "sample_orders_excel.csv")

        // when
        val elapsed = measureTimeMillis {
            FileOutputStream(file).use { fos ->
                csvBomExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // then
        assertThat(file.exists()).isTrue()
        // BOM 확인
        val bytes = file.readBytes()
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())
        println("✅ CSV (BOM) 파일 생성: ${file.name} | ${rowCount}건 | ${file.length() / 1024}KB | ${elapsed}ms")
    }

    @Test
    fun `generate styled Excel file`() {
        // given
        val rowCount = 50
        val data = generateStyledData(rowCount)
        val file = File(outputDir, "styled_report.xlsx")

        // when
        val elapsed = measureTimeMillis {
            FileOutputStream(file).use { fos ->
                excelExporter.export(data, StyledReportDto::class, fos)
            }
        }

        // then
        assertThat(file.exists()).isTrue()
        println("✅ 스타일 Excel 파일 생성: ${file.name} | ${rowCount}건 | ${file.length() / 1024}KB | ${elapsed}ms")
    }

    @Test
    fun `generate large Excel file with chunks`() {
        // given
        val totalRows = 10_000
        val chunkSize = 1_000
        val file = File(outputDir, "large_orders.xlsx")

        // when
        val elapsed = measureTimeMillis {
            FileOutputStream(file).use { fos ->
                excelExporter.exportWithChunks(OrderExportDto::class, fos) { consumer ->
                    var offset = 0
                    while (offset < totalRows) {
                        val chunk = generateSampleData(chunkSize, offset)
                        consumer(chunk)
                        offset += chunkSize
                    }
                }
            }
        }

        // then
        assertThat(file.exists()).isTrue()
        println("✅ 대용량 Excel 파일 생성: ${file.name} | ${totalRows}건 | ${file.length() / 1024}KB | ${elapsed}ms")
    }

    @Test
    fun `generate all format files`() {
        // given
        val rowCount = 200
        val data = generateSampleData(rowCount)

        // when - Excel
        val excelFile = File(outputDir, "all_formats.xlsx")
        val excelElapsed = measureTimeMillis {
            FileOutputStream(excelFile).use { fos ->
                excelExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // when - CSV
        val csvFile = File(outputDir, "all_formats.csv")
        val csvElapsed = measureTimeMillis {
            FileOutputStream(csvFile).use { fos ->
                csvExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // when - CSV with BOM
        val csvBomFile = File(outputDir, "all_formats_bom.csv")
        val csvBomElapsed = measureTimeMillis {
            FileOutputStream(csvBomFile).use { fos ->
                csvBomExporter.export(data, OrderExportDto::class, fos)
            }
        }

        // then
        println("\n📁 생성된 파일 목록 (${rowCount}건):")
        println("  - ${excelFile.name}: ${excelFile.length() / 1024}KB | ${excelElapsed}ms")
        println("  - ${csvFile.name}: ${csvFile.length() / 1024}KB | ${csvElapsed}ms")
        println("  - ${csvBomFile.name}: ${csvBomFile.length() / 1024}KB | ${csvBomElapsed}ms")
        println("\n📂 출력 디렉토리: ${outputDir.absolutePath}")
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    private fun generateSampleData(count: Int, startIndex: Int = 0): List<OrderExportDto> {
        val baseDate = LocalDate.of(2025, 1, 1)
        val baseDateTime = LocalDateTime.of(2025, 1, 1, 9, 0)
        val statuses = OrderStatus.entries
        val products = listOf("노트북", "스마트폰", "태블릿", "모니터", "키보드", "마우스", "헤드셋")
        val customers = listOf("김철수", "이영희", "박민수", "정소연", "최준혁", "강민지", "윤서준")

        return (1..count).map { i ->
            val idx = startIndex + i
            OrderExportDto(
                orderNo = "ORD-${String.format("%08d", idx)}",
                orderDate = baseDate.plusDays((idx % 365).toLong()),
                orderDateTime = baseDateTime.plusHours((idx % 24).toLong()).plusMinutes((idx % 60).toLong()),
                customerName = customers[idx % customers.size],
                productName = products[idx % products.size],
                quantity = 1 + (idx % 10),
                unitPrice = BigDecimal((idx % 50 + 1) * 10000),
                totalAmount = BigDecimal((idx % 50 + 1) * 10000 * (1 + idx % 10)),
                isPaid = idx % 3 != 0,
                status = statuses[idx % statuses.size],
            )
        }
    }

    private fun generateStyledData(count: Int): List<StyledReportDto> {
        return (1..count).map { i ->
            StyledReportDto(
                rank = i,
                name = "항목 $i",
                score = 100 - (i % 30),
                grade = when {
                    i % 30 < 10 -> "A"
                    i % 30 < 20 -> "B"
                    else -> "C"
                },
                amount = BigDecimal(i * 12345),
                isPass = i % 30 < 25,
                note = if (i % 5 == 0) "특이사항 있음" else "",
            )
        }
    }

    // ===========================================
    // Test DTOs
    // ===========================================

    @ExportSheet(name = "주문목록", includeIndex = true)
    data class OrderExportDto(
        @ExportColumn(header = "주문번호", order = 1, width = 18)
        val orderNo: String,

        @ExportColumn(header = "주문일", order = 2)
        val orderDate: LocalDate,

        @ExportColumn(header = "주문일시", order = 3, format = "yyyy-MM-dd HH:mm")
        val orderDateTime: LocalDateTime,

        @ExportColumn(header = "고객명", order = 4)
        val customerName: String,

        @ExportColumn(header = "상품명", order = 5, width = 15)
        val productName: String,

        @ExportColumn(header = "수량", order = 6)
        val quantity: Int,

        @ExportColumn(header = "단가", order = 7, format = "#,##0")
        val unitPrice: BigDecimal,

        @ExportColumn(header = "총액", order = 8, format = "#,##0")
        val totalAmount: BigDecimal,

        @ExportColumn(header = "결제여부", order = 9)
        val isPaid: Boolean,

        @ExportColumn(header = "상태", order = 10)
        val status: OrderStatus,
    )

    enum class OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    @ExportSheet(name = "성적표", includeIndex = true, indexHeader = "순위")
    data class StyledReportDto(
        @ExportColumn(header = "순위", order = 1)
        val rank: Int,

        @ExportColumn(
            header = "이름",
            order = 2,
            width = 15,
            headerStyle = ExportCellStyle(
                bold = true,
                bgColor = ExportColor.LIGHT_BLUE,
                alignment = ExportAlignment.CENTER,
            ),
        )
        val name: String,

        @ExportColumn(
            header = "점수",
            order = 3,
            headerStyle = ExportCellStyle(
                bold = true,
                bgColor = ExportColor.LIGHT_GREEN,
                alignment = ExportAlignment.CENTER,
            ),
            bodyStyle = ExportCellStyle(
                alignment = ExportAlignment.RIGHT,
            ),
        )
        val score: Int,

        @ExportColumn(
            header = "등급",
            order = 4,
            headerStyle = ExportCellStyle(
                bold = true,
                bgColor = ExportColor.LIGHT_YELLOW,
                alignment = ExportAlignment.CENTER,
            ),
            bodyStyle = ExportCellStyle(
                bold = true,
                alignment = ExportAlignment.CENTER,
            ),
        )
        val grade: String,

        @ExportColumn(
            header = "금액",
            order = 5,
            format = "#,##0",
            headerStyle = ExportCellStyle(
                bold = true,
                bgColor = ExportColor.LIGHT_ORANGE,
                alignment = ExportAlignment.CENTER,
            ),
            bodyStyle = ExportCellStyle(
                alignment = ExportAlignment.RIGHT,
            ),
        )
        val amount: BigDecimal,

        @ExportColumn(header = "합격여부", order = 6)
        val isPass: Boolean,

        @ExportColumn(header = "비고", order = 7, width = 20)
        val note: String,
    )
}
