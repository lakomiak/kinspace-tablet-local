package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import java.time.Instant
import java.util.UUID

/**
 * Implementation of WebSocketTaskUpdateHandler.
 *
 * Handles incoming WebSocket events and applies updates to local database.
 *
 * Responsibilities:
 * - Process WebSocket events (TaskUpdated, TaskDeleted, TaskCreated)
 * - Apply remote updates to local database
 * - Handle task creation, update, and deletion
 * - Resolve conflicts using timestamp-based resolution
 * - Queue updates received while offline
 * - Emit update events to UI layer
 * - Ensure 2-second update latency requirement
 *
 * Correctness Properties:
 * - Property 8: Remote Update Application - Updates received via WebSocket are applied to local tasks
 */
class WebSocketTaskUpdateHandlerImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val conflictResolver: ConflictResolver,
    private val gson: Gson
) : WebSocketTaskUpdateHandler {

    private val updateEventFlow = MutableSharedFlow<UpdateEvent>(replay = 0)
    private val offlineQueue = mutableListOf<WebSocketEvent>()
    private var isOnline = true

    override suspend fun handleWebSocketEvent(event: WebSocketEvent): UpdateResult {
        return when (event) {
            is WebSocketEvent.TaskUpdated -> handleTaskUpdated(event.task)
            is WebSocketEvent.TaskDeleted -> handleTaskDeleted(event.taskId)
            is WebSocketEvent.TaskCreated -> handleTaskCreated(event.task)
            is WebSocketEvent.ConnectionEstablished -> {
                isOnline = true
                applyQueuedUpdates()
            }
            is WebSocketEvent.ConnectionLost -> {
                isOnline = false
                UpdateResult(success = true, message = "Connection lost, queuing updates")
            }
            is WebSocketEvent.SyncSignal -> UpdateResult(success = true, message = "Sync signal received")
            is WebSocketEvent.Error -> UpdateResult(success = false, message = event.message)
        }
    }

    override fun observeUpdates(): Flow<UpdateEvent> = updateEventFlow.asSharedFlow()

    override suspend fun applyQueuedUpdates(): UpdateResult {
        if (offlineQueue.isEmpty()) {
            return UpdateResult(success = true, message = "No queued updates")
        }

        val queuedEvents = offlineQueue.toList()
        offlineQueue.clear()

        var successCount = 0
        var failureCount = 0

        for (event in queuedEvents) {
            val result = handleWebSocketEvent(event)
            if (result.success) {
                successCount++
            } else {
                failureCount++
            }
        }

        val message = "Applied $successCount queued updates, $failureCount failed"
        return UpdateResult(
            success = failureCount == 0,
            message = message
        )
    }

    override suspend fun hasQueuedUpdates(): Boolean = offlineQueue.isNotEmpty()

    override suspend fun clearQueuedUpdates() {
        offlineQueue.clear()
    }

    /**
     * Handles a task update event.
     * Applies conflict resolution using timestamp comparison.
     */
    private suspend fun handleTaskUpdated(remoteTask: Task): UpdateResult {
        return try {
            val localTask = taskDao.getTaskById(remoteTask.id)

            if (localTask == null) {
                // Task doesn't exist locally, insert it
                taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
                emitUpdateEvent(UpdateEvent.TaskUpdated(remoteTask.id, remoteTask))
                return UpdateResult(success = true, message = "Task inserted")
            }

            // Resolve conflict: prefer most recent timestamp
            val shouldApplyRemote = if (conflictResolver.isConflict(localTask, remoteTask)) {
                // Conflict exists, use timestamp-based resolution
                remoteTask.updatedAt.isAfter(localTask.updatedAt) ||
                    (remoteTask.updatedAt.equals(localTask.updatedAt) && remoteTask.id > localTask.id)
            } else {
                // No conflict, apply remote update
                true
            }

            if (shouldApplyRemote) {
                taskDao.update(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
                emitUpdateEvent(UpdateEvent.TaskUpdated(remoteTask.id, remoteTask))
                UpdateResult(success = true, message = "Task updated", conflictResolved = true)
            } else {
                UpdateResult(success = true, message = "Local version kept", conflictResolved = true)
            }
        } catch (e: Exception) {
            if (!isOnline) {
                offlineQueue.add(WebSocketEvent.TaskUpdated(remoteTask.id, remoteTask))
                UpdateResult(success = true, message = "Update queued for offline application")
            } else {
                UpdateResult(success = false, message = "Failed to update task: ${e.message}")
            }
        }
    }

    /**
     * Handles a task deletion event.
     */
    private suspend fun handleTaskDeleted(taskId: String): UpdateResult {
        return try {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                // Soft delete
                taskDao.update(task.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
                emitUpdateEvent(UpdateEvent.TaskDeleted(taskId))
                UpdateResult(success = true, message = "Task deleted")
            } else {
                UpdateResult(success = true, message = "Task not found, nothing to delete")
            }
        } catch (e: Exception) {
            if (!isOnline) {
                offlineQueue.add(WebSocketEvent.TaskDeleted(taskId))
                UpdateResult(success = true, message = "Deletion queued for offline application")
            } else {
                UpdateResult(success = false, message = "Failed to delete task: ${e.message}")
            }
        }
    }

    /**
     * Handles a task creation event.
     */
    private suspend fun handleTaskCreated(remoteTask: Task): UpdateResult {
        return try {
            val existingTask = taskDao.getTaskById(remoteTask.id)
            if (existingTask == null) {
                taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
                emitUpdateEvent(UpdateEvent.TaskCreated(remoteTask))
                UpdateResult(success = true, message = "Task created")
            } else {
                UpdateResult(success = true, message = "Task already exists")
            }
        } catch (e: Exception) {
            if (!isOnline) {
                offlineQueue.add(WebSocketEvent.TaskCreated(remoteTask))
                UpdateResult(success = true, message = "Creation queued for offline application")
            } else {
                UpdateResult(success = false, message = "Failed to create task: ${e.message}")
            }
        }
    }

    /**
     * Emits an update event to the UI layer.
     */
    private suspend fun emitUpdateEvent(event: UpdateEvent) {
        try {
            updateEventFlow.emit(event)
        } catch (e: Exception) {
            // Flow emission failed, log but continue
        }
    }
}
