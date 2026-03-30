package com.adhdfocus.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.userswitching.UserSwitchingManager
import com.adhdfocus.app.util.PinValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FamilyMemberSwitcherViewModel manages family member switching state.
 *
 * Manages:
 * - Household members list
 * - Current user
 * - Member selection modal state
 * - PIN validation for protected profiles
 * - Error handling for switching operations
 */
@HiltViewModel
class FamilyMemberSwitcherViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userSwitchingManager: UserSwitchingManager
) : ViewModel() {

    private val _householdMembers = MutableStateFlow<List<User>>(emptyList())
    val householdMembers: StateFlow<List<User>> = _householdMembers

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isModalOpen = MutableStateFlow(false)
    val isModalOpen: StateFlow<Boolean> = _isModalOpen

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSwitching = MutableStateFlow(false)
    val isSwitching: StateFlow<Boolean> = _isSwitching

    /**
     * Loads household members.
     *
     * @param householdId Household ID
     */
    fun loadHouseholdMembers(householdId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val members = userRepository.getUsersByHousehold(householdId)
                _householdMembers.value = members
                if (members.isEmpty()) {
                    _errorMessage.value = "No household members found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load household members: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Switches to a different family member.
     *
     * @param userId User ID to switch to
     * @param pin PIN if the profile is protected (optional)
     */
    fun switchToMember(userId: String, pin: String? = null) {
        viewModelScope.launch {
            _isSwitching.value = true
            _errorMessage.value = null
            try {
                val user = _householdMembers.value.find { it.id == userId }
                if (user == null) {
                    _errorMessage.value = "User not found"
                    return@launch
                }

                // Check if PIN is required
                if (user.isPinProtected) {
                    if (pin == null) {
                        _errorMessage.value = "PIN required for this profile"
                        return@launch
                    }
                    if (!validatePin(pin, user)) {
                        _errorMessage.value = "Invalid PIN"
                        return@launch
                    }
                }

                // Perform the actual user switch using UserSwitchingManager
                val switchSuccess = userSwitchingManager.switchUser(userId, user.householdId)
                if (switchSuccess) {
                    _currentUser.value = user
                    _isModalOpen.value = false
                } else {
                    _errorMessage.value = "Failed to switch user"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error switching user: ${e.message}"
            } finally {
                _isSwitching.value = false
            }
        }
    }

    /**
     * Opens the member selection modal.
     */
    fun openMemberSelector() {
        _isModalOpen.value = true
        _errorMessage.value = null
    }

    /**
     * Closes the member selection modal.
     */
    fun closeMemberSelector() {
        _isModalOpen.value = false
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Validates a PIN for a user.
     *
     * @param pin PIN to validate
     * @param user User to validate against
     * @return True if PIN is valid
     */
    private fun validatePin(pin: String, user: User): Boolean {
        if (user.pinHash == null) {
            return false
        }
        return PinValidator.validatePin(pin, user.pinHash)
    }
}
