package com.myrealtrip.testsupport.restdocs

import org.springframework.restdocs.payload.JsonFieldType
import kotlin.reflect.KClass

/**
 * Type-safe field type definitions for REST Docs
 */
sealed class DocsFieldType(val type: JsonFieldType) {

    data object ARRAY : DocsFieldType(JsonFieldType.ARRAY)
    data object BOOLEAN : DocsFieldType(JsonFieldType.BOOLEAN)
    data object NUMBER : DocsFieldType(JsonFieldType.NUMBER)
    data object OBJECT : DocsFieldType(JsonFieldType.OBJECT)
    data object STRING : DocsFieldType(JsonFieldType.STRING)
    data object NULL : DocsFieldType(JsonFieldType.NULL)
    data object VARIES : DocsFieldType(JsonFieldType.VARIES)
    data object DATE : DocsFieldType(JsonFieldType.STRING)
    data object DATETIME : DocsFieldType(JsonFieldType.STRING)

    class ENUM<T : Enum<T>>(
        val enums: Collection<T>,
    ) : DocsFieldType(JsonFieldType.STRING) {

        constructor(clazz: KClass<T>) : this(clazz.java.enumConstants.toList())
    }
}
