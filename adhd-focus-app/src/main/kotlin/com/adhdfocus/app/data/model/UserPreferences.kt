package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "user_preferences",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserPreferences(
    @PrimaryKey
    val userId: String,
    val theme: Theme = Theme.LIGHT,
    val visibleTodoGroups: String = "", // JSON serialized list
    val notificationPreferences: String = "", // JSON serialized NotificationPreferences
    val dailyResetTime: String = "00:00",
    val affirmationFrequency: Int = 3,
    val enableGamification: Boolean = true,
    val enableBadges: Boolean = true,
    val enableStreaks: Boolean = true,
    val enableEfficiencyMetrics: Boolean = true,
    val timerDefaultDuration: Int = 25,
    val autoLogoutTimeout: Int = 0
) {
    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(affirmationFrequency in 1..5) { "affirmationFrequency must be between 1 and 5" }
        require(timerDefaultDuration > 0) { "timerDefaultDuration must be positive" }
        require(autoLogoutTimeout >= 0) { "autoLogoutTimeout must be non-negative" }
    }
}

enum class Theme {
    LIGHT,
    DARK
}

@Serializable
data class NotificationPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val visualAlertsEnabled: Boolean = true
)
