package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.SyncQueueItem
import com.adhdfocus.app.data.model.SyncOperation
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for SyncQueueItem.
 * Provides CRUD operations and query methods for managing pending sync operations.
 * Supports FIFO ordering, retry tracking, and operation type filtering.
 */
@Dao
interface SyncQueueDao {
    @Insert
    suspend fun insert(item: SyncQueueItem): Long

    @Update
    suspend fun update(item: SyncQueueItem)

    @Delete
    suspend fun delete(item: SyncQueueItem)

    @Query("SELECT * FROM sync_queue WHERE id = :itemId")
    suspend fun getItemById(itemId: String): SyncQueueItem?

    @Query("SELECT * FROM sync_queue WHERE taskId = :taskId")
    suspend fun getItemsByTaskId(taskId: String): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE userId = :userId")
    fun getItemsByUser(userId: String): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE userId = :userId")
    suspend fun getItemsByUserOnce(userId: String): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE operation = :operation")
    fun getItemsByOperation(operation: SyncOperation): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE operation = :operation")
    suspend fun getItemsByOperationOnce(operation: SyncOperation): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        WHERE userId = :userId 
        ORDER BY timestamp ASC
    """)
    suspend fun getPendingItemsByUserFifo(userId: String): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        ORDER BY timestamp ASC
    """)
    suspend fun getAllPendingItemsFifo(): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        WHERE userId = :userId 
        AND operation = :operation
        ORDER BY timestamp ASC
    """)
    suspend fun getPendingItemsByUserAndOperation(
        userId: String,
        operation: SyncOperation
    ): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        WHERE userId = :userId 
        AND retryCount < :maxRetries
        ORDER BY timestamp ASC
    """)
    suspend fun getRetryableItemsByUser(userId: String, maxRetries: Int): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        WHERE retryCount < :maxRetries
        ORDER BY timestamp ASC
    """)
    suspend fun getAllRetryableItems(maxRetries: Int): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue 
        WHERE userId = :userId 
        AND timestamp >= :startTime 
        AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getItemsInTimeRange(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): List<SyncQueueItem>

    @Query("""
        SELECT COUNT(*) FROM sync_queue 
        WHERE userId = :userId
    """)
    suspend fun getPendingItemCount(userId: String): Int

    @Query("""
        SELECT COUNT(*) FROM sync_queue 
        WHERE userId = :userId 
        AND operation = :operation
    """)
    suspend fun getPendingItemCountByOperation(userId: String, operation: SyncOperation): Int

    @Query("""
        SELECT COUNT(*) FROM sync_queue 
        WHERE userId = :userId 
        AND retryCount < :maxRetries
    """)
    suspend fun getRetryableItemCount(userId: String, maxRetries: Int): Int

    @Query("DELETE FROM sync_queue WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM sync_queue WHERE taskId = :taskId")
    suspend fun deleteItemsByTaskId(taskId: String)

    @Query("DELETE FROM sync_queue WHERE userId = :userId")
    suspend fun deleteItemsByUserId(userId: String)

    @Query("DELETE FROM sync_queue WHERE userId = :userId AND operation = :operation")
    suspend fun deleteItemsByUserAndOperation(userId: String, operation: SyncOperation)

    @Query("""
        DELETE FROM sync_queue 
        WHERE userId = :userId 
        AND timestamp < :cutoffTime
    """)
    suspend fun deleteOldItems(userId: String, cutoffTime: Instant)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllItems()

    @Query("""
        UPDATE sync_queue 
        SET retryCount = retryCount + 1 
        WHERE id = :itemId
    """)
    suspend fun incrementRetryCount(itemId: String)

    @Query("""
        UPDATE sync_queue 
        SET retryCount = :retryCount 
        WHERE id = :itemId
    """)
    suspend fun setRetryCount(itemId: String, retryCount: Int)
}
