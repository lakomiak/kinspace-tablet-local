package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for handling WebSocket task update events.
 *
 * Responsibilities:
 * - Process incoming WebSocket events (TaskUpdated, TaskDeleted, TaskCreated)
 * - Apply updates to local database via TaskManager
 * - Emit update events to UI layer for real-time refresh
 * - Handle conflicts using existing ConflictResolver
 * - Maintain offline queue for updates received while offline
 * - Ensure 2-second update latency requirement
 *
 * Correctness Properties:
 * - Property 8: Remote Update Application - Updates received via WebSocket are applied to local tasks
 */
interface WebSocketTaskUpdateHandler {
    /**
     * Handles a WebSocket event and applies appropriate updates.
     *
     * Processes task creation, update, and deletion events from WebSocket.
     * Applies updates to local database and emits events for UI refresh.
     * Handles conflicts using timestamp-based resolution.
     * Queues updates received while offline for later application.
     *
     * @param event The WebSocket event to handle
     * @return UpdateResult indicating success or failure
     */
    suspend fun handleWebSocketEvent(event: WebSocketEvent): UpdateResult

    /**
     * Observes update events for UI refresh.
     *
     * Emits UpdateEvent whenever a remote update is applied.
     * Used by UI layer to refresh task display in real-time.
     *
     * @return Flow of UpdateEvent emitted when updates are applied
     */
    fun observeUpdates(): Flow<UpdateEvent>

    /**
     * Applies queued updates that were received while offline.
     *
     * Called when connectivity is restored to apply all pending updates
     * that were queued while the device was offline.
     *
     * @return UpdateResult indicating success or failure
     */
    suspend fun applyQueuedUpdates(): UpdateResult

    /**
     * Checks if there are queued updates waiting to be applied.
     *
     * @return true if there are queued updates, false otherwise
     */
    suspend fun hasQueuedUpdates(): Boolean

    /**
     * Clears all queued updates.
     *
     * Use with caution - this will discard all pending updates.
     */
    suspend fun clearQueuedUpdates()
}
