package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.UserDao
import com.adhdfocus.app.data.dao.UserSwitchingStateDao
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserSwitchingState
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/**
 * UserSwitchingRepository provides data access abstraction for user switching.
 *
 * Handles:
 * - Current user persistence
 * - User switching state management
 * - Session tracking
 * - Per-member data isolation
 */
class UserSwitchingRepository @Inject constructor(
    private val userSwitchingStateDao: UserSwitchingStateDao,
    private val userDao: UserDao
) {
    /**
     * Gets the current user switching state.
     *
     * @return Current user switching state or null if not set
     */
    suspend fun getCurrentUserState(): UserSwitchingState? {
        return userSwitchingStateDao.getCurrentUserState()
    }

    /**
     * Gets the current user switching state as a Flow.
     *
     * @return Flow of current user switching state
     */
    fun getCurrentUserStateFlow(): Flow<UserSwitchingState?> {
        return userSwitchingStateDao.getCurrentUserStateFlow()
    }

    /**
     * Gets the current active user.
     *
     * @return Current user or null if not set
     */
    suspend fun getCurrentUser(): User? {
        val state = getCurrentUserState() ?: return null
        return userDao.getUserById(state.userId)
    }

    /**
     * Sets the current user.
     *
     * @param userId User ID to set as current
     * @param householdId Household ID
     * @return True if successful, false otherwise
     */
    suspend fun setCurrentUser(userId: String, householdId: String): Boolean {
        // Verify user exists and belongs to household
        val user = userDao.getUserById(userId) ?: return false
        if (user.householdId != householdId) return false

        val currentState = getCurrentUserState()
        val newState = UserSwitchingState(
            userId = userId,
            householdId = householdId,
            lastSwitchTime = Instant.now(),
            sessionStartTime = currentState?.sessionStartTime ?: Instant.now()
        )

        if (currentState == null) {
            userSwitchingStateDao.insert(newState)
        } else {
            userSwitchingStateDao.update(newState)
        }
        return true
    }

    /**
     * Clears the current user.
     */
    suspend fun clearCurrentUser() {
        userSwitchingStateDao.clearCurrentUser()
    }

    /**
     * Validates if a user can be switched to.
     *
     * @param userId User ID to validate
     * @param householdId Household ID
     * @return True if user can be switched to, false otherwise
     */
    suspend fun validateUserSwitch(userId: String, householdId: String): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        return user.householdId == householdId
    }

    /**
     * Checks if user switching is enabled.
     *
     * @return True if user switching is enabled
     */
    suspend fun isUserSwitchingEnabled(): Boolean {
        return getCurrentUserState() != null
    }

    /**
     * Gets the session duration in milliseconds.
     *
     * @return Session duration or 0 if no active session
     */
    suspend fun getSessionDuration(): Long {
        val state = getCurrentUserState() ?: return 0
        return Instant.now().toEpochMilli() - state.sessionStartTime.toEpochMilli()
    }

    /**
     * Gets the time since last user switch in milliseconds.
     *
     * @return Time since last switch or 0 if no switch recorded
     */
    suspend fun getTimeSinceLastSwitch(): Long {
        val state = getCurrentUserState() ?: return 0
        return Instant.now().toEpochMilli() - state.lastSwitchTime.toEpochMilli()
    }
}
