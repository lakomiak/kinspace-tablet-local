package com.adhdfocus.app.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing cloud synchronization of pending changes.
 *
 * Responsibilities:
 * - Sync all pending changes when connectivity available
 * - Maintain sync status indicator
 * - Implement exponential backoff for failed attempts
 * - Resolve conflicts using timestamp-based resolution
 * - Refresh UI with latest data
 */
interface CloudSyncManager {
    /**
     * Synchronizes all pending changes to calendar-cloud.
     *
     * Retrieves pending changes from sync queue and sends them via REST API.
     * Implements exponential backoff for failed attempts.
     * Resolves conflicts by timestamp.
     *
     * @param householdId ID of the household
     * @param userId ID of the user
     * @return SyncResult with synced count, failed count, and conflicts
     */
    suspend fun syncPendingChanges(householdId: String, userId: String): SyncResult

    /**
     * Observes sync status changes.
     *
     * Emits sync status updates as they occur.
     * Emits immediately with current status on subscription.
     *
     * @return Flow that emits sync status changes
     */
    fun observeSyncStatus(): Flow<SyncStatus>

    /**
     * Gets the current sync status.
     *
     * @return Current SyncStatus
     */
    fun getCurrentSyncStatus(): SyncStatus
}
