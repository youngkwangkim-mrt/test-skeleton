package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.*
import com.myrealtrip.infrastructure.export.annotation.OverflowStrategy
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Component
import java.io.OutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * SXSSF 기반 Excel Exporter
 *
 * - Streaming 방식으로 대용량 데이터 처리
 * - 메모리에 지정된 행 수만 유지 (기본 500행)
 * - CellStyleFactory를 통한 스타일 캐싱 (POI 64K 제한 대응)
 * - 시트 최대 행 초과 시 멀티시트 분할 지원
 *
 * @param rowAccessWindowSize SXSSF 메모리에 유지할 행 수 (기본 500)
 * @param maxRowsPerSheet 시트당 최대 행 수 (기본 1,040,000, 테스트용으로 조정 가능)
 */
@Component
class ExcelExporter(
    private val rowAccessWindowSize: Int = DEFAULT_ROW_ACCESS_WINDOW_SIZE,
    private val maxRowsPerSheet: Int = MAX_ROWS_PER_SHEET,
) : DataExporter {

    override fun <T : Any> export(
        data: List<T>,
        clazz: KClass<T>,
        outputStream: OutputStream,
    ) {
        createWorkbook(clazz, outputStream) { context ->
            data.forEach { item ->
                writeRow(context, item)
            }
        }
    }

    override fun <T : Any> exportWithChunks(
        clazz: KClass<T>,
        outputStream: OutputStream,
        chunkFetcher: (consumer: (List<T>) -> Unit) -> Unit,
    ) {
        createWorkbook(clazz, outputStream) { context ->
            chunkFetcher { chunk ->
                chunk.forEach { item ->
                    writeRow(context, item)
                }
            }
        }
    }

    private fun <T : Any> writeRow(context: ExportContext, item: T) {
        // 시트 최대 행 도달 시 처리
        if (context.currentRowIndex >= maxRowsPerSheet) {
            when (context.sheetMeta.overflowStrategy) {
                OverflowStrategy.MULTI_SHEET -> {
                    setColumnWidths(context.currentSheet, context.columnMetas, context.sheetMeta)
                    context.createNextSheet()
                }
                OverflowStrategy.EXCEPTION -> {
                    throw ExcelRowLimitExceededException(
                        "Excel row limit exceeded: max $maxRowsPerSheet rows per sheet"
                    )
                }
                OverflowStrategy.CSV_FALLBACK -> {
                    throw UnsupportedOperationException("CSV_FALLBACK is not implemented yet")
                }
            }
        }

        createDataRow(
            context.currentSheet,
            context.currentRowIndex,
            context.globalRowIndex,
            item,
            context.columnMetas,
            context.sheetMeta,
            context.styleFactory,
        )
        context.currentRowIndex++
        context.globalRowIndex++
    }

    private fun <T : Any> createWorkbook(
        clazz: KClass<T>,
        outputStream: OutputStream,
        dataWriter: (ExportContext) -> Unit,
    ) {
        val workbook = SXSSFWorkbook(rowAccessWindowSize)
        workbook.use { workbook ->
            val sheetMeta = ColumnMetaExtractor.extractSheetMeta(clazz)
            val columnMetas = ColumnMetaExtractor.extractColumnMetas(clazz)
            val styleFactory = CellStyleFactory(workbook)

            val context = ExportContext(
                workbook = workbook,
                sheetMeta = sheetMeta,
                columnMetas = columnMetas,
                styleFactory = styleFactory,
            )

            context.createNextSheet()
            dataWriter(context)
            setColumnWidths(context.currentSheet, columnMetas, sheetMeta)

            workbook.write(outputStream)
        }
    }

    private fun initializeSheet(
        sheet: SXSSFSheet,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
        styleFactory: CellStyleFactory,
    ) {
        createHeaderRow(sheet, columnMetas, sheetMeta, styleFactory)
        if (sheetMeta.freezeHeader) {
            sheet.createFreezePane(0, 1)
        }
    }

    private fun createHeaderRow(
        sheet: SXSSFSheet,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
        styleFactory: CellStyleFactory,
    ) {
        val row = sheet.createRow(0)
        var colIndex = 0
        val defaultHeaderStyle = styleFactory.getDefaultHeaderStyle()

        if (sheetMeta.includeIndex) {
            val cell = row.createCell(colIndex++)
            cell.setCellValue(sheetMeta.indexHeader)
            cell.cellStyle = defaultHeaderStyle
        }

        columnMetas.forEach { meta ->
            val cell = row.createCell(colIndex++)
            cell.setCellValue(meta.header)

            // 커스텀 헤더 스타일이 있으면 사용, 없으면 기본 스타일
            cell.cellStyle = if (styleFactory.isDefaultStyle(meta.headerStyle)) {
                defaultHeaderStyle
            } else {
                styleFactory.getStyle(meta.headerStyle)
            }
        }
    }

    private fun <T : Any> createDataRow(
        sheet: SXSSFSheet,
        rowIndex: Int,
        indexNumber: Int,
        item: T,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
        styleFactory: CellStyleFactory,
    ) {
        val row = sheet.createRow(rowIndex)
        var colIndex = 0

        if (sheetMeta.includeIndex) {
            val cell = row.createCell(colIndex++)
            cell.setCellValue(indexNumber.toDouble())
        }

        columnMetas.forEach { meta ->
            val cell = row.createCell(colIndex++)

            @Suppress("UNCHECKED_CAST")
            val property = meta.property as KProperty1<T, *>
            val value = property.get(item)

            val bodyStyle = getBodyStyle(meta, styleFactory)
            ValueConverter.setCellValue(cell, value, meta.format, bodyStyle)
        }
    }

    private fun getBodyStyle(meta: ColumnMeta, styleFactory: CellStyleFactory): CellStyle? {
        val hasCustomStyle = !styleFactory.isDefaultStyle(meta.bodyStyle)
        val hasFormat = meta.format.isNotBlank()

        return if (hasCustomStyle || hasFormat) {
            styleFactory.getStyle(meta.bodyStyle, meta.format.ifBlank { null })
        } else {
            null
        }
    }

    private fun setColumnWidths(
        sheet: SXSSFSheet,
        columnMetas: List<ColumnMeta>,
        sheetMeta: SheetMeta,
    ) {
        var colIndex = 0

        if (sheetMeta.includeIndex) {
            sheet.setColumnWidth(colIndex++, sheetMeta.indexWidth * COLUMN_WIDTH_UNIT)
        }

        columnMetas.forEach { meta ->
            if (meta.width > 0) {
                sheet.setColumnWidth(colIndex, meta.width * COLUMN_WIDTH_UNIT)
            }
            colIndex++
        }
    }

    /**
     * Export 진행 상태를 관리하는 컨텍스트
     */
    private inner class ExportContext(
        val workbook: SXSSFWorkbook,
        val sheetMeta: SheetMeta,
        val columnMetas: List<ColumnMeta>,
        val styleFactory: CellStyleFactory,
    ) {
        lateinit var currentSheet: SXSSFSheet
            private set

        var sheetIndex: Int = 0
            private set

        var currentRowIndex: Int = 1
        var globalRowIndex: Int = 1

        fun createNextSheet() {
            val sheetName = if (sheetIndex == 0) {
                sheetMeta.name
            } else {
                "${sheetMeta.name} (${sheetIndex + 1})"
            }

            currentSheet = workbook.createSheet(sheetName)
            sheetIndex++
            currentRowIndex = 1

            initializeSheet(currentSheet, columnMetas, sheetMeta, styleFactory)
        }
    }

    companion object {
        private const val DEFAULT_ROW_ACCESS_WINDOW_SIZE = 500
        private const val COLUMN_WIDTH_UNIT = 256

        /**
         * Excel 시트 최대 행 수
         * 실제 Excel 최대 행 수(1,048,576)보다 여유를 두고 설정
         */
        const val MAX_ROWS_PER_SHEET = 1_040_000
    }
}

/**
 * Excel 행 제한 초과 예외
 */
class ExcelRowLimitExceededException(message: String) : RuntimeException(message)
