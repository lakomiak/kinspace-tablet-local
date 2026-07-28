package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index("householdId"),
        Index("assignedUserId"),
        Index("status"),
        Index("syncStatus"),
        Index("todoGroup"),
        Index("dueDate"),
        Index("createdAt"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["householdId", "status"]),
        Index(value = ["assignedUserId", "status"]),
        Index(value = ["householdId", "todoGroup"]),
        Index(value = ["assignedUserId", "todoGroup"]),
        Index(value = ["householdId", "syncStatus"]),
        Index(value = ["assignedUserId", "syncStatus"])
    ]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val assignedUserId: String,
    val title: String,
    val emoji: String? = null,
    val description: String? = null,
    val todoGroup: String,
    val repeatRule: String = "once",
    val estimatedDurationMinutes: Int? = null,
    val estimatedDurationSeconds: Int? = null,
    val timerDurationMs: Long? = null,
    val tokenValue: Int = 1,
    val actualDurationMinutes: Int? = null,
    val status: TaskStatus = TaskStatus.INCOMPLETE,
    val dueDate: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    init {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        require(assignedUserId.isNotBlank()) { "assignedUserId cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(todoGroup.isNotBlank()) { "todoGroup cannot be blank" }
        require(repeatRule.isNotBlank()) { "repeatRule cannot be blank" }
        require(estimatedDurationMinutes == null || estimatedDurationMinutes >= 0) {
            "estimatedDurationMinutes must be non-negative if provided"
        }
        require(estimatedDurationSeconds == null || estimatedDurationSeconds >= 0) {
            "estimatedDurationSeconds must be non-negative if provided"
        }
        require(actualDurationMinutes == null || actualDurationMinutes >= 0) {
            "actualDurationMinutes must be non-negative if provided"
        }
        require(tokenValue >= 0) {
            "tokenValue must be non-negative"
        }
    }
}

enum class TaskStatus {
    INCOMPLETE,
    IN_PROGRESS,
    COMPLETED
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    CONFLICT
}
