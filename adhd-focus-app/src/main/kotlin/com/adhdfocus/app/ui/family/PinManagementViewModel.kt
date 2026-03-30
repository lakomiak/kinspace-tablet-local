package com.adhdfocus.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.domain.userswitching.PinManagementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PIN status for a user profile.
 */
enum class PinStatus {
    UNPROTECTED,
    PROTECTED,
    CHANGING
}

/**
 * PinManagementViewModel manages PIN setup, change, and removal UI state.
 *
 * Manages:
 * - PIN setup/change/removal operations
 * - Loading state during operations
 * - Error messages for user feedback
 * - Success messages for confirmations
 * - Current PIN status
 */
@HiltViewModel
class PinManagementViewModel @Inject constructor(
    private val pinManagementManager: PinManagementManager
) : ViewModel() {

    private val _currentPinStatus = MutableStateFlow(PinStatus.UNPROTECTED)
    val currentPinStatus: StateFlow<PinStatus> = _currentPinStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private var currentUserId: String? = null

    /**
     * Initializes the ViewModel with a user ID and loads their PIN status.
     *
     * @param userId User ID
     */
    fun initialize(userId: String) {
        currentUserId = userId
        loadPinStatus(userId)
    }

    /**
     * Loads the current PIN status for a user.
     *
     * @param userId User ID
     */
    private fun loadPinStatus(userId: String) {
        viewModelScope.launch {
            try {
                val isPinProtected = pinManagementManager.isPinProtected(userId)
                _currentPinStatus.value = if (isPinProtected) {
                    PinStatus.PROTECTED
                } else {
                    PinStatus.UNPROTECTED
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load PIN status: ${e.message}"
            }
        }
    }

    /**
     * Sets up a PIN for the user.
     *
     * @param pin PIN to set (4-8 digits, numeric only)
     */
    fun setupPin(pin: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val success = pinManagementManager.setPinForUser(userId, pin)
                if (success) {
                    _currentPinStatus.value = PinStatus.PROTECTED
                    _successMessage.value = "PIN set successfully"
                } else {
                    _errorMessage.value = "Failed to set PIN. Please check the format (4-8 digits)."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error setting PIN: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Changes the PIN for the user.
     *
     * @param currentPin Current PIN
     * @param newPin New PIN (4-8 digits, numeric only)
     */
    fun changePin(currentPin: String, newPin: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val success = pinManagementManager.changePinForUser(userId, currentPin, newPin)
                if (success) {
                    _successMessage.value = "PIN changed successfully"
                } else {
                    _errorMessage.value = "Failed to change PIN. Please verify your current PIN and try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error changing PIN: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Removes PIN protection from the user.
     *
     * @param currentPin Current PIN
     */
    fun removePin(currentPin: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val success = pinManagementManager.removePinForUser(userId, currentPin)
                if (success) {
                    _currentPinStatus.value = PinStatus.UNPROTECTED
                    _successMessage.value = "PIN protection removed"
                } else {
                    _errorMessage.value = "Failed to remove PIN. Please verify your PIN and try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error removing PIN: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears error and success messages.
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
