package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import java.time.Instant
import java.util.UUID

/**
 * Basic unit tests for Affirmation serialization and deserialization.
 *
 * These tests verify core functionality with specific examples.
 */
class AffirmationSerializationBasicTest : FunSpec({
    val serializer = AffirmationSerializer()
    val parser = AffirmationParser()

    test("Serialize affirmation to JSON") {
        val affirmation = Affirmation(
            id = "aff-123",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great job!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3,
            createdAt = Instant.ofEpochMilli(1000000000000L)
        )

        val json = serializer.serialize(affirmation)

        json shouldContain "\"id\":\"aff-123\""
        json shouldContain "\"type\":\"TASK_COMPLETION\""
        json shouldContain "\"message\":\"Great job!\""
        json shouldContain "\"tone\":\"ENCOURAGING\""
        json shouldContain "\"ageAppropriatenessLevel\":3"
        json shouldContain "\"createdAt\":1000000000000"
    }

    test("Parse affirmation from JSON") {
        val json = """{"id":"aff-123","type":"TASK_COMPLETION","message":"Great job!","tone":"ENCOURAGING","ageAppropriatenessLevel":3,"createdAt":1000000000000}"""

        val affirmation = parser.parse(json)

        affirmation.id shouldBe "aff-123"
        affirmation.type shouldBe AffirmationType.TASK_COMPLETION
        affirmation.message shouldBe "Great job!"
        affirmation.tone shouldBe AffirmationTone.ENCOURAGING
        affirmation.ageAppropriatenessLevel shouldBe 3
        affirmation.createdAt shouldBe Instant.ofEpochMilli(1000000000000L)
    }

    test("Round-trip serialization preserves all fields") {
        val original = Affirmation(
            id = UUID.randomUUID().toString(),
            type = AffirmationType.DAY_COMPLETION,
            message = "Perfect day! You crushed it!",
            tone = AffirmationTone.CELEBRATORY,
            ageAppropriatenessLevel = 4,
            createdAt = Instant.now()
        )

        val json = serializer.serialize(original)
        val deserialized = parser.parse(json)

        deserialized shouldBe original
    }

    test("Serialize list of affirmations") {
        val affirmations = listOf(
            Affirmation(
                id = "aff-1",
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = AffirmationTone.ENCOURAGING,
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = "aff-2",
                type = AffirmationType.STREAK_MILESTONE,
                message = "3-day streak! Keep it up!",
                tone = AffirmationTone.MOTIVATIONAL,
                ageAppropriatenessLevel = 4
            )
        )

        val json = serializer.serializeList(affirmations)
        val deserialized = parser.parseList(json)

        deserialized.size shouldBe 2
        deserialized[0].id shouldBe "aff-1"
        deserialized[1].id shouldBe "aff-2"
    }

    test("Pretty-print JSON") {
        val affirmation = Affirmation(
            id = "aff-123",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great job!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )

        val prettyJson = serializer.serializePretty(affirmation)

        // Pretty-printed JSON should have newlines and indentation
        prettyJson shouldContain "\n"
        prettyJson shouldContain "  "
    }

    test("Parser rejects missing id") {
        val json = """{"type":"TASK_COMPLETION","message":"Great job!","tone":"ENCOURAGING","ageAppropriatenessLevel":3}"""

        try {
            parser.parse(json)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "id"
        }
    }

    test("Parser rejects missing message") {
        val json = """{"id":"aff-123","type":"TASK_COMPLETION","tone":"ENCOURAGING","ageAppropriatenessLevel":3}"""

        try {
            parser.parse(json)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "message"
        }
    }

    test("Parser rejects invalid type") {
        val json = """{"id":"aff-123","type":"INVALID_TYPE","message":"Great job!","tone":"ENCOURAGING","ageAppropriatenessLevel":3}"""

        try {
            parser.parse(json)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "AffirmationType"
        }
    }

    test("Parser rejects invalid tone") {
        val json = """{"id":"aff-123","type":"TASK_COMPLETION","message":"Great job!","tone":"INVALID_TONE","ageAppropriatenessLevel":3}"""

        try {
            parser.parse(json)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "AffirmationTone"
        }
    }

    test("Parser rejects invalid age appropriateness level") {
        val json = """{"id":"aff-123","type":"TASK_COMPLETION","message":"Great job!","tone":"ENCOURAGING","ageAppropriatenessLevel":0}"""

        try {
            parser.parse(json)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "ageAppropriatenessLevel"
        }
    }

    test("All AffirmationType enum values serialize and deserialize correctly") {
        AffirmationType.values().forEach { type ->
            val affirmation = Affirmation(
                id = "aff-123",
                type = type,
                message = "Test",
                tone = AffirmationTone.ENCOURAGING,
                ageAppropriatenessLevel = 3
            )

            val json = serializer.serialize(affirmation)
            val deserialized = parser.parse(json)

            deserialized.type shouldBe type
        }
    }

    test("All AffirmationTone enum values serialize and deserialize correctly") {
        AffirmationTone.values().forEach { tone ->
            val affirmation = Affirmation(
                id = "aff-123",
                type = AffirmationType.TASK_COMPLETION,
                message = "Test",
                tone = tone,
                ageAppropriatenessLevel = 3
            )

            val json = serializer.serialize(affirmation)
            val deserialized = parser.parse(json)

            deserialized.tone shouldBe tone
        }
    }
})
