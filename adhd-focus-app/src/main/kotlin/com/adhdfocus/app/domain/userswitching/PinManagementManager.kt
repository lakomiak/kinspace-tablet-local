package com.adhdfocus.app.domain.userswitching

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.util.PinValidator
import javax.inject.Inject

/**
 * PinManagementManager handles PIN setup, change, and removal for user profiles.
 *
 * Manages:
 * - Setting PIN for a user
 * - Changing existing PIN
 * - Removing PIN protection
 * - Validating current PIN
 * - Checking PIN protection status
 */
class PinManagementManager @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Sets a PIN for a user.
     *
     * @param userId User ID
     * @param pin PIN to set (4-8 digits, numeric only)
     * @return True if PIN was set successfully
     */
    suspend fun setPinForUser(userId: String, pin: String): Boolean {
        if (!PinValidator.isValidPinFormat(pin)) {
            return false
        }

        val user = userRepository.getUserById(userId) ?: return false

        if (user.isPinProtected) {
            return false // PIN already set
        }

        val pinHash = PinValidator.hashPin(pin)
        val updatedUser = user.copy(
            isPinProtected = true,
            pinHash = pinHash
        )

        return try {
            userRepository.updateUser(updatedUser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes the PIN for a user.
     *
     * @param userId User ID
     * @param currentPin Current PIN
     * @param newPin New PIN (4-8 digits, numeric only)
     * @return True if PIN was changed successfully
     */
    suspend fun changePinForUser(userId: String, currentPin: String, newPin: String): Boolean {
        if (!PinValidator.isValidPinFormat(currentPin) || !PinValidator.isValidPinFormat(newPin)) {
            return false
        }

        val user = userRepository.getUserById(userId) ?: return false

        if (!user.isPinProtected || user.pinHash == null) {
            return false // No PIN set
        }

        // Validate current PIN
        if (!PinValidator.validatePin(currentPin, user.pinHash)) {
            return false
        }

        val newPinHash = PinValidator.hashPin(newPin)
        val updatedUser = user.copy(pinHash = newPinHash)

        return try {
            userRepository.updateUser(updatedUser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Removes PIN protection from a user.
     *
     * @param userId User ID
     * @param currentPin Current PIN
     * @return True if PIN was removed successfully
     */
    suspend fun removePinForUser(userId: String, currentPin: String): Boolean {
        if (!PinValidator.isValidPinFormat(currentPin)) {
            return false
        }

        val user = userRepository.getUserById(userId) ?: return false

        if (!user.isPinProtected || user.pinHash == null) {
            return false // No PIN set
        }

        // Validate current PIN
        if (!PinValidator.validatePin(currentPin, user.pinHash)) {
            return false
        }

        val updatedUser = user.copy(
            isPinProtected = false,
            pinHash = null
        )

        return try {
            userRepository.updateUser(updatedUser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates the current PIN for a user.
     *
     * @param userId User ID
     * @param pin PIN to validate
     * @return True if PIN is valid
     */
    suspend fun validateCurrentPin(userId: String, pin: String): Boolean {
        if (!PinValidator.isValidPinFormat(pin)) {
            return false
        }

        val user = userRepository.getUserById(userId) ?: return false

        if (!user.isPinProtected || user.pinHash == null) {
            return false
        }

        return PinValidator.validatePin(pin, user.pinHash)
    }

    /**
     * Checks if a user has PIN protection enabled.
     *
     * @param userId User ID
     * @return True if user has PIN protection
     */
    suspend fun isPinProtected(userId: String): Boolean {
        val user = userRepository.getUserById(userId) ?: return false
        return user.isPinProtected
    }
}
