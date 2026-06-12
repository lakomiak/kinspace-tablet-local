package com.adhdfocus.app.data.repository

import android.util.Log
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.SyncResult
import javax.inject.Inject
import java.time.DayOfWeek
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
    private val tag = "TaskRepository"

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
            updatedAt = java.time.Instant.now()
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
            completedAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
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
            updatedAt = java.time.Instant.now()
        )
        taskDao.update(deletedTask)
    }

    /**
     * Gets the tasks that should be visible today for the household.
     *
     * The tablet Home screen is pinned to one family member, so we mirror the
     * source app's member-scoped Today view and advance repeating tasks before
     * applying the due-date filter.
     *
     * @param householdId Household ID
     * @param userId Assigned member ID, kept for logging/context
     * @param memberName Assigned member display name fallback, kept for logging/context
     * @return List of tasks that are active for today
     */
    suspend fun getTasksForToday(
        householdId: String,
        userId: String,
        memberName: String? = null
    ): List<Task> {
        return getTasksForDate(householdId, userId, LocalDate.now(), memberName)
    }

    suspend fun getTasksForDate(
        householdId: String,
        userId: String,
        targetDate: LocalDate,
        memberName: String? = null
    ): List<Task> {
        val today = targetDate
        val tasks = taskDao.getTasksByHouseholdOnce(householdId)
        val normalizedTasks = tasks.map { task -> advanceRepeatingTask(task, today) }
        return normalizedTasks.filter { task ->
            !task.isDeleted &&
                matchesPinnedMember(task, userId, memberName) &&
                shouldShowTaskOnDate(task, today)
        }.also { filtered ->
            val visibleSummary = filtered.joinToString(", ") { task ->
                "${task.title}|assignee=${task.assignedUserId}|due=${task.dueDate}|repeat=${task.repeatRule}|status=${task.status}"
            }
            Log.d(
                tag,
                "getTasksForDate householdId=$householdId userId=$userId memberName=$memberName targetDate=$today loaded=${tasks.size} filtered=${filtered.size} tasks=[$visibleSummary]"
            )
        }
    }

    private fun matchesPinnedMember(task: Task, userId: String, memberName: String?): Boolean {
        val assigned = task.assignedUserId.trim().lowercase()
        val userKey = userId.trim().lowercase()
        val memberKey = memberName?.trim()?.lowercase().orEmpty()

        if (assigned.isBlank()) return false
        if (userKey.isNotBlank() && assigned == userKey) return true
        if (memberKey.isNotBlank() && assigned == memberKey) return true
        return false
    }

    private fun advanceRepeatingTask(task: Task, today: LocalDate): Task {
        val dueInstant = task.dueDate ?: return task
        val repeat = task.repeatRule.trim().lowercase()
        if (repeat == "once") return task

        val dueDate = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        if (!dueDate.isBefore(today)) return task

        if (repeat == "weekdays" || repeat == "weekends") {
            var next = dueDate
            while (next.isBefore(today)) {
                next = next.plusDays(1)
                while (!matchesSpecialRepeat(next, repeat)) {
                    next = next.plusDays(1)
                }
            }

            return task.copy(
                dueDate = next.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                status = TaskStatus.INCOMPLETE,
                completedAt = null
            )
        }

        val (interval, unit) = parseRepeatConfig(repeat)
        if (interval <= 0 || unit == null) return task

        var next = dueDate
        while (next.isBefore(today)) {
            next = when (unit) {
                "day" -> next.plusDays(interval.toLong())
                "week" -> next.plusWeeks(interval.toLong())
                "month" -> next.plusMonths(interval.toLong())
                "year" -> next.plusYears(interval.toLong())
                else -> return task
            }
        }

        return task.copy(
            dueDate = next.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            status = TaskStatus.INCOMPLETE,
            completedAt = null
        )
    }

    private fun shouldShowTaskOnDate(task: Task, targetDate: LocalDate): Boolean {
        val dueInstant = task.dueDate ?: return true
        val dueDateLocal = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        val repeat = task.repeatRule.trim().lowercase()

        if (repeat == "daily") return true
        if (repeat == "weekdays") return !isWeekend(targetDate) && !targetDate.isBefore(dueDateLocal)
        if (repeat == "weekends") return isWeekend(targetDate) && !targetDate.isBefore(dueDateLocal)
        if (dueDateLocal == targetDate) return true
        return false
    }

    private fun parseRepeatConfig(value: String): Pair<Int, String?> {
        if (value.isBlank() || value == "once") return 0 to null
        if (value == "daily") return 1 to "day"
        if (value == "weekly") return 1 to "week"
        if (value == "monthly") return 1 to "month"
        if (value == "yearly") return 1 to "year"

        if (value.startsWith("custom:")) {
            val parts = value.split(":")
            val interval = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: return 0 to null
            val unit = parts.getOrNull(2)
            if (unit in setOf("day", "week", "month", "year")) {
                return interval to unit
            }
        }

        return 0 to null
    }

    private fun matchesSpecialRepeat(date: LocalDate, repeat: String): Boolean {
        return when (repeat) {
            "weekdays" -> !isWeekend(date)
            "weekends" -> isWeekend(date)
            else -> false
        }
    }

    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
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
        return taskDao.getTasksByHouseholdOnce(householdId)
    }

    suspend fun getDistinctTaskTitlesByHousehold(householdId: String): List<String> {
        return taskDao.getDistinctTaskTitlesByHousehold(householdId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    /**
     * Gets all tasks for a user.
     *
     * @param householdId Household ID
     * @param userId User ID
     * @return List of user's tasks
     */
    suspend fun getTasksByUser(householdId: String, userId: String): List<Task> {
        return taskDao.getTasksByUserOnce(userId)
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
