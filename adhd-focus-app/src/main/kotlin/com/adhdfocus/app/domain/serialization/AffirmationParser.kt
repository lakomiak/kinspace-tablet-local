package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import org.json.JSONObject
import java.time.Instant

/**
 * AffirmationParser converts JSON representations back into Affirmation objects.
 *
 * Parses all affirmation metadata including:
 * - Basic fields (id, message)
 * - Enum fields (type, tone)
 * - Age appropriateness level
 * - Timestamp information
 *
 * Handles optional fields gracefully with default values.
 */
class AffirmationParser {
    /**
     * Parses a JSON string into an Affirmation object.
     *
     * @param jsonString JSON string to parse
     * @return Parsed Affirmation object
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    fun parse(jsonString: String): Affirmation {
        return try {
            val json = JSONObject(jsonString)
            parseFromJson(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Affirmation JSON: ${e.message}", e)
        }
    }

    /**
     * Parses a JSONObject into an Affirmation object.
     *
     * @param json JSONObject to parse
     * @return Parsed Affirmation object
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    fun parseFromJson(json: JSONObject): Affirmation {
        // Validate and extract required fields
        val id = json.optString("id", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: id")
        val message = json.optString("message", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: message")

        // Parse type enum
        val typeString = json.optString("type", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: type")
        val type = try {
            AffirmationType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid AffirmationType: $typeString")
        }

        // Parse tone enum
        val toneString = json.optString("tone", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: tone")
        val tone = try {
            AffirmationTone.valueOf(toneString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid AffirmationTone: $toneString")
        }

        // Extract and validate age appropriateness level
        val ageAppropriatenessLevel = json.optInt("ageAppropriatenessLevel", -1)
        if (ageAppropriatenessLevel < 1 || ageAppropriatenessLevel > 5) {
            throw IllegalArgumentException("ageAppropriatenessLevel must be between 1 and 5, got: $ageAppropriatenessLevel")
        }

        // Parse timestamp
        val createdAt = parseInstant(json.optLong("createdAt", 0))

        return Affirmation(
            id = id,
            type = type,
            message = message,
            tone = tone,
            ageAppropriatenessLevel = ageAppropriatenessLevel,
            createdAt = createdAt
        )
    }

    /**
     * Parses a JSON array string into a list of Affirmation objects.
     *
     * @param jsonArrayString JSON array string to parse
     * @return List of parsed Affirmation objects
     * @throws IllegalArgumentException if parsing fails
     */
    fun parseList(jsonArrayString: String): List<Affirmation> {
        return try {
            val jsonArray = org.json.JSONArray(jsonArrayString)
            val affirmations = mutableListOf<Affirmation>()
            for (i in 0 until jsonArray.length()) {
                affirmations.add(parseFromJson(jsonArray.getJSONObject(i)))
            }
            affirmations
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Affirmation JSON array: ${e.message}", e)
        }
    }

    /**
     * Converts a timestamp (milliseconds since epoch) to an Instant.
     *
     * @param timestamp Milliseconds since epoch
     * @return Instant object
     */
    private fun parseInstant(timestamp: Long): Instant {
        return Instant.ofEpochMilli(timestamp)
    }
}
