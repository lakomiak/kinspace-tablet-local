package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.domain.task.TaskManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import java.time.Instant

/**
 * Implementation of TimerAwareUpdateApplier.
 *
 * Manages update application while respecting active timer state.
 * Queues updates when timer is active and applies them when timer completes.
 */
class TimerAwareUpdateApplierImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskManager: TaskManager,
    private val realTimeUpdateManager: RealTimeUpdateManager
) : TimerAwareUpdateApplier {

    private val queuedUpdateEventFlow = MutableSharedFlow<QueuedUpdateEvent>(replay = 0)
    private val updateQueue = mutableListOf<UpdateEvent>()
    private var isTimerActive = false

    override suspend fun applyUpdate(event: UpdateEvent): UpdateResult {
        return if (isTimerActive) {
            // Queue update if timer is active
            if (queueUpdate(event)) {
                UpdateResult(success = true, message = "Update queued due to active timer")
            } else {
                UpdateResult(success = false, message = "Failed to queue update")
            }
        } else {
            // Apply update immediately if timer is inactive
            applyUpdateDirectly(event)
        }
    }

    override suspend fun queueUpdate(event: UpdateEvent): Boolean {
        return try {
            updateQueue.add(event)
            val taskId = getTaskIdFromEvent(event)
            queuedUpdateEventFlow.emit(QueuedUpdateEvent.UpdateQueued(taskId, updateQueue.size))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun applyQueuedUpdates(): UpdateResult {
        return try {
            val count = updateQueue.size
            val failedUpdates = mutableListOf<String>()

            // Apply updates in FIFO order
            for (event in updateQueue) {
                try {
                    applyUpdateDirectly(event)
                } catch (e: Exception) {
                    failedUpdates.add(getTaskIdFromEvent(event))
                }
            }

            updateQueue.clear()
            queuedUpdateEventFlow.emit(QueuedUpdateEvent.UpdatesApplied(count - failedUpdates.size))

            UpdateResult(
                success = failedUpdates.isEmpty(),
                message = "Applied $count queued updates",
                conflictResolved = false
            )
        } catch (e: Exception) {
            UpdateResult(success = false, message = "Failed to apply queued updates")
        }
    }

    override suspend fun getQueuedUpdateCount(): Int = updateQueue.size

    override fun observeQueuedUpdates(): Flow<QueuedUpdateEvent> = queuedUpdateEventFlow.asSharedFlow()

    override suspend fun isTimerActive(): Boolean = isTimerActive

    override suspend fun setTimerActive(active: Boolean) {
        isTimerActive = active
        
        // Apply queued updates when timer completes
        if (!active && updateQueue.isNotEmpty()) {
            applyQueuedUpdates()
        }
    }

    override suspend fun clearQueuedUpdates(): Boolean {
        return try {
            val count = updateQueue.size
            updateQueue.clear()
            queuedUpdateEventFlow.emit(QueuedUpdateEvent.UpdatesCleared(count))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Applies an update directly to the database.
     */
    private suspend fun applyUpdateDirectly(event: UpdateEvent): UpdateResult {
        return try {
            when (event) {
                is UpdateEvent.TaskUpdated -> {
                    taskDao.update(event.task)
                    UpdateResult(success = true, message = "Task updated")
                }
                is UpdateEvent.TaskDeleted -> {
                    taskDao.deleteTaskById(event.taskId)
                    UpdateResult(success = true, message = "Task deleted")
                }
                is UpdateEvent.TaskCreated -> {
                    taskDao.insert(event.task)
                    UpdateResult(success = true, message = "Task created")
                }
                is UpdateEvent.UpdatesApplied -> {
                    UpdateResult(success = true, message = "Updates applied")
                }
                is UpdateEvent.Error -> {
                    UpdateResult(success = false, message = event.message)
                }
            }
        } catch (e: Exception) {
            UpdateResult(success = false, message = "Failed to apply update: ${e.message}")
        }
    }

    /**
     * Extracts task ID from an update event.
     */
    private fun getTaskIdFromEvent(event: UpdateEvent): String {
        return when (event) {
            is UpdateEvent.TaskUpdated -> event.taskId
            is UpdateEvent.TaskDeleted -> event.taskId
            is UpdateEvent.TaskCreated -> event.task.id
            else -> "unknown"
        }
    }
}
