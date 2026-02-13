package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.response.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BizExceptionTest {

    @Nested
    inner class ConstructorTests {

        @Test
        fun `should create exception with code only`(): Unit {
            // given
            val code = ErrorCode.INVALID_ARGUMENT

            // when
            val exception = BizException(code)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(code.message)
            assertThat(exception.cause).isNull()
            assertThat(exception.logStackTrace).isFalse()
        }

        @Test
        fun `should create exception with custom message`(): Unit {
            // given
            val code = ErrorCode.INVALID_ARGUMENT
            val customMessage = "User ID must be positive"

            // when
            val exception = BizException(code, customMessage)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(customMessage)
            assertThat(exception.cause).isNull()
            assertThat(exception.logStackTrace).isFalse()
        }

        @Test
        fun `should create exception with cause`(): Unit {
            // given
            val code = ErrorCode.SERVER_ERROR
            val cause = RuntimeException("Database connection failed")

            // when
            val exception = BizException(code, code.message, cause)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.cause).isEqualTo(cause)
        }

        @Test
        fun `should create exception with logStackTrace enabled`(): Unit {
            // given
            val code = ErrorCode.SERVER_ERROR

            // when
            val exception = BizException(code, code.message, null, true)

            // then
            assertThat(exception.logStackTrace).isTrue()
        }
    }

    @Nested
    inner class InheritanceTests {

        @Test
        fun `should be checked exception extending Exception`(): Unit {
            // given
            val exception = BizException(ErrorCode.INVALID_ARGUMENT)

            // then
            assertThat(exception).isInstanceOf(Exception::class.java)
            assertThat(exception).isNotInstanceOf(RuntimeException::class.java)
        }

        @Test
        fun `should implement BizExceptionInfo`(): Unit {
            // given
            val exception = BizException(ErrorCode.INVALID_ARGUMENT)

            // then
            assertThat(exception).isInstanceOf(BizExceptionInfo::class.java)
        }
    }

    @Nested
    inner class DescribeTests {

        @Test
        fun `should return formatted description`(): Unit {
            // given
            val code = ErrorCode.DATA_NOT_FOUND
            val customMessage = "Order not found: 12345"
            val exception = BizException(code, customMessage)

            // when
            val description = exception.describe()

            // then
            assertThat(description).isEqualTo("BizException[DATA_NOT_FOUND]: Order not found: 12345")
        }

        @Test
        fun `should return description with default message`(): Unit {
            // given
            val code = ErrorCode.UNAUTHORIZED
            val exception = BizException(code)

            // when
            val description = exception.describe()

            // then
            assertThat(description).isEqualTo("BizException[UNAUTHORIZED]: 인증이 필요합니다.")
        }
    }

    @Nested
    inner class ExtensionTests {

        @Test
        fun `should allow extending BizException`(): Unit {
            // given
            class OrderNotFoundException(orderId: Long) : BizException(
                code = ErrorCode.DATA_NOT_FOUND,
                message = "Order not found: $orderId",
            )

            // when
            val exception = OrderNotFoundException(12345)

            // then
            assertThat(exception.code).isEqualTo(ErrorCode.DATA_NOT_FOUND)
            assertThat(exception.message).isEqualTo("Order not found: 12345")
            assertThat(exception.describe()).isEqualTo("OrderNotFoundException[DATA_NOT_FOUND]: Order not found: 12345")
        }
    }
}
