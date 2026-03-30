package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing updates received while offline.
 *
 * Responsibilities:
 * - Queue updates received while offline
 * - Persist queued updates to local database
 * - Apply queued updates when connectivity restored
 * - Handle conflicts using timestamp-based resolution
 * - Track queue state and size
 * - Support clearing the queue
 *
 * Correctness Properties:
 * - Property 4: Offline Task Caching - Offline updates should be queued and persisted
 * - Property 11: Offline Capability - Queue updates received while offline and apply when connectivity restored
 */
interface OfflineUpdateQueue {
    /**
     * Adds an update to the offline queue.
     *
     * Called when a WebSocket update is received while offline.
     * Persists the update to local database for later application.
     *
     * @param taskId ID of the task being updated
     * @param userId ID of the user receiving the update
     * @param updateType Type of update (CREATED, UPDATED, DELETED)
     * @param payload JSON serialized task data
     * @return true if successfully queued, false otherwise
     */
    suspend fun addUpdate(
        taskId: String,
        userId: String,
        updateType: UpdateType,
        payload: String
    ): Boolean

    /**
     * Gets all queued updates for a user.
     *
     * Returns updates in FIFO order (oldest first).
     *
     * @param userId ID of the user
     * @return List of queued updates in FIFO order
     */
    suspend fun getQueuedUpdates(userId: String): List<OfflineUpdateQueueItem>

    /**
     * Gets all unapplied updates for a user.
     *
     * @param userId ID of the user
     * @return List of unapplied updates in FIFO order
     */
    suspend fun getUnappliedUpdates(userId: String): List<OfflineUpdateQueueItem>

    /**
     * Removes an update from the queue.
     *
     * Called after an update has been successfully applied.
     *
     * @param updateId ID of the update to remove
     * @return true if successfully removed, false otherwise
     */
    suspend fun removeUpdate(updateId: String): Boolean

    /**
     * Marks an update as applied.
     *
     * @param updateId ID of the update to mark as applied
     * @return true if successfully marked, false otherwise
     */
    suspend fun markAsApplied(updateId: String): Boolean

    /**
     * Clears all queued updates for a user.
     *
     * Use with caution - this will discard all pending updates.
     *
     * @param userId ID of the user
     * @return true if successfully cleared, false otherwise
     */
    suspend fun clearQueue(userId: String): Boolean

    /**
     * Gets the current queue size for a user.
     *
     * @param userId ID of the user
     * @return Number of queued updates
     */
    suspend fun getQueueSize(userId: String): Int

    /**
     * Gets the number of unapplied updates for a user.
     *
     * @param userId ID of the user
     * @return Number of unapplied updates
     */
    suspend fun getUnappliedQueueSize(userId: String): Int

    /**
     * Observes queue changes for a user.
     *
     * Emits whenever the queue is modified (add, remove, clear).
     *
     * @param userId ID of the user
     * @return Flow of queue state changes
     */
    fun observeQueueChanges(userId: String): Flow<QueueState>

    /**
     * Checks if there are any queued updates for a user.
     *
     * @param userId ID of the user
     * @return true if queue is not empty, false otherwise
     */
    suspend fun hasQueuedUpdates(userId: String): Boolean
}

/**
 * Represents the state of the offline update queue.
 */
data class QueueState(
    val userId: String,
    val queueSize: Int,
    val unappliedCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
