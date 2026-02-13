package com.myrealtrip.infrastructure.export.annotation

import org.apache.poi.ss.usermodel.BorderStyle

/**
 * Export 테두리 스타일 Enum
 *
 * POI BorderStyle과 매핑
 */
enum class ExportBorder(val poiBorderStyle: BorderStyle) {
    NONE(BorderStyle.NONE),
    THIN(BorderStyle.THIN),
    MEDIUM(BorderStyle.MEDIUM),
    THICK(BorderStyle.THICK),
    DASHED(BorderStyle.DASHED),
    DOTTED(BorderStyle.DOTTED),
    DOUBLE(BorderStyle.DOUBLE),
}
