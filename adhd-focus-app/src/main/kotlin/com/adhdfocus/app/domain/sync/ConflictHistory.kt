package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import java.time.Instant

/**
 * Data class for tracking sync conflicts and their resolution.
 *
 * Stores both versions and the resolution for debugging and audit purposes.
 */
data class ConflictHistory(
    val taskId: String,
    val localVersion: Task,
    val remoteVersion: Task,
    val resolvedVersion: Task,
    val resolutionReason: String,
    val timestamp: Instant = Instant.now()
)
