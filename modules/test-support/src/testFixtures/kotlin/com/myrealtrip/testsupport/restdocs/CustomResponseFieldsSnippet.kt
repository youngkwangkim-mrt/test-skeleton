package com.myrealtrip.testsupport.restdocs

import org.springframework.http.MediaType
import org.springframework.restdocs.operation.Operation
import org.springframework.restdocs.payload.AbstractFieldsSnippet
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadSubsectionExtractor
import org.springframework.restdocs.snippet.Attributes

/**
 * Custom REST Docs snippet for documenting response fields with enum support
 */
class CustomResponseFieldsSnippet(
    type: String,
    subsectionExtractor: PayloadSubsectionExtractor<*>?,
    descriptors: List<FieldDescriptor>,
    attributes: Map<String, Any>,
    ignoreUndocumentedFields: Boolean,
) : AbstractFieldsSnippet(type, descriptors, attributes, ignoreUndocumentedFields, subsectionExtractor) {

    override fun getContentType(operation: Operation): MediaType? =
        operation.response.headers.contentType

    override fun getContent(operation: Operation): ByteArray =
        operation.response.content

    companion object {

        fun customResponseFields(
            type: String,
            subsectionExtractor: PayloadSubsectionExtractor<*>?,
            attributes: Map<String, Any>,
            vararg descriptors: FieldDescriptor,
        ): CustomResponseFieldsSnippet =
            CustomResponseFieldsSnippet(type, subsectionExtractor, descriptors.toList(), attributes, true)

        /**
         * Convert enum values map to field descriptors with status attribute
         */
        fun responseEnumConvertFieldDescriptor(
            enumValues: Map<String, List<String>>,
        ): Array<FieldDescriptor> =
            enumValues.map { (key, values) ->
                fieldWithPath(key)
                    .attributes(Attributes.Attribute("status", values[0]))
                    .description(values[1])
            }.toTypedArray()

        /**
         * Convert enum values map to field descriptors for status documentation
         */
        fun statusEnumConvertFieldDescriptor(
            enumValues: Map<String, List<String>>,
        ): Array<FieldDescriptor> =
            enumValues.map { (key, values) ->
                fieldWithPath(key)
                    .description(values.first())
            }.toTypedArray()
    }
}
