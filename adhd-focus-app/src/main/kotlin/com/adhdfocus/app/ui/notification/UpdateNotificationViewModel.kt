package com.adhdfocus.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.domain.notification.NotificationEvent
import com.adhdfocus.app.domain.notification.UpdateNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing update notification state.
 *
 * Manages:
 * - Current notification display
 * - Notification visibility
 * - Notification queue
 * - Dismissal handling
 */
@HiltViewModel
class UpdateNotificationViewModel @Inject constructor(
    private val notificationManager: UpdateNotificationManager
) : ViewModel() {

    private val _currentNotification = MutableStateFlow<Task?>(null)
    val currentNotification: StateFlow<Task?> = _currentNotification

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationManager.observeNotifications().collect { event ->
                when (event) {
                    is NotificationEvent.NotificationShown -> {
                        _currentNotification.value = event.task
                        _isVisible.value = true
                        _queueSize.value = notificationManager.getQueueSize()
                    }
                    is NotificationEvent.NotificationDismissed -> {
                        _isVisible.value = false
                        _currentNotification.value = null
                        _queueSize.value = notificationManager.getQueueSize()
                    }
                    is NotificationEvent.NotificationQueued -> {
                        _queueSize.value = notificationManager.getQueueSize()
                    }
                }
            }
        }
    }

    fun dismissNotification() {
        viewModelScope.launch {
            _currentNotification.value?.let { task ->
                // Generate a notification ID based on task
                val notificationId = task.id
                notificationManager.dismissNotification(notificationId)
            }
        }
    }
}
