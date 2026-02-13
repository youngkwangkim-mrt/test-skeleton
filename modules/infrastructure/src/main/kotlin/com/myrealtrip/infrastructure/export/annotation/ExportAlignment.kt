package com.myrealtrip.infrastructure.export.annotation

import org.apache.poi.ss.usermodel.HorizontalAlignment

/**
 * Export 정렬 Enum
 *
 * POI HorizontalAlignment와 매핑
 */
enum class ExportAlignment(val poiAlignment: HorizontalAlignment) {
    LEFT(HorizontalAlignment.LEFT),
    CENTER(HorizontalAlignment.CENTER),
    RIGHT(HorizontalAlignment.RIGHT),
}
