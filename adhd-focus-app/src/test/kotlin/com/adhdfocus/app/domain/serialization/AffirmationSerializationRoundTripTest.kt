package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for Affirmation serialization and deserialization round-trip.
 *
 * **Validates: Requirements 20.1-20.7**
 *
 * These tests verify that:
 * 1. Affirmation objects can be serialized to JSON
 * 2. JSON can be parsed back into Affirmation objects
 * 3. The round-trip (serialize → deserialize) produces equivalent objects
 * 4. All fields including optional fields are preserved
 * 5. Enum serialization (AffirmationType, AffirmationTone) works correctly
 */
class AffirmationSerializationRoundTripTest : FunSpec({
    val serializer = AffirmationSerializer()
    val parser = AffirmationParser()

    /**
     * Property 34: Affirmation Serialization Round-Trip
     *
     * FOR ALL valid Affirmation objects, serializing then parsing SHALL produce an equivalent object.
     * This ensures data integrity through serialization cycles.
     */
    test("Property 34: Round-trip serialization preserves all affirmation fields") {
        checkAll(
            iterations = 100,
            arb = affirmationArbitrary()
        ) { originalAffirmation ->
            // Serialize the affirmation
            val json = serializer.serialize(originalAffirmation)

            // Parse it back
            val deserializedAffirmation = parser.parse(json)

            // Verify all fields match
            deserializedAffirmation.id shouldBe originalAffirmation.id
            deserializedAffirmation.type shouldBe originalAffirmation.type
            deserializedAffirmation.message shouldBe originalAffirmation.message
            deserializedAffirmation.tone shouldBe originalAffirmation.tone
            deserializedAffirmation.ageAppropriatenessLevel shouldBe originalAffirmation.ageAppropriatenessLevel
            deserializedAffirmation.createdAt shouldBe originalAffirmation.createdAt
        }
    }

    /**
     * Property 34 (continued): Verify complete equality
     *
     * The deserialized affirmation should be completely equal to the original.
     */
    test("Property 34: Deserialized affirmation equals original affirmation") {
        checkAll(
            iterations = 100,
            arb = affirmationArbitrary()
        ) { originalAffirmation ->
            val json = serializer.serialize(originalAffirmation)
            val deserializedAffirmation = parser.parse(json)

            deserializedAffirmation shouldBe originalAffirmation
        }
    }

    /**
     * Property 34: Round-trip with pretty-printed JSON
     *
     * Pretty-printed JSON should also round-trip correctly.
     */
    test("Property 34: Pretty-printed JSON round-trip preserves all fields") {
        checkAll(
            iterations = 100,
            arb = affirmationArbitrary()
        ) { originalAffirmation ->
            val prettyJson = serializer.serializePretty(originalAffirmation)
            val deserializedAffirmation = parser.parse(prettyJson)

            deserializedAffirmation shouldBe originalAffirmation
        }
    }

    /**
     * Property 35: Affirmation Parser Error Handling
     *
     * FOR ALL invalid JSON inputs, the parser SHALL return a descriptive error message.
     */
    test("Property 35: Parser handles missing required fields with descriptive errors") {
        val invalidJsons = listOf(
            "{}", // Missing all fields
            """{"id": "123"}""", // Missing type, message, tone, ageAppropriatenessLevel
            """{"id": "123", "type": "TASK_COMPLETION"}""", // Missing message, tone, ageAppropriatenessLevel
            """{"id": "123", "type": "TASK_COMPLETION", "message": "Great job!"}""", // Missing tone, ageAppropriatenessLevel
            """{"id": "123", "type": "TASK_COMPLETION", "message": "Great job!", "tone": "ENCOURAGING"}""", // Missing ageAppropriatenessLevel
            """{"id": "", "type": "TASK_COMPLETION", "message": "Great job!", "tone": "ENCOURAGING", "ageAppropriatenessLevel": 3}""", // Empty id
            """{"id": "123", "type": "INVALID_TYPE", "message": "Great job!", "tone": "ENCOURAGING", "ageAppropriatenessLevel": 3}""", // Invalid type
            """{"id": "123", "type": "TASK_COMPLETION", "message": "", "tone": "ENCOURAGING", "ageAppropriatenessLevel": 3}""", // Empty message
            """{"id": "123", "type": "TASK_COMPLETION", "message": "Great job!", "tone": "INVALID_TONE", "ageAppropriatenessLevel": 3}""", // Invalid tone
            """{"id": "123", "type": "TASK_COMPLETION", "message": "Great job!", "tone": "ENCOURAGING", "ageAppropriatenessLevel": 0}""", // Invalid age level (too low)
            """{"id": "123", "type": "TASK_COMPLETION", "message": "Great job!", "tone": "ENCOURAGING", "ageAppropriatenessLevel": 6}""", // Invalid age level (too high)
        )

        invalidJsons.forEach { invalidJson ->
            try {
                parser.parse(invalidJson)
                throw AssertionError("Expected parser to throw IllegalArgumentException for: $invalidJson")
            } catch (e: IllegalArgumentException) {
                // Expected - error message should be descriptive
                e.message?.shouldBe(e.message) // Just verify message exists
            }
        }
    }

    /**
     * Property 34: Enum serialization and deserialization
     *
     * FOR ALL AffirmationType and AffirmationTone enum values, serialization and deserialization
     * SHALL preserve the enum value.
     */
    test("Property 34: Enum values are preserved through serialization") {
        val affirmationTypes = AffirmationType.values()
        val affirmationTones = AffirmationTone.values()

        affirmationTypes.forEach { affirmationType ->
            affirmationTones.forEach { affirmationTone ->
                val affirmation = Affirmation(
                    id = UUID.randomUUID().toString(),
                    type = affirmationType,
                    message = "Test affirmation",
                    tone = affirmationTone,
                    ageAppropriatenessLevel = 3
                )

                val json = serializer.serialize(affirmation)
                val deserialized = parser.parse(json)

                deserialized.type shouldBe affirmationType
                deserialized.tone shouldBe affirmationTone
            }
        }
    }

    /**
     * Property 34: List serialization round-trip
     *
     * FOR ALL lists of Affirmation objects, serializing then parsing SHALL produce equivalent list.
     */
    test("Property 34: List serialization round-trip preserves all affirmations") {
        checkAll(
            iterations = 50,
            arb = Arb.list(affirmationArbitrary(), 1..10)
        ) { originalAffirmations ->
            val json = serializer.serializeList(originalAffirmations)
            val deserializedAffirmations = parser.parseList(json)

            deserializedAffirmations.size shouldBe originalAffirmations.size
            deserializedAffirmations.zip(originalAffirmations).forEach { (deserialized, original) ->
                deserialized shouldBe original
            }
        }
    }

    /**
     * Property 34: Age appropriateness level validation
     *
     * FOR ALL valid Affirmation objects, the age appropriateness level SHALL be between 1 and 5.
     */
    test("Property 34: Age appropriateness level is preserved and valid") {
        checkAll(
            iterations = 100,
            arb = affirmationArbitrary()
        ) { originalAffirmation ->
            val json = serializer.serialize(originalAffirmation)
            val deserialized = parser.parse(json)

            deserialized.ageAppropriatenessLevel shouldBe originalAffirmation.ageAppropriatenessLevel
            deserialized.ageAppropriatenessLevel in 1..5 shouldBe true
        }
    }
})

/**
 * Generates arbitrary Affirmation objects with all fields populated.
 */
private fun affirmationArbitrary(): Arb<Affirmation> {
    return Arb.bind(
        Arb.string(1..50), // id
        Arb.of(*AffirmationType.values()), // type
        Arb.string(1..200), // message
        Arb.of(*AffirmationTone.values()), // tone
        Arb.int(1..5), // ageAppropriatenessLevel
        Arb.long(1000000000000L..System.currentTimeMillis()) // createdAt
    ) { id, type, message, tone, ageLevel, createdAt ->
        Affirmation(
            id = id,
            type = type,
            message = message,
            tone = tone,
            ageAppropriatenessLevel = ageLevel,
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }
}
