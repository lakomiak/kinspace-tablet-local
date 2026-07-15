package com.adhdfocus.app.domain.completion

import com.adhdfocus.app.data.dao.TaskDayCompletionDao
import com.adhdfocus.app.data.model.TaskDayCompletion
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class TaskDayCompletionRepository @Inject constructor(
    private val taskDayCompletionDao: TaskDayCompletionDao
) {
    suspend fun setCompletionForDate(
        householdId: String,
        userId: String,
        taskId: String,
        date: LocalDate,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            taskDayCompletionDao.upsert(
                TaskDayCompletion(
                    householdId = householdId,
                    userId = userId,
                    taskId = taskId,
                    targetDate = date.toString(),
                    isCompleted = true,
                    updatedAt = Instant.now()
                )
            )
        } else {
            taskDayCompletionDao.delete(householdId, userId, taskId, date.toString())
        }
    }

    suspend fun getCompletionsForDate(
        householdId: String,
        userId: String,
        date: LocalDate
    ): List<TaskDayCompletion> {
        return taskDayCompletionDao.getCompletionsForDate(householdId, userId, date.toString())
    }

    suspend fun getCompletedCountForYear(
        householdId: String,
        userId: String,
        year: Int
    ): Int {
        return taskDayCompletionDao.getCompletedCountForUserInDateRange(
            householdId = householdId,
            userId = userId,
            startDate = LocalDate.of(year, 1, 1).toString(),
            endDate = LocalDate.of(year, 12, 31).toString()
        )
    }

    suspend fun getCompletedDatesForYear(
        householdId: String,
        userId: String,
        year: Int
    ): List<LocalDate> {
        return taskDayCompletionDao.getCompletedDatesForUserInDateRange(
            householdId = householdId,
            userId = userId,
            startDate = LocalDate.of(year, 1, 1).toString(),
            endDate = LocalDate.of(year, 12, 31).toString()
        ).mapNotNull { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }

    suspend fun replaceCompletionsForDate(
        householdId: String,
        userId: String,
        date: LocalDate,
        completedTaskIds: List<String>,
        updatedAt: Instant = Instant.now()
    ) {
        val targetDate = date.toString()
        taskDayCompletionDao.deleteForDate(householdId, userId, targetDate)
        if (completedTaskIds.isEmpty()) {
            return
        }

        taskDayCompletionDao.upsertAll(
            completedTaskIds.distinct().map { taskId ->
                TaskDayCompletion(
                    householdId = householdId,
                    userId = userId,
                    taskId = taskId,
                    targetDate = targetDate,
                    isCompleted = true,
                    updatedAt = updatedAt
                )
            }
        )
    }
}
