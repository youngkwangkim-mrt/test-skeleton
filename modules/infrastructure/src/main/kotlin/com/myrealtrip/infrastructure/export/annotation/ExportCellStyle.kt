package com.myrealtrip.infrastructure.export.annotation

/**
 * Export 셀 스타일 설정
 *
 * @property preset 스타일 프리셋 (NONE이 아니면 프리셋 기반, 개별 속성으로 오버라이드 가능)
 * @property bold 굵은 글씨
 * @property italic 기울임
 * @property fontSize 글자 크기 (-1: 기본값 11pt)
 * @property font 폰트
 * @property fontColor 글자 색상
 * @property bgColor 배경 색상
 * @property alignment 정렬
 * @property border 테두리 스타일 (상하좌우 동일)
 * @property borderColor 테두리 색상
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExportCellStyle(
    val preset: ExportStylePreset = ExportStylePreset.NONE,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Short = -1,
    val font: ExportFont = ExportFont.DEFAULT,
    val fontColor: ExportColor = ExportColor.BLACK,
    val bgColor: ExportColor = ExportColor.NONE,
    val alignment: ExportAlignment = ExportAlignment.LEFT,
    val border: ExportBorder = ExportBorder.NONE,
    val borderColor: ExportColor = ExportColor.BLACK,
)
