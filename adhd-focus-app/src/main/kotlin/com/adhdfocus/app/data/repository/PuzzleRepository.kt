package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.PuzzleProgressDao
import com.adhdfocus.app.data.model.PuzzleProgress

class PuzzleRepository(
    private val puzzleProgressDao: PuzzleProgressDao
) {
    suspend fun save(progress: PuzzleProgress) {
        puzzleProgressDao.insert(progress)
    }

    suspend fun update(progress: PuzzleProgress) {
        puzzleProgressDao.update(progress)
    }

    suspend fun getLatestPuzzleForBand(
        householdId: String,
        userId: String,
        ageBandKey: String
    ): PuzzleProgress? {
        return puzzleProgressDao.getLatestPuzzleForBand(householdId, userId, ageBandKey)
    }

    suspend fun getPuzzlesForUser(
        householdId: String,
        userId: String
    ): List<PuzzleProgress> {
        return puzzleProgressDao.getPuzzlesForUser(householdId, userId)
    }
}
