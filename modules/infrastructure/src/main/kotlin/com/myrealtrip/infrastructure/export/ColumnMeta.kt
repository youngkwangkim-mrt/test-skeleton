package com.myrealtrip.infrastructure.export

import com.myrealtrip.infrastructure.export.annotation.ExportCellStyle
import com.myrealtrip.infrastructure.export.annotation.OverflowStrategy
import kotlin.reflect.KProperty1

/**
 * Export 컬럼 메타데이터
 */
data class ColumnMeta(
    val header: String,
    val order: Int,
    val width: Int,
    val format: String,
    val property: KProperty1<*, *>,
    val headerStyle: ExportCellStyle,
    val bodyStyle: ExportCellStyle,
)

/**
 * Export 시트 메타데이터
 */
data class SheetMeta(
    val name: String,
    val freezeHeader: Boolean,
    val includeIndex: Boolean,
    val indexHeader: String,
    val indexWidth: Int,
    val overflowStrategy: OverflowStrategy,
)
