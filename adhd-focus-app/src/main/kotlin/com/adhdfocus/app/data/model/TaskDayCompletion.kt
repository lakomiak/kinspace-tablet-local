package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "task_day_completions",
    primaryKeys = ["householdId", "userId", "taskId", "targetDate"],
    indices = [
        Index("householdId"),
        Index("userId"),
        Index("taskId"),
        Index("targetDate"),
        Index(value = ["householdId", "userId", "targetDate"])
    ]
)
data class TaskDayCompletion(
    val householdId: String,
    val userId: String,
    val taskId: String,
    val targetDate: String,
    val isCompleted: Boolean,
    val updatedAt: Instant = Instant.now()
)
