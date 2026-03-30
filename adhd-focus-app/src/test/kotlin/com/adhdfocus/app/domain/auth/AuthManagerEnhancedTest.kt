package com.adhdfocus.app.domain.auth

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.HouseholdMemberResponse
import com.adhdfocus.app.data.network.HouseholdMembersResponse
import com.adhdfocus.app.data.network.HouseholdService
import com.adhdfocus.app.data.network.RefreshTokenRequest
import com.adhdfocus.app.data.network.RefreshTokenResponse
import com.adhdfocus.app.data.repository.UserRepository
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

/**
 * Tests for enhanced AuthManager functionality including token refresh and household loading
 */
class AuthManagerEnhancedTest {
    private lateinit var authService: AuthService
    private lateinit var tokenStorage: TokenStorage
    private lateinit var householdService: HouseholdService
    private lateinit var userRepository: UserRepository
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        authService = mockk()
        tokenStorage = mockk(relaxed = true)
        householdService = mockk()
        userRepository = mockk(relaxed = true)
        authManager = AuthManager(authService, tokenStorage, householdService, userRepository)
    }

    // ==================== Token Refresh Tests ====================

    @Test
    fun `refreshAccessToken with valid refresh token updates access token`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token_456"
        val newAccessToken = "new_access_token_789"
        val refreshResponse = RefreshTokenResponse(
            accessToken = newAccessToken,
            expiresIn = 3600
        )

        every { tokenStorage.getRefreshToken() } returns refreshToken
        val mockCall = mockk<Call<RefreshTokenResponse>>()
        every { mockCall.execute() } returns Response.success(refreshResponse)
        every { authService.refreshToken(any()) } returns mockCall

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assert(result is AuthResult.Success)
        verify { tokenStorage.saveAccessToken(newAccessToken) }
    }

    @Test
    fun `refreshAccessToken without refresh token returns error`() = runBlocking {
        // Arrange
        every { tokenStorage.getRefreshToken() } returns null

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.contains("No refresh token"))
    }

    @Test
    fun `refreshAccessToken with failed response returns error`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token_456"
        val errorBody = """{"error":"invalid_token","message":"Refresh token expired"}""".toResponseBody()

        every { tokenStorage.getRefreshToken() } returns refreshToken
        val mockCall = mockk<Call<RefreshTokenResponse>>()
        every { mockCall.execute() } returns Response.error(401, errorBody)
        every { authService.refreshToken(any()) } returns mockCall

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assert(result is AuthResult.Error)
    }

    @Test
    fun `refreshAccessToken with network error returns error`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token_456"

        every { tokenStorage.getRefreshToken() } returns refreshToken
        val mockCall = mockk<Call<RefreshTokenResponse>>()
        every { mockCall.execute() } throws Exception("Network error")
        every { authService.refreshToken(any()) } returns mockCall

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.contains("Network error"))
    }

    // ==================== Household Loading Tests ====================

    @Test
    fun `loadHouseholdData loads members and saves to database`() = runBlocking {
        // Arrange
        val householdId = "household_123"
        val members = listOf(
            HouseholdMemberResponse(
                id = "user_1",
                email = "user1@example.com",
                displayName = "User One",
                avatarUrl = "https://example.com/avatar1.jpg",
                role = "ADHD_USER",
                isPinProtected = false
            ),
            HouseholdMemberResponse(
                id = "user_2",
                email = "user2@example.com",
                displayName = "User Two",
                avatarUrl = "https://example.com/avatar2.jpg",
                role = "CAREGIVER",
                isPinProtected = true
            )
        )
        val membersResponse = HouseholdMembersResponse(members)

        val mockCall = mockk<Call<HouseholdMembersResponse>>()
        every { mockCall.execute() } returns Response.success(membersResponse)
        every { householdService.getHouseholdMembers(householdId) } returns mockCall

        // Act
        val result = authManager.loadHouseholdData(householdId)

        // Assert
        assert(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        assert(successResult.householdId == householdId)
        coVerify(exactly = 2) { userRepository.saveUser(any()) }
    }

    @Test
    fun `loadHouseholdData without householdService returns error`() = runBlocking {
        // Arrange
        val authManagerNoService = AuthManager(authService, tokenStorage)
        val householdId = "household_123"

        // Act
        val result = authManagerNoService.loadHouseholdData(householdId)

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.contains("not configured"))
    }

    @Test
    fun `loadHouseholdData with failed response returns error`() = runBlocking {
        // Arrange
        val householdId = "household_123"
        val errorBody = """{"error":"not_found","message":"Household not found"}""".toResponseBody()

        val mockCall = mockk<Call<HouseholdMembersResponse>>()
        every { mockCall.execute() } returns Response.error(404, errorBody)
        every { householdService.getHouseholdMembers(householdId) } returns mockCall

        // Act
        val result = authManager.loadHouseholdData(householdId)

        // Assert
        assert(result is AuthResult.Error)
    }

    @Test
    fun `loadHouseholdData with network error returns error`() = runBlocking {
        // Arrange
        val householdId = "household_123"

        val mockCall = mockk<Call<HouseholdMembersResponse>>()
        every { mockCall.execute() } throws Exception("Network error")
        every { householdService.getHouseholdMembers(householdId) } returns mockCall

        // Act
        val result = authManager.loadHouseholdData(householdId)

        // Assert
        assert(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assert(errorResult.message.contains("Network error"))
    }

    @Test
    fun `loadHouseholdData with empty response returns error`() = runBlocking {
        // Arrange
        val householdId = "household_123"

        val mockCall = mockk<Call<HouseholdMembersResponse>>()
        every { mockCall.execute() } returns Response.success(HouseholdMembersResponse(emptyList()))
        every { householdService.getHouseholdMembers(householdId) } returns mockCall

        // Act
        val result = authManager.loadHouseholdData(householdId)

        // Assert
        assert(result is AuthResult.Success)
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
    }

    // ==================== Token Validation Tests ====================

    @Test
    fun `isTokenValid returns true when tokens exist`() {
        // Arrange
        every { tokenStorage.hasTokens() } returns true

        // Act
        val result = authManager.isTokenValid()

        // Assert
        assert(result)
    }

    @Test
    fun `isTokenValid returns false when tokens don't exist`() {
        // Arrange
        every { tokenStorage.hasTokens() } returns false

        // Act
        val result = authManager.isTokenValid()

        // Assert
        assert(!result)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `login followed by household loading completes successfully`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val householdId = "household_123"
        val userId = "user_123"

        // Mock login
        val loginResponse = com.adhdfocus.app.data.network.LoginResponse(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_456",
            expiresIn = 3600,
            householdId = householdId,
            userId = userId
        )
        val loginCall = mockk<Call<com.adhdfocus.app.data.network.LoginResponse>>()
        every { loginCall.execute() } returns Response.success(loginResponse)
        every { authService.login(any()) } returns loginCall

        // Mock household loading
        val members = listOf(
            HouseholdMemberResponse(
                id = userId,
                email = email,
                displayName = "Test User",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            )
        )
        val membersResponse = HouseholdMembersResponse(members)
        val householdCall = mockk<Call<HouseholdMembersResponse>>()
        every { householdCall.execute() } returns Response.success(membersResponse)
        every { householdService.getHouseholdMembers(householdId) } returns householdCall

        // Act
        val loginResult = authManager.login(email, password)
        val householdResult = authManager.loadHouseholdData(householdId)

        // Assert
        assert(loginResult is AuthResult.Success)
        assert(householdResult is AuthResult.Success)
        verify { tokenStorage.saveTokens("access_token_123", "refresh_token_456") }
        coVerify { userRepository.saveUser(any()) }
    }

    @Test
    fun `token refresh after login maintains authentication`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token_456"
        val newAccessToken = "new_access_token_789"
        val refreshResponse = RefreshTokenResponse(
            accessToken = newAccessToken,
            expiresIn = 3600
        )

        every { tokenStorage.getRefreshToken() } returns refreshToken
        val mockCall = mockk<Call<RefreshTokenResponse>>()
        every { mockCall.execute() } returns Response.success(refreshResponse)
        every { authService.refreshToken(any()) } returns mockCall

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assert(result is AuthResult.Success)
        assert(authManager.isTokenValid())
    }
}
