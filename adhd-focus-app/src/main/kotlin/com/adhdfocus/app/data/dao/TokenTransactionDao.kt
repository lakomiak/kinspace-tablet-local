package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adhdfocus.app.data.model.TokenTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TokenTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TokenTransaction): Long

    @Query(
        """
        SELECT * FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        ORDER BY createdAt DESC
        """
    )
    fun observeTransactionsForUser(householdId: String, userId: String): Flow<List<TokenTransaction>>

    @Query(
        """
        SELECT * FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        ORDER BY createdAt DESC
        """
    )
    suspend fun getTransactionsForUser(householdId: String, userId: String): List<TokenTransaction>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        """
    )
    fun observeBalance(householdId: String, userId: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        """
    )
    suspend fun getBalance(householdId: String, userId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        AND amount > 0
        AND type = 'TASK_AWARD'
        AND createdAt >= :start
        AND createdAt < :end
        """
    )
    fun observeEarnedBetween(
        householdId: String,
        userId: String,
        start: Instant,
        end: Instant
    ): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM token_transactions
            WHERE householdId = :householdId
            AND userId = :userId
            AND taskId = :taskId
            AND targetDate = :targetDate
            AND type = 'TASK_AWARD'
        )
        """
    )
    suspend fun hasTaskAward(
        householdId: String,
        userId: String,
        taskId: String,
        targetDate: String
    ): Boolean

    @Query(
        """
        DELETE FROM token_transactions
        WHERE householdId = :householdId
        AND userId = :userId
        AND taskId = :taskId
        AND targetDate = :targetDate
        AND type = 'TASK_AWARD'
        """
    )
    suspend fun deleteTaskAward(
        householdId: String,
        userId: String,
        taskId: String,
        targetDate: String
    ): Int
}
