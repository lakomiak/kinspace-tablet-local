package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adhdfocus.app.data.model.TaskDayCompletion

@Dao
interface TaskDayCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: TaskDayCompletion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(completions: List<TaskDayCompletion>)

    @Query(
        """
        DELETE FROM task_day_completions
        WHERE householdId = :householdId
        AND userId = :userId
        AND taskId = :taskId
        AND targetDate = :targetDate
        """
    )
    suspend fun delete(
        householdId: String,
        userId: String,
        taskId: String,
        targetDate: String
    )

    @Query(
        """
        DELETE FROM task_day_completions
        WHERE householdId = :householdId
        AND userId = :userId
        AND targetDate = :targetDate
        """
    )
    suspend fun deleteForDate(
        householdId: String,
        userId: String,
        targetDate: String
    )

    @Query(
        """
        SELECT * FROM task_day_completions
        WHERE householdId = :householdId
        AND userId = :userId
        AND targetDate = :targetDate
        """
    )
    suspend fun getCompletionsForDate(
        householdId: String,
        userId: String,
        targetDate: String
    ): List<TaskDayCompletion>

    @Query(
        """
        SELECT COUNT(*) FROM task_day_completions
        WHERE householdId = :householdId
        AND userId = :userId
        AND isCompleted = 1
        """
    )
    suspend fun getCompletedCountForUser(
        householdId: String,
        userId: String
    ): Int

    @Query(
        """
        SELECT * FROM task_day_completions
        WHERE householdId = :householdId
        AND userId = :userId
        AND isCompleted = 1
        ORDER BY updatedAt DESC
        """
    )
    suspend fun getCompletedEntriesForUser(
        householdId: String,
        userId: String
    ): List<TaskDayCompletion>
}
