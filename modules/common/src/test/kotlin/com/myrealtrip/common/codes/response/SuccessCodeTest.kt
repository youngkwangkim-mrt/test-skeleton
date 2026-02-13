package com.myrealtrip.common.codes.response

import com.myrealtrip.common.codes.ResponseCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuccessCodeTest {

    @Test
    fun `SUCCESS should have status 200`(): Unit {
        // given
        val code = SuccessCode.SUCCESS

        // when
        val status = code.status

        // then
        assertThat(status).isEqualTo(200)
        assertThat(code.message).isEqualTo("성공")
    }

    @Test
    fun `CREATED should have status 201`(): Unit {
        // given
        val code = SuccessCode.CREATED

        // when
        val status = code.status

        // then
        assertThat(status).isEqualTo(201)
        assertThat(code.message).isEqualTo("생성 성공")
    }

    @Test
    fun `ACCEPTED should have status 202`(): Unit {
        // given
        val code = SuccessCode.ACCEPTED

        // when
        val status = code.status

        // then
        assertThat(status).isEqualTo(202)
        assertThat(code.message).isEqualTo("접수 성공")
    }

    @Test
    fun `should implement ResponseCode interface`(): Unit {
        // given
        val code = SuccessCode.SUCCESS

        // when & then
        assertThat(code).isInstanceOf(ResponseCode::class.java)
    }

    @Test
    fun `all codes should have 2xx status`(): Unit {
        // given
        val codes = SuccessCode.entries

        // when & then
        assertThat(codes).allSatisfy { code ->
            assertThat(code.status).isBetween(200, 299)
        }
    }

}
