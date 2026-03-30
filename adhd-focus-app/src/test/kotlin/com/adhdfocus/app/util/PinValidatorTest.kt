package com.adhdfocus.app.util

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for PinValidator.
 *
 * **Validates: PIN validation and hashing functionality**
 *
 * These tests verify:
 * 1. PIN format validation
 * 2. PIN hashing
 * 3. PIN validation against hashes
 * 4. Edge cases (empty, too short, too long, non-numeric)
 */
class PinValidatorTest {

    @Test
    fun `isValidPinFormat accepts valid 4-digit PIN`() {
        assertTrue(PinValidator.isValidPinFormat("1234"))
    }

    @Test
    fun `isValidPinFormat accepts valid 8-digit PIN`() {
        assertTrue(PinValidator.isValidPinFormat("12345678"))
    }

    @Test
    fun `isValidPinFormat rejects PIN shorter than 4 digits`() {
        assertFalse(PinValidator.isValidPinFormat("123"))
    }

    @Test
    fun `isValidPinFormat rejects PIN longer than 8 digits`() {
        assertFalse(PinValidator.isValidPinFormat("123456789"))
    }

    @Test
    fun `isValidPinFormat rejects empty PIN`() {
        assertFalse(PinValidator.isValidPinFormat(""))
    }

    @Test
    fun `isValidPinFormat rejects PIN with non-numeric characters`() {
        assertFalse(PinValidator.isValidPinFormat("12a4"))
    }

    @Test
    fun `isValidPinFormat rejects PIN with spaces`() {
        assertFalse(PinValidator.isValidPinFormat("12 34"))
    }

    @Test
    fun `hashPin produces consistent hash`() {
        val pin = "1234"
        val hash1 = PinValidator.hashPin(pin)
        val hash2 = PinValidator.hashPin(pin)

        assertTrue(hash1 == hash2)
    }

    @Test
    fun `hashPin produces different hashes for different PINs`() {
        val hash1 = PinValidator.hashPin("1234")
        val hash2 = PinValidator.hashPin("5678")

        assertFalse(hash1 == hash2)
    }

    @Test
    fun `validatePin returns true for correct PIN`() {
        val pin = "1234"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }

    @Test
    fun `validatePin returns false for incorrect PIN`() {
        val correctPin = "1234"
        val incorrectPin = "5678"
        val hash = PinValidator.hashPin(correctPin)

        assertFalse(PinValidator.validatePin(incorrectPin, hash))
    }

    @Test
    fun `validatePin returns false for invalid PIN format`() {
        val hash = PinValidator.hashPin("1234")

        assertFalse(PinValidator.validatePin("123", hash))
    }

    @Test
    fun `validatePin returns false for empty PIN`() {
        val hash = PinValidator.hashPin("1234")

        assertFalse(PinValidator.validatePin("", hash))
    }

    @Test
    fun `validatePin returns false for PIN with non-numeric characters`() {
        val hash = PinValidator.hashPin("1234")

        assertFalse(PinValidator.validatePin("12a4", hash))
    }

    @Test
    fun `hashPin produces Base64 encoded output`() {
        val pin = "1234"
        val hash = PinValidator.hashPin(pin)

        // Base64 strings should only contain alphanumeric, +, /, and =
        assertTrue(hash.all { it.isLetterOrDigit() || it in "+=/" })
    }

    @Test
    fun `validatePin with all zeros PIN`() {
        val pin = "0000"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }

    @Test
    fun `validatePin with all nines PIN`() {
        val pin = "9999"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }

    @Test
    fun `validatePin with 5-digit PIN`() {
        val pin = "12345"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }

    @Test
    fun `validatePin with 6-digit PIN`() {
        val pin = "123456"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }

    @Test
    fun `validatePin with 7-digit PIN`() {
        val pin = "1234567"
        val hash = PinValidator.hashPin(pin)

        assertTrue(PinValidator.validatePin(pin, hash))
    }
}
