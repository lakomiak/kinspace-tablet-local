package com.adhdfocus.app.domain.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adhdfocus.app.data.model.CustomTimePeriodReminderPreference
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager,
    private val setupManager: TabletSetupManager
) {
    private val alarmManager: AlarmManager? by lazy {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }

    suspend fun rescheduleForCurrentSetup() {
        val memberId = setupManager.getAssignedMemberId().orEmpty()
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (memberId.isBlank() || householdId.isBlank()) {
            cancelAll()
            return
        }

        val preferences = userPreferencesManager.getPreferencesOrDefault(memberId)
        val notificationPreferences = userPreferencesManager.deserializeNotificationPreferences(
            preferences.notificationPreferences
        )
        val customGroups = userPreferencesManager.deserializeCustomTodoGroups(preferences.customTodoGroups)
        if (!notificationPreferences.categoryReminderPreferences.enabled) {
            cancelAll()
            return
        }

        cancelAll()
        TodoCategoryReminder.values().forEach { category ->
            scheduleCategoryReminder(
                category = category,
                preferences = notificationPreferences,
                memberId = memberId,
                memberName = setupManager.getAssignedMemberName().orEmpty(),
                householdId = householdId
            )
        }
        buildCustomReminderPreferences(
            customGroups = customGroups,
            notificationPreferences = notificationPreferences
        ).forEachIndexed { index, reminder ->
            scheduleCustomTimePeriodReminder(
                reminder = reminder,
                reminderIndex = index,
                preferences = notificationPreferences,
                memberId = memberId,
                memberName = setupManager.getAssignedMemberName().orEmpty(),
                householdId = householdId
            )
        }
    }

    private suspend fun cancelAll() {
        val memberId = setupManager.getAssignedMemberId().orEmpty()
        val preferences = if (memberId.isBlank()) null else userPreferencesManager.getPreferences(memberId)
        val notificationPreferences = preferences?.let {
            userPreferencesManager.deserializeNotificationPreferences(it.notificationPreferences)
        } ?: NotificationPreferences()
        val customGroups = preferences?.let {
            userPreferencesManager.deserializeCustomTodoGroups(it.customTodoGroups)
        }.orEmpty()

        TodoCategoryReminder.values().forEach { category ->
            val pendingIntent = reminderPendingIntent(
                category = category,
                endTime = category.defaultEndTime,
                flag = PendingIntent.FLAG_NO_CREATE,
                memberId = memberId,
                memberName = setupManager.getAssignedMemberName().orEmpty(),
                householdId = setupManager.getHouseholdId().orEmpty()
            )
            if (pendingIntent != null) {
                alarmManager?.cancel(pendingIntent)
            }
        }
        buildCustomReminderPreferences(
            customGroups = customGroups,
            notificationPreferences = notificationPreferences
        ).forEachIndexed { index, reminder ->
            val pendingIntent = customReminderPendingIntent(
                reminder = reminder,
                reminderIndex = index,
                flag = PendingIntent.FLAG_NO_CREATE,
                memberId = memberId,
                memberName = setupManager.getAssignedMemberName().orEmpty(),
                householdId = setupManager.getHouseholdId().orEmpty()
            )
            if (pendingIntent != null) {
                alarmManager?.cancel(pendingIntent)
            }
        }
    }

    private fun scheduleCategoryReminder(
        category: TodoCategoryReminder,
        preferences: NotificationPreferences,
        memberId: String,
        memberName: String,
        householdId: String
    ) {
        val leadMinutes = categoryLeadMinutes(category, preferences).coerceAtLeast(0)
        if (!categoryEnabled(category, preferences)) return
        val endTime = categoryEndTime(category, preferences)
        val triggerAtMillis = computeNextTriggerAtMillis(endTime, leadMinutes)
        val pendingIntent = reminderPendingIntent(
            category = category,
            endTime = endTime,
            flag = PendingIntent.FLAG_UPDATE_CURRENT,
            memberId = memberId,
            memberName = memberName,
            householdId = householdId
        )
            ?: return

        val alarm = alarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun reminderPendingIntent(
        category: TodoCategoryReminder,
        endTime: LocalTime,
        flag: Int,
        memberId: String,
        memberName: String,
        householdId: String
    ): PendingIntent? {
        return reminderPendingIntent(
            groupName = category.groupName,
            endTime = endTime,
            requestCode = category.ordinal + REQUEST_CODE_BASE,
            flag = flag,
            memberId = memberId,
            memberName = memberName,
            householdId = householdId
        )
    }

    private fun scheduleCustomTimePeriodReminder(
        reminder: CustomTimePeriodReminderPreference,
        reminderIndex: Int,
        preferences: NotificationPreferences,
        memberId: String,
        memberName: String,
        householdId: String
    ) {
        if (!preferences.categoryReminderPreferences.enabled || !reminder.enabled) return
        val endTime = runCatching { LocalTime.parse(reminder.endTime) }.getOrDefault(LocalTime.of(18, 0))
        val triggerAtMillis = computeNextTriggerAtMillis(endTime, reminder.leadMinutes.coerceAtLeast(0))
        val pendingIntent = customReminderPendingIntent(
            reminder = reminder,
            reminderIndex = reminderIndex,
            flag = PendingIntent.FLAG_UPDATE_CURRENT,
            memberId = memberId,
            memberName = memberName,
            householdId = householdId
        ) ?: return

        val alarm = alarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun customReminderPendingIntent(
        reminder: CustomTimePeriodReminderPreference,
        reminderIndex: Int,
        flag: Int,
        memberId: String,
        memberName: String,
        householdId: String
    ): PendingIntent? {
        val endTime = runCatching { LocalTime.parse(reminder.endTime) }.getOrDefault(LocalTime.of(18, 0))
        return reminderPendingIntent(
            groupName = reminder.groupName,
            endTime = endTime,
            requestCode = customRequestCode(reminder.groupName, reminderIndex),
            flag = flag,
            memberId = memberId,
            memberName = memberName,
            householdId = householdId
        )
    }

    private fun buildCustomReminderPreferences(
        customGroups: List<String>,
        notificationPreferences: NotificationPreferences
    ): List<CustomTimePeriodReminderPreference> {
        val overridesByGroup = notificationPreferences.customTimePeriodReminderPreferences
            .associateBy { it.groupName.lowercase() }
        return customGroups
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { group ->
                overridesByGroup[group.lowercase()] ?: CustomTimePeriodReminderPreference(groupName = group)
            }
    }

    private fun reminderPendingIntent(
        groupName: String,
        endTime: LocalTime,
        requestCode: Int,
        flag: Int,
        memberId: String,
        memberName: String,
        householdId: String
    ): PendingIntent? {
        val intent = Intent(context, CategoryReminderReceiver::class.java).apply {
            action = CategoryReminderReceiver.ACTION_CATEGORY_REMINDER
            putExtra(CategoryReminderReceiver.EXTRA_CATEGORY_GROUP, groupName)
            putExtra(CategoryReminderReceiver.EXTRA_CATEGORY_END_TIME, endTime.toString())
            putExtra(CategoryReminderReceiver.EXTRA_MEMBER_ID, memberId)
            putExtra(CategoryReminderReceiver.EXTRA_MEMBER_NAME, memberName)
            putExtra(CategoryReminderReceiver.EXTRA_HOUSEHOLD_ID, householdId)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flag or pendingIntentImmutableFlag()
        )
    }

    private fun categoryLeadMinutes(
        category: TodoCategoryReminder,
        preferences: NotificationPreferences
    ): Int {
        val reminderPrefs = preferences.categoryReminderPreferences
        return when (category) {
            TodoCategoryReminder.MORNING -> reminderPrefs.morningLeadMinutes
            TodoCategoryReminder.AFTERNOON -> reminderPrefs.afternoonLeadMinutes
            TodoCategoryReminder.EVENING -> reminderPrefs.eveningLeadMinutes
            TodoCategoryReminder.BEDTIME -> reminderPrefs.bedtimeLeadMinutes
        }
    }

    private fun categoryEnabled(
        category: TodoCategoryReminder,
        preferences: NotificationPreferences
    ): Boolean {
        val reminderPrefs = preferences.categoryReminderPreferences
        return when (category) {
            TodoCategoryReminder.MORNING -> reminderPrefs.morningEnabled
            TodoCategoryReminder.AFTERNOON -> reminderPrefs.afternoonEnabled
            TodoCategoryReminder.EVENING -> reminderPrefs.eveningEnabled
            TodoCategoryReminder.BEDTIME -> reminderPrefs.bedtimeEnabled
        }
    }

    private fun categoryEndTime(
        category: TodoCategoryReminder,
        preferences: NotificationPreferences
    ): LocalTime {
        val reminderPrefs = preferences.categoryReminderPreferences
        val raw = when (category) {
            TodoCategoryReminder.MORNING -> reminderPrefs.morningEndTime
            TodoCategoryReminder.AFTERNOON -> reminderPrefs.afternoonEndTime
            TodoCategoryReminder.EVENING -> reminderPrefs.eveningEndTime
            TodoCategoryReminder.BEDTIME -> reminderPrefs.bedtimeEndTime
        }
        return runCatching { LocalTime.parse(raw) }.getOrDefault(category.defaultEndTime)
    }

    private fun customRequestCode(groupName: String, reminderIndex: Int): Int {
        return CUSTOM_REQUEST_CODE_BASE + groupName.lowercase().hashCode().absoluteValue + reminderIndex
    }

    private fun computeNextTriggerAtMillis(
        endTime: LocalTime,
        leadMinutes: Int
    ): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        val candidate = LocalDate.now(zone)
            .atTime(endTime)
            .minusMinutes(leadMinutes.toLong())
            .atZone(zone)
        val next = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private companion object {
        const val REQUEST_CODE_BASE = 42000
        const val CUSTOM_REQUEST_CODE_BASE = 52000
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}

private val Int.absoluteValue: Int
    get() = if (this == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(this)
