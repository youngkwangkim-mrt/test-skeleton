package com.myrealtrip.testsupport.restdocs

import com.myrealtrip.testsupport.restdocs.DocsFieldType.*
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.dataResponseFields
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper

/**
 * Base class for REST Docs tests
 *
 * Usage:
 * ```kotlin
 * class MyControllerDocsTest : RestDocsSupport() {
 *
 *     override fun initController(): Any = MyController(mockService)
 *
 *     @Test
 *     fun `should document get user API`() {
 *         mockMvc.perform(get("/api/users/1"))
 *             .andExpect(status().isOk)
 *             .andDo(restDocs.document(
 *                 responseFields(
 *                     *resourceCommonFormat().toTypedArray()
 *                 )
 *             ))
 *     }
 * }
 * ```
 */
@ExtendWith(RestDocumentationExtension::class)
abstract class RestDocsSupport {

    protected val jsonMapper: JsonMapper = JsonMapper.builder().build()
    protected val restDocs: RestDocumentationResultHandler = createRestDocumentationResultHandler()
    protected lateinit var mockMvc: MockMvc

    /**
     * Return the controller instance under test.
     * Dependencies should be mocked.
     */
    protected abstract fun initController(): Any

    @BeforeEach
    fun setUpMockMvc(provider: RestDocumentationContextProvider) {
        val docConfigurer = MockMvcRestDocumentation.documentationConfiguration(provider)
        docConfigurer.uris().withScheme("http").withHost("localhost").withPort(8080)

        this.mockMvc = MockMvcBuilders
            .standaloneSetup(initController())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .addFilters<StandaloneMockMvcBuilder>(CharacterEncodingFilter(Charsets.UTF_8.name(), true))
            .apply<StandaloneMockMvcBuilder>(docConfigurer)
            .alwaysDo<StandaloneMockMvcBuilder>(restDocs)
            .build()
    }

    private fun createRestDocumentationResultHandler(): RestDocumentationResultHandler =
        MockMvcRestDocumentation.document(
            "{class-name}/{method-name}",
            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
        )

