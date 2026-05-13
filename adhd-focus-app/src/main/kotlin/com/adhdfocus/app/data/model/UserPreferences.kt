package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "user_preferences"
)
data class UserPreferences(
    @PrimaryKey
    val userId: String,
    val theme: Theme = Theme.LIGHT,
    val visibleTodoGroups: String = "", // JSON serialized list
    val customTodoGroups: String = "", // JSON serialized list
    val notificationPreferences: String = "", // JSON serialized NotificationPreferences
    val settingsPasscodeHash: String? = null,
    val enableTodoEditing: Boolean = false,
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
    val visualAlertsEnabled: Boolean = true,
    val timerAlarmSound: TimerAlarmSound = TimerAlarmSound.ALARM,
    val categoryReminderPreferences: CategoryReminderPreferences = CategoryReminderPreferences()
)

@Serializable
enum class TimerAlarmSound {
    ALARM,
    NOTIFICATION,
    BEEP,
    MULTI_BEEP,
    SILENT
}

@Serializable
data class CategoryReminderPreferences(
    val enabled: Boolean = true,
    val morningLeadMinutes: Int = 15,
    val afternoonLeadMinutes: Int = 15,
    val eveningLeadMinutes: Int = 15,
    val bedtimeLeadMinutes: Int = 15
) {
    init {
        require(morningLeadMinutes >= 0) { "morningLeadMinutes must be non-negative" }
        require(afternoonLeadMinutes >= 0) { "afternoonLeadMinutes must be non-negative" }
        require(eveningLeadMinutes >= 0) { "eveningLeadMinutes must be non-negative" }
        require(bedtimeLeadMinutes >= 0) { "bedtimeLeadMinutes must be non-negative" }
    }
}
