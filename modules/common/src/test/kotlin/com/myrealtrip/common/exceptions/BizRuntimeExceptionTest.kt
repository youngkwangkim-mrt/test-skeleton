package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.response.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BizRuntimeExceptionTest {

    @Nested
    inner class ConstructorTests {

        @Test
        fun `should create exception with code only`(): Unit {
            // given
            val code = ErrorCode.ILLEGAL_STATE

            // when
            val exception = BizRuntimeException(code)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(code.message)
            assertThat(exception.cause).isNull()
            assertThat(exception.logStackTrace).isFalse()
        }

        @Test
        fun `should create exception with custom message`(): Unit {
            // given
            val code = ErrorCode.ILLEGAL_STATE
            val customMessage = "Cannot cancel completed order"

            // when
            val exception = BizRuntimeException(code, customMessage)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(customMessage)
            assertThat(exception.cause).isNull()
            assertThat(exception.logStackTrace).isFalse()
        }

        @Test
        fun `should create exception with cause`(): Unit {
            // given
            val code = ErrorCode.DB_ACCESS_ERROR
            val cause = IllegalStateException("Connection pool exhausted")

            // when
            val exception = BizRuntimeException(code, code.message, cause)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.cause).isEqualTo(cause)
        }

        @Test
        fun `should create exception with logStackTrace enabled`(): Unit {
            // given
            val code = ErrorCode.SERVER_ERROR

            // when
            val exception = BizRuntimeException(code, code.message, null, true)

            // then
            assertThat(exception.logStackTrace).isTrue()
        }
    }

    @Nested
    inner class InheritanceTests {

        @Test
        fun `should be unchecked exception extending RuntimeException`(): Unit {
            // given
            val exception = BizRuntimeException(ErrorCode.ILLEGAL_STATE)

            // then
            assertThat(exception).isInstanceOf(RuntimeException::class.java)
        }

        @Test
        fun `should implement BizExceptionInfo`(): Unit {
            // given
            val exception = BizRuntimeException(ErrorCode.ILLEGAL_STATE)

            // then
            assertThat(exception).isInstanceOf(BizExceptionInfo::class.java)
        }
    }

    @Nested
    inner class DescribeTests {

        @Test
        fun `should return formatted description`(): Unit {
            // given
            val code = ErrorCode.ILLEGAL_STATE
            val customMessage = "Invalid state transition"
            val exception = BizRuntimeException(code, customMessage)

            // when
            val description = exception.describe()

            // then
            assertThat(description).isEqualTo("BizRuntimeException[ILLEGAL_STATE]: Invalid state transition")
        }

        @Test
        fun `should return description with default message`(): Unit {
            // given
            val code = ErrorCode.SERVER_ERROR
            val exception = BizRuntimeException(code)

            // when
            val description = exception.describe()

            // then
            assertThat(description).isEqualTo("BizRuntimeException[SERVER_ERROR]: 일시적인 오류입니다. 잠시 후 다시 시도해주세요.")
        }
    }

    @Nested
    inner class ExtensionTests {

        @Test
        fun `should allow extending BizRuntimeException`(): Unit {
            // given
            class PaymentFailedException(reason: String) : BizRuntimeException(
                code = ErrorCode.CALL_RESPONSE_ERROR,
                message = "Payment failed: $reason",
                logStackTrace = true,
            )

            // when
            val exception = PaymentFailedException("Insufficient balance")

            // then
            assertThat(exception.code).isEqualTo(ErrorCode.CALL_RESPONSE_ERROR)
            assertThat(exception.message).isEqualTo("Payment failed: Insufficient balance")
            assertThat(exception.logStackTrace).isTrue()
            assertThat(exception.describe()).isEqualTo("PaymentFailedException[CALL_RESPONSE_ERROR]: Payment failed: Insufficient balance")
        }
    }

    @Nested
    inner class ExceptionChainTests {

        @Test
        fun `should preserve exception chain`(): Unit {
            // given
            val rootCause = NullPointerException("Unexpected null value")
            val intermediateCause = IllegalStateException("Processing failed", rootCause)

            // when
            val exception = BizRuntimeException(
                code = ErrorCode.SERVER_ERROR,
                message = "Operation failed",
                cause = intermediateCause,
            )

            // then
            assertThat(exception.cause).isEqualTo(intermediateCause)
            assertThat(exception.cause?.cause).isEqualTo(rootCause)
        }
    }
}
