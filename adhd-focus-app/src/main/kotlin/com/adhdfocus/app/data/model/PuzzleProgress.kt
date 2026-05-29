package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "puzzle_progress",
    indices = [
        Index("householdId"),
        Index("userId"),
        Index(value = ["householdId", "userId", "ageBandKey"]),
        Index(value = ["householdId", "userId", "ageBandKey", "cycleIndex"], unique = true)
    ]
)
data class PuzzleProgress(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val userId: String,
    val ageBandKey: String,
    val cycleIndex: Int = 0,
    val puzzleKey: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val totalPieces: Int = 30,
    val piecesUnlocked: Int = 0,
    val lastCompletedDay: LocalDate? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun isComplete(): Boolean = piecesUnlocked >= totalPieces
}
