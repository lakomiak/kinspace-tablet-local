package com.adhdfocus.app.data.database

import androidx.room.TypeConverter
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.TaskSessionOutcome
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import com.adhdfocus.app.data.model.SyncOperation
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun fromLocalDateString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromTheme(value: String?): Theme? {
        return value?.let { Theme.valueOf(it) }
    }

    @TypeConverter
    fun themeToString(theme: Theme?): String? {
        return theme?.name
    }

    @TypeConverter
    fun fromUserRole(value: String?): UserRole? {
        return value?.let { UserRole.valueOf(it) }
    }

    @TypeConverter
    fun userRoleToString(role: UserRole?): String? {
        return role?.name
    }

    @TypeConverter
    fun fromTaskStatus(value: String?): TaskStatus? {
        return value?.let { TaskStatus.valueOf(it) }
    }

    @TypeConverter
    fun taskStatusToString(status: TaskStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun fromSyncStatus(value: String?): SyncStatus? {
        return value?.let { SyncStatus.valueOf(it) }
    }

    @TypeConverter
    fun syncStatusToString(status: SyncStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun fromTaskSessionOutcome(value: String?): TaskSessionOutcome? {
        return value?.let { TaskSessionOutcome.valueOf(it) }
    }

    @TypeConverter
    fun taskSessionOutcomeToString(outcome: TaskSessionOutcome?): String? {
        return outcome?.name
    }

    @TypeConverter
    fun fromAffirmationType(value: String?): AffirmationType? {
        return value?.let { AffirmationType.valueOf(it) }
    }

    @TypeConverter
    fun affirmationTypeToString(type: AffirmationType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromAffirmationTone(value: String?): AffirmationTone? {
        return value?.let { AffirmationTone.valueOf(it) }
    }

    @TypeConverter
    fun affirmationToneToString(tone: AffirmationTone?): String? {
        return tone?.name
    }

    @TypeConverter
    fun fromNotificationPreferences(value: String?): NotificationPreferences? {
        return value?.let {
            try {
                Json.decodeFromString(it)
            } catch (e: Exception) {
                NotificationPreferences()
            }
        }
    }

    @TypeConverter
    fun notificationPreferencesToString(prefs: NotificationPreferences?): String? {
        return prefs?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.let {
            try {
                Json.decodeFromString(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return list?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun fromSyncOperation(value: String?): SyncOperation? {
        return value?.let { SyncOperation.valueOf(it) }
    }

    @TypeConverter
    fun syncOperationToString(operation: SyncOperation?): String? {
        return operation?.name
    }
}
