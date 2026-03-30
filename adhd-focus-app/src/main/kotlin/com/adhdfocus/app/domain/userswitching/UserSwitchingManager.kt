package com.adhdfocus.app.domain.userswitching

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.repository.UserSwitchingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UserSwitchingManager handles user switching logic and state management.
 *
 * Responsibilities:
 * - Switch between family members
 * - Validate user switches
 * - Track current user
 * - Manage session state
 * - Provide per-member data isolation
 */
class UserSwitchingManager @Inject constructor(
    private val userSwitchingRepository: UserSwitchingRepository
) {
    /**
     * Switches to a different user.
     *
     * @param userId User ID to switch to
     * @param householdId Household ID
     * @return True if switch successful, false otherwise
     */
    suspend fun switchUser(userId: String, householdId: String): Boolean {
        // Validate the user switch
        if (!validateUserSwitch(userId, householdId)) {
            return false
        }

        // Set the current user
        return userSwitchingRepository.setCurrentUser(userId, householdId)
    }

    /**
     * Gets the current active user.
     *
     * @return Current user or null if not set
     */
    suspend fun getCurrentUser(): User? {
        return userSwitchingRepository.getCurrentUser()
    }

    /**
     * Gets the current user as a Flow.
     *
     * @return Flow of current user
     */
    fun getCurrentUserFlow(): Flow<User?> {
        return kotlinx.coroutines.flow.flow {
            userSwitchingRepository.getCurrentUserStateFlow().collect { state ->
                if (state != null) {
                    val user = userSwitchingRepository.getCurrentUser()
                    emit(user)
                } else {
                    emit(null)
                }
            }
        }
    }

    /**
     * Validates if a user switch is allowed.
     *
     * @param userId User ID to validate
     * @param householdId Household ID
     * @return True if switch is valid, false otherwise
     */
    suspend fun validateUserSwitch(userId: String, householdId: String): Boolean {
        return userSwitchingRepository.validateUserSwitch(userId, householdId)
    }

    /**
     * Checks if user switching is enabled.
     *
     * @return True if user switching is enabled
     */
    suspend fun isUserSwitchingEnabled(): Boolean {
        return userSwitchingRepository.isUserSwitchingEnabled()
    }

    /**
     * Clears the current user (logout).
     */
    suspend fun clearCurrentUser() {
        userSwitchingRepository.clearCurrentUser()
    }

    /**
     * Gets the session duration in milliseconds.
     *
     * @return Session duration or 0 if no active session
     */
    suspend fun getSessionDuration(): Long {
        return userSwitchingRepository.getSessionDuration()
    }

    /**
     * Gets the time since last user switch in milliseconds.
     *
     * @return Time since last switch or 0 if no switch recorded
     */
    suspend fun getTimeSinceLastSwitch(): Long {
        return userSwitchingRepository.getTimeSinceLastSwitch()
    }
}
