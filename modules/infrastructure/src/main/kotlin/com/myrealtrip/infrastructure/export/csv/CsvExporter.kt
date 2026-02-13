package com.myrealtrip.infrastructure.export.csv

import com.myrealtrip.infrastructure.export.ColumnMeta
import com.myrealtrip.infrastructure.export.ColumnMetaExtractor
import com.myrealtrip.infrastructure.export.DataExporter
import com.myrealtrip.infrastructure.export.SheetMeta
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * CSV Exporter
 *
 * - 스트리밍 방식으로 대용량 데이터 처리
 * - Excel과 달리 행 제한 없음
 * - BOM 옵션으로 Excel 호환성 지원
 *
 * @property charset 문자 인코딩 (기본: UTF-8)
 * @property delimiter 구분자 (기본: 쉼표)
 * @property includeHeader 헤더 포함 여부
 * @property includeBom UTF-8 BOM 포함 여부 (Excel 호환용)
 */
@Component
class CsvExporter(
    private val charset: Charset = Charsets.UTF_8,
    private val delimiter: Char = DEFAULT_DELIMITER,
    private val includeHeader: Boolean = true,
    private val includeBom: Boolean = false,
) : DataExporter {

    override fun <T : Any> export(
        data: List<T>,
        clazz: KClass<T>,
        outputStream: OutputStream,
    ) {
        val columnMetas = ColumnMetaExtractor.extractColumnMetas(clazz)
        val sheetMeta = ColumnMetaExtractor.extractSheetMeta(clazz)

        writeBomIfNeeded(outputStream)

        BufferedWriter(OutputStreamWriter(outputStream, charset)).use { writer ->
            if (includeHeader) {
                writeHeader(writer, columnMetas, sheetMeta)
            }

            var rowIndex = 1
            data.forEach { item ->
                writeDataRow(writer, rowIndex++, item, columnMetas, sheetMeta)
            }
        }
    }

    override fun <T : Any> exportWithChunks(
        clazz: KClass<T>,
        outputStream: OutputStream,
        chunkFetcher: (consumer: (List<T>) -> Unit) -> Unit,
    ) {
        val columnMetas = ColumnMetaExtractor.extractColumnMetas(clazz)
        val sheetMeta = ColumnMetaExtractor.extractSheetMeta(clazz)

        writeBomIfNeeded(outputStream)

        BufferedWriter(OutputStreamWriter(outputStream, charset)).use { writer ->
            if (includeHeader) {
                writeHeader(writer, columnMetas, sheetMeta)
            }

            var rowIndex = 1
            chunkFetcher { chunk ->
                chunk.forEach { item ->
                    writeDataRow(writer, rowIndex++, item, columnMetas, sheetMeta)
                }
            }
        }
    }

    private fun writeBomIfNeeded(outputStream: OutputStream) {
        if (includeBom && charset == Charsets.UTF_8) {
            outputStream.write(UTF8_BOM)
        }
    }

    private fun writeHeader(
        writer: BufferedWriter,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
    ) {
        val headers = buildList {
            if (sheetMeta.includeIndex) {
                add(escapeCsvValue(sheetMeta.indexHeader))
            }
            columnMetas.forEach { meta ->
                add(escapeCsvValue(meta.header))
            }
        }
        writer.write(headers.joinToString(delimiter.toString()))
        writer.newLine()
    }

    private fun <T : Any> writeDataRow(
        writer: BufferedWriter,
        rowIndex: Int,
        item: T,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
    ) {
        val values = buildList {
            if (sheetMeta.includeIndex) {
                add(rowIndex.toString())
            }
            columnMetas.forEach { meta ->
                @Suppress("UNCHECKED_CAST")
                val property = meta.property as KProperty1<T, *>
                val value = property.get(item)
                add(escapeCsvValue(convertToString(value, meta.format)))
            }
        }
        writer.write(values.joinToString(delimiter.toString()))
        writer.newLine()
    }

    private fun convertToString(value: Any?, format: String): String {
        return when (value) {
            null -> ""
            is String -> value
            is Boolean -> if (value) "Y" else "N"
            is Int, is Long, is Float, is Double, is BigDecimal -> {
                if (format.isNotBlank()) {
                    formatNumber(value as Number, format)
                } else {
                    value.toString()
                }
            }
            is LocalDate -> formatDate(value, format)
            is LocalDateTime -> formatDateTime(value, format)
            is LocalTime -> formatTime(value, format)
            is ZonedDateTime -> formatDateTime(value.toLocalDateTime(), format)
            is Enum<*> -> value.name
            else -> value.toString()
        }
    }

    private fun formatNumber(value: Number, format: String): String {
        // Excel 포맷을 간단히 처리 (예: #,##0 -> 천단위 콤마)
        return when {
            format.contains("#,##0") -> {
                val decimalFormat = java.text.DecimalFormat(format.replace("#,##0", "#,##0"))
                decimalFormat.format(value)
            }
            else -> value.toString()
        }
    }

    private fun formatDate(value: LocalDate, format: String): String {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            DEFAULT_DATE_FORMAT
        }
        return value.format(formatter)
    }

    private fun formatDateTime(value: LocalDateTime, format: String): String {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            DEFAULT_DATETIME_FORMAT
        }
        return value.format(formatter)
    }

    private fun formatTime(value: LocalTime, format: String): String {
        val formatter = if (format.isNotBlank()) {
            DateTimeFormatter.ofPattern(format)
        } else {
            DEFAULT_TIME_FORMAT
        }
        return value.format(formatter)
    }

    /**
     * CSV 값 이스케이프 처리
     *
     * - 쌍따옴표, 쉼표, 개행이 포함된 경우 쌍따옴표로 감싸기
     * - 내부 쌍따옴표는 두 번 연속으로 이스케이프
     */
    private fun escapeCsvValue(value: String): String {
        return if (value.contains('"') || value.contains(delimiter) || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    companion object {
        private const val DEFAULT_DELIMITER = ','
        private val DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DEFAULT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val DEFAULT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

        /** UTF-8 BOM (Byte Order Mark) for Excel compatibility */
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
