package com.myrealtrip.infrastructure.export.annotation

/**
 * 엑셀/CSV Export 시 컬럼 설정
 *
 * @property header 헤더명 (필수)
 * @property order 컬럼 순서 (낮을수록 왼쪽)
 * @property width 컬럼 너비 (256 단위, -1: 자동)
 * @property format 셀 포맷 (예: "#,##0", "yyyy-MM-dd")
 * @property headerStyle 헤더 셀 스타일 (미지정 시 기본 스타일 적용)
 * @property bodyStyle 바디 셀 스타일
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExportColumn(
    val header: String,
    val order: Int = 0,
    val width: Int = -1,
    val format: String = "",
    val headerStyle: ExportCellStyle = ExportCellStyle(),
    val bodyStyle: ExportCellStyle = ExportCellStyle(),
)
