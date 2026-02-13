package com.myrealtrip.infrastructure.export.excel

import com.myrealtrip.infrastructure.export.annotation.ExportAlignment
import com.myrealtrip.infrastructure.export.annotation.ExportBorder
import com.myrealtrip.infrastructure.export.annotation.ExportCellStyle
import com.myrealtrip.infrastructure.export.annotation.ExportColor
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CellStyleFactoryTest {

    private lateinit var workbook: SXSSFWorkbook
    private lateinit var factory: CellStyleFactory

    @BeforeEach
    fun setUp(): Unit {
        workbook = SXSSFWorkbook()
        factory = CellStyleFactory(workbook)
    }

    @AfterEach
    fun tearDown(): Unit {
        workbook.close()
    }

    @Test
    fun `should create default header style with bold grey background and center alignment`(): Unit {
        // when
        val style = factory.getDefaultHeaderStyle()

        // then
        assertThat(style.fillForegroundColor).isEqualTo(ExportColor.GREY_25.index)
        assertThat(style.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
        assertThat(style.alignment).isEqualTo(HorizontalAlignment.CENTER)
    }

    @Test
    fun `should create style with custom background color`(): Unit {
        // given
        val customStyle = ExportCellStyle(bgColor = ExportColor.LIGHT_BLUE)

        // when
        val style = factory.getStyle(customStyle)

        // then
        assertThat(style.fillForegroundColor).isEqualTo(ExportColor.LIGHT_BLUE.index)
        assertThat(style.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
    }

    @Test
    fun `should create style with custom alignment`(): Unit {
        // given
        val rightAligned = ExportCellStyle(alignment = ExportAlignment.RIGHT)

        // when
        val style = factory.getStyle(rightAligned)

        // then
        assertThat(style.alignment).isEqualTo(HorizontalAlignment.RIGHT)
    }

    @Test
    fun `should create style with bold font`(): Unit {
        // given
        val boldStyle = ExportCellStyle(bold = true)

        // when
        val style = factory.getStyle(boldStyle)

        // then
        val font = workbook.getFontAt(style.fontIndex)
        assertThat(font.bold).isTrue
    }

    @Test
    fun `should create style with custom font color`(): Unit {
        // given
        val redFontStyle = ExportCellStyle(fontColor = ExportColor.RED)

        // when
        val style = factory.getStyle(redFontStyle)

        // then
        val font = workbook.getFontAt(style.fontIndex)
        assertThat(font.color).isEqualTo(ExportColor.RED.index)
    }

    @Test
    fun `should cache and reuse same style`(): Unit {
        // given
        val customStyle = ExportCellStyle(bold = true, bgColor = ExportColor.LIGHT_GREEN)

        // when
        val style1 = factory.getStyle(customStyle)
        val style2 = factory.getStyle(customStyle)

        // then
        assertThat(style1).isSameAs(style2)
    }

    @Test
    fun `should create different styles for different configurations`(): Unit {
        // given
        val style1Config = ExportCellStyle(bold = true)
        val style2Config = ExportCellStyle(italic = true)

        // when
        val style1 = factory.getStyle(style1Config)
        val style2 = factory.getStyle(style2Config)

        // then
        assertThat(style1).isNotSameAs(style2)
    }

    @Test
    fun `should include format in style key`(): Unit {
        // given
        val baseStyle = ExportCellStyle()

        // when
        val styleWithFormat1 = factory.getStyle(baseStyle, "#,##0")
        val styleWithFormat2 = factory.getStyle(baseStyle, "yyyy-MM-dd")

        // then
        assertThat(styleWithFormat1).isNotSameAs(styleWithFormat2)
    }

    @Test
    fun `isDefaultStyle should return true for default style`(): Unit {
        // given
        val defaultStyle = ExportCellStyle()

        // when & then
        assertThat(factory.isDefaultStyle(defaultStyle)).isTrue
    }

    @Test
    fun `isDefaultStyle should return false for customized style`(): Unit {
        // given
        val customStyles = listOf(
            ExportCellStyle(bold = true),
            ExportCellStyle(italic = true),
            ExportCellStyle(fontSize = 14),
            ExportCellStyle(fontColor = ExportColor.RED),
            ExportCellStyle(bgColor = ExportColor.LIGHT_BLUE),
            ExportCellStyle(alignment = ExportAlignment.CENTER),
            ExportCellStyle(border = ExportBorder.THIN),
        )

        // when & then
        customStyles.forEach { style ->
            assertThat(factory.isDefaultStyle(style)).isFalse
        }
    }

    @Test
    fun `should create style with thin border`(): Unit {
        // given
        val borderedStyle = ExportCellStyle(border = ExportBorder.THIN)

        // when
        val style = factory.getStyle(borderedStyle)

        // then
        assertThat(style.borderTop).isEqualTo(BorderStyle.THIN)
        assertThat(style.borderBottom).isEqualTo(BorderStyle.THIN)
        assertThat(style.borderLeft).isEqualTo(BorderStyle.THIN)
        assertThat(style.borderRight).isEqualTo(BorderStyle.THIN)
    }

    @Test
    fun `should create style with medium border`(): Unit {
        // given
        val borderedStyle = ExportCellStyle(border = ExportBorder.MEDIUM)

        // when
        val style = factory.getStyle(borderedStyle)

        // then
        assertThat(style.borderTop).isEqualTo(BorderStyle.MEDIUM)
        assertThat(style.borderBottom).isEqualTo(BorderStyle.MEDIUM)
        assertThat(style.borderLeft).isEqualTo(BorderStyle.MEDIUM)
        assertThat(style.borderRight).isEqualTo(BorderStyle.MEDIUM)
    }

    @Test
    fun `should create style with custom border color`(): Unit {
        // given
        val borderedStyle = ExportCellStyle(
            border = ExportBorder.THIN,
            borderColor = ExportColor.RED,
        )

        // when
        val style = factory.getStyle(borderedStyle)

        // then
        assertThat(style.borderTop).isEqualTo(BorderStyle.THIN)
        assertThat(style.topBorderColor).isEqualTo(ExportColor.RED.index)
        assertThat(style.bottomBorderColor).isEqualTo(ExportColor.RED.index)
        assertThat(style.leftBorderColor).isEqualTo(ExportColor.RED.index)
        assertThat(style.rightBorderColor).isEqualTo(ExportColor.RED.index)
    }

    @Test
    fun `should not apply border color when border is NONE`(): Unit {
        // given
        val noBorderStyle = ExportCellStyle(
            border = ExportBorder.NONE,
            borderColor = ExportColor.RED,
        )

        // when
        val style = factory.getStyle(noBorderStyle)

        // then
        assertThat(style.borderTop).isEqualTo(BorderStyle.NONE)
        assertThat(style.borderBottom).isEqualTo(BorderStyle.NONE)
    }

    @Test
    fun `should create different styles for different border configurations`(): Unit {
        // given
        val thinBorder = ExportCellStyle(border = ExportBorder.THIN)
        val mediumBorder = ExportCellStyle(border = ExportBorder.MEDIUM)

        // when
        val style1 = factory.getStyle(thinBorder)
        val style2 = factory.getStyle(mediumBorder)

        // then
        assertThat(style1).isNotSameAs(style2)
    }
}
