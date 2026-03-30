package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE householdId = :householdId AND isDeleted = 0")
    fun getTasksByHousehold(householdId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE assignedUserId = :userId AND isDeleted = 0")
    fun getTasksByUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE householdId = :householdId AND isDeleted = 0")
    suspend fun getTasksByHouseholdOnce(householdId: String): List<Task>

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    // Filtering and sorting queries
    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND status = :status
        ORDER BY createdAt DESC
    """)
    fun getTasksByStatus(householdId: String, status: TaskStatus): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND status = :status
        ORDER BY createdAt DESC
    """)
    fun getUserTasksByStatus(userId: String, status: TaskStatus): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND todoGroup = :todoGroup
        ORDER BY createdAt DESC
    """)
    fun getTasksByTodoGroup(householdId: String, todoGroup: String): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND todoGroup = :todoGroup
        ORDER BY createdAt DESC
    """)
    fun getUserTasksByTodoGroup(userId: String, todoGroup: String): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND status = :status 
        AND todoGroup = :todoGroup
        ORDER BY createdAt DESC
    """)
    fun getTasksByStatusAndGroup(
        householdId: String,
        status: TaskStatus,
        todoGroup: String
    ): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND status = :status 
        AND todoGroup = :todoGroup
        ORDER BY createdAt DESC
    """)
    fun getUserTasksByStatusAndGroup(
        userId: String,
        status: TaskStatus,
        todoGroup: String
    ): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND syncStatus = :syncStatus
        ORDER BY updatedAt ASC
    """)
    fun getTasksBySyncStatus(householdId: String, syncStatus: SyncStatus): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND syncStatus = :syncStatus
        ORDER BY updatedAt ASC
    """)
    fun getUserTasksBySyncStatus(userId: String, syncStatus: SyncStatus): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentTasks(householdId: String, limit: Int): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getUserRecentTasks(userId: String, limit: Int): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND createdAt >= :startTime 
        AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    suspend fun getTasksInDateRange(
        householdId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND createdAt >= :startTime 
        AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    suspend fun getUserTasksInDateRange(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Task>

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0
    """)
    suspend fun getTaskCount(householdId: String): Int

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0
    """)
    suspend fun getUserTaskCount(userId: String): Int

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND status = :status
    """)
    suspend fun getTaskCountByStatus(householdId: String, status: TaskStatus): Int

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE assignedUserId = :userId 
        AND isDeleted = 0 
        AND status = :status
    """)
    suspend fun getUserTaskCountByStatus(userId: String, status: TaskStatus): Int

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND syncStatus = :syncStatus
    """)
    suspend fun getPendingSyncTaskCount(householdId: String, syncStatus: SyncStatus): Int

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND status = :status
        ORDER BY updatedAt DESC
    """)
    suspend fun getTasksByStatusOnce(householdId: String, status: TaskStatus): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND todoGroup = :todoGroup
        ORDER BY createdAt DESC
    """)
    suspend fun getTasksByTodoGroupOnce(householdId: String, todoGroup: String): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE householdId = :householdId 
        AND isDeleted = 0 
        AND syncStatus = :syncStatus
        ORDER BY updatedAt ASC
    """)
    suspend fun getTasksBySyncStatusOnce(householdId: String, syncStatus: SyncStatus): List<Task>

    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String)

    @Query("UPDATE tasks SET isDeleted = 1 WHERE householdId = :householdId")
    suspend fun softDeleteAllHouseholdTasks(householdId: String)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND updatedAt < :cutoffTime")
    suspend fun deleteOldSoftDeletedTasks(cutoffTime: Instant)
}
