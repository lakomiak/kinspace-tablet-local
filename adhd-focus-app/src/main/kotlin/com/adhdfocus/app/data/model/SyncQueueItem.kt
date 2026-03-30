package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Represents a pending sync operation in the queue.
 * Tracks operations that need to be synchronized with calendar-cloud.
 * Supports FIFO ordering, retry tracking, and operation type classification.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index("taskId"),
        Index("userId"),
        Index("operation"),
        Index("timestamp"),
        Index("retryCount"),
        Index(value = ["userId", "timestamp"]),
        Index(value = ["operation", "timestamp"]),
        Index(value = ["retryCount", "timestamp"])
    ],
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
    ]
)
data class SyncQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val userId: String,
    val operation: SyncOperation,
    val payload: String, // JSON serialized task data
    val timestamp: Instant = Instant.now(),
    val retryCount: Int = 0
) {
    init {
        require(taskId.isNotBlank()) { "taskId cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(payload.isNotBlank()) { "payload cannot be blank" }
    }
}

/**
 * Enum representing the type of sync operation.
 * Supports CREATE, UPDATE, and DELETE operations.
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}
