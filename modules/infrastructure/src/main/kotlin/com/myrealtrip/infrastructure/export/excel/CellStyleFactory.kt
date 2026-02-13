package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.annotation.ExportAlignment
import com.myrealtrip.infrastructure.export.annotation.ExportBorder
import com.myrealtrip.infrastructure.export.annotation.ExportCellStyle
import com.myrealtrip.infrastructure.export.annotation.ExportColor
import com.myrealtrip.infrastructure.export.annotation.ExportFont
import com.myrealtrip.infrastructure.export.annotation.ExportStylePreset
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/**
 * Excel CellStyle 팩토리
 *
 * - POI 64K CellStyle 제한 대응을 위한 스타일 캐싱
 * - 동일 스타일은 재사용하여 메모리 효율화
 * - 스타일 프리셋 지원 (프리셋 + 개별 속성 오버라이드)
 */
class CellStyleFactory(val workbook: SXSSFWorkbook) {

    private val styleCache = mutableMapOf<String, CellStyle>()
    private val fontCache = mutableMapOf<String, Font>()
    private val dataFormat by lazy { workbook.createDataFormat() }

    /**
     * ExportCellStyle 어노테이션 기반 CellStyle 생성/조회
     *
     * @param style 스타일 어노테이션 (프리셋 + 개별 속성)
     * @param format 셀 포맷 (숫자/날짜)
     * @return 캐싱된 또는 새로 생성된 CellStyle
     */
    fun getStyle(style: ExportCellStyle, format: String? = null): CellStyle {
        val resolved = resolveStyle(style)
        val key = buildStyleKey(resolved, format)
        return styleCache.getOrPut(key) {
            createCellStyle(resolved, format)
        }
    }

    /**
     * 기본 헤더 스타일 생성
     *
     * - 굵은 글씨
     * - 회색 배경
     * - 가운데 정렬
     */
    fun getDefaultHeaderStyle(): CellStyle {
        return getStyle(DEFAULT_HEADER_STYLE)
    }

    /**
     * 커스텀 스타일이 기본 스타일인지 확인
     */
    fun isDefaultStyle(style: ExportCellStyle): Boolean {
        val resolved = resolveStyle(style)
        return !resolved.bold &&
            !resolved.italic &&
            resolved.fontSize <= 0 &&
            resolved.font == ExportFont.DEFAULT &&
            resolved.fontColor == ExportColor.BLACK &&
            resolved.bgColor == ExportColor.NONE &&
            resolved.alignment == ExportAlignment.LEFT &&
            resolved.border == ExportBorder.NONE
    }

    /**
     * 프리셋과 개별 속성을 합성하여 최종 스타일 결정
     *
     * - preset이 NONE이면 개별 속성 그대로 사용
     * - preset이 NONE이 아니면 프리셋을 기본으로, 개별 속성이 기본값이 아니면 오버라이드
     */
    private fun resolveStyle(style: ExportCellStyle): ResolvedStyle {
        val preset = style.preset
        if (preset == ExportStylePreset.NONE) {
            return ResolvedStyle(
                bold = style.bold,
                italic = style.italic,
                fontSize = style.fontSize,
                font = style.font,
                fontColor = style.fontColor,
                bgColor = style.bgColor,
                alignment = style.alignment,
                border = style.border,
                borderColor = style.borderColor,
            )
        }

        // 프리셋 기반 + 개별 속성 오버라이드
        return ResolvedStyle(
            bold = style.bold || preset.bold,
            italic = style.italic || preset.italic,
            fontSize = if (style.fontSize > 0) style.fontSize else preset.fontSize,
            font = if (style.font != ExportFont.DEFAULT) style.font else preset.font,
            fontColor = if (style.fontColor != ExportColor.BLACK) style.fontColor else preset.fontColor,
            bgColor = if (style.bgColor != ExportColor.NONE) style.bgColor else preset.bgColor,
            alignment = if (style.alignment != ExportAlignment.LEFT) style.alignment else preset.alignment,
            border = if (style.border != ExportBorder.NONE) style.border else preset.border,
            borderColor = if (style.borderColor != ExportColor.BLACK) style.borderColor else preset.borderColor,
        )
    }

    private fun createCellStyle(style: ResolvedStyle, format: String?): CellStyle {
        return workbook.createCellStyle().apply {
            // 배경색
            if (style.bgColor != ExportColor.NONE) {
                fillForegroundColor = style.bgColor.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            // 폰트 스타일
            if (needsFont(style)) {
                setFont(getFont(style))
            }

            // 정렬
            alignment = style.alignment.poiAlignment

            // 테두리
            if (style.border != ExportBorder.NONE) {
                borderTop = style.border.poiBorderStyle
                borderBottom = style.border.poiBorderStyle
                borderLeft = style.border.poiBorderStyle
                borderRight = style.border.poiBorderStyle

                if (style.borderColor != ExportColor.BLACK) {
                    topBorderColor = style.borderColor.index
                    bottomBorderColor = style.borderColor.index
                    leftBorderColor = style.borderColor.index
                    rightBorderColor = style.borderColor.index
                }
            }

            // 포맷
            if (!format.isNullOrBlank()) {
                dataFormat = this@CellStyleFactory.dataFormat.getFormat(format)
            }
        }
    }

    private fun needsFont(style: ResolvedStyle): Boolean {
        return style.bold ||
            style.italic ||
            style.fontSize > 0 ||
            style.font != ExportFont.DEFAULT ||
            style.fontColor != ExportColor.BLACK
    }

    private fun getFont(style: ResolvedStyle): Font {
        val key = buildFontKey(style)
        return fontCache.getOrPut(key) {
            workbook.createFont().apply {
                bold = style.bold
                italic = style.italic
                if (style.fontSize > 0) {
                    fontHeightInPoints = style.fontSize
                }
                if (style.font != ExportFont.DEFAULT) {
                    fontName = style.font.fontName
                }
                if (style.fontColor != ExportColor.BLACK) {
                    color = style.fontColor.index
                }
            }
        }
    }

    private fun buildStyleKey(style: ResolvedStyle, format: String?): String {
        return "style_${style.bgColor}_${style.fontColor}_${style.bold}_${style.italic}_${style.fontSize}_${style.font}_${style.alignment}_${style.border}_${style.borderColor}_$format"
    }

    private fun buildFontKey(style: ResolvedStyle): String {
        return "font_${style.bold}_${style.italic}_${style.fontSize}_${style.font}_${style.fontColor}"
    }

    /**
     * 프리셋과 개별 속성이 합성된 최종 스타일
     */
    private data class ResolvedStyle(
        val bold: Boolean,
        val italic: Boolean,
        val fontSize: Short,
        val font: ExportFont,
        val fontColor: ExportColor,
        val bgColor: ExportColor,
        val alignment: ExportAlignment,
        val border: ExportBorder,
        val borderColor: ExportColor,
    )

    companion object {
        private val DEFAULT_HEADER_STYLE = ExportCellStyle(
            bold = true,
            bgColor = ExportColor.GREY_25,
            alignment = ExportAlignment.CENTER,
        )
    }
}
