package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.PuzzleProgress

@Dao
interface PuzzleProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: PuzzleProgress): Long

    @Update
    suspend fun update(progress: PuzzleProgress)

    @Query("SELECT * FROM puzzle_progress WHERE id = :progressId")
    suspend fun getPuzzleById(progressId: String): PuzzleProgress?

    @Query(
        """
        SELECT * FROM puzzle_progress
        WHERE householdId = :householdId
          AND userId = :userId
          AND ageBandKey = :ageBandKey
        ORDER BY cycleIndex DESC, updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestPuzzleForBand(
        householdId: String,
        userId: String,
        ageBandKey: String
    ): PuzzleProgress?

    @Query(
        """
        SELECT * FROM puzzle_progress
        WHERE householdId = :householdId
          AND userId = :userId
        ORDER BY updatedAt DESC
        """
    )
    suspend fun getPuzzlesForUser(
        householdId: String,
        userId: String
    ): List<PuzzleProgress>

    @Query(
        """
        DELETE FROM puzzle_progress
        WHERE householdId = :householdId
          AND userId = :userId
        """
    )
    suspend fun deletePuzzlesForUser(householdId: String, userId: String)

    @Query("DELETE FROM puzzle_progress WHERE id = :progressId")
    suspend fun deletePuzzleById(progressId: String)
}
