package com.adhdfocus.app.ui.auth

import com.adhdfocus.app.domain.auth.AuthManager
import com.adhdfocus.app.domain.auth.AuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AuthViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var authManager: AuthManager
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authManager = mockk()
        viewModel = AuthViewModel(authManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Login Tests ====================

    @Test
    fun `login with valid credentials sets authenticated state`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val householdId = "household_123"
        val userId = "user_123"

        coEvery { authManager.login(email, password) } returns AuthResult.Success(
            householdId = householdId,
            userId = userId
        )
        coEvery { authManager.loadHouseholdData(householdId) } returns AuthResult.Success(
            householdId = householdId
        )

        // Act
        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `login with invalid credentials sets error message`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"
        val errorMessage = "Invalid email or password"

        coEvery { authManager.login(email, password) } returns AuthResult.Error(errorMessage)

        // Act
        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    @Test
    fun `login sets loading state during authentication`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"

        coEvery { authManager.login(email, password) } returns AuthResult.Success()
        coEvery { authManager.loadHouseholdData(any()) } returns AuthResult.Success()

        // Act
        viewModel.login(email, password)

        // Assert - loading should be true initially
        assertTrue(viewModel.isLoading.value)

        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - loading should be false after completion
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `login with household loading failure sets error`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val householdId = "household_123"
        val householdError = "Failed to load household"

        coEvery { authManager.login(email, password) } returns AuthResult.Success(
            householdId = householdId
        )
        coEvery { authManager.loadHouseholdData(householdId) } returns AuthResult.Error(householdError)

        // Act
        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(householdError, viewModel.errorMessage.value)
    }

    // ==================== Logout Tests ====================

    @Test
    fun `logout clears authentication state`() = runTest {
        // Arrange
        coEvery { authManager.logout() } returns AuthResult.Success()

        // Act
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `logout with error sets error message`() = runTest {
        // Arrange
        val errorMessage = "Logout failed"
        coEvery { authManager.logout() } returns AuthResult.Error(errorMessage)

        // Act
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ==================== Token Refresh Tests ====================

    @Test
    fun `refreshToken updates state on success`() = runTest {
        // Arrange
        coEvery { authManager.refreshAccessToken() } returns AuthResult.Success()

        // Act
        viewModel.refreshToken()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `refreshToken sets unauthenticated on failure`() = runTest {
        // Arrange
        val errorMessage = "Token refresh failed"
        coEvery { authManager.refreshAccessToken() } returns AuthResult.Error(errorMessage)

        // Act
        viewModel.refreshToken()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `clearError removes error message`() {
        // Arrange
        viewModel.clearError()

        // Assert
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `error message is cleared on successful login`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"

        coEvery { authManager.login(email, password) } returns AuthResult.Success()
        coEvery { authManager.loadHouseholdData(any()) } returns AuthResult.Success()

        // Act
        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== State Management Tests ====================

    @Test
    fun `initial state is unauthenticated`() {
        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `multiple login attempts work correctly`() = runTest {
        // Arrange
        val email1 = "user1@example.com"
        val password1 = "password1"
        val email2 = "user2@example.com"
        val password2 = "password2"

        coEvery { authManager.login(email1, password1) } returns AuthResult.Success()
        coEvery { authManager.login(email2, password2) } returns AuthResult.Success()
        coEvery { authManager.loadHouseholdData(any()) } returns AuthResult.Success()

        // Act - First login
        viewModel.login(email1, password1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Authenticated, viewModel.authState.value)

        // Act - Logout
        coEvery { authManager.logout() } returns AuthResult.Success()
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)

        // Act - Second login
        viewModel.login(email2, password2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    // ==================== Per-Member Household Isolation Tests ====================

    @Test
    fun `login loads household members for current user`() = runTest {
        // Arrange
        val email = "user1@example.com"
        val password = "password123"
        val householdId = "household_123"
        val userId = "user_1"

        coEvery { authManager.login(email, password) } returns AuthResult.Success(
            householdId = householdId,
            userId = userId
        )
        coEvery { authManager.loadHouseholdData(householdId) } returns AuthResult.Success(
            householdId = householdId
        )

        // Act
        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { authManager.loadHouseholdData(householdId) }
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun `logout clears household data`() = runTest {
        // Arrange
        coEvery { authManager.logout() } returns AuthResult.Success()

        // Act
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        coVerify { authManager.logout() }
    }
}
