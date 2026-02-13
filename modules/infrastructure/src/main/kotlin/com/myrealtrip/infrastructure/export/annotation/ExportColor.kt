package com.myrealtrip.infrastructure.export.annotation

import org.apache.poi.ss.usermodel.IndexedColors

/**
 * Export 색상 Enum
 *
 * POI IndexedColors와 매핑
 */
enum class ExportColor(val index: Short) {
    // 기본
    NONE(IndexedColors.AUTOMATIC.index),
    BLACK(IndexedColors.BLACK.index),
    WHITE(IndexedColors.WHITE.index),

    // 그레이 계열 (배경용)
    GREY_25(IndexedColors.GREY_25_PERCENT.index),
    GREY_40(IndexedColors.GREY_40_PERCENT.index),
    GREY_50(IndexedColors.GREY_50_PERCENT.index),

    // 밝은 색상 (배경용)
    LIGHT_BLUE(IndexedColors.PALE_BLUE.index),
    LIGHT_GREEN(IndexedColors.LIGHT_GREEN.index),
    LIGHT_YELLOW(IndexedColors.LIGHT_YELLOW.index),
    LIGHT_ORANGE(IndexedColors.LIGHT_ORANGE.index),

    // 진한 색상 (글자용)
    RED(IndexedColors.RED.index),
    DARK_RED(IndexedColors.DARK_RED.index),
    BLUE(IndexedColors.BLUE.index),
    DARK_BLUE(IndexedColors.DARK_BLUE.index),
    GREEN(IndexedColors.GREEN.index),
    ORANGE(IndexedColors.ORANGE.index),
}
