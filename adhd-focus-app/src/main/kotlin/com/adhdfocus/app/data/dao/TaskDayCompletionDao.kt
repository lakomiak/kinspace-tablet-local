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
}
