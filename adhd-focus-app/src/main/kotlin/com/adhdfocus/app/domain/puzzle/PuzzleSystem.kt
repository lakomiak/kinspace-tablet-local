package com.adhdfocus.app.domain.puzzle

import com.adhdfocus.app.data.model.PuzzleProgress
import com.adhdfocus.app.data.repository.PuzzleRepository
import com.adhdfocus.app.data.repository.UserRepository
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import javax.inject.Inject

class PuzzleSystem @Inject constructor(
    private val puzzleRepository: PuzzleRepository,
    private val userRepository: UserRepository
) {
    suspend fun getSelectedAgeBand(userId: String): PuzzleAgeBand {
        val birthDate = userRepository.getUserById(userId)?.birthDate ?: return PuzzleAgeBand.DEFAULT
        val age = runCatching { Period.between(birthDate, LocalDate.now()).years }.getOrNull()
        return PuzzleAgeBand.fromAge(age)
    }

    suspend fun getCurrentPuzzle(
        householdId: String,
        userId: String,
        ageBand: PuzzleAgeBand? = null
    ): PuzzleProgress {
        val resolvedAgeBand = ageBand ?: getSelectedAgeBand(userId)
        val latest = puzzleRepository.getLatestPuzzleForBand(householdId, userId, resolvedAgeBand.key)
        if (latest == null) {
            return createPuzzle(householdId, userId, resolvedAgeBand, cycleIndex = 0)
        }

        if (latest.isComplete() && latest.completedAt != null) {
            return createPuzzle(
                householdId = householdId,
                userId = userId,
                ageBand = resolvedAgeBand,
                cycleIndex = latest.cycleIndex + 1
            )
        }

        return latest
    }

    suspend fun recordDailyCompletion(
        householdId: String,
        userId: String,
        completionDate: LocalDate
    ): PuzzleProgress? {
        val ageBand = getSelectedAgeBand(userId)
        val alreadyAwarded = puzzleRepository.getPuzzlesForUser(householdId, userId)
            .any { progress ->
                progress.ageBandKey == ageBand.key && progress.lastCompletedDay == completionDate
            }
        if (alreadyAwarded) {
            return getCurrentPuzzle(householdId, userId, ageBand)
        }

        val current = getCurrentPuzzle(householdId, userId, ageBand)
        if (current.lastCompletedDay == completionDate) {
            return current
        }

        val updatedPieces = (current.piecesUnlocked + 1).coerceAtMost(current.totalPieces)
        val now = Instant.now()
        val updated = current.copy(
            piecesUnlocked = updatedPieces,
            lastCompletedDay = completionDate,
            completedAt = if (updatedPieces >= current.totalPieces) now else current.completedAt,
            updatedAt = now
        )
        puzzleRepository.update(updated)

        if (updatedPieces >= current.totalPieces) {
            createPuzzle(
                householdId = householdId,
                userId = userId,
                ageBand = ageBand,
                cycleIndex = current.cycleIndex + 1
            )
        }

        return updated
    }

    suspend fun getPuzzleHistory(
        householdId: String,
        userId: String
    ): List<PuzzleProgress> {
        return puzzleRepository.getPuzzlesForUser(householdId, userId)
    }

    private suspend fun createPuzzle(
        householdId: String,
        userId: String,
        ageBand: PuzzleAgeBand,
        cycleIndex: Int
    ): PuzzleProgress {
        val definition = PuzzleCatalog.definitionFor(ageBand, cycleIndex)
        val progress = PuzzleProgress(
            id = UUID.randomUUID().toString(),
            householdId = householdId,
            userId = userId,
            ageBandKey = ageBand.key,
            cycleIndex = cycleIndex,
            puzzleKey = definition.puzzleKey,
            title = definition.title,
            subtitle = definition.subtitle,
            imageUrl = definition.imageUrl,
            totalPieces = 30,
            piecesUnlocked = 0,
            lastCompletedDay = null,
            completedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        puzzleRepository.save(progress)
        return progress
    }
}
