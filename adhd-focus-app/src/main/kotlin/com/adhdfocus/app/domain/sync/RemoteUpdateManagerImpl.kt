package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of RemoteUpdateManager.
 *
 * Handles applying remote updates to the local database with conflict resolution,
 * emitting update events for UI refresh, and tracking timer state.
 */
class RemoteUpdateManagerImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val conflictResolver: ConflictResolver
) : RemoteUpdateManager {
    private val _updateEvents = MutableSharedFlow<UpdateEvent>(replay = 0)
    private val timerActive = AtomicBoolean(false)
    private val queuedUpdates = mutableListOf<WebSocketEvent>()

    override suspend fun applyRemoteUpdate(event: WebSocketEvent): UpdateResult {
        return try {
            when (event) {
                is WebSocketEvent.TaskUpdated -> applyTaskUpdate(event.task)
                is WebSocketEvent.TaskDeleted -> applyTaskDeletion(event.taskId)
                is WebSocketEvent.TaskCreated -> applyTaskCreation(event.task)
                else -> UpdateResult(success = false, message = "Unknown event type")
            }
        } catch (e: Exception) {
            _updateEvents.emit(UpdateEvent.Error("Failed to apply update: ${e.message}"))
            UpdateResult(success = false, message = e.message ?: "Unknown error")
        }
    }

    override fun observeUpdates(): Flow<UpdateEvent> = _updateEvents.asSharedFlow()

    override fun isTimerActive(): Boolean = timerActive.get()

    override fun setTimerActive(active: Boolean) {
        timerActive.set(active)
    }

    override suspend fun applyQueuedUpdates() {
        val updates = queuedUpdates.toList()
        queuedUpdates.clear()

        var appliedCount = 0
        for (update in updates) {
            val result = applyRemoteUpdate(update)
            if (result.success) {
                appliedCount++
            }
        }

        if (appliedCount > 0) {
            _updateEvents.emit(UpdateEvent.UpdatesApplied(appliedCount))
        }
    }

    /**
     * Applies a task update with timestamp-based conflict resolution.
     */
    private suspend fun applyTaskUpdate(remoteTask: Task): UpdateResult {
        val localTask = taskDao.getTaskById(remoteTask.id)

        if (localTask == null) {
            // Task doesn't exist locally, insert it
            taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
            _updateEvents.emit(UpdateEvent.TaskUpdated(remoteTask.id, remoteTask))
            return UpdateResult(success = true, message = "Task inserted")
        }

        // Check for conflict
        if (conflictResolver.isConflict(localTask, remoteTask)) {
            val resolvedTask = conflictResolver.resolveConflict(localTask, remoteTask)
            val reason = conflictResolver.getConflictReason(localTask, remoteTask)
            
            // Log conflict for debugging
            println("Conflict resolved for task ${remoteTask.id}: $reason")
            
            taskDao.update(resolvedTask.copy(syncStatus = SyncStatus.SYNCED))
            _updateEvents.emit(UpdateEvent.TaskUpdated(remoteTask.id, resolvedTask))
            return UpdateResult(
                success = true,
                message = "Task updated with conflict resolution",
                conflictResolved = true
            )
        } else {
            // No conflict, apply remote version
            taskDao.update(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
            _updateEvents.emit(UpdateEvent.TaskUpdated(remoteTask.id, remoteTask))
            return UpdateResult(success = true, message = "Task updated")
        }
    }

    /**
     * Applies a task deletion (soft delete).
     */
    private suspend fun applyTaskDeletion(taskId: String): UpdateResult {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            // Soft delete
            taskDao.update(task.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
            _updateEvents.emit(UpdateEvent.TaskDeleted(taskId))
            return UpdateResult(success = true, message = "Task deleted")
        }
        return UpdateResult(success = false, message = "Task not found")
    }

    /**
     * Applies a task creation.
     */
    private suspend fun applyTaskCreation(remoteTask: Task): UpdateResult {
        val existingTask = taskDao.getTaskById(remoteTask.id)
        if (existingTask == null) {
            taskDao.insert(remoteTask.copy(syncStatus = SyncStatus.SYNCED))
            _updateEvents.emit(UpdateEvent.TaskCreated(remoteTask))
            return UpdateResult(success = true, message = "Task created")
        }
        return UpdateResult(success = false, message = "Task already exists")
    }
}
