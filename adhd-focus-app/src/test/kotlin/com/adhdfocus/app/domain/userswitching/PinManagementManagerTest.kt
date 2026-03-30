package com.adhdfocus.app.domain.userswitching

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.util.PinValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinManagementManagerTest {
    private lateinit var userRepository: UserRepository
    private lateinit var pinManagementManager: PinManagementManager

    private val testUserId = "user-123"
    private val testHouseholdId = "household-456"
    private val testPin = "1234"
    private val testNewPin = "5678"

    private val testUser = User(
        id = testUserId,
        householdId = testHouseholdId,
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.ADHD_USER,
        isPinProtected = false,
        pinHash = null
    )

    @Before
    fun setup() {
        userRepository = mockk()
        pinManagementManager = PinManagementManager(userRepository)
    }

    // ============ setPinForUser Tests ============

    @Test
    fun `setPinForUser should set PIN for unprotected user`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser
        coEvery { userRepository.updateUser(any()) } returns Unit

        val result = pinManagementManager.setPinForUser(testUserId, testPin)

        assertTrue(result)
        coVerify { userRepository.updateUser(any()) }
    }

    @Test
    fun `setPinForUser should fail with invalid PIN format`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.setPinForUser(testUserId, "123") // Too short

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `setPinForUser should fail with non-numeric PIN`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.setPinForUser(testUserId, "12ab")

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `setPinForUser should fail if user not found`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns null

        val result = pinManagementManager.setPinForUser(testUserId, testPin)

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `setPinForUser should fail if PIN already set`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.setPinForUser(testUserId, testNewPin)

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `setPinForUser should fail on repository exception`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser
        coEvery { userRepository.updateUser(any()) } throws Exception("Database error")

        val result = pinManagementManager.setPinForUser(testUserId, testPin)

        assertFalse(result)
    }

    @Test
    fun `setPinForUser should accept 4-digit PIN`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser
        coEvery { userRepository.updateUser(any()) } returns Unit

        val result = pinManagementManager.setPinForUser(testUserId, "1234")

        assertTrue(result)
    }

    @Test
    fun `setPinForUser should accept 8-digit PIN`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser
        coEvery { userRepository.updateUser(any()) } returns Unit

        val result = pinManagementManager.setPinForUser(testUserId, "12345678")

        assertTrue(result)
    }

    @Test
    fun `setPinForUser should reject 3-digit PIN`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.setPinForUser(testUserId, "123")

        assertFalse(result)
    }

    @Test
    fun `setPinForUser should reject 9-digit PIN`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.setPinForUser(testUserId, "123456789")

        assertFalse(result)
    }

    // ============ changePinForUser Tests ============

    @Test
    fun `changePinForUser should change PIN for protected user`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser
        coEvery { userRepository.updateUser(any()) } returns Unit

        val result = pinManagementManager.changePinForUser(testUserId, testPin, testNewPin)

        assertTrue(result)
        coVerify { userRepository.updateUser(any()) }
    }

    @Test
    fun `changePinForUser should fail with invalid current PIN`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.changePinForUser(testUserId, "9999", testNewPin)

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `changePinForUser should fail if user not found`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns null

        val result = pinManagementManager.changePinForUser(testUserId, testPin, testNewPin)

        assertFalse(result)
    }

    @Test
    fun `changePinForUser should fail if no PIN set`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.changePinForUser(testUserId, testPin, testNewPin)

        assertFalse(result)
    }

    @Test
    fun `changePinForUser should fail with invalid new PIN format`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.changePinForUser(testUserId, testPin, "123")

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `changePinForUser should fail on repository exception`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser
        coEvery { userRepository.updateUser(any()) } throws Exception("Database error")

        val result = pinManagementManager.changePinForUser(testUserId, testPin, testNewPin)

        assertFalse(result)
    }

    // ============ removePinForUser Tests ============

    @Test
    fun `removePinForUser should remove PIN for protected user`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser
        coEvery { userRepository.updateUser(any()) } returns Unit

        val result = pinManagementManager.removePinForUser(testUserId, testPin)

        assertTrue(result)
        coVerify { userRepository.updateUser(any()) }
    }

    @Test
    fun `removePinForUser should fail with invalid PIN`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.removePinForUser(testUserId, "9999")

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `removePinForUser should fail if user not found`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns null

        val result = pinManagementManager.removePinForUser(testUserId, testPin)

        assertFalse(result)
    }

    @Test
    fun `removePinForUser should fail if no PIN set`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.removePinForUser(testUserId, testPin)

        assertFalse(result)
    }

    @Test
    fun `removePinForUser should fail with invalid PIN format`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.removePinForUser(testUserId, "123")

        assertFalse(result)
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }

    @Test
    fun `removePinForUser should fail on repository exception`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser
        coEvery { userRepository.updateUser(any()) } throws Exception("Database error")

        val result = pinManagementManager.removePinForUser(testUserId, testPin)

        assertFalse(result)
    }

    // ============ validateCurrentPin Tests ============

    @Test
    fun `validateCurrentPin should return true for correct PIN`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.validateCurrentPin(testUserId, testPin)

        assertTrue(result)
    }

    @Test
    fun `validateCurrentPin should return false for incorrect PIN`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.validateCurrentPin(testUserId, "9999")

        assertFalse(result)
    }

    @Test
    fun `validateCurrentPin should return false if user not found`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns null

        val result = pinManagementManager.validateCurrentPin(testUserId, testPin)

        assertFalse(result)
    }

    @Test
    fun `validateCurrentPin should return false if no PIN set`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.validateCurrentPin(testUserId, testPin)

        assertFalse(result)
    }

    @Test
    fun `validateCurrentPin should return false with invalid PIN format`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.validateCurrentPin(testUserId, "123")

        assertFalse(result)
    }

    // ============ isPinProtected Tests ============

    @Test
    fun `isPinProtected should return true for protected user`() = runTest {
        val protectedUser = testUser.copy(
            isPinProtected = true,
            pinHash = PinValidator.hashPin(testPin)
        )
        coEvery { userRepository.getUserById(testUserId) } returns protectedUser

        val result = pinManagementManager.isPinProtected(testUserId)

        assertTrue(result)
    }

    @Test
    fun `isPinProtected should return false for unprotected user`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns testUser

        val result = pinManagementManager.isPinProtected(testUserId)

        assertFalse(result)
    }

    @Test
    fun `isPinProtected should return false if user not found`() = runTest {
        coEvery { userRepository.getUserById(testUserId) } returns null

        val result = pinManagementManager.isPinProtected(testUserId)

        assertFalse(result)
    }
}
