package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * UserSwitchingState tracks the current user and user switching state.
 *
 * Stores:
 * - Current active user ID
 * - Household ID
 * - Last switch time
 * - Session state
 */
@Entity(
    tableName = "current_user",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSwitchingState(
    @PrimaryKey
    val id: String = "current_user",
    val userId: String,
    val householdId: String,
    val lastSwitchTime: Instant = Instant.now(),
    val sessionStartTime: Instant = Instant.now()
) {
    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
    }
}
