package com.myrealtrip.common.utils

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.KnownException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PreconditionUtilTest : DescribeSpec({

    describe("knownRequired") {

        context("when condition is true") {
            it("should not throw exception") {
                shouldNotThrowAny {
                    knownRequired(true) { "This should not be thrown" }
                }
            }
        }

        context("when condition is false") {
            it("should throw KnownException with ILLEGAL_ARGUMENT code") {
                val exception = shouldThrow<KnownException> {
                    knownRequired(false) { "Validation failed" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
                exception.message shouldBe "Validation failed"
            }

            it("should evaluate lazy message only when throwing") {
                var messageEvaluated = false

                shouldThrow<KnownException> {
                    knownRequired(false) {
                        messageEvaluated = true
                        "Error message"
                    }
                }

                messageEvaluated shouldBe true
            }
        }
    }

    describe("knownRequiredNotNull") {

        context("when value is not null") {
            it("should return the value") {
                val result = knownRequiredNotNull("valid") { "Should not throw" }

                result shouldBe "valid"
            }

            it("should return the same object instance") {
                val original = listOf(1, 2, 3)
                val result = knownRequiredNotNull(original) { "Should not throw" }

                result shouldBe original
            }
        }

        context("when value is null") {
            it("should throw KnownException with ILLEGAL_ARGUMENT code") {
                val exception = shouldThrow<KnownException> {
                    knownRequiredNotNull<String>(null) { "Value must not be null" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
                exception.message shouldBe "Value must not be null"
            }
        }
    }

    describe("knownNotBlank") {

        context("when value is valid string") {
            it("should return the string") {
                val result = knownNotBlank("hello") { "Should not throw" }

                result shouldBe "hello"
            }

            it("should return string with whitespace if not blank") {
                val result = knownNotBlank("  hello  ") { "Should not throw" }

                result shouldBe "  hello  "
            }
        }

        context("when value is null") {
            it("should throw KnownException") {
                val exception = shouldThrow<KnownException> {
                    knownNotBlank(null) { "String must not be blank" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
                exception.message shouldBe "String must not be blank"
            }
        }

        context("when value is empty string") {
            it("should throw KnownException") {
                val exception = shouldThrow<KnownException> {
                    knownNotBlank("") { "String must not be empty" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
            }
        }

        context("when value is blank string") {
            it("should throw KnownException for whitespace only") {
                val exception = shouldThrow<KnownException> {
                    knownNotBlank("   ") { "String must not be blank" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
            }

            it("should throw KnownException for tabs and newlines") {
                val exception = shouldThrow<KnownException> {
                    knownNotBlank("\t\n") { "String must not be blank" }
                }

                exception.code shouldBe ErrorCode.ILLEGAL_ARGUMENT
            }
        }
    }
})
