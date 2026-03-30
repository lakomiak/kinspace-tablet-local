package com.adhdfocus.app.domain.auth

import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.HouseholdService
import com.adhdfocus.app.data.network.LoginRequest
import com.adhdfocus.app.data.network.LogoutRequest
import com.adhdfocus.app.data.network.RefreshTokenRequest
import com.adhdfocus.app.data.network.RefreshTokenResponse
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.data.security.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call

/**
 * Authentication manager handling login, logout, and token refresh operations
 * Manages authentication state and secure token storage
 */
class AuthManager(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage,
    private val householdService: HouseholdService? = null,
    private val userRepository: UserRepository? = null
) {
    /**
     * Sign in with email and password
     * Stores tokens securely on successful authentication
     */
    suspend fun login(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = authService.login(request).execute()

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        tokenStorage.saveTokens(
                            loginResponse.accessToken,
                            loginResponse.refreshToken
                        )
                        AuthResult.Success(
                            householdId = loginResponse.householdId,
                            userId = loginResponse.userId
                        )
                    } else {
                        AuthResult.Error("Empty response body")
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Login failed"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Unknown error during login")
            }
        }
    }

    /**
     * Sign out and clear stored tokens
     */
    suspend fun logout(): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenStorage.getRefreshToken()
                if (refreshToken != null) {
                    val request = LogoutRequest(refreshToken)
                    authService.logout(request).execute()
                }
                tokenStorage.clearTokens()
                AuthResult.Success()
            } catch (e: Exception) {
                // Clear tokens even if logout request fails
                tokenStorage.clearTokens()
                AuthResult.Error(e.message ?: "Error during logout")
            }
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenStorage.hasTokens()
    }

    /**
     * Get current access token
     */
    fun getAccessToken(): String? {
        return tokenStorage.getAccessToken()
    }

    /**
     * Get current refresh token
     */
    fun getRefreshToken(): String? {
        return tokenStorage.getRefreshToken()
    }

    /**
     * Refresh the access token using the refresh token
     */
    suspend fun refreshAccessToken(): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenStorage.getRefreshToken()
                    ?: return@withContext AuthResult.Error("No refresh token available")

                val request = RefreshTokenRequest(refreshToken)
                val response = authService.refreshToken(request).execute()

                if (response.isSuccessful) {
                    val refreshResponse = response.body()
                    if (refreshResponse != null) {
                        tokenStorage.saveAccessToken(refreshResponse.accessToken)
                        AuthResult.Success()
                    } else {
                        AuthResult.Error("Empty response body")
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Token refresh failed"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Unknown error during token refresh")
            }
        }
    }

    /**
     * Load household data after successful authentication
     */
    suspend fun loadHouseholdData(householdId: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                if (householdService == null || userRepository == null) {
                    return@withContext AuthResult.Error("Household service not configured")
                }

                // Load household members
                val membersResponse = householdService.getHouseholdMembers(householdId).execute()
                if (!membersResponse.isSuccessful) {
                    val errorMessage = membersResponse.errorBody()?.string() ?: "Failed to load household members"
                    return@withContext AuthResult.Error(errorMessage)
                }

                val members = membersResponse.body()?.members
                    ?: return@withContext AuthResult.Error("Empty members response")

                // Save household members to local database
                for (member in members) {
                    val user = com.adhdfocus.app.data.model.User(
                        id = member.id,
                        householdId = householdId,
                        email = member.email,
                        displayName = member.displayName,
                        avatarUrl = member.avatarUrl,
                        role = com.adhdfocus.app.data.model.UserRole.valueOf(member.role.uppercase()),
                        isPinProtected = member.isPinProtected
                    )
                    userRepository.saveUser(user)
                }

                AuthResult.Success(householdId = householdId)
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Unknown error loading household data")
            }
        }
    }

    /**
     * Validate if the current token is still valid
     */
    fun isTokenValid(): Boolean {
        return tokenStorage.hasTokens()
    }
}

/**
 * Result type for authentication operations
 */
sealed class AuthResult {
    data class Success(
        val householdId: String? = null,
        val userId: String? = null
    ) : AuthResult()

    data class Error(val message: String) : AuthResult()
}
