package com.myrealtrip.testsupport.restdocs

import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

/**
 * Attribute key constants for REST Docs custom attributes
 */
object RestDocsAttributes {
    const val KEY_FORMAT = "format"
    const val KEY_SAMPLE = "sample"
    const val KEY_DEFAULT_VALUE = "defaultValue"

    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val DATETIME_MS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS"

    fun emptyAttribute(key: String) = org.springframework.restdocs.snippet.Attributes.key(key).value("")
    fun attribute(key: String, value: String) = org.springframework.restdocs.snippet.Attributes.key(key).value(value)
}

/**
 * Chainable field descriptor builder for REST Docs DSL.
 *
 * Supports property-based access for format, sample, and default value attributes,
 * and infix functions for fluent chaining:
 *
 * ```kotlin
 * "data.id" type NUMBER means "User ID" example "12345"
 * "data.createdAt" type DATETIME means "Created date"
 * "data.status" type ENUM(Status::class) means "Order status"
 * "data.name" type STRING means "User name" isOptional true withDefaultValue "Unknown"
 * ```
 */
open class Field(val descriptor: FieldDescriptor) {

    var format: String
        get() = descriptor.attributes.getOrDefault(RestDocsAttributes.KEY_FORMAT, "") as String
        set(value) {
            descriptor.attributes(RestDocsAttributes.attribute(RestDocsAttributes.KEY_FORMAT, value))
        }

    var sample: String
        get() = descriptor.attributes.getOrDefault(RestDocsAttributes.KEY_SAMPLE, "") as String
        set(value) {
            descriptor.attributes(RestDocsAttributes.attribute(RestDocsAttributes.KEY_SAMPLE, value))
        }

    var default: String
        get() = descriptor.attributes.getOrDefault(RestDocsAttributes.KEY_DEFAULT_VALUE, "") as String
        set(value) {
            descriptor.attributes(RestDocsAttributes.attribute(RestDocsAttributes.KEY_DEFAULT_VALUE, value))
        }

    infix fun means(description: String): Field {
        descriptor.description(description)
        return this
    }

    infix fun attributes(block: Field.() -> Unit): Field {
        block()
        return this
    }

    infix fun formattedAs(value: String): Field {
        format = value
        return this
    }

    infix fun example(value: String): Field {
        sample = value
        return this
    }

    infix fun withDefaultValue(value: String): Field {
        default = value
        return this
    }

    infix fun isOptional(value: Boolean): Field {
        if (value) descriptor.optional()
        return this
    }

    infix fun isIgnored(value: Boolean): Field {
        if (value) descriptor.ignored()
        return this
    }
}

/**
 * Create a field with type using infix notation.
 *
 * Automatically applies format attributes for DATE, DATETIME types,
 * and generates enum value listing for ENUM type.
 *
 * Usage:
 * ```kotlin
 * "data.id" type NUMBER means "User ID"
 * "data.createdAt" type DATETIME
 * "data.status" type ENUM(Status::class)
 * ```
 */
infix fun String.type(docsFieldType: DocsFieldType): Field {
    val descriptor = createField(this, docsFieldType.type)
    val field = Field(descriptor)
    when (docsFieldType) {
        is DocsFieldType.DATE -> field.format = RestDocsAttributes.DATE_FORMAT
        is DocsFieldType.DATETIME -> field.format = RestDocsAttributes.DATETIME_FORMAT
        is DocsFieldType.ENUM<*> -> field.format = docsFieldType.enums.joinToString(" / ") { "`${it.name}`" }
        else -> {}
    }
    return field
}

/**
 * Convert a list of [Field] to an array of [FieldDescriptor] for use with REST Docs snippets.
 *
 * Usage:
 * ```kotlin
 * responseFields(
 *     *fields(
 *         "data.id" type NUMBER means "User ID",
 *         "data.name" type STRING means "User name",
 *     )
 * )
 * ```
 */
fun fields(vararg fields: Field): Array<FieldDescriptor> =
    fields.map { it.descriptor }.toTypedArray()

private fun createField(path: String, type: JsonFieldType): FieldDescriptor =
    fieldWithPath(path)
        .type(type)
        .description("")
        .attributes(
            RestDocsAttributes.emptyAttribute(RestDocsAttributes.KEY_FORMAT),
            RestDocsAttributes.emptyAttribute(RestDocsAttributes.KEY_SAMPLE),
            RestDocsAttributes.emptyAttribute(RestDocsAttributes.KEY_DEFAULT_VALUE),
        )
