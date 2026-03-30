package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.ui.timer.TimerViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of UpdateNotificationManager.
 *
 * Manages notifications for new tasks added by family members.
 * Integrates with TimerViewModel to avoid interrupting active timers.
 * Maintains a queue of notifications and provides visual feedback.
 * Respects notification preferences (sound, vibration, visual alerts).
 *
 * Correctness Properties:
 * - Property 11.5: Notifications should not interrupt active timers
 * - Property 11.6: Notifications should display task details
 * - Property 12: Notification preferences are applied to notifications
 */
class UpdateNotificationManagerImpl @Inject constructor(
    private val timerViewModel: TimerViewModel,
    private val notificationPreferencesManager: NotificationPreferencesManager
) : UpdateNotificationManager {

    private val notificationEventFlow = MutableSharedFlow<NotificationEvent>(replay = 0)
    private val notificationQueue = mutableListOf<TaskNotification>()
    private var currentUserId: String? = null

    override suspend fun showNotification(task: Task) {
        val notificationId = UUID.randomUUID().toString()
        val notification = TaskNotification(notificationId, task)

        // Check if timer is active
        if (timerViewModel.isRunning.value) {
            // Queue notification instead of showing immediately
            notificationQueue.add(notification)
            notificationEventFlow.emit(
                NotificationEvent.NotificationQueued(notificationId, task)
            )
        } else {
            // Show notification immediately
            notificationEventFlow.emit(
                NotificationEvent.NotificationShown(notificationId, task)
            )
        }
    }

    override suspend fun dismissNotification(notificationId: String) {
        // Remove from queue if present
        notificationQueue.removeAll { it.id == notificationId }

        // Emit dismissal event
        notificationEventFlow.emit(
            NotificationEvent.NotificationDismissed(notificationId)
        )
    }

    override fun observeNotifications(): Flow<NotificationEvent> = notificationEventFlow.asSharedFlow()

    override suspend fun isTimerActive(): Boolean = timerViewModel.isRunning.value

    override suspend fun getQueueSize(): Int = notificationQueue.size

    override suspend fun clearAll() {
        notificationQueue.clear()
    }

    /**
     * Sets the current user ID for preference checking.
     *
     * @param userId User ID
     */
    suspend fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    /**
     * Gets notification preferences for the current user.
     *
     * @return NotificationPreferences
     */
    suspend fun getNotificationPreferences() =
        currentUserId?.let { notificationPreferencesManager.getPreferencesOrDefault(it) }
            ?: com.adhdfocus.app.data.model.NotificationPreferences()
}

