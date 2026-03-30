package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.EfficiencyMetric
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface EfficiencyMetricDao {
    // ==================== Basic CRUD Operations ====================

    @Insert
    suspend fun insert(metric: EfficiencyMetric): Long

    @Update
    suspend fun update(metric: EfficiencyMetric)

    @Delete
    suspend fun delete(metric: EfficiencyMetric)

    // ==================== Retrieve by ID ====================

    @Query("SELECT * FROM efficiency_metrics WHERE id = :metricId")
    suspend fun getMetricById(metricId: String): EfficiencyMetric?

    @Query("SELECT * FROM efficiency_metrics WHERE id = :metricId")
    fun getMetricByIdFlow(metricId: String): Flow<EfficiencyMetric?>

    // ==================== Retrieve by Task ====================

    @Query("SELECT * FROM efficiency_metrics WHERE taskId = :taskId")
    suspend fun getMetricByTask(taskId: String): EfficiencyMetric?

    @Query("SELECT * FROM efficiency_metrics WHERE taskId = :taskId")
    fun getMetricByTaskFlow(taskId: String): Flow<EfficiencyMetric?>

    // ==================== Retrieve by User ====================

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY completedAt DESC")
    suspend fun getMetricsByUser(userId: String): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY completedAt DESC")
    fun getMetricsByUserFlow(userId: String): Flow<List<EfficiencyMetric>>

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getMetricsByUserOnce(userId: String): List<EfficiencyMetric>

    // ==================== Retrieve by Household ====================

    @Query("SELECT * FROM efficiency_metrics WHERE householdId = :householdId ORDER BY completedAt DESC")
    suspend fun getMetricsByHousehold(householdId: String): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE householdId = :householdId ORDER BY completedAt DESC")
    fun getMetricsByHouseholdFlow(householdId: String): Flow<List<EfficiencyMetric>>

    // ==================== Retrieve All ====================

    @Query("SELECT * FROM efficiency_metrics ORDER BY completedAt DESC")
    suspend fun getAllMetrics(): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics ORDER BY completedAt DESC")
    fun getAllMetricsFlow(): Flow<List<EfficiencyMetric>>

    // ==================== Filtering and Sorting ====================

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage >= :minEfficiency
        ORDER BY completedAt DESC
    """)
    suspend fun getMetricsWithMinEfficiency(userId: String, minEfficiency: Float): List<EfficiencyMetric>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage >= :minEfficiency
        ORDER BY completedAt DESC
    """)
    fun getMetricsWithMinEfficiencyFlow(userId: String, minEfficiency: Float): Flow<List<EfficiencyMetric>>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage <= :maxEfficiency
        ORDER BY completedAt DESC
    """)
    suspend fun getMetricsWithMaxEfficiency(userId: String, maxEfficiency: Float): List<EfficiencyMetric>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage <= :maxEfficiency
        ORDER BY completedAt DESC
    """)
    fun getMetricsWithMaxEfficiencyFlow(userId: String, maxEfficiency: Float): Flow<List<EfficiencyMetric>>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage BETWEEN :minEfficiency AND :maxEfficiency
        ORDER BY completedAt DESC
    """)
    suspend fun getMetricsInEfficiencyRange(userId: String, minEfficiency: Float, maxEfficiency: Float): List<EfficiencyMetric>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage BETWEEN :minEfficiency AND :maxEfficiency
        ORDER BY completedAt DESC
    """)
    fun getMetricsInEfficiencyRangeFlow(userId: String, minEfficiency: Float, maxEfficiency: Float): Flow<List<EfficiencyMetric>>

    // ==================== Sorting ====================

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY efficiencyPercentage DESC LIMIT :limit")
    suspend fun getTopMetricsByEfficiency(userId: String, limit: Int): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY efficiencyPercentage DESC LIMIT :limit")
    fun getTopMetricsByEfficiencyFlow(userId: String, limit: Int): Flow<List<EfficiencyMetric>>

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentMetrics(userId: String, limit: Int): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE userId = :userId ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentMetricsFlow(userId: String, limit: Int): Flow<List<EfficiencyMetric>>

    // ==================== Date Range Queries ====================

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
        ORDER BY completedAt DESC
    """)
    suspend fun getMetricsInDateRange(userId: String, startTime: Instant, endTime: Instant): List<EfficiencyMetric>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE userId = :userId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
        ORDER BY completedAt DESC
    """)
    fun getMetricsInDateRangeFlow(userId: String, startTime: Instant, endTime: Instant): Flow<List<EfficiencyMetric>>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE householdId = :householdId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
        ORDER BY completedAt DESC
    """)
    suspend fun getHouseholdMetricsInDateRange(householdId: String, startTime: Instant, endTime: Instant): List<EfficiencyMetric>

    @Query("""
        SELECT * FROM efficiency_metrics 
        WHERE householdId = :householdId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
        ORDER BY completedAt DESC
    """)
    fun getHouseholdMetricsInDateRangeFlow(householdId: String, startTime: Instant, endTime: Instant): Flow<List<EfficiencyMetric>>

    // ==================== Aggregation Queries ====================

    @Query("SELECT AVG(efficiencyPercentage) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getAverageEfficiency(userId: String): Float?

    @Query("SELECT AVG(efficiencyPercentage) FROM efficiency_metrics WHERE householdId = :householdId")
    suspend fun getAverageEfficiencyByHousehold(householdId: String): Float?

    @Query("SELECT MAX(efficiencyPercentage) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getMaxEfficiency(userId: String): Float?

    @Query("SELECT MIN(efficiencyPercentage) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getMinEfficiency(userId: String): Float?

    @Query("SELECT AVG(actualDurationMinutes) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getAverageActualDuration(userId: String): Float?

    @Query("SELECT AVG(estimatedDurationMinutes) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getAverageEstimatedDuration(userId: String): Float?

    @Query("""
        SELECT AVG(efficiencyPercentage) FROM efficiency_metrics 
        WHERE userId = :userId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
    """)
    suspend fun getAverageEfficiencyInDateRange(userId: String, startTime: Instant, endTime: Instant): Float?

    @Query("""
        SELECT AVG(efficiencyPercentage) FROM efficiency_metrics 
        WHERE householdId = :householdId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
    """)
    suspend fun getAverageHouseholdEfficiencyInDateRange(householdId: String, startTime: Instant, endTime: Instant): Float?

    // ==================== Count Operations ====================

    @Query("SELECT COUNT(*) FROM efficiency_metrics")
    suspend fun getTotalMetricCount(): Int

    @Query("SELECT COUNT(*) FROM efficiency_metrics WHERE userId = :userId")
    suspend fun getMetricCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM efficiency_metrics WHERE householdId = :householdId")
    suspend fun getMetricCountByHousehold(householdId: String): Int

    @Query("""
        SELECT COUNT(*) FROM efficiency_metrics 
        WHERE userId = :userId 
        AND efficiencyPercentage >= :minEfficiency
    """)
    suspend fun getMetricCountWithMinEfficiency(userId: String, minEfficiency: Float): Int

    @Query("""
        SELECT COUNT(*) FROM efficiency_metrics 
        WHERE userId = :userId 
        AND completedAt >= :startTime 
        AND completedAt <= :endTime
    """)
    suspend fun getMetricCountInDateRange(userId: String, startTime: Instant, endTime: Instant): Int

    // ==================== Delete Operations ====================

    @Query("DELETE FROM efficiency_metrics WHERE id = :metricId")
    suspend fun deleteMetricById(metricId: String)

    @Query("DELETE FROM efficiency_metrics WHERE userId = :userId")
    suspend fun deleteUserMetrics(userId: String)

    @Query("DELETE FROM efficiency_metrics WHERE householdId = :householdId")
    suspend fun deleteHouseholdMetrics(householdId: String)

    @Query("DELETE FROM efficiency_metrics WHERE taskId = :taskId")
    suspend fun deleteTaskMetrics(taskId: String)

    @Query("DELETE FROM efficiency_metrics")
    suspend fun deleteAllMetrics()

    // ==================== Batch Operations ====================

    @Insert
    suspend fun insertAll(metrics: List<EfficiencyMetric>)

    @Update
    suspend fun updateAll(metrics: List<EfficiencyMetric>)

    @Query("SELECT * FROM efficiency_metrics WHERE userId IN (:userIds) ORDER BY completedAt DESC")
    suspend fun getMetricsByUserIds(userIds: List<String>): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE userId IN (:userIds) ORDER BY completedAt DESC")
    fun getMetricsByUserIdsFlow(userIds: List<String>): Flow<List<EfficiencyMetric>>

    @Query("SELECT * FROM efficiency_metrics WHERE taskId IN (:taskIds) ORDER BY completedAt DESC")
    suspend fun getMetricsByTaskIds(taskIds: List<String>): List<EfficiencyMetric>

    @Query("SELECT * FROM efficiency_metrics WHERE taskId IN (:taskIds) ORDER BY completedAt DESC")
    fun getMetricsByTaskIdsFlow(taskIds: List<String>): Flow<List<EfficiencyMetric>>
}
