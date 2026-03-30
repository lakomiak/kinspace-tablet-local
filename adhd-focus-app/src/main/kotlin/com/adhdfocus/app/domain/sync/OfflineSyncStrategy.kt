package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task

/**
 * Interface for offline-first sync strategy.
 *
 * Implements the offline-first pattern where:
 * 1. Local changes are queued immediately
 * 2. Changes are synced when connectivity available
 * 3. Remote updates are applied with conflict resolution
 * 4. Automatic sync on reconnection
 *
 * Responsibilities:
 * - Queue local changes with timestamps
 * - Sync queued changes when online
 * - Retrieve queued changes
 * - Clear sync queue
 * - Integrate with connectivity monitoring
 */
interface OfflineSyncStrategy {
    /**
     * Queues a local change for synchronization.
     *
     * @param task The task being changed
     * @param operation Type of operation (CREATE, UPDATE, DELETE)
     * @param userId ID of the user making the change
     * @throws IllegalArgumentException if task or userId is invalid
     */
    suspend fun queueLocalChange(task: Task, operation: SyncOperation, userId: String)

    /**
     * Synchronizes all queued changes to calendar-cloud.
     *
     * @param householdId ID of the household
     * @param userId ID of the user
     * @return SyncResult with synced count, failed count, and conflicts
     */
    suspend fun syncQueuedChanges(householdId: String, userId: String): SyncResult

    /**
     * Retrieves all queued changes for a user.
     *
     * @param userId ID of the user
     * @return List of queued sync changes
     */
    suspend fun getQueuedChanges(userId: String): List<SyncChange>

    /**
     * Clears all queued changes for a user.
     *
     * @param userId ID of the user
     */
    suspend fun clearQueue(userId: String)

    /**
     * Gets the count of queued changes for a user.
     *
     * @param userId ID of the user
     * @return Number of queued changes
     */
    suspend fun getQueuedChangeCount(userId: String): Int

    /**
     * Checks if there are any queued changes for a user.
     *
     * @param userId ID of the user
     * @return true if there are queued changes, false otherwise
     */
    suspend fun hasQueuedChanges(userId: String): Boolean
}
