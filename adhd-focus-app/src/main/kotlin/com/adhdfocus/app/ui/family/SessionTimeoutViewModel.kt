package com.adhdfocus.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.domain.userswitching.SessionTimeoutManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SessionTimeoutViewModel manages session timeout UI state and operations.
 *
 * Responsibilities:
 * - Manage timeout UI state
 * - Expose StateFlow properties for reactive UI updates
 * - Initialize session timeout for a user
 * - Record user activity
 * - Extend session
 * - Dismiss warning
 * - Handle timeout configuration
 *
 * State Properties:
 * - isSessionActive: Whether a session is currently active
 * - timeRemaining: Time remaining in session (in seconds)
 * - showWarning: Whether to show the warning dialog
 * - warningTimeRemaining: Time remaining before logout (in seconds)
 */
@HiltViewModel
class SessionTimeoutViewModel @Inject constructor(
    private val sessionTimeoutManager: SessionTimeoutManager,
    private val userPreferencesDao: UserPreferencesDao
) : ViewModel() {

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive

    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining

    private val _showWarning = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning

    private val _warningTimeRemaining = MutableStateFlow(0L)
    val warningTimeRemaining: StateFlow<Long> = _warningTimeRemaining

    init {
        // Observe session timeout manager state
        viewModelScope.launch {
            sessionTimeoutManager.isSessionActive.collect { isActive ->
                _isSessionActive.value = isActive
            }
        }

        viewModelScope.launch {
            sessionTimeoutManager.timeRemaining.collect { remaining ->
                _timeRemaining.value = remaining
            }
        }

        viewModelScope.launch {
            sessionTimeoutManager.showWarning.collect { show ->
                _showWarning.value = show
            }
        }

        viewModelScope.launch {
            sessionTimeoutManager.warningTimeRemaining.collect { remaining ->
                _warningTimeRemaining.value = remaining
            }
        }
    }

    /**
     * Initializes session timeout for a user.
     *
     * @param userId User ID
     */
    fun initialize(userId: String) {
        viewModelScope.launch {
            try {
                // Get user's timeout preference
                val preferences = userPreferencesDao.getPreferencesByUserId(userId)
                val timeoutMinutes = preferences?.autoLogoutTimeout ?: 0

                // Start session if timeout is enabled (> 0)
                if (timeoutMinutes > 0) {
                    sessionTimeoutManager.startSession(userId, timeoutMinutes, viewModelScope)
                }
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    /**
     * Records user activity and resets the timeout.
     */
    fun recordActivity() {
        sessionTimeoutManager.recordActivity()
    }

    /**
     * Extends the current session.
     */
    fun extendSession() {
        sessionTimeoutManager.extendSession()
    }

    /**
     * Dismisses the warning dialog.
     */
    fun dismissWarning() {
        sessionTimeoutManager.dismissWarning()
    }

    /**
     * Ends the current session.
     */
    fun endSession() {
        sessionTimeoutManager.endSession()
    }

    override fun onCleared() {
        super.onCleared()
        endSession()
    }
}
