package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implementation of offline-first sync strategy.
 *
 * Handles:
 * - Queuing local changes with timestamps
 * - Syncing queued changes when connectivity available
 * - Automatic sync on reconnection
 * - Conflict resolution for queued changes
 * - Integration with local persistence and cloud sync
 */
class OfflineSyncStrategyImpl @Inject constructor(
    private val syncQueueManager: SyncQueueManager,
    private val taskPersistenceManager: TaskPersistenceManager,
    private val cloudSyncManager: CloudSyncManager,
    private val connectivityManager: ConnectivityManager,
    private val syncChangeSerializer: SyncChangeSerializer
) : OfflineSyncStrategy {

    override suspend fun queueLocalChange(
        task: Task,
        operation: SyncOperation,
        userId: String
    ) {
        require(task.id.isNotBlank()) { "Task ID cannot be empty" }
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        // Serialize task to JSON payload
        val payload = syncChangeSerializer.serializeTask(task)

        // Queue the change
        syncQueueManager.queueItem(
            taskId = task.id,
            userId = userId,
            operation = operation,
            payload = payload
        )

        // Persist task locally
        taskPersistenceManager.saveTask(task)
    }

    override suspend fun syncQueuedChanges(
        householdId: String,
        userId: String
    ): SyncResult {
        require(householdId.isNotBlank()) { "Household ID cannot be empty" }
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        // Check connectivity
        if (!connectivityManager.isOnline()) {
            return SyncResult(syncedCount = 0, failedCount = 0, conflicts = emptyList())
        }

        // Sync pending changes via CloudSyncManager
        return cloudSyncManager.syncPendingChanges(householdId, userId)
    }

    override suspend fun getQueuedChanges(userId: String): List<SyncChange> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        val queuedItems = syncQueueManager.getPendingItemsByUser(userId)

        return queuedItems.map { item ->
            SyncChange(
                taskId = item.taskId,
                operation = item.operation,
                payload = item.payload,
                timestamp = item.timestamp.toEpochMilli()
            )
        }
    }

    override suspend fun clearQueue(userId: String) {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        syncQueueManager.removeItemsByUser(userId)
    }

    override suspend fun getQueuedChangeCount(userId: String): Int {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueManager.getPendingItemCount(userId)
    }

    override suspend fun hasQueuedChanges(userId: String): Boolean {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return syncQueueManager.hasPendingItems(userId)
    }
}
