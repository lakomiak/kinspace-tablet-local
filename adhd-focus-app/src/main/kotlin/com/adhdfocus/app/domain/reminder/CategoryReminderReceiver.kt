package com.adhdfocus.app.domain.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adhdfocus.app.MainActivity
import com.adhdfocus.app.R
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class CategoryReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var audioNotificationManager: AudioNotificationManager
    @Inject lateinit var userPreferencesManager: UserPreferencesManager
    @Inject lateinit var setupManager: TabletSetupManager
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var categoryReminderScheduler: CategoryReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleReminder(context, intent)
            } finally {
                runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, intent: Intent) {
        val categoryGroup = intent.getStringExtra(EXTRA_CATEGORY_GROUP)
            ?: return
        val householdId = intent.getStringExtra(EXTRA_HOUSEHOLD_ID)
            ?: setupManager.getHouseholdId().orEmpty()
        val memberId = intent.getStringExtra(EXTRA_MEMBER_ID)
            ?: setupManager.getAssignedMemberId().orEmpty()
        val memberName = intent.getStringExtra(EXTRA_MEMBER_NAME)
            ?: setupManager.getAssignedMemberName().orEmpty()

        if (householdId.isBlank() || memberId.isBlank()) return

        val todaysTasks = taskRepository.getTasksForDate(
            householdId = householdId,
            userId = memberId,
            targetDate = LocalDate.now(),
            memberName = memberName
        )

        val outstanding = todaysTasks.filter { task ->
            task.todoGroup.equals(categoryGroup, ignoreCase = true) &&
                task.status != TaskStatus.COMPLETED &&
                !task.isDeleted
        }

        if (outstanding.isEmpty()) {
            return
        }

        val prefs = userPreferencesManager.getPreferences(memberId)
            ?: userPreferencesManager.getPreferencesOrDefault(memberId)
        val notificationPrefs = userPreferencesManager.deserializeNotificationPreferences(
            prefs.notificationPreferences
        )

        if (notificationPrefs.soundEnabled) {
            audioNotificationManager.playTimerCompletionSound(
                notificationPrefs.timerAlarmSound
            )
        }

        if (notificationPrefs.visualAlertsEnabled || notificationPrefs.soundEnabled || notificationPrefs.vibrationEnabled) {
            showReminderNotification(
                context = context,
                categoryGroup = categoryGroup,
                outstanding = outstanding.size
            )
        }
    }

    private fun showReminderNotification(
        context: Context,
        categoryGroup: String,
        outstanding: Int
    ) {
        ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            categoryGroup.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentTitle = "$categoryGroup reminder"
        val contentText = if (outstanding == 1) {
            "1 todo is still outstanding."
        } else {
            "$outstanding todos are still outstanding."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            categoryGroup.hashCode(),
            notification
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Category Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Audible reminders before category time windows end"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_CATEGORY_REMINDER = "com.adhdfocus.app.ACTION_CATEGORY_REMINDER"
        const val EXTRA_CATEGORY_GROUP = "extra_category_group"
        const val EXTRA_CATEGORY_END_TIME = "extra_category_end_time"
        const val EXTRA_MEMBER_ID = "extra_member_id"
        const val EXTRA_MEMBER_NAME = "extra_member_name"
        const val EXTRA_HOUSEHOLD_ID = "extra_household_id"
        private const val CHANNEL_ID = "category_reminders"
    }
}
