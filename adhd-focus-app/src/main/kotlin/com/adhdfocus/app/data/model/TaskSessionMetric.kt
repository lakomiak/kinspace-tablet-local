package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "task_session_metrics",
    indices = [
        Index("taskId"),
        Index("userId"),
        Index("householdId"),
        Index("endedAt"),
        Index(value = ["userId", "endedAt"], name = "idx_task_session_user_ended_at")
    ]
)
data class TaskSessionMetric(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val userId: String,
    val householdId: String,
    val configuredDurationSeconds: Int? = null,
    val activeDurationSeconds: Int,
    val totalPausedSeconds: Int = 0,
    val pauseCount: Int = 0,
    val resetCount: Int = 0,
    val timerStartedAt: Instant,
    val endedAt: Instant,
    val outcome: TaskSessionOutcome,
    val completedTask: Boolean,
    val completedAfterTimerEnded: Boolean,
    val stoppedBeforeTimerEnded: Boolean
)

enum class TaskSessionOutcome {
    COMPLETED_BEFORE_TIME_END,
    COMPLETED_AFTER_TIME_END,
    CANCELED_BEFORE_TIME_END,
    CANCELED_AFTER_TIME_END
}
