package com.myrealtrip.common.exceptions

import com.myrealtrip.common.codes.response.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KnownExceptionTest {

    @Nested
    inner class ConstructorTests {

        @Test
        fun `should create exception with code only`(): Unit {
            // given
            val code = ErrorCode.NOT_FOUND

            // when
            val exception = KnownException(code)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(code.message)
            assertThat(exception.cause).isNull()
        }

        @Test
        fun `should create exception with custom message`(): Unit {
            // given
            val code = ErrorCode.DATA_NOT_FOUND
            val customMessage = "User with ID 123 not found"

            // when
            val exception = KnownException(code, customMessage)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.message).isEqualTo(customMessage)
        }

        @Test
        fun `should create exception with cause`(): Unit {
            // given
            val code = ErrorCode.NOT_FOUND
            val cause = IllegalArgumentException("Invalid resource identifier")

            // when
            val exception = KnownException(code, code.message, cause)

            // then
            assertThat(exception.code).isEqualTo(code)
            assertThat(exception.cause).isEqualTo(cause)
        }
    }

    @Nested
    inner class LogStackTraceTests {

        @Test
        fun `should always have logStackTrace false`(): Unit {
            // given & when
            val exception1 = KnownException(ErrorCode.NOT_FOUND)
            val exception2 = KnownException(ErrorCode.INVALID_ARGUMENT, "Custom message")
            val exception3 = KnownException(ErrorCode.DATA_NOT_FOUND, "Message", RuntimeException())

            // then
            assertThat(exception1.logStackTrace).isFalse()
            assertThat(exception2.logStackTrace).isFalse()
            assertThat(exception3.logStackTrace).isFalse()
        }
    }

    @Nested
    inner class InheritanceTests {

        @Test
        fun `should extend BizRuntimeException`(): Unit {
            // given
            val exception = KnownException(ErrorCode.NOT_FOUND)

            // then
            assertThat(exception).isInstanceOf(BizRuntimeException::class.java)
        }

        @Test
        fun `should be unchecked exception`(): Unit {
            // given
            val exception = KnownException(ErrorCode.NOT_FOUND)

            // then
            assertThat(exception).isInstanceOf(RuntimeException::class.java)
        }

        @Test
        fun `should implement BizExceptionInfo`(): Unit {
            // given
            val exception = KnownException(ErrorCode.NOT_FOUND)

            // then
            assertThat(exception).isInstanceOf(BizExceptionInfo::class.java)
        }
    }

    @Nested
    inner class DescribeTests {

        @Test
        fun `should return formatted description`(): Unit {
            // given
            val code = ErrorCode.NOT_FOUND
            val customMessage = "Product not found: SKU-12345"
            val exception = KnownException(code, customMessage)

            // when
            val description = exception.describe()

            // then
            assertThat(description).isEqualTo("KnownException[NOT_FOUND]: Product not found: SKU-12345")
        }
    }

    @Nested
    inner class ExtensionTests {

        @Test
        fun `should allow extending KnownException for domain-specific errors`(): Unit {
            // given
            class UserNotFoundException(userId: Long) : KnownException(
                code = ErrorCode.DATA_NOT_FOUND,
                message = "User not found: $userId",
            )

            // when
            val exception = UserNotFoundException(42)

            // then
            assertThat(exception.code).isEqualTo(ErrorCode.DATA_NOT_FOUND)
            assertThat(exception.message).isEqualTo("User not found: 42")
            assertThat(exception.logStackTrace).isFalse()
            assertThat(exception.describe()).isEqualTo("UserNotFoundException[DATA_NOT_FOUND]: User not found: 42")
        }

        @Test
        fun `extended exception should inherit logStackTrace false behavior`(): Unit {
            // given
            class ValidationException(field: String, reason: String) : KnownException(
                code = ErrorCode.INVALID_ARGUMENT,
                message = "Validation failed for '$field': $reason",
            )

            // when
            val exception = ValidationException("email", "invalid format")

            // then
            assertThat(exception.logStackTrace).isFalse()
        }
    }

    @Nested
    inner class UseCaseTests {

        @Test
        fun `should be suitable for expected validation errors`(): Unit {
            // given
            val validationError = KnownException(
                ErrorCode.INVALID_ARGUMENT,
                "Email format is invalid",
            )

            // then
            assertThat(validationError.code.status).isEqualTo(400)
            assertThat(validationError.logStackTrace).isFalse()
        }

        @Test
        fun `should be suitable for resource not found errors`(): Unit {
            // given
            val notFoundError = KnownException(
                ErrorCode.NOT_FOUND,
                "Resource with ID 999 not found",
            )

            // then
            assertThat(notFoundError.code.status).isEqualTo(404)
            assertThat(notFoundError.logStackTrace).isFalse()
        }

        @Test
        fun `should be suitable for authorization errors`(): Unit {
            // given
            val forbiddenError = KnownException(
                ErrorCode.FORBIDDEN,
                "User does not have permission to access this resource",
            )

            // then
            assertThat(forbiddenError.code.status).isEqualTo(403)
            assertThat(forbiddenError.logStackTrace).isFalse()
        }
    }
}
