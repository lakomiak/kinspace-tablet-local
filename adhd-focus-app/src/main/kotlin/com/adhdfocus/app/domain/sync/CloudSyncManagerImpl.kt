package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Implementation of CloudSyncManager.
 *
 * Handles:
 * - Syncing pending changes when connectivity available
 * - Tracking sync status
 * - Exponential backoff for failed syncs
 * - Conflict resolution by timestamp
 * - UI refresh on sync completion
 * - Retry attempt tracking and logging
 */
class CloudSyncManagerImpl @Inject constructor(
    private val restApiClient: RestApiClient,
    private val syncQueueManager: SyncQueueManager,
    private val taskDao: TaskDao,
    private val connectivityManager: ConnectivityManager,
    private val conflictResolver: ConflictResolver,
    private val retryPolicy: RetryPolicy = ExponentialBackoffRetryPolicy()
) : CloudSyncManager {

    private val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    private val retryAttempts = mutableMapOf<String, Int>()  // Track retries per sync operation

    init {
        // Monitor connectivity and sync when online
        // This will be handled by the caller or a separate orchestrator
    }

    override suspend fun syncPendingChanges(householdId: String, userId: String): SyncResult {
        // Check if online
        if (!connectivityManager.isOnline()) {
            syncStatusFlow.value = SyncStatus.OFFLINE
            return SyncResult(syncedCount = 0, failedCount = 0, conflicts = emptyList())
        }

        syncStatusFlow.value = SyncStatus.SYNCING

        return try {
            // Get pending changes for user
            val pendingItems = syncQueueManager.getPendingItemsByUser(userId)

            if (pendingItems.isEmpty()) {
                syncStatusFlow.value = SyncStatus.SYNCED
                return SyncResult(syncedCount = 0, failedCount = 0, conflicts = emptyList())
            }

            // Convert to SyncChange objects
            val changes = pendingItems.map { item ->
                SyncChange(
                    taskId = item.taskId,
                    operation = item.operation,
                    payload = item.payload,
                    timestamp = item.timestamp.toEpochMilli()
                )
            }

            // Send to cloud
            val result = restApiClient.batchSync(householdId, changes)

            // Remove successfully synced items
            pendingItems.forEach { item ->
                if (result.conflicts.none { it.taskId == item.taskId }) {
                    syncQueueManager.removeItem(item.id)
                    retryAttempts.remove(item.id)  // Clear retry count on success
                }
            }

            // Handle conflicts - apply timestamp-based resolution
            result.conflicts.forEach { conflict ->
                val resolvedTask = conflictResolver.resolveConflict(
                    conflict.localVersion,
                    conflict.remoteVersion
                )
                val reason = conflictResolver.getConflictReason(
                    conflict.localVersion,
                    conflict.remoteVersion
                )
                
                // Log conflict for debugging
                println("Conflict resolved for task ${conflict.taskId}: $reason")
                
                // Apply resolved version
                taskDao.insert(resolvedTask)

                // Remove from sync queue
                pendingItems.find { it.taskId == conflict.taskId }?.let {
                    syncQueueManager.removeItem(it.id)
                    retryAttempts.remove(it.id)  // Clear retry count on conflict resolution
                }
            }

            syncStatusFlow.value = SyncStatus.SYNCED
            result
        } catch (e: Exception) {
            // Log retry attempt
            val operationId = "$householdId-$userId"
            val currentAttempt = retryAttempts.getOrDefault(operationId, 0)
            retryAttempts[operationId] = currentAttempt + 1
            
            println("Sync failed for $operationId, attempt ${currentAttempt + 1}/${retryPolicy.getMaxRetries()}: ${e.message}")
            
            syncStatusFlow.value = SyncStatus.ERROR
            SyncResult(syncedCount = 0, failedCount = pendingItems.size, conflicts = emptyList())
        }
    }

    override fun observeSyncStatus(): Flow<SyncStatus> {
        return syncStatusFlow.asStateFlow().distinctUntilChanged()
    }

    override fun getCurrentSyncStatus(): SyncStatus {
        return syncStatusFlow.value
    }
}
