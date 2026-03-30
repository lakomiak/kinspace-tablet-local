package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing remote updates received via WebSocket.
 *
 * Responsibilities:
 * - Apply remote updates to local database
 * - Resolve conflicts using timestamp-based resolution
 * - Emit update events for UI refresh
 * - Track timer state to avoid interrupting active timers
 * - Queue updates received while offline
 */
interface RemoteUpdateManager {
    /**
     * Applies a remote update received via WebSocket.
     *
     * @param event The WebSocket event containing the remote update
     * @return UpdateResult indicating success or failure
     */
    suspend fun applyRemoteUpdate(event: WebSocketEvent): UpdateResult

    /**
     * Observes update events for UI refresh.
     *
     * @return Flow of UpdateEvent emitted when updates are applied
     */
    fun observeUpdates(): Flow<UpdateEvent>

    /**
     * Checks if a timer is currently active.
     *
     * @return true if timer is running, false otherwise
     */
    fun isTimerActive(): Boolean

    /**
     * Sets the timer active state.
     *
     * @param active true if timer is running, false otherwise
     */
    fun setTimerActive(active: Boolean)

    /**
     * Applies queued updates that were received while offline.
     */
    suspend fun applyQueuedUpdates()
}

/**
 * Result of applying a remote update.
 */
data class UpdateResult(
    val success: Boolean,
    val message: String = "",
    val conflictResolved: Boolean = false
)

/**
 * Sealed class representing different types of update events.
 */
sealed class UpdateEvent {
    /**
     * Emitted when a task is updated.
     */
    data class TaskUpdated(val taskId: String, val task: Task) : UpdateEvent()

    /**
     * Emitted when a task is deleted.
     */
    data class TaskDeleted(val taskId: String) : UpdateEvent()

    /**
     * Emitted when a task is created.
     */
    data class TaskCreated(val task: Task) : UpdateEvent()

    /**
     * Emitted when multiple updates are applied.
     */
    data class UpdatesApplied(val count: Int) : UpdateEvent()

    /**
     * Emitted when an error occurs during update application.
     */
    data class Error(val message: String) : UpdateEvent()
}
