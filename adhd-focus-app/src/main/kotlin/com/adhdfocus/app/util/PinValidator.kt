package com.adhdfocus.app.util

import java.security.MessageDigest
import java.util.Base64

/**
 * PinValidator provides PIN validation and hashing utilities.
 *
 * Handles:
 * - PIN hashing for secure storage
 * - PIN validation against stored hashes
 * - PIN format validation
 */
object PinValidator {
    private const val MIN_PIN_LENGTH = 4
    private const val MAX_PIN_LENGTH = 8

    /**
     * Validates PIN format.
     *
     * @param pin PIN to validate
     * @return True if PIN format is valid
     */
    fun isValidPinFormat(pin: String): Boolean {
        if (pin.length < MIN_PIN_LENGTH || pin.length > MAX_PIN_LENGTH) {
            return false
        }
        return pin.all { it.isDigit() }
    }

    /**
     * Hashes a PIN using SHA-256.
     *
     * @param pin PIN to hash
     * @return Hashed PIN as Base64 string
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Validates a PIN against a stored hash.
     *
     * @param pin PIN to validate
     * @param pinHash Stored PIN hash
     * @return True if PIN matches hash
     */
    fun validatePin(pin: String, pinHash: String): Boolean {
        if (!isValidPinFormat(pin)) {
            return false
        }
        val hash = hashPin(pin)
        return hash == pinHash
    }
}
