package com.myrealtrip.testsupport.restdocs.common

import com.myrealtrip.testsupport.restdocs.CustomResponseFieldsSnippet.Companion.customResponseFields
import com.myrealtrip.testsupport.restdocs.CustomResponseFieldsSnippet.Companion.responseEnumConvertFieldDescriptor
import com.myrealtrip.testsupport.restdocs.RestDocsSupport
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.PayloadDocumentation.beneathPath
import org.springframework.restdocs.snippet.Attributes.attributes
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

open class CommonCodeDocsTest : RestDocsSupport() {

    override fun initController(): Any = CommonResponseController()

    @Test
    open fun `response codes`() {
        // given
        val result = mockMvc.perform(
            get("/docs/response-codes")
                .accept(MediaType.APPLICATION_JSON)
        )

        val mvcResult = result.andReturn()
        val codeDocs = getResponseCodeDocs(mvcResult)

        val snippetName = "common-response-code"

        // when & then
        result.andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    customResponseFields(
                        snippetName,
                        beneathPath("data.successCode").withSubsectionId("success"),
                        attributes(key("code").value("description")),
                        *responseEnumConvertFieldDescriptor(codeDocs.successCode),
                    ),
                    customResponseFields(
                        snippetName,
                        beneathPath("data.errorCode").withSubsectionId("error"),
                        attributes(key("code").value("description")),
                        *responseEnumConvertFieldDescriptor(codeDocs.errorCode),
                    ),
                )
            )
    }

    private fun getResponseCodeDocs(result: org.springframework.test.web.servlet.MvcResult): ResponseCodeDocs {
        val mapper = JsonMapper.builder().build()
        val tree = mapper.readTree(result.response.contentAsByteArray)
        val dataNode = tree["data"]
        return mapper.treeToValue(dataNode, ResponseCodeDocs::class.java)
    }
}
