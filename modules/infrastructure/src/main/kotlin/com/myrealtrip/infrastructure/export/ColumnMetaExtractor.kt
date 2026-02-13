package com.myrealtrip.infrastructure.export

import com.myrealtrip.infrastructure.export.annotation.ExportColumn
import com.myrealtrip.infrastructure.export.annotation.ExportSheet
import com.myrealtrip.infrastructure.export.annotation.OverflowStrategy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * 어노테이션에서 Export 메타데이터를 추출
 */
object ColumnMetaExtractor {

    private val sheetMetaCache = ConcurrentHashMap<KClass<*>, SheetMeta>()
    private val columnMetaCache = ConcurrentHashMap<KClass<*>, List<ColumnMeta>>()

    /**
     * 클래스에서 시트 메타데이터 추출
     */
    fun extractSheetMeta(clazz: KClass<*>): SheetMeta {
        return sheetMetaCache.getOrPut(clazz) {
            val annotation = clazz.findAnnotation<ExportSheet>()
            SheetMeta(
                name = annotation?.name ?: "Sheet1",
                freezeHeader = annotation?.freezeHeader ?: true,
                includeIndex = annotation?.includeIndex ?: false,
                indexHeader = annotation?.indexHeader ?: "No.",
                indexWidth = annotation?.indexWidth ?: 6,
                overflowStrategy = annotation?.overflowStrategy ?: OverflowStrategy.MULTI_SHEET,
            )
        }
    }

    /**
     * 클래스에서 컬럼 메타데이터 목록 추출 (order 순 정렬)
     */
    fun extractColumnMetas(clazz: KClass<*>): List<ColumnMeta> {
        return columnMetaCache.getOrPut(clazz) {
            clazz.memberProperties
                .mapNotNull { property -> createColumnMeta(property) }
                .sortedBy { it.order }
        }
    }

    private fun createColumnMeta(property: KProperty1<*, *>): ColumnMeta? {
        val annotation = property.findAnnotation<ExportColumn>() ?: return null

        // Java 클래스의 private 필드 접근 허용
        property.isAccessible = true

        return ColumnMeta(
            header = annotation.header,
            order = annotation.order,
            width = annotation.width,
            format = annotation.format,
            property = property,
            headerStyle = annotation.headerStyle,
            bodyStyle = annotation.bodyStyle,
        )
    }

    /**
     * 캐시 초기화 (테스트용)
     */
    fun clearCache() {
        sheetMetaCache.clear()
        columnMetaCache.clear()
    }
}
