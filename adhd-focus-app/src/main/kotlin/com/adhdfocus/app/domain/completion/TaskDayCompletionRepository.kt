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
}
