package com.adhdfocus.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.domain.auth.AuthManager
import com.adhdfocus.app.domain.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication state management
 * Handles login, logout, and authentication state
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Check if user is already authenticated
        if (authManager.isAuthenticated()) {
            _authState.value = AuthState.Authenticated
        }
    }

    /**
     * Attempt to sign in with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authManager.login(email, password)
            when (result) {
                is AuthResult.Success -> {
                    // Load household data after successful login
                    if (result.householdId != null) {
                        val householdResult = authManager.loadHouseholdData(result.householdId)
                        when (householdResult) {
                            is AuthResult.Success -> {
                                _authState.value = AuthState.Authenticated
                                _isLoading.value = false
                            }
                            is AuthResult.Error -> {
                                _errorMessage.value = householdResult.message
                                _isLoading.value = false
                            }
                        }
                    } else {
                        _authState.value = AuthState.Authenticated
                        _isLoading.value = false
                    }
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Sign out and clear authentication
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authManager.logout()
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.Unauthenticated
                    _isLoading.value = false
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh the authentication token
     */
    fun refreshToken() {
        viewModelScope.launch {
            val result = authManager.refreshAccessToken()
            when (result) {
                is AuthResult.Success -> {
                    // Token refreshed successfully
                    _errorMessage.value = null
                }
                is AuthResult.Error -> {
                    // Token refresh failed, user needs to re-authenticate
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = result.message
                }
            }
        }
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
}
