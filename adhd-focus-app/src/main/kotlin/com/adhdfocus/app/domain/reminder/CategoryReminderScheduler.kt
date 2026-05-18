package com.adhdfocus.app.domain.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
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
    }

    fun cancelAll() {
        TodoCategoryReminder.values().forEach { category ->
            val pendingIntent = reminderPendingIntent(
                category = category,
                flag = PendingIntent.FLAG_NO_CREATE,
                memberId = setupManager.getAssignedMemberId().orEmpty(),
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
        val triggerAtMillis = computeNextTriggerAtMillis(category, leadMinutes)
        val pendingIntent = reminderPendingIntent(
            category = category,
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
        flag: Int,
        memberId: String,
        memberName: String,
        householdId: String
    ): PendingIntent? {
        val intent = Intent(context, CategoryReminderReceiver::class.java).apply {
            action = CategoryReminderReceiver.ACTION_CATEGORY_REMINDER
            putExtra(CategoryReminderReceiver.EXTRA_CATEGORY_GROUP, category.groupName)
            putExtra(CategoryReminderReceiver.EXTRA_CATEGORY_END_TIME, category.endTime.toString())
            putExtra(CategoryReminderReceiver.EXTRA_MEMBER_ID, memberId)
            putExtra(CategoryReminderReceiver.EXTRA_MEMBER_NAME, memberName)
            putExtra(CategoryReminderReceiver.EXTRA_HOUSEHOLD_ID, householdId)
        }

        return PendingIntent.getBroadcast(
            context,
            category.ordinal + REQUEST_CODE_BASE,
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

    private fun computeNextTriggerAtMillis(
        category: TodoCategoryReminder,
        leadMinutes: Int
    ): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        val candidate = LocalDate.now(zone)
            .atTime(category.endTime)
            .minusMinutes(leadMinutes.toLong())
            .atZone(zone)
        val next = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private companion object {
        const val REQUEST_CODE_BASE = 42000
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}