    companion object {

        const val BEARER_TOKEN = "Bearer test-token"

        // =========================================================================
        // ApiResource common field descriptors
        // =========================================================================

        /**
         * API response wrapper: status, meta, data (object)
         */
        fun resourceCommonFormat(): List<FieldDescriptor> = listOf(
            "status" type OBJECT means "Status information",
            "meta" type OBJECT means "Meta information",
            "data" type OBJECT means "Response data",
        ).map { it.descriptor }

        /**
         * API response wrapper: status, meta, data (array)
         */
        fun resourceArrayCommonFormat(): List<FieldDescriptor> = listOf(
            "status" type OBJECT means "Status information",
            "meta" type OBJECT means "Meta information",
            "data" type ARRAY means "Response data array",
        ).map { it.descriptor }

        /**
         * API response wrapper: status, meta, data (string)
         *
         * Use for void/delete operations that return empty string data.
         */
        fun resourceStringCommonFormat(): List<FieldDescriptor> = listOf(
            "status" type OBJECT means "Status information",
            "meta" type OBJECT means "Meta information",
            "data" type STRING means "Response data",
        ).map { it.descriptor }

        /**
         * Status object fields: status, code, message
         */
        fun statusCommonFormat(): List<FieldDescriptor> = listOf(
            "status" type NUMBER means "HTTP status code",
            "code" type STRING means "Response code",
            "message" type STRING means "Response message",
        ).map { it.descriptor }

        /**
         * Meta object fields: traceId, appTraceId, responseTs, size
         */
        fun metaCommonFormat(): List<FieldDescriptor> = listOf(
            ("x-b3-traceid" type STRING means "Distributed trace ID") isOptional true,
            ("appTraceId" type STRING means "Application trace ID") isOptional true,
            ("responseTs" type NUMBER means "Response timestamp (epoch millis)") isOptional true,
            ("size" type NUMBER means "Collection size") isOptional true,
        ).map { it.descriptor }

        /**
         * Collection response meta: size
         */
        fun collectionCommonFormat(): List<FieldDescriptor> = listOf(
            "size" type NUMBER means "Total data count",
        ).map { it.descriptor }

        /**
         * Page response: pageInfo fields
         */
        fun pageCommonFormat(): List<FieldDescriptor> = listOf(
            "totalPages" type NUMBER means "Total page count",
            "totalElements" type NUMBER means "Total data count",
            "pageNumber" type NUMBER means "Current page number (starts from 0)",
            "pageElements" type NUMBER means "Data count in current page",
            "isFirst" type BOOLEAN means "First page flag",
            "isLast" type BOOLEAN means "Last page flag",
            "isEmpty" type BOOLEAN means "Empty data flag",
        ).map { it.descriptor }

        /**
         * No-offset page response: offsetInfo fields
         */
        fun noOffsetCommonFormat(): List<FieldDescriptor> = listOf(
            "lastIndex" type STRING means "Last index for cursor",
            "totalPages" type NUMBER means "Total page count",
            "totalElements" type NUMBER means "Total data count",
            "pageNumber" type NUMBER means "Current page number (starts from 0)",
            "pageElements" type NUMBER means "Data count in current page",
            "isLast" type BOOLEAN means "Last page flag",
            "isEmpty" type BOOLEAN means "Empty data flag",
        ).map { it.descriptor }

        /**
         * Common page request parameters
         */
        fun pageRequestFormat(): List<ParameterDescriptor> = listOf(
            parameterWithName("page").optional().description("Page number (start: 0, default: 0)"),
            parameterWithName("size").optional().description("Page size"),
        )

        // =========================================================================
        // Pre-prefixed common fields for responseFields()
        // =========================================================================

        /**
         * Common response fields for object data response.
         *
         * Includes: status (OBJECT), status.*, meta (OBJECT), meta.*, data (OBJECT)
         *
         * Usage:
         * ```kotlin
         * responseFields(
         *     *responseCommonFields(),
         *     *fields(
         *         "data.id" type NUMBER means "User ID",
         *     )
         * )
         * ```
         */
        fun responseCommonFields(): Array<FieldDescriptor> = arrayOf(
            *resourceCommonFormat().toTypedArray(),
            *applyPathPrefix("status.", statusCommonFormat()).toTypedArray(),
            *applyPathPrefix("meta.", metaCommonFormat()).toTypedArray(),
        )

        /**
         * Common response fields for array data response.
         *
         * Includes: status (OBJECT), status.*, meta (OBJECT), meta.*, data (ARRAY)
         *
         * Usage:
         * ```kotlin
         * responseFields(
         *     *responseArrayCommonFields(),
         *     *fields(
         *         "data[].id" type NUMBER means "Item ID",
         *     )
         * )
         * ```
         */
        fun responseArrayCommonFields(): Array<FieldDescriptor> = arrayOf(
            *resourceArrayCommonFormat().toTypedArray(),
            *applyPathPrefix("status.", statusCommonFormat()).toTypedArray(),
            *applyPathPrefix("meta.", metaCommonFormat()).toTypedArray(),
        )

        /**
         * Common response fields for string data response (e.g., void/delete operations).
         *
         * Includes: status (OBJECT), status.*, meta (OBJECT), meta.*, data (STRING)
         *
         * Usage:
         * ```kotlin
         * responseFields(*responseStringCommonFields())
         * ```
         */
        fun responseStringCommonFields(): Array<FieldDescriptor> = arrayOf(
            *resourceStringCommonFormat().toTypedArray(),
            *applyPathPrefix("status.", statusCommonFormat()).toTypedArray(),
            *applyPathPrefix("meta.", metaCommonFormat()).toTypedArray(),
        )

        // =========================================================================
        // Subsection-based common fields (splits response-fields / response-fields-data)
        // =========================================================================

        /**
         * Common response fields with data as subsection (object response).
         *
         * Generates `response-fields.adoc` with only status/meta fields.
         * Use with [dataResponseFields] to generate separate `response-fields-data.adoc`.
         *
         * Usage:
         * ```kotlin
         * restDocs.document(
         *     responseFields(*responseCommonFieldsSubsection()),
         *     dataResponseFields(
         *         "id" type NUMBER means "User ID",
         *         "name" type STRING means "User name",
         *     )
         * )
         * ```
         */
        fun responseCommonFieldsSubsection(): Array<FieldDescriptor> = arrayOf(
            *listOf(
                "status" type OBJECT means "Status information",
                "meta" type OBJECT means "Meta information",
            ).map { it.descriptor }.toTypedArray(),
            subsectionWithPath("data").type(JsonFieldType.OBJECT).description("Response data"),
            *applyPathPrefix("status.", statusCommonFormat()).toTypedArray(),
            *applyPathPrefix("meta.", metaCommonFormat()).toTypedArray(),
        )

        /**
         * Common response fields with data as subsection (array response).
         *
         * Generates `response-fields.adoc` with only status/meta fields.
         * Use with [dataResponseFields] to generate separate `response-fields-data.adoc`.
         *
         * Usage:
         * ```kotlin
         * restDocs.document(
         *     responseFields(*responseArrayCommonFieldsSubsection()),
         *     dataResponseFields(
         *         "id" type NUMBER means "Item ID",
         *         "title" type STRING means "Item title",
         *     )
         * )
         * ```
         */
        fun responseArrayCommonFieldsSubsection(): Array<FieldDescriptor> = arrayOf(
            *listOf(
                "status" type OBJECT means "Status information",
                "meta" type OBJECT means "Meta information",
            ).map { it.descriptor }.toTypedArray(),
            subsectionWithPath("data[]").type(JsonFieldType.ARRAY).description("Response data array"),
            *applyPathPrefix("status.", statusCommonFormat()).toTypedArray(),
            *applyPathPrefix("meta.", metaCommonFormat()).toTypedArray(),
        )

        /**
         * Generate `response-fields-data.adoc` snippet for data fields.
         *
         * Field paths are relative to `data` (e.g., `"id"` instead of `"data.id"`).
         *
         * Usage:
         * ```kotlin
         * restDocs.document(
         *     responseFields(*responseCommonFieldsSubsection()),
         *     dataResponseFields(
         *         "id" type NUMBER means "User ID",
         *         "name" type STRING means "User name",
         *     )
         * )
         * ```
         */
        fun dataResponseFields(vararg dataFields: Field): ResponseFieldsSnippet =
            responseFields(
                beneathPath("data").withSubsectionId("data"),
                *fields(*dataFields),
            )
    }
}
