package com.adhdfocus.app.domain.timer

import java.time.Instant

data class TaskCompletionSessionMetrics(
    val taskId: String,
    val householdId: String,
    val userId: String,
    val estimatedDurationMinutes: Int?,
    val configuredDurationSeconds: Int?,
    val actualDurationSeconds: Int,
    val actualDurationMinutes: Int?,
    val totalPausedSeconds: Int,
    val pauseCount: Int,
    val resetCount: Int,
    val timerStartedAt: Instant?,
    val timerStoppedAt: Instant,
    val completedAt: Instant = timerStoppedAt
)
