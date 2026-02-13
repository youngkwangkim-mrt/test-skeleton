package com.myrealtrip.infrastructure.export.csv

import com.myrealtrip.infrastructure.export.ColumnMetaExtractor
import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class CsvExporterTest {

    private lateinit var exporter: CsvExporter

    @BeforeEach
    fun setUp(): Unit {
        exporter = CsvExporter()
        ColumnMetaExtractor.clearCache()
    }

    @Test
    fun `should export simple data to csv`(): Unit {
        // given
        val data = listOf(
            SimpleDto(name = "홍길동", age = 30),
            SimpleDto(name = "김철수", age = 25),
        )
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(3)
        assertThat(lines[0]).isEqualTo("이름,나이")
        assertThat(lines[1]).isEqualTo("홍길동,30")
        assertThat(lines[2]).isEqualTo("김철수,25")
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
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(3)
        assertThat(lines[0]).isEqualTo("No.,이름")
        assertThat(lines[1]).isEqualTo("1,첫번째")
        assertThat(lines[2]).isEqualTo("2,두번째")
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
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(2)
        assertThat(lines[0]).isEqualTo("텍스트,숫자,소수,날짜,일시,플래그,상태")
        assertThat(lines[1]).isEqualTo("텍스트,1000,1234.56,2025-01-17,2025-01-17 14:30,Y,COMPLETED")
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
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(101) // header + 100 data rows
        assertThat(lines[0]).isEqualTo("이름,나이")
        assertThat(lines[1]).isEqualTo("이름1,1")
        assertThat(lines[100]).isEqualTo("이름100,100")
    }

    @Test
    fun `should sort columns by order`(): Unit {
        // given
        val data = listOf(OrderedDto(third = "C", first = "A", second = "B"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, OrderedDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines[0]).isEqualTo("첫번째,두번째,세번째")
        assertThat(lines[1]).isEqualTo("A,B,C")
    }

    @Test
    fun `should escape values containing comma`(): Unit {
        // given
        val data = listOf(SimpleDto(name = "홍길동,김철수", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines[1]).isEqualTo("\"홍길동,김철수\",30")
    }

    @Test
    fun `should escape values containing double quote`(): Unit {
        // given
        val data = listOf(SimpleDto(name = "홍\"길\"동", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines[1]).isEqualTo("\"홍\"\"길\"\"동\",30")
    }

    @Test
    fun `should escape values containing newline`(): Unit {
        // given
        val data = listOf(SimpleDto(name = "홍길동\n김철수", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val content = outputStream.toString(Charsets.UTF_8)
        assertThat(content).contains("\"홍길동\n김철수\"")
    }

    @Test
    fun `should handle null values`(): Unit {
        // given
        val data = listOf(NullableDto(name = null, value = "test"))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, NullableDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines[1]).isEqualTo(",test")
    }

    @Test
    fun `should format number with custom format`(): Unit {
        // given
        val data = listOf(FormattedNumberDto(amount = 1234567))
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, FormattedNumberDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines[1]).isEqualTo("\"1,234,567\"")
    }

    @Test
    fun `should export empty data with header only`(): Unit {
        // given
        val data = emptyList<SimpleDto>()
        val outputStream = ByteArrayOutputStream()

        // when
        exporter.export(data, SimpleDto::class, outputStream)

        // then
        val lines = outputStream.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(1)
        assertThat(lines[0]).isEqualTo("이름,나이")
    }

    @Test
    fun `should include UTF-8 BOM when includeBom is true`(): Unit {
        // given
        val bomExporter = CsvExporter(includeBom = true)
        val data = listOf(SimpleDto(name = "홍길동", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        bomExporter.export(data, SimpleDto::class, outputStream)

        // then
        val bytes = outputStream.toByteArray()
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())

        // BOM 이후 내용 확인
        val content = String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        val lines = content.lines().filter { it.isNotBlank() }
        assertThat(lines[0]).isEqualTo("이름,나이")
        assertThat(lines[1]).isEqualTo("홍길동,30")
    }

    @Test
    fun `should not include BOM when includeBom is false`(): Unit {
        // given
        val noBomExporter = CsvExporter(includeBom = false)
        val data = listOf(SimpleDto(name = "홍길동", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        noBomExporter.export(data, SimpleDto::class, outputStream)

        // then
        val bytes = outputStream.toByteArray()
        // BOM이 없으면 첫 바이트가 BOM이 아님
        assertThat(bytes[0]).isNotEqualTo(0xEF.toByte())
    }

    @Test
    fun `should include BOM with chunks export`(): Unit {
        // given
        val bomExporter = CsvExporter(includeBom = true)
        val allData = listOf(SimpleDto(name = "홍길동", age = 30))
        val outputStream = ByteArrayOutputStream()

        // when
        bomExporter.exportWithChunks(SimpleDto::class, outputStream) { consumer ->
            consumer(allData)
        }

        // then
        val bytes = outputStream.toByteArray()
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())
    }

    // Test DTOs

    data class SimpleDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String,
        @ExportColumn(header = "나이", order = 2)
        val age: Int,
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

    data class NullableDto(
        @ExportColumn(header = "이름", order = 1)
        val name: String?,
        @ExportColumn(header = "값", order = 2)
        val value: String,
    )

    data class FormattedNumberDto(
        @ExportColumn(header = "금액", order = 1, format = "#,##0")
        val amount: Int,
    )

    enum class OrderStatus {
        PENDING, COMPLETED, CANCELLED
    }
}
