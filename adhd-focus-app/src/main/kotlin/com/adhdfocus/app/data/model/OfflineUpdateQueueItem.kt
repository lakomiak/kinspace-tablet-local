package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Represents an update received while offline that needs to be applied when connectivity is restored.
 * Tracks updates from WebSocket events with FIFO ordering and conflict detection.
 */
@Entity(
    tableName = "offline_update_queue",
    indices = [
        Index("taskId"),
        Index("userId"),
        Index("timestamp"),
        Index(value = ["userId", "timestamp"]),
        Index("applied")
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
data class OfflineUpdateQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val userId: String,
    val updateType: UpdateType,
    val payload: String, // JSON serialized task data
    val timestamp: Instant = Instant.now(),
    val applied: Boolean = false
) {
    init {
        require(taskId.isNotBlank()) { "taskId cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(payload.isNotBlank()) { "payload cannot be blank" }
    }
}

/**
 * Enum representing the type of update received from WebSocket.
 */
enum class UpdateType {
    CREATED,
    UPDATED,
    DELETED
}
