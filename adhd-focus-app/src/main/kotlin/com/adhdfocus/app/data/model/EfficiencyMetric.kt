package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "efficiency_metrics",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("userId"),
        Index("householdId"),
        Index("completedAt"),
        Index(value = ["userId", "completedAt"], name = "idx_user_completed_at"),
        Index(value = ["householdId", "completedAt"], name = "idx_household_completed_at")
    ]
)
data class EfficiencyMetric(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val userId: String,
    val householdId: String,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int? = null,
    val efficiencyPercentage: Float? = null,
    val configuredDurationSeconds: Int? = null,
    val actualDurationSeconds: Int? = null,
    val totalPausedSeconds: Int = 0,
    val pauseCount: Int = 0,
    val resetCount: Int = 0,
    val timerStartedAt: Instant? = null,
    val timerStoppedAt: Instant? = null,
    val completedAt: Instant = Instant.now()
)
