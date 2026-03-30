package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "affirmations",
    indices = [
        Index("type"),
        Index("tone"),
        Index("ageAppropriatenessLevel"),
        Index("createdAt")
    ]
)
data class Affirmation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: AffirmationType,
    val message: String,
    val tone: AffirmationTone,
    val ageAppropriatenessLevel: Int,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(message.isNotBlank()) { "message cannot be blank" }
        require(ageAppropriatenessLevel in 1..5) { "ageAppropriatenessLevel must be between 1 and 5" }
    }
}

enum class AffirmationType {
    TASK_COMPLETION,
    DAY_COMPLETION,
    STREAK_MILESTONE
}

enum class AffirmationTone {
    ENCOURAGING,
    CELEBRATORY,
    MOTIVATIONAL,
    SUPPORTIVE
}
