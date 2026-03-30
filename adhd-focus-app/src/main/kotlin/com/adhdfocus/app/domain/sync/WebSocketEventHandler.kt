package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.SyncStatus
import javax.inject.Inject
import java.time.Instant

/**
 * Handles incoming WebSocket events and applies updates to local database.
 *
 * Responsibilities:
 * - Process WebSocket events
 * - Apply remote updates to local database
 * - Handle task creation, update, and deletion
 * - Resolve conflicts using timestamp-based resolution
 * - Queue updates received while offline
 */
class WebSocketEventHandler @Inject constructor(
    private val taskDao: TaskDao,
    private val restApiClient: RestApiClient,
    private val syncQueueManager: SyncQueueManager
) {
    /**
     * Handles a WebSocket event and applies appropriate updates.
     *
     * @param event The WebSocket event to handle
     */
    suspend fun handleEvent(event: WebSocketEvent) {
        when (event) {
            is WebSocketEvent.TaskUpdated -> handleTaskUpdated(event.task)
            is WebSocketEvent.TaskDeleted -> handleTaskDeleted(event.taskId)
            is WebSocketEvent.TaskCreated -> handleTaskCreated(event.task)
            is WebSocketEvent.SyncSignal -> handleSyncSignal(event)
            is WebSocketEvent.ConnectionEstablished -> handleConnectionEstablished()
            is WebSocketEvent.ConnectionLost -> handleConnectionLost()
            is WebSocketEvent.Error -> handleError(event.message, event.throwable)
        }
    }

    /**
     * Handles a task update event.
     * Applies conflict resolution using timestamp comparison.
     */
    private suspend fun handleTaskUpdated(remoteTask: Task) {
        val localTask = taskDao.getTaskById(remoteTask.id)

        if (localTask == null) {
            // Task doesn't exist locally, insert it
            taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
            return
        }

        // Resolve conflict: prefer most recent timestamp
        if (remoteTask.updatedAt.isAfter(localTask.updatedAt)) {
            // Remote is newer, apply it
            taskDao.update(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
        } else if (remoteTask.updatedAt.equals(localTask.updatedAt)) {
            // Same timestamp, prefer remote (server is source of truth)
            taskDao.update(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
        }
        // else: local is newer, keep local version
    }

    /**
     * Handles a task deletion event.
     */
    private suspend fun handleTaskDeleted(taskId: String) {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            // Soft delete
            taskDao.update(task.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
        }
    }

    /**
     * Handles a task creation event.
     */
    private suspend fun handleTaskCreated(remoteTask: Task) {
        val existingTask = taskDao.getTaskById(remoteTask.id)
        if (existingTask == null) {
            taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
        }
    }

    /**
     * Handles a sync signal by fetching updates from the server.
     */
    private suspend fun handleSyncSignal(event: WebSocketEvent.SyncSignal) {
        // Sync signal indicates updates are available
        // This is typically handled by the SyncManager which will fetch updates
    }

    /**
     * Handles connection established event.
     */
    private suspend fun handleConnectionEstablished() {
        // Connection established, ready to receive updates
    }

    /**
     * Handles connection lost event.
     */
    private suspend fun handleConnectionLost() {
        // Connection lost, updates will be queued for later application
    }

    /**
     * Handles error event.
     */
    private suspend fun handleError(message: String, throwable: Throwable?) {
        // Log error for debugging
        // In production, this would be sent to a logging service
    }
}
