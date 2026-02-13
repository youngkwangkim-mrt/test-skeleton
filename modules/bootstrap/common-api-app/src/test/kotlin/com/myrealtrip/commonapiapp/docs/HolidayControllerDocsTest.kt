package com.myrealtrip.commonapiapp.docs

import com.myrealtrip.commonapiapp.api.HolidayController
import com.myrealtrip.commonapiapp.dto.response.HolidayDto
import com.myrealtrip.commonapiapp.dto.response.HolidaysResponse
import com.myrealtrip.commonapiapp.facade.HolidayFacade
import com.myrealtrip.domain.holiday.dto.HolidayInfo
import com.myrealtrip.testsupport.restdocs.DocsFieldType.ARRAY
import com.myrealtrip.testsupport.restdocs.DocsFieldType.DATE
import com.myrealtrip.testsupport.restdocs.DocsFieldType.NUMBER
import com.myrealtrip.testsupport.restdocs.DocsFieldType.STRING
import com.myrealtrip.testsupport.restdocs.RestDocsSupport
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.dataResponseFields
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.pageCommonFormat
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.pageRequestFormat
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.responseArrayCommonFieldsSubsection
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.responseCommonFieldsSubsection
import com.myrealtrip.testsupport.restdocs.RestDocsSupport.Companion.responseStringCommonFields
import com.myrealtrip.testsupport.restdocs.fields
import com.myrealtrip.testsupport.restdocs.type
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.beneathPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class HolidayControllerDocsTest : RestDocsSupport() {

    private val holidayFacade: HolidayFacade = mock()

    override fun initController(): Any = HolidayController(holidayFacade)

    @Test
    fun `get holidays by year`(): Unit {
        // given
        val holidays = listOf(
            HolidayDto(1L, LocalDate.of(2026, 2, 16), "설날"),
            HolidayDto(2L, LocalDate.of(2026, 2, 17), "설날"),
            HolidayDto(3L, LocalDate.of(2026, 3, 1), "삼일절"),
        )
        val page = PageImpl(holidays, PageRequest.of(0, 20), holidays.size.toLong())
        given(holidayFacade.findPageByYear(any(), any())).willReturn(page)

        // when & then
        mockMvc.perform(
            get("/api/holidays/{year}", 2026)
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    pathParameters(
                        parameterWithName("year").description("조회할 연도"),
                    ),
                    queryParameters(
                        *pageRequestFormat().toTypedArray(),
                    ),
                    responseFields(
                        *responseArrayCommonFieldsSubsection(),
                        subsectionWithPath("meta.pageInfo").type(JsonFieldType.OBJECT)
                            .optional().description("페이지 정보"),
                    ),
                    responseFields(
                        beneathPath("meta.pageInfo").withSubsectionId("data.page"),
                        *pageCommonFormat().toTypedArray(),
                    ),
                    dataResponseFields(
                        "id" type NUMBER means "공휴일 ID",
                        "holidayDate" type DATE means "공휴일 날짜",
                        "name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `get holidays by year and month`(): Unit {
        // given
        val holidays = listOf(
            HolidayDto(1L, LocalDate.of(2026, 2, 16), "설날"),
            HolidayDto(2L, LocalDate.of(2026, 2, 17), "설날"),
        )
        val page = PageImpl(holidays, PageRequest.of(0, 20), holidays.size.toLong())
        given(holidayFacade.findPageByYearAndMonth(any(), any(), any())).willReturn(page)

        // when & then
        mockMvc.perform(
            get("/api/holidays/{year}/{month}", 2026, 2)
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    pathParameters(
                        parameterWithName("year").description("조회할 연도"),
                        parameterWithName("month").description("조회할 월"),
                    ),
                    queryParameters(
                        *pageRequestFormat().toTypedArray(),
                    ),
                    responseFields(
                        *responseArrayCommonFieldsSubsection(),
                        subsectionWithPath("meta.pageInfo").type(JsonFieldType.OBJECT)
                            .optional().description("페이지 정보"),
                    ),
                    responseFields(
                        beneathPath("meta.pageInfo").withSubsectionId("data.page"),
                        *pageCommonFormat().toTypedArray(),
                    ),
                    dataResponseFields(
                        "id" type NUMBER means "공휴일 ID",
                        "holidayDate" type DATE means "공휴일 날짜",
                        "name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `get holidays by date`(): Unit {
        // given
        val response = HolidaysResponse.from(
            listOf(
                HolidayInfo(1L, LocalDate.of(2026, 2, 16), "설날"),
            )
        )
        given(holidayFacade.findByDate(2026, 2, 16)).willReturn(response)

        // when & then
        mockMvc.perform(get("/api/holidays/{year}/{month}/{day}", 2026, 2, 16))
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    pathParameters(
                        parameterWithName("year").description("조회할 연도"),
                        parameterWithName("month").description("조회할 월"),
                        parameterWithName("day").description("조회할 일"),
                    ),
                    responseFields(*responseCommonFieldsSubsection()),
                    dataResponseFields(
                        "holidays" type ARRAY means "공휴일 목록",
                        "holidays[].date" type DATE means "공휴일 날짜",
                        "holidays[].name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `create holiday`(): Unit {
        // given
        val holidayDto = HolidayDto(1L, LocalDate.of(2026, 1, 1), "신정")
        given(holidayFacade.create(any())).willReturn(holidayDto)

        val request = """
            {
                "holidayDate": "2026-01-01",
                "name": "신정"
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            post("/api/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    requestFields(
                        *fields(
                            "holidayDate" type DATE means "공휴일 날짜",
                            "name" type STRING means "공휴일 이름",
                        )
                    ),
                    responseFields(*responseCommonFieldsSubsection()),
                    dataResponseFields(
                        "id" type NUMBER means "공휴일 ID",
                        "holidayDate" type DATE means "공휴일 날짜",
                        "name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `create holidays bulk`(): Unit {
        // given
        val holidayDtos = listOf(
            HolidayDto(1L, LocalDate.of(2026, 2, 16), "설날"),
            HolidayDto(2L, LocalDate.of(2026, 2, 17), "설날"),
        )
        given(holidayFacade.createAll(any())).willReturn(holidayDtos)

        val request = """
            {
                "holidays": [
                    { "holidayDate": "2026-02-16", "name": "설날" },
                    { "holidayDate": "2026-02-17", "name": "설날" }
                ]
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            post("/api/holidays/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    requestFields(
                        *fields(
                            "holidays" type ARRAY means "생성할 공휴일 목록",
                            "holidays[].holidayDate" type DATE means "공휴일 날짜",
                            "holidays[].name" type STRING means "공휴일 이름",
                        )
                    ),
                    responseFields(*responseArrayCommonFieldsSubsection()),
                    dataResponseFields(
                        "id" type NUMBER means "공휴일 ID",
                        "holidayDate" type DATE means "공휴일 날짜",
                        "name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `update holiday`(): Unit {
        // given
        val holidayDto = HolidayDto(1L, LocalDate.of(2026, 2, 16), "설날 (수정)")
        given(holidayFacade.update(any(), any())).willReturn(holidayDto)

        val request = """
            {
                "holidayDate": "2026-02-16",
                "name": "설날 (수정)"
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            put("/api/holidays/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    pathParameters(
                        parameterWithName("id").description("수정할 공휴일 ID"),
                    ),
                    requestFields(
                        *fields(
                            "holidayDate" type DATE means "공휴일 날짜",
                            "name" type STRING means "공휴일 이름",
                        )
                    ),
                    responseFields(*responseCommonFieldsSubsection()),
                    dataResponseFields(
                        "id" type NUMBER means "공휴일 ID",
                        "holidayDate" type DATE means "공휴일 날짜",
                        "name" type STRING means "공휴일 이름",
                    ),
                )
            )
    }

    @Test
    fun `delete holiday`(): Unit {
        // given
        doNothing().`when`(holidayFacade).delete(any())

        // when & then
        mockMvc.perform(delete("/api/holidays/{id}", 1L))
            .andExpect(status().isOk)
            .andDo(
                restDocs.document(
                    pathParameters(
                        parameterWithName("id").description("삭제할 공휴일 ID"),
                    ),
                    responseFields(*responseStringCommonFields()),
                )
            )
    }
}
