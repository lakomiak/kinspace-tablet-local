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
    val autoLogoutTimeout: Int = 0,
    val puzzleAgeBand: String = "5-6"
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
    val morningEnabled: Boolean = true,
    val afternoonEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    val bedtimeEnabled: Boolean = true,
    val morningEndTime: String = "12:00",
    val afternoonEndTime: String = "17:00",
    val eveningEndTime: String = "20:00",
    val bedtimeEndTime: String = "22:00",
    val morningLeadMinutes: Int = 15,
    val afternoonLeadMinutes: Int = 15,
    val eveningLeadMinutes: Int = 15,
    val bedtimeLeadMinutes: Int = 15,
    val morningEndTime: String = "12:00",
    val afternoonEndTime: String = "17:00",
    val eveningEndTime: String = "20:00",
    val bedtimeEndTime: String = "22:00"
) {
    init {
        require(morningEndTime.matches(Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$"))) { "morningEndTime must be HH:mm" }
        require(afternoonEndTime.matches(Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$"))) { "afternoonEndTime must be HH:mm" }
        require(eveningEndTime.matches(Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$"))) { "eveningEndTime must be HH:mm" }
        require(bedtimeEndTime.matches(Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$"))) { "bedtimeEndTime must be HH:mm" }
        require(morningLeadMinutes >= 0) { "morningLeadMinutes must be non-negative" }
        require(afternoonLeadMinutes >= 0) { "afternoonLeadMinutes must be non-negative" }
        require(eveningLeadMinutes >= 0) { "eveningLeadMinutes must be non-negative" }
        require(bedtimeLeadMinutes >= 0) { "bedtimeLeadMinutes must be non-negative" }
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        require(timeRegex.matches(morningEndTime)) { "morningEndTime must be in HH:mm format" }
        require(timeRegex.matches(afternoonEndTime)) { "afternoonEndTime must be in HH:mm format" }
        require(timeRegex.matches(eveningEndTime)) { "eveningEndTime must be in HH:mm format" }
        require(timeRegex.matches(bedtimeEndTime)) { "bedtimeEndTime must be in HH:mm format" }
    }
}
