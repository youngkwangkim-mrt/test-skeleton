package com.myrealtrip.infrastructure.export

import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import com.myrealtrip.infrastructure.export.csv.CsvExporter
import com.myrealtrip.infrastructure.export.excel.ExcelExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * Export 성능 벤치마크 테스트
 *
 * CI에서는 실행하지 않음 (@Disabled)
 * 로컬에서 성능 측정 시 @Disabled 제거 후 실행
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :modules:infrastructure:test --tests "com.myrealtrip.infrastructure.export.ExportBenchmarkTest" -Dbenchmark=true
 * ```
 */
@Tag("benchmark")
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class ExportBenchmarkTest {

    private lateinit var excelExporter: ExcelExporter
    private lateinit var csvExporter: CsvExporter
    private lateinit var csvBomExporter: CsvExporter

    @BeforeEach
    fun setUp(): Unit {
        excelExporter = ExcelExporter()
        csvExporter = CsvExporter()
        csvBomExporter = CsvExporter(includeBom = true)
        ColumnMetaExtractor.clearCache()
    }

    // ===========================================
    // Excel Benchmarks
    // ===========================================

    @Test
    fun `benchmark Excel export - 10K rows`(): Unit {
        benchmarkExcel(10_000)
    }

    @Test
    fun `benchmark Excel export - 100K rows`(): Unit {
        benchmarkExcel(100_000)
    }

    @Test
    fun `benchmark Excel export - 500K rows`(): Unit {
        benchmarkExcel(500_000)
    }

    /**
     * 1M rows 테스트 - 메모리 부족 가능성으로 별도 실행 권장
     * JVM 옵션: -Xmx2g
     */
    // @Test
    // fun `benchmark Excel export - 1M rows`(): Unit {
    //     benchmarkExcel(1_000_000)
    // }

    // ===========================================
    // CSV Benchmarks
    // ===========================================

    @Test
    fun `benchmark CSV export - 10K rows`(): Unit {
        benchmarkCsv(10_000)
    }

    @Test
    fun `benchmark CSV export - 100K rows`(): Unit {
        benchmarkCsv(100_000)
    }

    @Test
    fun `benchmark CSV export - 500K rows`(): Unit {
        benchmarkCsv(500_000)
    }

    /**
     * 1M rows 테스트 - 메모리 부족 가능성으로 별도 실행 권장
     * JVM 옵션: -Xmx2g
     */
    // @Test
    // fun `benchmark CSV export - 1M rows`(): Unit {
    //     benchmarkCsv(1_000_000)
    // }

    // ===========================================
    // Chunk Size Comparison
    // ===========================================

    @Test
    fun `compare chunk sizes for Excel - 100K rows`(): Unit {
        val rowCount = 100_000
        val chunkSizes = listOf(100, 500, 1000, 5000, 10000)

        println("\n=== Chunk Size Comparison (Excel, $rowCount rows) ===")
        println("| Chunk Size | Time (ms) | Rows/sec |")
        println("|------------|-----------|----------|")

        chunkSizes.forEach { chunkSize ->
            val data = generateTestData(rowCount)
            val outputStream = ByteArrayOutputStream()

            val time = measureTimeMillis {
                excelExporter.exportWithChunks(BenchmarkDto::class, outputStream) { consumer ->
                    data.chunked(chunkSize).forEach { chunk ->
                        consumer(chunk)
                    }
                }
            }

            val rowsPerSec = (rowCount * 1000L) / time
            println("| %10d | %9d | %8d |".format(chunkSize, time, rowsPerSec))
        }
    }

    @Test
    fun `compare rowAccessWindowSize for Excel - 100K rows`(): Unit {
        val rowCount = 100_000
        val windowSizes = listOf(100, 500, 1000, 2000)

        println("\n=== RowAccessWindowSize Comparison (Excel, $rowCount rows) ===")
        println("| Window Size | Time (ms) | Rows/sec |")
        println("|-------------|-----------|----------|")

        windowSizes.forEach { windowSize ->
            val exporter = ExcelExporter(rowAccessWindowSize = windowSize)
            val data = generateTestData(rowCount)
            val outputStream = ByteArrayOutputStream()

            val time = measureTimeMillis {
                exporter.export(data, BenchmarkDto::class, outputStream)
            }

            val rowsPerSec = (rowCount * 1000L) / time
            println("| %11d | %9d | %8d |".format(windowSize, time, rowsPerSec))
        }
    }

    // ===========================================
    // Memory Usage Test
    // ===========================================

    @Test
    fun `memory usage test - Excel 100K rows`(): Unit {
        val rowCount = 100_000
        val runtime = Runtime.getRuntime()

        // GC before measurement
        System.gc()
        Thread.sleep(100)

        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        val data = generateTestData(rowCount)

        val memoryAfterData = runtime.totalMemory() - runtime.freeMemory()
        val dataMemory = (memoryAfterData - memoryBefore) / 1024 / 1024

        val outputStream = ByteArrayOutputStream()
        val time = measureTimeMillis {
            excelExporter.export(data, BenchmarkDto::class, outputStream)
        }

        val memoryAfterExport = runtime.totalMemory() - runtime.freeMemory()
        val exportMemory = (memoryAfterExport - memoryAfterData) / 1024 / 1024

        println("\n=== Memory Usage (Excel, $rowCount rows) ===")
        println("Data generation memory: ${dataMemory}MB")
        println("Export memory: ${exportMemory}MB")
        println("Total memory used: ${dataMemory + exportMemory}MB")
        println("Export time: ${time}ms")
        println("Output size: ${outputStream.size() / 1024}KB")
    }

    // ===========================================
    // File Output Test
    // ===========================================

    @Test
    fun `export to file - Excel 100K rows`(): Unit {
        val rowCount = 100_000
        val data = generateTestData(rowCount)
        val file = File.createTempFile("benchmark_", ".xlsx")

        try {
            val time = measureTimeMillis {
                FileOutputStream(file).use { fos ->
                    excelExporter.export(data, BenchmarkDto::class, fos)
                }
            }

            println("\n=== File Export (Excel, $rowCount rows) ===")
            println("File: ${file.absolutePath}")
            println("File size: ${file.length() / 1024}KB")
            println("Time: ${time}ms")

            assertThat(file.exists()).isTrue
            assertThat(file.length()).isGreaterThan(0)
        } finally {
            file.delete()
        }
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    private fun benchmarkExcel(rowCount: Int) {
        val data = generateTestData(rowCount)
        val outputStream = ByteArrayOutputStream()

        val time = measureTimeMillis {
            excelExporter.export(data, BenchmarkDto::class, outputStream)
        }

        val rowsPerSec = (rowCount * 1000L) / time
        val sizeKb = outputStream.size() / 1024

        println("\n=== Excel Export Benchmark ===")
        println("Rows: $rowCount")
        println("Time: ${time}ms")
        println("Rows/sec: $rowsPerSec")
        println("Output size: ${sizeKb}KB")

        assertThat(outputStream.size()).isGreaterThan(0)
    }

    private fun benchmarkCsv(rowCount: Int) {
        val data = generateTestData(rowCount)
        val outputStream = ByteArrayOutputStream()

        val time = measureTimeMillis {
            csvExporter.export(data, BenchmarkDto::class, outputStream)
        }

        val rowsPerSec = (rowCount * 1000L) / time
        val sizeKb = outputStream.size() / 1024

        println("\n=== CSV Export Benchmark ===")
        println("Rows: $rowCount")
        println("Time: ${time}ms")
        println("Rows/sec: $rowsPerSec")
        println("Output size: ${sizeKb}KB")

        assertThat(outputStream.size()).isGreaterThan(0)
    }

    private fun generateTestData(count: Int): List<BenchmarkDto> {
        val baseDate = LocalDate.of(2025, 1, 1)
        val baseDateTime = LocalDateTime.of(2025, 1, 1, 0, 0)

        return (1..count).map { i ->
            BenchmarkDto(
                id = i.toLong(),
                name = "사용자_$i",
                email = "user$i@example.com",
                age = 20 + (i % 50),
                salary = BigDecimal("50000.00").add(BigDecimal(i * 100)),
                isActive = i % 2 == 0,
                registeredDate = baseDate.plusDays(i.toLong() % 365),
                lastLoginAt = baseDateTime.plusMinutes(i.toLong()),
                status = BenchmarkStatus.entries[i % BenchmarkStatus.entries.size],
                department = "부서_${i % 10}",
            )
        }
    }

    // ===========================================
    // Test DTO
    // ===========================================

    @ExportSheet(name = "벤치마크")
    data class BenchmarkDto(
        @ExportColumn(header = "ID", order = 1)
        val id: Long,
        @ExportColumn(header = "이름", order = 2)
        val name: String,
        @ExportColumn(header = "이메일", order = 3)
        val email: String,
        @ExportColumn(header = "나이", order = 4)
        val age: Int,
        @ExportColumn(header = "급여", order = 5, format = "#,##0.00")
        val salary: BigDecimal,
        @ExportColumn(header = "활성화", order = 6)
        val isActive: Boolean,
        @ExportColumn(header = "가입일", order = 7)
        val registeredDate: LocalDate,
        @ExportColumn(header = "최종로그인", order = 8, format = "yyyy-MM-dd HH:mm")
        val lastLoginAt: LocalDateTime,
        @ExportColumn(header = "상태", order = 9)
        val status: BenchmarkStatus,
        @ExportColumn(header = "부서", order = 10)
        val department: String,
    )

    enum class BenchmarkStatus {
        ACTIVE, INACTIVE, PENDING, SUSPENDED
    }
}
