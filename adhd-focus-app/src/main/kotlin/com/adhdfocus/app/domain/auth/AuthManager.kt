package com.adhdfocus.app.domain.auth

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.adhdfocus.app.data.network.ApiConfig
import com.adhdfocus.app.data.security.TokenStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manages Cognito OIDC authentication using AppAuth.
 * Mirrors the auth mechanism used in calendar-mobile (flutter_appauth + Cognito).
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage
) {
    private val tag = "AuthManager"
    private var serviceConfig: AuthorizationServiceConfiguration? = null

    /** Build the authorization Intent to launch the Cognito hosted UI. */
    suspend fun buildSignInIntent(): Intent = withContext(Dispatchers.IO) {
        val config = getServiceConfig()
        val authService = AuthorizationService(context)
        val request = AuthorizationRequest.Builder(
            config,
            ApiConfig.Cognito.CLIENT_ID,
            ResponseTypeValues.CODE,
            android.net.Uri.parse(ApiConfig.Cognito.REDIRECT_URI)
        )
            .setScopes(ApiConfig.Cognito.SCOPES)
            .setPrompt("login")
            .build()
        authService.getAuthorizationRequestIntent(request)
    }

    /** Exchange the authorization code for tokens and persist them. */
    suspend fun handleAuthorizationResponse(
        response: AuthorizationResponse?,
        exception: AuthorizationException?
    ): AuthResult = withContext(Dispatchers.IO) {
        if (exception != null || response == null) {
            log("handleAuthorizationResponse:error ${exception?.message}")
            return@withContext AuthResult.Error(exception?.message ?: "Authorization failed")
        }

        try {
            val appAuthService = AuthorizationService(context)
            var tokenResp: net.openid.appauth.TokenResponse? = null
            suspendCoroutine<Unit> { cont ->
                appAuthService.performTokenRequest(response.createTokenExchangeRequest()) { resp, ex ->
                    if (ex != null) cont.resumeWithException(ex)
                    else { tokenResp = resp; cont.resume(Unit) }
                }
            }
            val tr = tokenResp ?: return@withContext AuthResult.Error("Token exchange returned null")

            val accessToken = tr.accessToken ?: return@withContext AuthResult.Error("Missing access token")
            val idToken = tr.idToken ?: return@withContext AuthResult.Error("Missing ID token")
            val refreshToken = tr.refreshToken ?: return@withContext AuthResult.Error("Missing refresh token")
            val expiry = tr.accessTokenExpirationTime?.let { Instant.ofEpochMilli(it) }
                ?: Instant.now().plusSeconds(3600)
            tokenStorage.saveTokens(accessToken, idToken, refreshToken, expiry)
            log("handleAuthorizationResponse:success")

            val userId = extractSubFromIdToken(idToken)
            AuthResult.Success(userId = userId)
        } catch (e: Exception) {
            log("handleAuthorizationResponse:exception ${e.message}")
            AuthResult.Error(e.message ?: "Token exchange failed")
        }
    }

    /** Get a valid access token, refreshing if needed. */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        if (!tokenStorage.isExpiringSoon()) {
            return@withContext tokenStorage.getAccessToken()
        }
        log("getValidAccessToken:refreshing")
        val refreshed = refreshTokens()
        if (refreshed is AuthResult.Success) tokenStorage.getAccessToken() else null
    }

    /** Refresh tokens using the stored refresh token. */
    suspend fun refreshTokens(): AuthResult = withContext(Dispatchers.IO) {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return@withContext AuthResult.Error("No refresh token")
        try {
            val config = getServiceConfig()
            val refreshAuthService = AuthorizationService(context)
            val tokenRequest = TokenRequest.Builder(config, ApiConfig.Cognito.CLIENT_ID)
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .setScopes(ApiConfig.Cognito.SCOPES)
                .build()

            val appAuthService2 = AuthorizationService(context)
            var tokenResp2: net.openid.appauth.TokenResponse? = null
            suspendCoroutine<Unit> { cont ->
                refreshAuthService.performTokenRequest(tokenRequest) { resp, ex ->
                    if (ex != null) cont.resumeWithException(ex)
                    else { tokenResp2 = resp; cont.resume(Unit) }
                }
            }
            val tr2 = tokenResp2 ?: return@withContext AuthResult.Error("Refresh returned null")

            val newAccess = tr2.accessToken ?: return@withContext AuthResult.Error("Missing access token")
            val newId = tr2.idToken ?: tokenStorage.getIdToken() ?: ""
            val newRefresh = tr2.refreshToken ?: refreshToken
            val expiry = tr2.accessTokenExpirationTime?.let { Instant.ofEpochMilli(it) }
                ?: Instant.now().plusSeconds(3600)

            tokenStorage.saveTokens(newAccess, newId, newRefresh, expiry)
            log("refreshTokens:success")
            AuthResult.Success()
        } catch (e: Exception) {
            log("refreshTokens:error ${e.message}")
            tokenStorage.clearTokens()
            AuthResult.Error(e.message ?: "Token refresh failed")
        }
    }

    fun isAuthenticated(): Boolean = tokenStorage.hasTokens()

    fun getAccessToken(): String? = tokenStorage.getAccessToken()

    fun getIdToken(): String? = tokenStorage.getIdToken()

    fun getRefreshToken(): String? = tokenStorage.getRefreshToken()

    suspend fun logout() {
        tokenStorage.clearTokens()
    }

    private suspend fun getServiceConfig(): AuthorizationServiceConfiguration {
        serviceConfig?.let { return it }
        return suspendCoroutine { cont ->
            AuthorizationServiceConfiguration.fetchFromIssuer(
                android.net.Uri.parse(ApiConfig.Cognito.ISSUER)
            ) { config, ex ->
                if (ex != null) cont.resumeWithException(ex)
                else cont.resume(config!!)
            }
        }.also { serviceConfig = it }
    }

    private fun extractSubFromIdToken(idToken: String): String? = runCatching {
        val parts = idToken.split(".")
        if (parts.size != 3) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
        val json = JSONObject(payload)
        json.optString("sub").takeIf { it.isNotEmpty() }
            ?: json.optString("cognito:username").takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun log(msg: String) = Log.d(tag, "${Instant.now()} $msg")
}

sealed class AuthResult {
    data class Success(val householdId: String? = null, val userId: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
