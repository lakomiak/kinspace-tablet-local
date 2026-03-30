package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing update notifications for new tasks.
 *
 * Responsibilities:
 * - Show notifications when new tasks are added by family members
 * - Dismiss notifications
 * - Observe notification events
 * - Check if timer is active to avoid interruptions
 * - Manage notification queue
 * - Provide notification preferences
 *
 * Correctness Properties:
 * - Property 11.5: Notifications should not interrupt active timers
 * - Property 11.6: Notifications should display task details
 */
interface UpdateNotificationManager {
    /**
     * Shows a notification for a new task.
     *
     * @param task The new task to notify about
     */
    suspend fun showNotification(task: Task)

    /**
     * Dismisses a notification by ID.
     *
     * @param notificationId The ID of the notification to dismiss
     */
    suspend fun dismissNotification(notificationId: String)

    /**
     * Observes notification events.
     *
     * @return Flow of NotificationEvent
     */
    fun observeNotifications(): Flow<NotificationEvent>

    /**
     * Checks if a timer is currently active.
     *
     * @return true if timer is running, false otherwise
     */
    suspend fun isTimerActive(): Boolean

    /**
     * Gets the current notification queue size.
     *
     * @return Number of pending notifications
     */
    suspend fun getQueueSize(): Int

    /**
     * Clears all pending notifications.
     */
    suspend fun clearAll()
}

/**
 * Represents a notification event.
 */
sealed class NotificationEvent {
    /**
     * Emitted when a notification is shown.
     */
    data class NotificationShown(
        val notificationId: String,
        val task: Task,
        val timestamp: Long = System.currentTimeMillis()
    ) : NotificationEvent()

    /**
     * Emitted when a notification is dismissed.
     */
    data class NotificationDismissed(
        val notificationId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : NotificationEvent()

    /**
     * Emitted when a notification is queued (timer active).
     */
    data class NotificationQueued(
        val notificationId: String,
        val task: Task,
        val timestamp: Long = System.currentTimeMillis()
    ) : NotificationEvent()
}

/**
 * Represents a notification with task details.
 */
data class TaskNotification(
    val id: String,
    val task: Task,
    val createdAt: Long = System.currentTimeMillis()
)
