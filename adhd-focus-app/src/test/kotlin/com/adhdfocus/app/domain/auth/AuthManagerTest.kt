package com.adhdfocus.app.domain.auth

import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.LoginRequest
import com.adhdfocus.app.data.network.LoginResponse
import com.adhdfocus.app.data.network.LogoutRequest
import com.adhdfocus.app.data.network.RefreshTokenRequest
import com.adhdfocus.app.data.network.RefreshTokenResponse
import com.adhdfocus.app.data.security.TokenStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class AuthManagerTest {
    private lateinit var authService: AuthService
    private lateinit var tokenStorage: TokenStorage
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        authService = mockk()
        tokenStorage = mockk(relaxed = true)
        authManager = AuthManager(authService, tokenStorage)
    }

    @Test
    fun `login with valid credentials stores tokens and returns success`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val loginResponse = LoginResponse(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_456",
            expiresIn = 3600,
            householdId = "household_123",
            userId = "user_123"
        )

        val mockCall = mockk<Call<LoginResponse>>()
        every { mockCall.execute() } returns Response.success(loginResponse)
        every { authService.login(any()) } returns mockCall

        // Act
        val result = authManager.login(email, password)

        // Assert
        assert(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        assert(successResult.householdId == "household_123")
        assert(successResult.userId == "user_123")
        verify { tokenStorage.saveTokens("access_token_123", "refresh_token_456") }
    }

    @Test
    fun `login with invalid credentials returns error`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"
        val errorBody = """{"error":"invalid_credentials","message":"Invalid email or password"}""".toResponseBody()

        val mockCall = mockk<Call<LoginResponse>>()
        every { mockCall.execute() } returns Response.error(401, errorBody)
        every { authService.login(any()) } returns mockCall

        // Act
        val result = authManager.login(email, password)

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.isNotEmpty())
    }

    @Test
    fun `login with network error returns error`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"

        val mockCall = mockk<Call<LoginResponse>>()
        every { mockCall.execute() } throws Exception("Network error")
        every { authService.login(any()) } returns mockCall

        // Act
        val result = authManager.login(email, password)

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.contains("Network error"))
    }

    @Test
    fun `logout clears tokens and returns success`() = runBlocking {
        // Arrange
        every { tokenStorage.getRefreshToken() } returns "refresh_token_456"

        val mockCall = mockk<Call<Unit>>()
        every { mockCall.execute() } returns Response.success(Unit)
        every { authService.logout(any()) } returns mockCall

        // Act
        val result = authManager.logout()

        // Assert
        assert(result is AuthResult.Success)
        verify { tokenStorage.clearTokens() }
    }

    @Test
    fun `logout clears tokens even if request fails`() = runBlocking {
        // Arrange
        every { tokenStorage.getRefreshToken() } returns "refresh_token_456"

        val mockCall = mockk<Call<Unit>>()
        every { mockCall.execute() } throws Exception("Network error")
        every { authService.logout(any()) } returns mockCall

        // Act
        val result = authManager.logout()

        // Assert
        assert(result is AuthResult.Error)
        verify { tokenStorage.clearTokens() }
    }

    @Test
    fun `isAuthenticated returns true when tokens exist`() {
        // Arrange
        every { tokenStorage.hasTokens() } returns true

        // Act
        val result = authManager.isAuthenticated()

        // Assert
        assert(result)
    }

    @Test
    fun `isAuthenticated returns false when tokens don't exist`() {
        // Arrange
        every { tokenStorage.hasTokens() } returns false

        // Act
        val result = authManager.isAuthenticated()

        // Assert
        assert(!result)
    }

    @Test
    fun `getAccessToken returns stored token`() {
        // Arrange
        val token = "access_token_123"
        every { tokenStorage.getAccessToken() } returns token

        // Act
        val result = authManager.getAccessToken()

        // Assert
        assert(result == token)
    }

    @Test
    fun `getRefreshToken returns stored token`() {
        // Arrange
        val token = "refresh_token_456"
        every { tokenStorage.getRefreshToken() } returns token

        // Act
        val result = authManager.getRefreshToken()

        // Assert
        assert(result == token)
    }
}
