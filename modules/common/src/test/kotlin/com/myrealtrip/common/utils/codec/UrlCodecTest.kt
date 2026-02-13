package com.myrealtrip.common.utils.codec

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UrlCodecTest : FunSpec({

    context("encode") {
        test("should encode spaces as plus signs") {
            // given
            val value = "Hello World"

            // when
            val result = UrlCodec.encode(value)

            // then
            result shouldBe "Hello+World"
        }

        test("should encode special characters") {
            // given
            val value = "test&param=value"

            // when
            val result = UrlCodec.encode(value)

            // then
            result shouldBe "test%26param%3Dvalue"
        }

        test("should encode Korean characters") {
            // given
            val value = "안녕하세요"

            // when
            val result = UrlCodec.encode(value)

            // then
            result shouldNotBe value
            result shouldBe "%EC%95%88%EB%85%95%ED%95%98%EC%84%B8%EC%9A%94"
        }
    }

    context("decode") {
        test("should decode plus signs as spaces") {
            // given
            val value = "Hello+World"

            // when
            val result = UrlCodec.decode(value)

            // then
            result shouldBe "Hello World"
        }

        test("should decode percent-encoded characters") {
            // given
            val value = "test%26param%3Dvalue"

            // when
            val result = UrlCodec.decode(value)

            // then
            result shouldBe "test&param=value"
        }

        test("should decode Korean characters") {
            // given
            val value = "%EC%95%88%EB%85%95%ED%95%98%EC%84%B8%EC%9A%94"

            // when
            val result = UrlCodec.decode(value)

            // then
            result shouldBe "안녕하세요"
        }
    }

    context("normalize") {
        test("should normalize regular string") {
            // given
            val value = "Hello World!"

            // when
            val result = UrlCodec.normalize(value)

            // then
            result shouldBe value
        }

        test("should normalize URL-unsafe characters") {
            // given
            val value = "test&param=value"

            // when
            val result = UrlCodec.normalize(value)

            // then
            result shouldBe value
        }

        test("should normalize Korean characters") {
            // given
            val value = "안녕하세요"

            // when
            val result = UrlCodec.normalize(value)

            // then
            result shouldBe value
        }

        test("should normalize mixed content") {
            // given
            val value = "Hello 안녕 Test&Param=123"

            // when
            val result = UrlCodec.normalize(value)

            // then
            result shouldBe value
        }
    }

    context("roundtrip") {
        test("should encode and decode back to original") {
            // given
            val original = "Complex string: 한글 + special chars &?="

            // when
            val encoded = UrlCodec.encode(original)
            val decoded = UrlCodec.decode(encoded)

            // then
            decoded shouldBe original
        }
    }
})
