package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.SyncResult
import javax.inject.Inject
import java.time.LocalDate
import java.time.ZoneId

/**
 * TaskRepository provides data access abstraction for tasks.
 *
 * Handles:
 * - Task CRUD operations
 * - Local persistence
 * - Sync queue management
 * - Task filtering and retrieval
 */
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    /**
     * Creates a new task.
     *
     * @param task Task to create
     * @return Created task
     */
    suspend fun createTask(task: Task): Task {
        taskDao.insert(task)
        return task
    }

    /**
     * Updates an existing task.
     *
     * @param taskId ID of task to update
     * @param updates Map of field names to new values
     * @return Updated task
     */
    suspend fun updateTask(taskId: String, updates: Map<String, Any>): Task {
        val task = taskDao.getTaskById(taskId) ?: throw IllegalArgumentException("Task not found")
        val updatedTask = task.copy(
            title = (updates["title"] as? String) ?: task.title,
            description = (updates["description"] as? String) ?: task.description,
            todoGroup = (updates["todoGroup"] as? String) ?: task.todoGroup,
            estimatedDurationMinutes = (updates["estimatedDurationMinutes"] as? Int) ?: task.estimatedDurationMinutes,
            status = (updates["status"] as? TaskStatus) ?: task.status,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.update(updatedTask)
        return updatedTask
    }

    /**
     * Marks a task as completed.
     *
     * @param taskId ID of task to complete
     * @return Completed task
     */
    suspend fun completeTask(taskId: String): Task {
        val task = taskDao.getTaskById(taskId) ?: throw IllegalArgumentException("Task not found")
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.update(completedTask)
        return completedTask
    }

    /**
     * Deletes a task (soft delete).
     *
     * @param taskId ID of task to delete
     */
    suspend fun deleteTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: throw IllegalArgumentException("Task not found")
        val deletedTask = task.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.update(deletedTask)
    }

    /**
     * Gets all tasks for today.
     *
     * @param householdId Household ID
     * @param userId User ID (optional)
     * @return List of today's tasks
     */
    suspend fun getTasksForToday(householdId: String, userId: String? = null): List<Task> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return if (userId != null) {
            taskDao.getTasksByUserAndDateRange(householdId, userId, startOfDay, endOfDay)
        } else {
            taskDao.getTasksByHouseholdAndDateRange(householdId, startOfDay, endOfDay)
        }
    }

    /**
     * Gets a task by ID.
     *
     * @param taskId Task ID
     * @return Task or null if not found
     */
    suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)
    }

    /**
     * Gets all tasks for a household.
     *
     * @param householdId Household ID
     * @return List of all tasks
     */
    suspend fun getTasksByHousehold(householdId: String): List<Task> {
        return taskDao.getTasksByHousehold(householdId)
    }

    /**
     * Gets all tasks for a user.
     *
     * @param householdId Household ID
     * @param userId User ID
     * @return List of user's tasks
     */
    suspend fun getTasksByUser(householdId: String, userId: String): List<Task> {
        return taskDao.getTasksByUser(householdId, userId)
    }

    /**
     * Synchronizes pending changes to cloud.
     *
     * @return Sync result
     */
    suspend fun syncPendingChanges(): SyncResult {
        // TODO: Implement actual sync logic
        return SyncResult(success = true, syncedCount = 0)
    }
}
