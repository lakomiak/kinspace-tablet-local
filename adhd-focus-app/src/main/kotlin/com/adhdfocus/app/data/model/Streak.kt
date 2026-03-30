package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val householdId: String,
    val currentCount: Int = 0,
    val bestCount: Int = 0,
    val lastCompletionDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val updatedAt: Instant = Instant.now()
)
