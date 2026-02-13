package com.myrealtrip.infrastructure.export.annotation

/**
 * Export 스타일 프리셋
 *
 * 자주 사용하는 헤더/바디 스타일을 미리 정의
 */
enum class ExportStylePreset(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Short = -1,
    val font: ExportFont = ExportFont.DEFAULT,
    val fontColor: ExportColor = ExportColor.BLACK,
    val bgColor: ExportColor = ExportColor.NONE,
    val alignment: ExportAlignment = ExportAlignment.LEFT,
    val border: ExportBorder = ExportBorder.NONE,
    val borderColor: ExportColor = ExportColor.BLACK,
) {
    /** 기본 스타일 (없음) */
    NONE,

    /** 기본 헤더: 굵게, 회색 배경, 가운데 정렬 */
    HEADER_DEFAULT(
        bold = true,
        bgColor = ExportColor.GREY_25,
        alignment = ExportAlignment.CENTER,
    ),

    /** 파란 헤더: 굵게, 파란 배경, 가운데 정렬, 얇은 테두리 */
    HEADER_BLUE(
        bold = true,
        bgColor = ExportColor.LIGHT_BLUE,
        alignment = ExportAlignment.CENTER,
        border = ExportBorder.THIN,
    ),

    /** 진한 파란 헤더: 굵게, 진한 파란 배경, 흰 글씨, 가운데 정렬, 얇은 테두리 */
    HEADER_DARK_BLUE(
        bold = true,
        bgColor = ExportColor.DARK_BLUE,
        fontColor = ExportColor.WHITE,
        alignment = ExportAlignment.CENTER,
        border = ExportBorder.THIN,
    ),

    /** 녹색 헤더: 굵게, 녹색 배경, 가운데 정렬, 얇은 테두리 */
    HEADER_GREEN(
        bold = true,
        bgColor = ExportColor.LIGHT_GREEN,
        alignment = ExportAlignment.CENTER,
        border = ExportBorder.THIN,
    ),

    /** 주황 헤더: 굵게, 주황 배경, 가운데 정렬, 얇은 테두리 */
    HEADER_ORANGE(
        bold = true,
        bgColor = ExportColor.LIGHT_ORANGE,
        alignment = ExportAlignment.CENTER,
        border = ExportBorder.THIN,
    ),

    /** 노란 헤더: 굵게, 노란 배경, 가운데 정렬, 얇은 테두리 */
    HEADER_YELLOW(
        bold = true,
        bgColor = ExportColor.LIGHT_YELLOW,
        alignment = ExportAlignment.CENTER,
        border = ExportBorder.THIN,
    ),

    /** 기본 바디: 왼쪽 정렬 */
    BODY_DEFAULT(
        alignment = ExportAlignment.LEFT,
    ),

    /** 가운데 정렬 바디 */
    BODY_CENTER(
        alignment = ExportAlignment.CENTER,
    ),

    /** 오른쪽 정렬 바디 (숫자용) */
    BODY_RIGHT(
        alignment = ExportAlignment.RIGHT,
    ),

    /** 강조 바디: 굵게, 노란 배경 */
    BODY_HIGHLIGHT(
        bold = true,
        bgColor = ExportColor.LIGHT_YELLOW,
    ),

    /** 경고 바디: 굵게, 주황 배경, 빨간 글씨 */
    BODY_WARNING(
        bold = true,
        bgColor = ExportColor.LIGHT_ORANGE,
        fontColor = ExportColor.DARK_RED,
    ),

    /** 성공 바디: 녹색 배경 */
    BODY_SUCCESS(
        bgColor = ExportColor.LIGHT_GREEN,
    ),

    /** 금액 바디: 굵게, 오른쪽 정렬 */
    BODY_CURRENCY(
        bold = true,
        alignment = ExportAlignment.RIGHT,
    ),

    /** 링크 바디: 파란 글씨 */
    BODY_LINK(
        fontColor = ExportColor.BLUE,
    ),
}
