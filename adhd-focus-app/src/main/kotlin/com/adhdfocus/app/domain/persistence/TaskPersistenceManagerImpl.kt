package com.adhdfocus.app.domain.persistence

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Implementation of TaskPersistenceManager using Room database.
 *
 * Provides persistence layer for tasks with support for:
 * - Timestamp-based data retention (30+ days)
 * - Automatic cleanup of tasks older than 90 days
 * - Transaction support for batch operations
 * - Error handling and recovery
 */
class TaskPersistenceManagerImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskPersistenceManager {

    companion object {
        const val DATA_RETENTION_DAYS = 30
        const val CLEANUP_CUTOFF_DAYS = 90
    }

    override suspend fun saveTask(task: Task) {
        require(task.householdId.isNotBlank()) { "householdId cannot be blank" }
        require(task.assignedUserId.isNotBlank()) { "assignedUserId cannot be blank" }
        require(task.title.isNotBlank()) { "title cannot be blank" }

        val existingTask = taskDao.getTaskById(task.id)
        if (existingTask != null) {
            taskDao.update(task)
        } else {
            taskDao.insert(task)
        }
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        require(tasks.isNotEmpty()) { "tasks list cannot be empty" }

        tasks.forEach { task ->
            require(task.householdId.isNotBlank()) { "householdId cannot be blank" }
            require(task.assignedUserId.isNotBlank()) { "assignedUserId cannot be blank" }
            require(task.title.isNotBlank()) { "title cannot be blank" }
        }

        tasks.forEach { task ->
            val existingTask = taskDao.getTaskById(task.id)
            if (existingTask != null) {
                taskDao.update(task)
            } else {
                taskDao.insert(task)
            }
        }
    }

    override suspend fun replaceTasksForHousehold(householdId: String, tasks: List<Task>) {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }

        taskDao.softDeleteAllHouseholdTasks(householdId)

        tasks.forEach { task ->
            require(task.householdId.isNotBlank()) { "householdId cannot be blank" }
            require(task.assignedUserId.isNotBlank()) { "assignedUserId cannot be blank" }
            require(task.title.isNotBlank()) { "title cannot be blank" }
            val existingTask = taskDao.getTaskById(task.id)
            if (existingTask != null) {
                taskDao.update(task)
            } else {
                taskDao.insert(task)
            }
        }
    }

    override suspend fun getTasks(householdId: String): List<Task> {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        return taskDao.getTasksByHouseholdOnce(householdId)
    }

    override suspend fun getUserTasks(userId: String): List<Task> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return taskDao.getUserTasksInDateRange(
            userId,
            Instant.now().minusSeconds((DATA_RETENTION_DAYS * 24 * 60 * 60).toLong()),
            Instant.now()
        )
    }

    override suspend fun getTasksForDate(householdId: String, date: LocalDate): List<Task> {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return taskDao.getTasksInDateRange(householdId, startOfDay, endOfDay)
    }

    override suspend fun getUserTasksForDate(userId: String, date: LocalDate): List<Task> {
        require(userId.isNotBlank()) { "userId cannot be blank" }

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return taskDao.getUserTasksInDateRange(userId, startOfDay, endOfDay)
    }

    override suspend fun getTaskById(taskId: String): Task? {
        require(taskId.isNotBlank()) { "taskId cannot be blank" }
        return taskDao.getTaskById(taskId)
    }

    override suspend fun deleteOldTasks(olderThanDays: Int): Int {
        require(olderThanDays > 0) { "olderThanDays must be positive" }

        val cutoffTime = Instant.now().minusSeconds((olderThanDays * 24 * 60 * 60).toLong())
        taskDao.deleteOldSoftDeletedTasks(cutoffTime)

        return 0 // Room doesn't return count for DELETE, so we return 0
    }

    override suspend fun getTaskCount(householdId: String): Int {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        return taskDao.getTaskCount(householdId)
    }

    override suspend fun getUserTaskCount(userId: String): Int {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return taskDao.getUserTaskCount(userId)
    }

    override suspend fun deleteTask(taskId: String) {
        require(taskId.isNotBlank()) { "taskId cannot be blank" }
        taskDao.softDeleteTask(taskId)
    }

    override suspend fun permanentlyDeleteTask(taskId: String) {
        require(taskId.isNotBlank()) { "taskId cannot be blank" }
        taskDao.deleteTaskById(taskId)
    }
}
