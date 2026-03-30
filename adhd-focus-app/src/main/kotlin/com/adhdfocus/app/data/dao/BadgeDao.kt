package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.Badge
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface BadgeDao {
    @Insert
    suspend fun insert(badge: Badge): Long

    @Update
    suspend fun update(badge: Badge)

    @Delete
    suspend fun delete(badge: Badge)

    @Query("SELECT * FROM badges WHERE id = :badgeId")
    suspend fun getBadgeById(badgeId: String): Badge?

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 0 ORDER BY earnedAt DESC")
    fun getEarnedBadgesByUser(userId: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 0 ORDER BY earnedAt DESC")
    suspend fun getEarnedBadgesByUserOnce(userId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 1 ORDER BY name ASC")
    fun getLockedBadgesByUser(userId: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 1 ORDER BY name ASC")
    suspend fun getLockedBadgesByUserOnce(userId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAt DESC")
    suspend fun getBadgesByUserOnce(userId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAt DESC")
    fun getBadgesByUser(userId: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE householdId = :householdId AND isLocked = 0 ORDER BY earnedAt DESC")
    suspend fun getEarnedBadgesByHousehold(householdId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE householdId = :householdId AND isLocked = 0 ORDER BY earnedAt DESC")
    fun getEarnedBadgesByHouseholdFlow(householdId: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE householdId = :householdId AND isLocked = 1 ORDER BY name ASC")
    suspend fun getLockedBadgesByHousehold(householdId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE householdId = :householdId AND isLocked = 1 ORDER BY name ASC")
    fun getLockedBadgesByHouseholdFlow(householdId: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE badgeType = :badgeType ORDER BY earnedAt DESC")
    suspend fun getBadgesByType(badgeType: String): List<Badge>

    @Query("SELECT * FROM badges WHERE badgeType = :badgeType ORDER BY earnedAt DESC")
    fun getBadgesByTypeFlow(badgeType: String): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE userId = :userId AND badgeType = :badgeType ORDER BY earnedAt DESC")
    suspend fun getBadgesByUserAndType(userId: String, badgeType: String): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId AND badgeType = :badgeType ORDER BY earnedAt DESC")
    fun getBadgesByUserAndTypeFlow(userId: String, badgeType: String): Flow<List<Badge>>

    @Query("""
        SELECT * FROM badges 
        WHERE earnedAt >= :startTime 
        AND earnedAt <= :endTime
        ORDER BY earnedAt DESC
    """)
    suspend fun getBadgesInDateRange(startTime: Instant, endTime: Instant): List<Badge>

    @Query("""
        SELECT * FROM badges 
        WHERE earnedAt >= :startTime 
        AND earnedAt <= :endTime
        ORDER BY earnedAt DESC
    """)
    fun getBadgesInDateRangeFlow(startTime: Instant, endTime: Instant): Flow<List<Badge>>

    @Query("""
        SELECT * FROM badges 
        WHERE userId = :userId 
        AND earnedAt >= :startTime 
        AND earnedAt <= :endTime
        ORDER BY earnedAt DESC
    """)
    suspend fun getUserBadgesInDateRange(userId: String, startTime: Instant, endTime: Instant): List<Badge>

    @Query("""
        SELECT * FROM badges 
        WHERE userId = :userId 
        AND earnedAt >= :startTime 
        AND earnedAt <= :endTime
        ORDER BY earnedAt DESC
    """)
    fun getUserBadgesInDateRangeFlow(userId: String, startTime: Instant, endTime: Instant): Flow<List<Badge>>

    @Query("SELECT * FROM badges ORDER BY earnedAt DESC")
    suspend fun getAllBadges(): List<Badge>

    @Query("SELECT * FROM badges ORDER BY earnedAt DESC")
    fun getAllBadgesFlow(): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE isLocked = 0 ORDER BY earnedAt DESC")
    suspend fun getAllEarnedBadges(): List<Badge>

    @Query("SELECT * FROM badges WHERE isLocked = 0 ORDER BY earnedAt DESC")
    fun getAllEarnedBadgesFlow(): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE isLocked = 1 ORDER BY name ASC")
    suspend fun getAllLockedBadges(): List<Badge>

    @Query("SELECT * FROM badges WHERE isLocked = 1 ORDER BY name ASC")
    fun getAllLockedBadgesFlow(): Flow<List<Badge>>

    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId")
    suspend fun getBadgeCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId AND isLocked = 0")
    suspend fun getEarnedBadgeCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId AND isLocked = 1")
    suspend fun getLockedBadgeCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE householdId = :householdId")
    suspend fun getBadgeCountByHousehold(householdId: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE householdId = :householdId AND isLocked = 0")
    suspend fun getEarnedBadgeCountByHousehold(householdId: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE badgeType = :badgeType")
    suspend fun getBadgeCountByType(badgeType: String): Int

    @Query("SELECT COUNT(*) FROM badges")
    suspend fun getTotalBadgeCount(): Int

    @Query("DELETE FROM badges WHERE id = :badgeId")
    suspend fun deleteBadgeById(badgeId: String)

    @Query("DELETE FROM badges WHERE userId = :userId")
    suspend fun deleteUserBadges(userId: String)

    @Query("DELETE FROM badges WHERE userId = :userId AND badgeType = :badgeType")
    suspend fun deleteUserBadgesByType(userId: String, badgeType: String)

    @Query("DELETE FROM badges WHERE householdId = :householdId")
    suspend fun deleteHouseholdBadges(householdId: String)

    @Query("DELETE FROM badges")
    suspend fun deleteAllBadges()

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 0 ORDER BY earnedAt DESC LIMIT :limit")
    suspend fun getRecentEarnedBadges(userId: String, limit: Int): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId AND isLocked = 0 ORDER BY earnedAt DESC LIMIT :limit")
    fun getRecentEarnedBadgesFlow(userId: String, limit: Int): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE userId = :userId AND progress IS NOT NULL ORDER BY progress DESC")
    suspend fun getBadgesWithProgress(userId: String): List<Badge>

    @Query("SELECT * FROM badges WHERE userId = :userId AND progress IS NOT NULL ORDER BY progress DESC")
    fun getBadgesWithProgressFlow(userId: String): Flow<List<Badge>>
}
