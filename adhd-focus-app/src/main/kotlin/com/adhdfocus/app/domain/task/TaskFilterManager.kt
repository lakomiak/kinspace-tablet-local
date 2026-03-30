package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId

/**
 * TaskFilterManager handles filtering and organization of tasks.
 *
 * Provides:
 * - Daily task filtering (Property 1: Daily Task Filtering)
 * - Task organization by Todo_Group (Property 3: Task Organization)
 * - Status-based filtering
 * - Offline task caching support
 */
class TaskFilterManager {

    /**
     * Filters tasks to only include those created today.
     *
     * Property 1: Daily Task Filtering
     * - Only today's tasks should be displayed
     * - Tasks from other days should be filtered out
     *
     * @param tasks List of all tasks
     * @return List of tasks created today
     */
    fun filterTodaysTasks(tasks: List<Task>): List<Task> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return tasks.filter { task ->
            !task.isDeleted && 
            task.createdAt >= startOfDay && 
            task.createdAt < endOfDay
        }
    }

    /**
     * Filters tasks by status.
     *
     * @param tasks List of tasks
     * @param status Status to filter by
     * @return Filtered list
     */
    fun filterByStatus(tasks: List<Task>, status: TaskStatus): List<Task> {
        return tasks.filter { it.status == status && !it.isDeleted }
    }

    /**
     * Filters incomplete tasks.
     *
     * @param tasks List of tasks
     * @return List of incomplete tasks
     */
    fun filterIncomplete(tasks: List<Task>): List<Task> {
        return filterByStatus(tasks, TaskStatus.INCOMPLETE)
    }

    /**
     * Filters in-progress tasks.
     *
     * @param tasks List of tasks
     * @return List of in-progress tasks
     */
    fun filterInProgress(tasks: List<Task>): List<Task> {
        return filterByStatus(tasks, TaskStatus.IN_PROGRESS)
    }

    /**
     * Filters completed tasks.
     *
     * @param tasks List of tasks
     * @return List of completed tasks
     */
    fun filterCompleted(tasks: List<Task>): List<Task> {
        return filterByStatus(tasks, TaskStatus.COMPLETED)
    }

    /**
     * Organizes tasks by Todo_Group.
     *
     * Property 3: Task Organization
     * - Tasks should be organized by Todo_Group
     * - Each group should have clear section headers
     *
     * @param tasks List of tasks
     * @return Map of Todo_Group to list of tasks
     */
    fun organizeByTodoGroup(tasks: List<Task>): Map<String, List<Task>> {
        return tasks
            .filter { !it.isDeleted }
            .groupBy { it.todoGroup }
            .toSortedMap()
    }

    /**
     * Gets unique Todo_Groups from tasks.
     *
     * @param tasks List of tasks
     * @return Set of unique Todo_Group names
     */
    fun getUniqueTodoGroups(tasks: List<Task>): Set<String> {
        return tasks
            .filter { !it.isDeleted }
            .map { it.todoGroup }
            .toSet()
    }

    /**
     * Filters tasks by Todo_Group.
     *
     * @param tasks List of tasks
     * @param todoGroup Todo_Group to filter by
     * @return Filtered list
     */
    fun filterByTodoGroup(tasks: List<Task>, todoGroup: String): List<Task> {
        return tasks.filter { it.todoGroup == todoGroup && !it.isDeleted }
    }

    /**
     * Filters tasks that are pending sync.
     *
     * @param tasks List of tasks
     * @return List of tasks with PENDING sync status
     */
    fun filterPendingSync(tasks: List<Task>): List<Task> {
        return tasks.filter { 
            it.syncStatus == com.adhdfocus.app.data.model.SyncStatus.PENDING && !it.isDeleted 
        }
    }

    /**
     * Filters tasks that have been synced.
     *
     * @param tasks List of tasks
     * @return List of synced tasks
     */
    fun filterSynced(tasks: List<Task>): List<Task> {
        return tasks.filter { 
            it.syncStatus == com.adhdfocus.app.data.model.SyncStatus.SYNCED && !it.isDeleted 
        }
    }

    /**
     * Sorts tasks by creation date (newest first).
     *
     * @param tasks List of tasks
     * @return Sorted list
     */
    fun sortByCreatedDateDesc(tasks: List<Task>): List<Task> {
        return tasks.sortedByDescending { it.createdAt }
    }

    /**
     * Sorts tasks by creation date (oldest first).
     *
     * @param tasks List of tasks
     * @return Sorted list
     */
    fun sortByCreatedDateAsc(tasks: List<Task>): List<Task> {
        return tasks.sortedBy { it.createdAt }
    }

    /**
     * Sorts tasks by status (incomplete -> in-progress -> completed).
     *
     * @param tasks List of tasks
     * @return Sorted list
     */
    fun sortByStatus(tasks: List<Task>): List<Task> {
        return tasks.sortedBy { task ->
            when (task.status) {
                TaskStatus.INCOMPLETE -> 0
                TaskStatus.IN_PROGRESS -> 1
                TaskStatus.COMPLETED -> 2
            }
        }
    }

    /**
     * Gets task count by status.
     *
     * @param tasks List of tasks
     * @return Map of status to count
     */
    fun getCountByStatus(tasks: List<Task>): Map<TaskStatus, Int> {
        return tasks
            .filter { !it.isDeleted }
            .groupBy { it.status }
            .mapValues { it.value.size }
    }

    /**
     * Gets task count by Todo_Group.
     *
     * @param tasks List of tasks
     * @return Map of Todo_Group to count
     */
    fun getCountByTodoGroup(tasks: List<Task>): Map<String, Int> {
        return organizeByTodoGroup(tasks).mapValues { it.value.size }
    }
}