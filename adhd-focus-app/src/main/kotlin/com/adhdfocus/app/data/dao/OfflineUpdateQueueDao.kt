package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for OfflineUpdateQueueItem.
 * Provides CRUD operations for managing updates received while offline.
 * Supports FIFO ordering, conflict detection, and batch operations.
 */
@Dao
interface OfflineUpdateQueueDao {
    @Insert
    suspend fun insert(item: OfflineUpdateQueueItem): Long

    @Update
    suspend fun update(item: OfflineUpdateQueueItem)

    @Delete
    suspend fun delete(item: OfflineUpdateQueueItem)

    @Query("SELECT * FROM offline_update_queue WHERE id = :itemId")
    suspend fun getItemById(itemId: String): OfflineUpdateQueueItem?

    @Query("SELECT * FROM offline_update_queue WHERE taskId = :taskId ORDER BY timestamp ASC")
    suspend fun getItemsByTaskId(taskId: String): List<OfflineUpdateQueueItem>

    @Query("SELECT * FROM offline_update_queue WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getItemsByUserId(userId: String): List<OfflineUpdateQueueItem>

    @Query("SELECT * FROM offline_update_queue WHERE userId = :userId ORDER BY timestamp ASC")
    fun observeItemsByUserId(userId: String): Flow<List<OfflineUpdateQueueItem>>

    @Query("SELECT * FROM offline_update_queue WHERE applied = 0 ORDER BY timestamp ASC")
    suspend fun getUnappliedItems(): List<OfflineUpdateQueueItem>

    @Query("SELECT * FROM offline_update_queue WHERE userId = :userId AND applied = 0 ORDER BY timestamp ASC")
    suspend fun getUnappliedItemsByUserId(userId: String): List<OfflineUpdateQueueItem>

    @Query("SELECT * FROM offline_update_queue WHERE updateType = :updateType ORDER BY timestamp ASC")
    suspend fun getItemsByUpdateType(updateType: UpdateType): List<OfflineUpdateQueueItem>

    @Query("""
        SELECT * FROM offline_update_queue 
        WHERE userId = :userId 
        AND updateType = :updateType 
        ORDER BY timestamp ASC
    """)
    suspend fun getItemsByUserAndUpdateType(userId: String, updateType: UpdateType): List<OfflineUpdateQueueItem>

    @Query("SELECT COUNT(*) FROM offline_update_queue WHERE userId = :userId")
    suspend fun getQueueSize(userId: String): Int

    @Query("SELECT COUNT(*) FROM offline_update_queue WHERE userId = :userId AND applied = 0")
    suspend fun getUnappliedQueueSize(userId: String): Int

    @Query("SELECT COUNT(*) FROM offline_update_queue")
    suspend fun getTotalQueueSize(): Int

    @Query("DELETE FROM offline_update_queue WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM offline_update_queue WHERE taskId = :taskId")
    suspend fun deleteItemsByTaskId(taskId: String)

    @Query("DELETE FROM offline_update_queue WHERE userId = :userId")
    suspend fun deleteItemsByUserId(userId: String)

    @Query("DELETE FROM offline_update_queue WHERE applied = 1")
    suspend fun deleteAppliedItems()

    @Query("DELETE FROM offline_update_queue")
    suspend fun deleteAllItems()

    @Query("UPDATE offline_update_queue SET applied = 1 WHERE id = :itemId")
    suspend fun markAsApplied(itemId: String)

    @Query("UPDATE offline_update_queue SET applied = 1 WHERE userId = :userId")
    suspend fun markAllAsApplied(userId: String)

    @Query("""
        SELECT * FROM offline_update_queue 
        WHERE userId = :userId 
        AND timestamp >= :startTime 
        AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getItemsInTimeRange(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): List<OfflineUpdateQueueItem>
}
