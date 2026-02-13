package com.myrealtrip.common.utils.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StringExtTest : FunSpec({

    context("ifNullOrBlank") {
        test("should return the string itself when not null or blank") {
            "hello".ifNullOrBlank() shouldBe "hello"
            "hello".ifNullOrBlank("default") shouldBe "hello"
        }

        test("should return default value when null") {
            val nullString: String? = null
            nullString.ifNullOrBlank() shouldBe ""
            nullString.ifNullOrBlank("default") shouldBe "default"
        }

        test("should return default value when blank") {
            "".ifNullOrBlank() shouldBe ""
            "".ifNullOrBlank("default") shouldBe "default"
            "   ".ifNullOrBlank("default") shouldBe "default"
        }
    }

    context("removeAllSpaces") {
        test("should remove all spaces from string") {
            "hello world".removeAllSpaces() shouldBe "helloworld"
            "  a  b  c  ".removeAllSpaces() shouldBe "abc"
            "no spaces".removeAllSpaces() shouldBe "nospaces"
        }

        test("should return empty string when input is only spaces") {
            "   ".removeAllSpaces() shouldBe ""
        }

        test("should return same string when no spaces") {
            "hello".removeAllSpaces() shouldBe "hello"
        }
    }

})
