package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UpdateNotificationManager.
 * Uses a shared timer state rather than direct ViewModel dependency.
 */
@Singleton
class UpdateNotificationManagerImpl @Inject constructor(
    private val notificationPreferencesManager: NotificationPreferencesManager
) : UpdateNotificationManager {

    private val notificationEventFlow = MutableSharedFlow<NotificationEvent>(replay = 0)
    private val notificationQueue = mutableListOf<TaskNotification>()
    private var currentUserId: String? = null

    // Shared timer state - updated externally by TimerViewModel
    private val timerRunning = MutableStateFlow(false)

    fun setTimerRunning(running: Boolean) {
        timerRunning.value = running
    }

    override suspend fun showNotification(task: Task) {
        val notificationId = UUID.randomUUID().toString()
        val notification = TaskNotification(notificationId, task)

        if (timerRunning.value) {
            notificationQueue.add(notification)
            notificationEventFlow.emit(NotificationEvent.NotificationQueued(notificationId, task))
        } else {
            notificationEventFlow.emit(NotificationEvent.NotificationShown(notificationId, task))
        }
    }

    override suspend fun dismissNotification(notificationId: String) {
        notificationQueue.removeAll { it.id == notificationId }
        notificationEventFlow.emit(NotificationEvent.NotificationDismissed(notificationId))
    }

    override fun observeNotifications(): Flow<NotificationEvent> = notificationEventFlow.asSharedFlow()

    override suspend fun isTimerActive(): Boolean = timerRunning.value

    override suspend fun getQueueSize(): Int = notificationQueue.size

    override suspend fun clearAll() {
        notificationQueue.clear()
    }

    suspend fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    suspend fun getNotificationPreferences() =
        currentUserId?.let { notificationPreferencesManager.getPreferencesOrDefault(it) }
            ?: com.adhdfocus.app.data.model.NotificationPreferences()
}
