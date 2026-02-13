package com.myrealtrip.common.utils.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UuidExtTest : FunSpec({

    context("extractTimestamp") {
        test("should extract timestamp close to current time from UUID v7") {
            // given
            val beforeMs = System.currentTimeMillis()
            val uuid = Uuid.generateV7()
            val afterMs = System.currentTimeMillis()

            // when
            val extractedTimestamp = uuid.extractTimestamp()

            // then
            extractedTimestamp shouldBeGreaterThan (beforeMs - 1)
            extractedTimestamp shouldBeLessThanOrEqual afterMs
        }

        test("should extract same timestamp for UUID generated at same millisecond") {
            // given
            val uuid1 = Uuid.generateV7()
            val timestamp1 = uuid1.extractTimestamp()

            // UUID v7 generated within same millisecond should have same timestamp (48-bit precision)
            val uuid2 = Uuid.generateV7()
            val timestamp2 = uuid2.extractTimestamp()

            // when & then
            val diff = kotlin.math.abs(timestamp2 - timestamp1)
            diff shouldBeLessThanOrEqual 1L // within 1ms tolerance
        }

        test("should extract increasing timestamps for UUIDs generated sequentially") {
            // given
            val uuid1 = Uuid.generateV7()
            Thread.sleep(10)
            val uuid2 = Uuid.generateV7()

            // when
            val timestamp1 = uuid1.extractTimestamp()
            val timestamp2 = uuid2.extractTimestamp()

            // then
            timestamp2 shouldBeGreaterThan timestamp1
        }
    }

    context("extractLocalDateTime with system default timezone") {
        test("should extract LocalDateTime close to current time") {
            // given
            val beforeDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val uuid = Uuid.generateV7()

            // when
            val extractedDateTime = uuid.extractLocalDateTime().truncatedTo(ChronoUnit.SECONDS)

            // then
            extractedDateTime shouldBe beforeDateTime
        }

        test("should return consistent result with extractTimestamp") {
            // given
            val uuid = Uuid.generateV7()

            // when
            val timestampMs = uuid.extractTimestamp()
            val localDateTime = uuid.extractLocalDateTime()

            // then
            val expectedDateTime = Instant.ofEpochMilli(timestampMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            localDateTime shouldBe expectedDateTime
        }
    }

    context("extractLocalDateTime with specified timezone") {
        test("should extract LocalDateTime in UTC timezone") {
            // given
            val uuid = Uuid.generateV7()
            val utcZone = ZoneId.of("UTC")

            // when
            val utcDateTime = uuid.extractLocalDateTime(utcZone)

            // then
            val expectedDateTime = Instant.ofEpochMilli(uuid.extractTimestamp())
                .atZone(utcZone)
                .toLocalDateTime()

            utcDateTime shouldBe expectedDateTime
        }

        test("should extract LocalDateTime in Asia/Seoul timezone") {
            // given
            val uuid = Uuid.generateV7()
            val seoulZone = ZoneId.of("Asia/Seoul")

            // when
            val seoulDateTime = uuid.extractLocalDateTime(seoulZone)

            // then
            val expectedDateTime = Instant.ofEpochMilli(uuid.extractTimestamp())
                .atZone(seoulZone)
                .toLocalDateTime()

            seoulDateTime shouldBe expectedDateTime
        }

        test("should return different LocalDateTime for different timezones") {
            // given
            val uuid = Uuid.generateV7()
            val utcZone = ZoneOffset.UTC
            val seoulZone = ZoneId.of("Asia/Seoul")

            // when
            val utcDateTime = uuid.extractLocalDateTime(utcZone)
            val seoulDateTime = uuid.extractLocalDateTime(seoulZone)

            // then (Seoul is UTC+9)
            val hoursDiff = java.time.Duration.between(utcDateTime, seoulDateTime).toHours()
            hoursDiff shouldBe 9L
        }
    }
})
