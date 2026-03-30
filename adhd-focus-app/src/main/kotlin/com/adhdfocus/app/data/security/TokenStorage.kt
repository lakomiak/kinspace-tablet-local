package com.adhdfocus.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.adhdfocus.app.data.network.ApiConfig

/**
 * Secure token storage using Android Security Crypto
 * Encrypts tokens at rest using the device's secure storage mechanism
 */
class TokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Store access token securely
     */
    fun saveAccessToken(token: String) {
        encryptedPreferences.edit().putString(ApiConfig.Token.ACCESS_TOKEN_KEY, token).apply()
    }

    /**
     * Retrieve access token
     */
    fun getAccessToken(): String? {
        return encryptedPreferences.getString(ApiConfig.Token.ACCESS_TOKEN_KEY, null)
    }

    /**
     * Store refresh token securely
     */
    fun saveRefreshToken(token: String) {
        encryptedPreferences.edit().putString(ApiConfig.Token.REFRESH_TOKEN_KEY, token).apply()
    }

    /**
     * Retrieve refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedPreferences.getString(ApiConfig.Token.REFRESH_TOKEN_KEY, null)
    }

    /**
     * Store both tokens
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        encryptedPreferences.edit().apply {
            putString(ApiConfig.Token.ACCESS_TOKEN_KEY, accessToken)
            putString(ApiConfig.Token.REFRESH_TOKEN_KEY, refreshToken)
            apply()
        }
    }

    /**
     * Clear all stored tokens
     */
    fun clearTokens() {
        encryptedPreferences.edit().apply {
            remove(ApiConfig.Token.ACCESS_TOKEN_KEY)
            remove(ApiConfig.Token.REFRESH_TOKEN_KEY)
            apply()
        }
    }

    /**
     * Check if tokens are available
     */
    fun hasTokens(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }
}
