package com.adhdfocus.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.adhdfocus.app.data.network.ApiConfig
import java.time.Instant

/**
 * Secure token storage using Android Security Crypto.
 * Key names match calendar-mobile for consistency across the Kinspace ecosystem.
 */
class TokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, idToken: String, refreshToken: String, expiry: Instant) {
        prefs.edit().apply {
            putString(ApiConfig.Token.ACCESS_TOKEN_KEY, accessToken)
            putString(ApiConfig.Token.ID_TOKEN_KEY, idToken)
            putString(ApiConfig.Token.REFRESH_TOKEN_KEY, refreshToken)
            putString(ApiConfig.Token.EXPIRY_KEY, expiry.toString())
            apply()
        }
    }

    fun saveAccessToken(token: String) =
        prefs.edit().putString(ApiConfig.Token.ACCESS_TOKEN_KEY, token).apply()

    fun getAccessToken(): String? = prefs.getString(ApiConfig.Token.ACCESS_TOKEN_KEY, null)

    fun getIdToken(): String? = prefs.getString(ApiConfig.Token.ID_TOKEN_KEY, null)

    fun saveRefreshToken(token: String) =
        prefs.edit().putString(ApiConfig.Token.REFRESH_TOKEN_KEY, token).apply()

    fun getRefreshToken(): String? = prefs.getString(ApiConfig.Token.REFRESH_TOKEN_KEY, null)

    fun getExpiry(): Instant? =
        prefs.getString(ApiConfig.Token.EXPIRY_KEY, null)?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        }

    fun isExpiringSoon(): Boolean {
        val expiry = getExpiry() ?: return true
        return expiry.isBefore(Instant.now().plusSeconds(300)) // 5 min buffer
    }

    fun clearTokens() {
        prefs.edit().apply {
            remove(ApiConfig.Token.ACCESS_TOKEN_KEY)
            remove(ApiConfig.Token.ID_TOKEN_KEY)
            remove(ApiConfig.Token.REFRESH_TOKEN_KEY)
            remove(ApiConfig.Token.EXPIRY_KEY)
            apply()
        }
    }

    fun hasTokens(): Boolean = getAccessToken() != null && getRefreshToken() != null
}
