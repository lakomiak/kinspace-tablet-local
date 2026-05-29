package com.adhdfocus.app.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.domain.auth.AuthManager
import com.adhdfocus.app.domain.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

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

    // Holds the Intent to launch the Cognito hosted UI
    private val _signInIntent = MutableStateFlow<Intent?>(null)
    val signInIntent: StateFlow<Intent?> = _signInIntent.asStateFlow()

    init {
        validateExistingSession()
    }

    private fun validateExistingSession() {
        if (!authManager.isAuthenticated()) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val hasValidSession = withContext(Dispatchers.IO) {
                val idToken = authManager.getIdToken()?.takeIf { it.isNotBlank() }
                val accessToken = authManager.getValidAccessToken()?.takeIf { it.isNotBlank() }
                !idToken.isNullOrBlank() || !accessToken.isNullOrBlank()
            }
            _authState.value = if (hasValidSession) {
                AuthState.Authenticated
            } else {
                authManager.logout()
                AuthState.Unauthenticated
            }
            _isLoading.value = false
        }
    }

    /** Fetch the AppAuth Intent for the Cognito hosted UI. */
    fun startSignIn() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val intent = authManager.buildSignInIntent()
                _signInIntent.value = intent
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start sign-in: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /** Called after the Cognito redirect returns to the app. */
    fun handleAuthResult(data: Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val response = data?.let { AuthorizationResponse.fromIntent(it) }
            val exception = data?.let { AuthorizationException.fromIntent(it) }
            when (val result = authManager.handleAuthorizationResponse(response, exception)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.Authenticated
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
            _isLoading.value = false
            _signInIntent.value = null
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSignInIntent() { _signInIntent.value = null }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
}
