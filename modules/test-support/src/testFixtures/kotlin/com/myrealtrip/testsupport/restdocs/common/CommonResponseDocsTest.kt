package com.myrealtrip.testsupport.restdocs.common

import com.myrealtrip.testsupport.restdocs.RestDocsSupport
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

open class CommonResponseDocsTest : RestDocsSupport() {

    override fun initController(): Any = CommonResponseController()

    @Test
    open fun `common response format`() {
        mockMvc.perform(get("/docs/response"))
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    responseFields(
                        subsectionWithPath("status").type(JsonFieldType.OBJECT).description("상태 정보"),
                        subsectionWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
                        fieldWithPath("data").type(JsonFieldType.VARIES).optional().description("데이터"),
                    ),
                    responseFields(
                        beneathPath("status").withSubsectionId("data.status"),
                        *statusCommonFormat().toTypedArray(),
                    ),
                    responseFields(
                        beneathPath("meta").withSubsectionId("data.meta"),
                        *metaCommonFormat().toTypedArray(),
                    ),
                )
            )
    }

    @Test
    open fun `common page format`() {
        mockMvc.perform(get("/docs/response/page"))
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    responseFields(
                        *responseArrayCommonFields(),
                        subsectionWithPath("meta.pageInfo").type(JsonFieldType.OBJECT)
                            .optional().description("페이지 정보"),
                    ),
                    responseFields(
                        beneathPath("meta.pageInfo").withSubsectionId("data.page"),
                        *pageCommonFormat().toTypedArray(),
                    ),
                )
            )
    }

    @Test
    open fun `common no offset page format`() {
        mockMvc.perform(get("/docs/response/no-offset-page"))
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    responseFields(
                        *responseArrayCommonFields(),
                        subsectionWithPath("meta.offsetInfo").type(JsonFieldType.OBJECT)
                            .optional().description("오프셋 정보 (커서 기반 페이지네이션)"),
                    ),
                    responseFields(
                        beneathPath("meta.offsetInfo").withSubsectionId("data.no-offset"),
                        *noOffsetCommonFormat().toTypedArray(),
                    ),
                )
            )
    }
}
