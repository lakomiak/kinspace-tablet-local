package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Affirmation
import org.json.JSONObject

/**
 * AffirmationSerializer converts Affirmation objects to JSON representation.
 *
 * Serializes all affirmation metadata including:
 * - Basic fields (id, message)
 * - Enum fields (type, tone)
 * - Age appropriateness level
 * - Timestamp information
 */
class AffirmationSerializer {
    /**
     * Serializes an Affirmation object to JSON.
     *
     * @param affirmation Affirmation to serialize
     * @return JSON string representation
     */
    fun serialize(affirmation: Affirmation): String {
        val json = buildAffirmationJson(affirmation)
        return json.toString()
    }

    /**
     * Serializes an Affirmation object to pretty-printed JSON.
     *
     * @param affirmation Affirmation to serialize
     * @return Pretty-printed JSON string
     */
    fun serializePretty(affirmation: Affirmation): String {
        val json = buildAffirmationJson(affirmation)
        return json.toString(2)
    }

    /**
     * Serializes a list of Affirmation objects to JSON array.
     *
     * @param affirmations List of affirmations to serialize
     * @return JSON array string
     */
    fun serializeList(affirmations: List<Affirmation>): String {
        val jsonArray = org.json.JSONArray()
        affirmations.forEach { affirmation ->
            jsonArray.put(buildAffirmationJson(affirmation))
        }
        return jsonArray.toString()
    }

    /**
     * Builds a JSONObject from an Affirmation, handling all field conversions.
     *
     * @param affirmation Affirmation to convert
     * @return JSONObject representation
     */
    private fun buildAffirmationJson(affirmation: Affirmation): JSONObject {
        val json = JSONObject()
        json.put("id", affirmation.id)
        json.put("type", affirmation.type.name)
        json.put("message", affirmation.message)
        json.put("tone", affirmation.tone.name)
        json.put("ageAppropriatenessLevel", affirmation.ageAppropriatenessLevel)
        json.put("createdAt", affirmation.createdAt.toEpochMilli())
        return json
    }
}
