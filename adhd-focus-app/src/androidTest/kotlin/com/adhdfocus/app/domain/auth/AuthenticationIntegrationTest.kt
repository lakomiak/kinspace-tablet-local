package com.adhdfocus.app.domain.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.HouseholdMemberResponse
import com.adhdfocus.app.data.network.HouseholdMembersResponse
import com.adhdfocus.app.data.network.HouseholdService
import com.adhdfocus.app.data.network.LoginResponse
import com.adhdfocus.app.data.network.RefreshTokenResponse
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.data.security.TokenStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for authentication flow with household management
 */
class AuthenticationIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: AdhdfocusDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var tokenStorage: TokenStorage
    private lateinit var authService: AuthService
    private lateinit var householdService: HouseholdService
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AdhdfocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(database.userDao())
        tokenStorage = TokenStorage(context)
        authService = mockk()
        householdService = mockk()
        authManager = AuthManager(authService, tokenStorage, householdService, userRepository)
    }

    @After
    fun tearDown() {
        database.close()
        tokenStorage.clearTokens()
    }

    // ==================== Authentication Flow Tests ====================

    @Test
    fun `complete authentication flow with household loading`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val householdId = "household_123"
        val userId = "user_123"

        val loginResponse = LoginResponse(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_456",
            expiresIn = 3600,
            householdId = householdId,
            userId = userId
        )

        val members = listOf(
            HouseholdMemberResponse(
                id = userId,
                email = email,
                displayName = "Test User",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            ),
            HouseholdMemberResponse(
                id = "user_456",
                email = "caregiver@example.com",
                displayName = "Caregiver",
                avatarUrl = null,
                role = "CAREGIVER",
                isPinProtected = false
            )
        )

        val loginCall = mockk<Call<LoginResponse>>()
        every { loginCall.execute() } returns Response.success(loginResponse)
        every { authService.login(any()) } returns loginCall

        val householdCall = mockk<Call<HouseholdMembersResponse>>()
        every { householdCall.execute() } returns Response.success(HouseholdMembersResponse(members))
        every { householdService.getHouseholdMembers(householdId) } returns householdCall

        // Act
        val loginResult = authManager.login(email, password)
        val householdResult = authManager.loadHouseholdData(householdId)

        // Assert
        assertTrue(loginResult is AuthResult.Success)
        assertTrue(householdResult is AuthResult.Success)
        assertTrue(authManager.isAuthenticated())
        assertTrue(authManager.isTokenValid())

        // Verify tokens are stored
        assertNotNull(authManager.getAccessToken())
        assertNotNull(authManager.getRefreshToken())

        // Verify household members are saved
        val savedMembers = userRepository.getUsersByHousehold(householdId)
        assertEquals(2, savedMembers.size)
        assertEquals("Test User", savedMembers[0].displayName)
        assertEquals("Caregiver", savedMembers[1].displayName)
    }

    @Test
    fun `token refresh maintains authentication`() = runBlocking {
        // Arrange
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val newAccessToken = "new_access_token_789"

        tokenStorage.saveTokens(accessToken, refreshToken)

        val refreshResponse = RefreshTokenResponse(
            accessToken = newAccessToken,
            expiresIn = 3600
        )

        val refreshCall = mockk<Call<RefreshTokenResponse>>()
        every { refreshCall.execute() } returns Response.success(refreshResponse)
        every { authService.refreshToken(any()) } returns refreshCall

        // Act
        val result = authManager.refreshAccessToken()

        // Assert
        assertTrue(result is AuthResult.Success)
        assertTrue(authManager.isAuthenticated())
        assertEquals(newAccessToken, authManager.getAccessToken())
        assertEquals(refreshToken, authManager.getRefreshToken())
    }

    @Test
    fun `logout clears all authentication data`() = runBlocking {
        // Arrange
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        tokenStorage.saveTokens(accessToken, refreshToken)

        val logoutCall = mockk<Call<Unit>>()
        every { logoutCall.execute() } returns Response.success(Unit)
        every { authService.logout(any()) } returns logoutCall

        // Act
        val result = authManager.logout()

        // Assert
        assertTrue(result is AuthResult.Success)
        assertFalse(authManager.isAuthenticated())
        assertFalse(authManager.isTokenValid())
    }

    // ==================== Per-Member Household Isolation Tests ====================

    @Test
    fun `household members are isolated per household`() = runBlocking {
        // Arrange
        val household1Id = "household_1"
        val household2Id = "household_2"

        val household1Members = listOf(
            HouseholdMemberResponse(
                id = "user_1",
                email = "user1@example.com",
                displayName = "User 1",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            )
        )

        val household2Members = listOf(
            HouseholdMemberResponse(
                id = "user_2",
                email = "user2@example.com",
                displayName = "User 2",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            )
        )

        val call1 = mockk<Call<HouseholdMembersResponse>>()
        every { call1.execute() } returns Response.success(HouseholdMembersResponse(household1Members))
        every { householdService.getHouseholdMembers(household1Id) } returns call1

        val call2 = mockk<Call<HouseholdMembersResponse>>()
        every { call2.execute() } returns Response.success(HouseholdMembersResponse(household2Members))
        every { householdService.getHouseholdMembers(household2Id) } returns call2

        // Act
        authManager.loadHouseholdData(household1Id)
        authManager.loadHouseholdData(household2Id)

        // Assert
        val members1 = userRepository.getUsersByHousehold(household1Id)
        val members2 = userRepository.getUsersByHousehold(household2Id)

        assertEquals(1, members1.size)
        assertEquals(1, members2.size)
        assertEquals("User 1", members1[0].displayName)
        assertEquals("User 2", members2[0].displayName)
    }

    @Test
    fun `user roles are preserved during household loading`() = runBlocking {
        // Arrange
        val householdId = "household_123"

        val members = listOf(
            HouseholdMemberResponse(
                id = "user_1",
                email = "adhd@example.com",
                displayName = "ADHD User",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            ),
            HouseholdMemberResponse(
                id = "user_2",
                email = "caregiver@example.com",
                displayName = "Caregiver",
                avatarUrl = null,
                role = "CAREGIVER",
                isPinProtected = false
            ),
            HouseholdMemberResponse(
                id = "user_3",
                email = "admin@example.com",
                displayName = "Admin",
                avatarUrl = null,
                role = "ADMIN",
                isPinProtected = false
            )
        )

        val call = mockk<Call<HouseholdMembersResponse>>()
        every { call.execute() } returns Response.success(HouseholdMembersResponse(members))
        every { householdService.getHouseholdMembers(householdId) } returns call

        // Act
        authManager.loadHouseholdData(householdId)

        // Assert
        val savedMembers = userRepository.getUsersByHousehold(householdId)
        assertEquals(3, savedMembers.size)
        assertEquals(UserRole.ADHD_USER, savedMembers[0].role)
        assertEquals(UserRole.CAREGIVER, savedMembers[1].role)
        assertEquals(UserRole.ADMIN, savedMembers[2].role)
    }

    @Test
    fun `pin protection status is preserved during household loading`() = runBlocking {
        // Arrange
        val householdId = "household_123"

        val members = listOf(
            HouseholdMemberResponse(
                id = "user_1",
                email = "user1@example.com",
                displayName = "User 1",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = false
            ),
            HouseholdMemberResponse(
                id = "user_2",
                email = "user2@example.com",
                displayName = "User 2",
                avatarUrl = null,
                role = "ADHD_USER",
                isPinProtected = true
            )
        )

        val call = mockk<Call<HouseholdMembersResponse>>()
        every { call.execute() } returns Response.success(HouseholdMembersResponse(members))
        every { householdService.getHouseholdMembers(householdId) } returns call

        // Act
        authManager.loadHouseholdData(householdId)

        // Assert
        val savedMembers = userRepository.getUsersByHousehold(householdId)
        assertFalse(savedMembers[0].isPinProtected)
        assertTrue(savedMembers[1].isPinProtected)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `authentication error prevents household loading`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"

        val loginCall = mockk<Call<LoginResponse>>()
        every { loginCall.execute() } returns Response.error(401, mockk())
        every { authService.login(any()) } returns loginCall

        // Act
        val result = authManager.login(email, password)

        // Assert
        assertTrue(result is AuthResult.Error)
        assertFalse(authManager.isAuthenticated())
    }

    @Test
    fun `household loading error doesn't affect token storage`() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val householdId = "household_123"

        val loginResponse = LoginResponse(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_456",
            expiresIn = 3600,
            householdId = householdId,
            userId = "user_123"
        )

        val loginCall = mockk<Call<LoginResponse>>()
        every { loginCall.execute() } returns Response.success(loginResponse)
        every { authService.login(any()) } returns loginCall

        val householdCall = mockk<Call<HouseholdMembersResponse>>()
        every { householdCall.execute() } returns Response.error(500, mockk())
        every { householdService.getHouseholdMembers(householdId) } returns householdCall

        // Act
        authManager.login(email, password)
        val householdResult = authManager.loadHouseholdData(householdId)

        // Assert
        assertTrue(householdResult is AuthResult.Error)
        assertTrue(authManager.isAuthenticated())
        assertNotNull(authManager.getAccessToken())
    }
}
