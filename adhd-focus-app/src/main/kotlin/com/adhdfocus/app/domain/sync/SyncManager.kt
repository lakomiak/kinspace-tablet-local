package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.repository.SyncRepository
import javax.inject.Inject

/**
 * SyncManager handles bidirectional synchronization with calendar-cloud.
 *
 * Manages:
 * - WebSocket connection establishment and maintenance
 * - Queuing local changes for synchronization
 * - Sending pending changes via REST API
 * - Receiving and applying remote updates
 * - Conflict resolution using timestamp-based resolution
 * - Sync status indicators
 * - Exponential backoff for failed attempts
 */
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository
) {
    /**
     * Initializes the sync manager and establishes WebSocket connection.
     *
     * @param householdId Household ID
     * @param userId User ID
     */
    suspend fun initialize(householdId: String, userId: String) {
        syncRepository.initializeWebSocket(householdId, userId)
    }

    /**
     * Queues a task change for synchronization.
     *
     * @param task Task to sync
     * @param operation Operation type (CREATE, UPDATE, DELETE)
     */
    suspend fun queueTaskChange(task: Task, operation: String) {
        syncRepository.queueTaskChange(task, operation)
    }

    /**
     * Synchronizes all pending changes to cloud.
     *
     * @return Sync result with success/failure status
     */
    suspend fun syncPendingChanges(): SyncResult {
        return syncRepository.syncPendingChanges()
    }

    /**
     * Applies a remote update to a local task.
     *
     * @param remoteTask Task from remote source
     * @param localTask Local task (if exists)
     * @return Resolved task (either remote or local based on conflict resolution)
     */
    suspend fun applyRemoteUpdate(remoteTask: Task, localTask: Task?): Task {
        return if (localTask == null) {
            // No local version, use remote
            remoteTask
        } else {
            // Resolve conflict using timestamp
            if (remoteTask.updatedAt > localTask.updatedAt) {
                remoteTask
            } else {
                localTask
            }
        }
    }

    /**
     * Gets the current sync status.
     *
     * @return Current sync status
     */
    suspend fun getSyncStatus(): SyncStatus {
        return syncRepository.getSyncStatus()
    }

    /**
     * Checks if there are pending changes to sync.
     *
     * @return True if there are pending changes
     */
    suspend fun hasPendingChanges(): Boolean {
        return syncRepository.hasPendingChanges()
    }

    /**
     * Clears the sync queue (use with caution).
     */
    suspend fun clearSyncQueue() {
        syncRepository.clearSyncQueue()
    }

    /**
     * Shuts down the sync manager and closes WebSocket connection.
     */
    suspend fun shutdown() {
        syncRepository.shutdown()
    }
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Current sync status.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}
