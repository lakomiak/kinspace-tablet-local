package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.UserDao
import com.adhdfocus.app.data.model.User
import javax.inject.Inject

/**
 * UserRepository provides data access abstraction for users.
 *
 * Handles:
 * - User CRUD operations
 * - User retrieval by household
 * - User preferences management
 * - User persistence
 */
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    /**
     * Gets all users in a household.
     *
     * @param householdId Household ID
     * @return List of users in the household
     */
    suspend fun getUsersByHousehold(householdId: String): List<User> {
        return userDao.getUsersByHouseholdOnce(householdId)
    }

    /**
     * Gets a user by ID.
     *
     * @param userId User ID
     * @return User or null if not found
     */
    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    /**
     * Saves a user.
     *
     * @param user User to save
     */
    suspend fun saveUser(user: User) {
        userDao.insert(user)
    }

    /**
     * Updates a user.
     *
     * @param user User to update
     */
    suspend fun updateUser(user: User) {
        userDao.update(user)
    }

    /**
     * Deletes a user.
     *
     * @param userId User ID
     */
    suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    /**
     * Gets the current user.
     *
     * @return Current user or null if not set
     */
    suspend fun getCurrentUser(): User? {
        // TODO: Implement current user retrieval from preferences
        return null
    }

    /**
     * Sets the current user.
     *
     * @param userId User ID to set as current
     */
    suspend fun setCurrentUser(userId: String) {
        // TODO: Implement current user storage in preferences
    }
}
