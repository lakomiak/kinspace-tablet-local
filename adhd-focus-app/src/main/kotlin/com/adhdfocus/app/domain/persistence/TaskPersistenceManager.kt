package com.adhdfocus.app.domain.persistence

import com.adhdfocus.app.data.model.Task
import java.time.LocalDate

/**
 * TaskPersistenceManager handles persistence of tasks to local database.
 *
 * Responsibilities:
 * - Save tasks to local database
 * - Retrieve tasks by various criteria
 * - Delete old tasks based on retention policy
 * - Manage task timestamps
 * - Support transaction-based operations
 *
 * This interface enables offline capability and data retention policies
 * as specified in Requirement 12: Data Persistence and Offline Capability.
 */
interface TaskPersistenceManager {
    /**
     * Saves a single task to the local database.
     * If the task already exists, it will be updated.
     *
     * @param task The task to save
     * @throws IllegalArgumentException if task data is invalid
     */
    suspend fun saveTask(task: Task)

    /**
     * Saves multiple tasks to the local database in a transaction.
     * All tasks are saved atomically - either all succeed or all fail.
     *
     * @param tasks List of tasks to save
     * @throws IllegalArgumentException if any task data is invalid
     */
    suspend fun saveTasks(tasks: List<Task>)

    /**
     * Replaces all tasks for a household with the provided set.
     *
     * This is used for cloud refreshes where the remote household list is the source of truth.
     * Tasks that are not present in the incoming list are soft deleted locally.
     *
     * @param householdId The household whose task set should be replaced
     * @param tasks The current canonical tasks for that household
     */
    suspend fun replaceTasksForHousehold(householdId: String, tasks: List<Task>)

    /**
     * Retrieves all tasks for a household.
     *
     * @param householdId The household ID
     * @return List of all non-deleted tasks for the household
     */
    suspend fun getTasks(householdId: String): List<Task>

    /**
     * Retrieves tasks for a specific user.
     *
     * @param userId The user ID
     * @return List of all non-deleted tasks assigned to the user
     */
    suspend fun getUserTasks(userId: String): List<Task>

    /**
     * Retrieves tasks for a specific date.
     *
     * @param householdId The household ID
     * @param date The date to retrieve tasks for
     * @return List of tasks created on the specified date
     */
    suspend fun getTasksForDate(householdId: String, date: LocalDate): List<Task>

    /**
     * Retrieves tasks for a specific user on a specific date.
     *
     * @param userId The user ID
     * @param date The date to retrieve tasks for
     * @return List of tasks assigned to the user created on the specified date
     */
    suspend fun getUserTasksForDate(userId: String, date: LocalDate): List<Task>

    /**
     * Retrieves a single task by ID.
     *
     * @param taskId The task ID
     * @return The task or null if not found
     */
    suspend fun getTaskById(taskId: String): Task?

    /**
     * Deletes tasks older than the specified number of days.
     * This implements the cleanup mechanism for data retention policy.
     *
     * @param olderThanDays Number of days - tasks older than this will be deleted
     * @return Number of tasks deleted
     */
    suspend fun deleteOldTasks(olderThanDays: Int): Int

    /**
     * Gets the total count of tasks for a household.
     *
     * @param householdId The household ID
     * @return Total number of non-deleted tasks
     */
    suspend fun getTaskCount(householdId: String): Int

    /**
     * Gets the total count of tasks for a user.
     *
     * @param userId The user ID
     * @return Total number of non-deleted tasks assigned to the user
     */
    suspend fun getUserTaskCount(userId: String): Int

    /**
     * Soft deletes a task (marks as deleted without removing from database).
     *
     * @param taskId The task ID to delete
     */
    suspend fun deleteTask(taskId: String)

    /**
     * Permanently removes a task from the database.
     *
     * @param taskId The task ID to remove
     */
    suspend fun permanentlyDeleteTask(taskId: String)
}
