package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adhdfocus.app.data.model.TaskSessionMetric

@Dao
interface TaskSessionMetricDao {
    @Insert
    suspend fun insert(metric: TaskSessionMetric): Long

    @Query(
        """
        SELECT * FROM task_session_metrics
        WHERE householdId = :householdId AND userId = :userId
        ORDER BY endedAt DESC
        """
    )
    suspend fun getSessionsForUser(householdId: String, userId: String): List<TaskSessionMetric>

    @Query(
        """
        SELECT * FROM task_session_metrics
        WHERE householdId = :householdId
        ORDER BY endedAt DESC
        """
    )
    suspend fun getSessionsForHousehold(householdId: String): List<TaskSessionMetric>
}
