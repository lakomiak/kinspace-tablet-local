package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import java.time.Instant
import java.util.UUID

/**
 * SyncQueueManager handles management of pending sync operations.
 *
 * Responsibilities:
 * - Queue changes for synchronization
 * - Retrieve pending items in FIFO order
 * - Track retry attempts
 * - Clean up old items
 * - Support offline-first sync strategy
 *
 * The sync queue enables offline capability by storing all local changes
 * that need to be synchronized with calendar-cloud when connectivity is restored.
 */
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        const val MAX_RETRIES = 5
        const val CLEANUP_CUTOFF_DAYS = 30
    }

    /**
     * Adds an item to the sync queue.
     *
     * @param taskId ID of the task being synced
     * @param userId ID of the user who made the change
     * @param operation Type of operation (CREATE, UPDATE, DELETE)
     * @param payload JSON serialized task data
     * @return The queued item
     */
    suspend fun queueItem(
        taskId: String,
        userId: String,
        operation: SyncOperation,
        payload: String
    ): SyncQueueItem {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        require(payload.isNotBlank()) { "Payload cannot be empty" }

        val item = SyncQueueItem(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            userId = userId,
            operation = operation,
            payload = payload,
            timestamp = Instant.now(),
            retryCount = 0
        )

        syncQueueDao.insert(item)
        return item
    }

    /**
     * Retrieves all pending items for a user in FIFO order.
     *
     * @param userId User ID
     * @return List of pending items ordered by timestamp (oldest first)
     */
    suspend fun getPendingItemsByUser(userId: String): List<SyncQueueItem> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getPendingItemsByUserFifo(userId)
    }

    /**
     * Retrieves all pending items across all users in FIFO order.
     *
     * @return List of pending items ordered by timestamp (oldest first)
     */
    suspend fun getAllPendingItems(): List<SyncQueueItem> {
        return syncQueueDao.getAllPendingItemsFifo()
    }

    /**
     * Retrieves pending items for a specific operation type.
     *
     * @param userId User ID
     * @param operation Operation type to filter by
     * @return List of pending items for the specified operation
     */
    suspend fun getPendingItemsByOperation(
        userId: String,
        operation: SyncOperation
    ): List<SyncQueueItem> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getPendingItemsByUserAndOperation(userId, operation)
    }

    /**
     * Retrieves retryable items (those that haven't exceeded max retries).
     *
     * @param userId User ID
     * @return List of items that can be retried
     */
    suspend fun getRetryableItems(userId: String): List<SyncQueueItem> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getRetryableItemsByUser(userId, MAX_RETRIES)
    }

    /**
     * Increments the retry count for an item.
     *
     * @param itemId ID of the sync queue item
     */
    suspend fun incrementRetryCount(itemId: String) {
        require(itemId.isNotBlank()) { "Item ID cannot be empty" }
        syncQueueDao.incrementRetryCount(itemId)
    }

    /**
     * Removes an item from the sync queue (after successful sync).
     *
     * @param itemId ID of the sync queue item
     */
    suspend fun removeItem(itemId: String) {
        require(itemId.isNotBlank()) { "Item ID cannot be empty" }
        syncQueueDao.deleteItemById(itemId)
    }

    /**
     * Removes all items for a specific task.
     *
     * @param taskId ID of the task
     */
    suspend fun removeItemsByTask(taskId: String) {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }
        syncQueueDao.deleteItemsByTaskId(taskId)
    }

    /**
     * Removes all items for a specific user.
     *
     * @param userId ID of the user
     */
    suspend fun removeItemsByUser(userId: String) {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        syncQueueDao.deleteItemsByUserId(userId)
    }

    /**
     * Removes all items for a specific operation type.
     *
     * @param userId ID of the user
     * @param operation Operation type to remove
     */
    suspend fun removeItemsByOperation(userId: String, operation: SyncOperation) {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        syncQueueDao.deleteItemsByUserAndOperation(userId, operation)
    }

    /**
     * Gets the count of pending items for a user.
     *
     * @param userId User ID
     * @return Number of pending items
     */
    suspend fun getPendingItemCount(userId: String): Int {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getPendingItemCount(userId)
    }

    /**
     * Gets the count of pending items for a specific operation.
     *
     * @param userId User ID
     * @param operation Operation type
     * @return Number of pending items for the operation
     */
    suspend fun getPendingItemCountByOperation(userId: String, operation: SyncOperation): Int {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getPendingItemCountByOperation(userId, operation)
    }

    /**
     * Gets the count of retryable items for a user.
     *
     * @param userId User ID
     * @return Number of retryable items
     */
    suspend fun getRetryableItemCount(userId: String): Int {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueDao.getRetryableItemCount(userId, MAX_RETRIES)
    }

    /**
     * Checks if there are any pending items for a user.
     *
     * @param userId User ID
     * @return true if there are pending items, false otherwise
     */
    suspend fun hasPendingItems(userId: String): Boolean {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return getPendingItemCount(userId) > 0
    }

    /**
     * Cleans up old sync queue items that are older than the cutoff period.
     *
     * @param userId User ID
     * @param cutoffDays Number of days to keep (default 30)
     */
    suspend fun cleanupOldItems(userId: String, cutoffDays: Int = CLEANUP_CUTOFF_DAYS) {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        require(cutoffDays > 0) { "Cutoff days must be positive" }

        val cutoffTime = Instant.now().minusSeconds((cutoffDays * 24 * 60 * 60).toLong())
        syncQueueDao.deleteOldItems(userId, cutoffTime)
    }

    /**
     * Cleans up all sync queue items.
     *
     * Use with caution - this will remove all pending sync operations.
     */
    suspend fun clearAllItems() {
        syncQueueDao.deleteAllItems()
    }

    /**
     * Gets items in a specific time range.
     *
     * @param userId User ID
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of items in the time range
     */
    suspend fun getItemsInTimeRange(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): List<SyncQueueItem> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        require(!startTime.isAfter(endTime)) { "Start time must be before end time" }
        return syncQueueDao.getItemsInTimeRange(userId, startTime, endTime)
    }

    /**
     * Retrieves a specific item by ID.
     *
     * @param itemId ID of the sync queue item
     * @return The item or null if not found
     */
    suspend fun getItemById(itemId: String): SyncQueueItem? {
        require(itemId.isNotBlank()) { "Item ID cannot be empty" }
        return syncQueueDao.getItemById(itemId)
    }

    /**
     * Updates a sync queue item.
     *
     * @param item The updated item
     */
    suspend fun updateItem(item: SyncQueueItem) {
        syncQueueDao.update(item)
    }
}
