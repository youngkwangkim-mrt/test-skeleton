package com.myrealtrip.infrastructure.export

import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ColumnMetaExtractorTest {

    @BeforeEach
    fun setUp(): Unit {
        ColumnMetaExtractor.clearCache()
    }

    @Test
    fun `should extract sheet meta with defaults when no annotation`(): Unit {
        // given
        class NoAnnotationDto

        // when
        val result = ColumnMetaExtractor.extractSheetMeta(NoAnnotationDto::class)

        // then
        assertThat(result.name).isEqualTo("Sheet1")
        assertThat(result.freezeHeader).isTrue()
        assertThat(result.includeIndex).isFalse()
    }

    @Test
    fun `should extract sheet meta from annotation`(): Unit {
        // given
        @ExportSheet(
            name = "테스트시트",
            freezeHeader = false,
            includeIndex = true,
            indexHeader = "번호",
            indexWidth = 10,
        )
        class AnnotatedDto

        // when
        val result = ColumnMetaExtractor.extractSheetMeta(AnnotatedDto::class)

        // then
        assertThat(result.name).isEqualTo("테스트시트")
        assertThat(result.freezeHeader).isFalse()
        assertThat(result.includeIndex).isTrue()
        assertThat(result.indexHeader).isEqualTo("번호")
        assertThat(result.indexWidth).isEqualTo(10)
    }

    @Test
    fun `should extract column metas sorted by order`(): Unit {
        // given
        data class TestDto(
            @ExportColumn(header = "세번째", order = 3)
            val third: String,
            @ExportColumn(header = "첫번째", order = 1)
            val first: String,
            @ExportColumn(header = "두번째", order = 2)
            val second: String,
        )

        // when
        val result = ColumnMetaExtractor.extractColumnMetas(TestDto::class)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].header).isEqualTo("첫번째")
        assertThat(result[1].header).isEqualTo("두번째")
        assertThat(result[2].header).isEqualTo("세번째")
    }

    @Test
    fun `should extract column meta with format and width`(): Unit {
        // given
        data class TestDto(
            @ExportColumn(header = "금액", order = 1, width = 15, format = "#,##0")
            val amount: Int,
        )

        // when
        val result = ColumnMetaExtractor.extractColumnMetas(TestDto::class)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].header).isEqualTo("금액")
        assertThat(result[0].width).isEqualTo(15)
        assertThat(result[0].format).isEqualTo("#,##0")
    }

    @Test
    fun `should ignore properties without ExportColumn annotation`(): Unit {
        // given
        data class TestDto(
            @ExportColumn(header = "이름", order = 1)
            val name: String,
            val ignoredField: String,
        )

        // when
        val result = ColumnMetaExtractor.extractColumnMetas(TestDto::class)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].header).isEqualTo("이름")
    }

    @Test
    fun `should cache extracted metas`(): Unit {
        // given
        data class TestDto(
            @ExportColumn(header = "이름", order = 1)
            val name: String,
        )

        // when
        val first = ColumnMetaExtractor.extractColumnMetas(TestDto::class)
        val second = ColumnMetaExtractor.extractColumnMetas(TestDto::class)

        // then
        assertThat(first).isSameAs(second)
    }
}
