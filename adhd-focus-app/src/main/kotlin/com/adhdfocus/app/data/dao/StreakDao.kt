package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.Streak
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StreakDao {
    // ==================== Basic CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: Streak): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: Streak): Long

    @Update
    suspend fun update(streak: Streak)

    @Delete
    suspend fun delete(streak: Streak)

    // ==================== Retrieve by ID ====================

    @Query("SELECT * FROM streaks WHERE id = :streakId")
    suspend fun getStreakById(streakId: String): Streak?

    @Query("SELECT * FROM streaks WHERE id = :streakId")
    fun getStreakByIdFlow(streakId: String): Flow<Streak?>

    // ==================== Retrieve by User ====================

    @Query("SELECT * FROM streaks WHERE userId = :userId")
    suspend fun getStreakByUser(userId: String): Streak?

    @Query("SELECT * FROM streaks WHERE userId = :userId")
    suspend fun getStreakByUserId(userId: String): Streak?

    @Query("SELECT * FROM streaks WHERE userId = :userId")
    fun getStreakByUserFlow(userId: String): Flow<Streak?>

    // ==================== Retrieve by User and Household ====================

    @Query("SELECT * FROM streaks WHERE userId = :userId AND householdId = :householdId")
    suspend fun getStreak(userId: String, householdId: String): Streak?

    @Query("SELECT * FROM streaks WHERE userId = :userId AND householdId = :householdId")
    fun getStreakFlow(userId: String, householdId: String): Flow<Streak?>

    // ==================== Retrieve by Household ====================

    @Query("SELECT * FROM streaks WHERE householdId = :householdId")
    suspend fun getStreaksByHousehold(householdId: String): List<Streak>

    @Query("SELECT * FROM streaks WHERE householdId = :householdId")
    fun getStreaksByHouseholdFlow(householdId: String): Flow<List<Streak>>

    // ==================== Retrieve All ====================

    @Query("SELECT * FROM streaks ORDER BY currentCount DESC")
    suspend fun getAllStreaks(): List<Streak>

    @Query("SELECT * FROM streaks ORDER BY currentCount DESC")
    fun getAllStreaksFlow(): Flow<List<Streak>>

    // ==================== Filtering and Sorting ====================

    @Query("SELECT * FROM streaks WHERE currentCount > 0 ORDER BY currentCount DESC")
    suspend fun getActiveStreaks(): List<Streak>

    @Query("SELECT * FROM streaks WHERE currentCount > 0 ORDER BY currentCount DESC")
    fun getActiveStreaksFlow(): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE currentCount = 0 ORDER BY updatedAt DESC")
    suspend fun getInactiveStreaks(): List<Streak>

    @Query("SELECT * FROM streaks WHERE currentCount = 0 ORDER BY updatedAt DESC")
    fun getInactiveStreaksFlow(): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE bestCount >= :minBestCount ORDER BY bestCount DESC")
    suspend fun getStreaksWithMinBestCount(minBestCount: Int): List<Streak>

    @Query("SELECT * FROM streaks WHERE bestCount >= :minBestCount ORDER BY bestCount DESC")
    fun getStreaksWithMinBestCountFlow(minBestCount: Int): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE currentCount >= :minCurrentCount ORDER BY currentCount DESC")
    suspend fun getStreaksWithMinCurrentCount(minCurrentCount: Int): List<Streak>

    @Query("SELECT * FROM streaks WHERE currentCount >= :minCurrentCount ORDER BY currentCount DESC")
    fun getStreaksWithMinCurrentCountFlow(minCurrentCount: Int): Flow<List<Streak>>

    // ==================== Sorting ====================

    @Query("SELECT * FROM streaks ORDER BY currentCount DESC LIMIT :limit")
    suspend fun getTopStreaksByCurrentCount(limit: Int): List<Streak>

    @Query("SELECT * FROM streaks ORDER BY currentCount DESC LIMIT :limit")
    fun getTopStreaksByCurrentCountFlow(limit: Int): Flow<List<Streak>>

    @Query("SELECT * FROM streaks ORDER BY bestCount DESC LIMIT :limit")
    suspend fun getTopStreaksByBestCount(limit: Int): List<Streak>

    @Query("SELECT * FROM streaks ORDER BY bestCount DESC LIMIT :limit")
    fun getTopStreaksByBestCountFlow(limit: Int): Flow<List<Streak>>

    @Query("SELECT * FROM streaks ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedStreaks(limit: Int): List<Streak>

    @Query("SELECT * FROM streaks ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentlyUpdatedStreaksFlow(limit: Int): Flow<List<Streak>>

    // ==================== Date Range Queries ====================

    @Query("""
        SELECT * FROM streaks 
        WHERE lastCompletionDate >= :startDate 
        AND lastCompletionDate <= :endDate
        ORDER BY lastCompletionDate DESC
    """)
    suspend fun getStreaksCompletedInDateRange(startDate: LocalDate, endDate: LocalDate): List<Streak>

    @Query("""
        SELECT * FROM streaks 
        WHERE lastCompletionDate >= :startDate 
        AND lastCompletionDate <= :endDate
        ORDER BY lastCompletionDate DESC
    """)
    fun getStreaksCompletedInDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Streak>>

    @Query("""
        SELECT * FROM streaks 
        WHERE startDate >= :startDate 
        AND startDate <= :endDate
        ORDER BY startDate DESC
    """)
    suspend fun getStreaksStartedInDateRange(startDate: LocalDate, endDate: LocalDate): List<Streak>

    @Query("""
        SELECT * FROM streaks 
        WHERE startDate >= :startDate 
        AND startDate <= :endDate
        ORDER BY startDate DESC
    """)
    fun getStreaksStartedInDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Streak>>

    // ==================== Count Operations ====================

    @Query("SELECT COUNT(*) FROM streaks")
    suspend fun getTotalStreakCount(): Int

    @Query("SELECT COUNT(*) FROM streaks WHERE householdId = :householdId")
    suspend fun getStreakCountByHousehold(householdId: String): Int

    @Query("SELECT COUNT(*) FROM streaks WHERE currentCount > 0")
    suspend fun getActiveStreakCount(): Int

    @Query("SELECT COUNT(*) FROM streaks WHERE currentCount = 0")
    suspend fun getInactiveStreakCount(): Int

    @Query("SELECT COUNT(*) FROM streaks WHERE bestCount >= :minBestCount")
    suspend fun getStreakCountWithMinBestCount(minBestCount: Int): Int

    // ==================== Aggregation Queries ====================

    @Query("SELECT AVG(currentCount) FROM streaks WHERE householdId = :householdId")
    suspend fun getAverageCurrentCountByHousehold(householdId: String): Double?

    @Query("SELECT AVG(bestCount) FROM streaks WHERE householdId = :householdId")
    suspend fun getAverageBestCountByHousehold(householdId: String): Double?

    @Query("SELECT MAX(bestCount) FROM streaks WHERE householdId = :householdId")
    suspend fun getMaxBestCountByHousehold(householdId: String): Int?

    @Query("SELECT MAX(currentCount) FROM streaks WHERE householdId = :householdId")
    suspend fun getMaxCurrentCountByHousehold(householdId: String): Int?

    // ==================== Delete Operations ====================

    @Query("DELETE FROM streaks WHERE id = :streakId")
    suspend fun deleteStreakById(streakId: String)

    @Query("DELETE FROM streaks WHERE userId = :userId")
    suspend fun deleteUserStreaks(userId: String)

    @Query("DELETE FROM streaks WHERE householdId = :householdId")
    suspend fun deleteHouseholdStreaks(householdId: String)

    @Query("DELETE FROM streaks WHERE currentCount = 0")
    suspend fun deleteInactiveStreaks()

    @Query("DELETE FROM streaks")
    suspend fun deleteAllStreaks()

    // ==================== Batch Operations ====================

    @Insert
    suspend fun insertAll(streaks: List<Streak>)

    @Update
    suspend fun updateAll(streaks: List<Streak>)

    @Query("SELECT * FROM streaks WHERE userId IN (:userIds) ORDER BY currentCount DESC")
    suspend fun getStreaksByUserIds(userIds: List<String>): List<Streak>

    @Query("SELECT * FROM streaks WHERE userId IN (:userIds) ORDER BY currentCount DESC")
    fun getStreaksByUserIdsFlow(userIds: List<String>): Flow<List<Streak>>
}
