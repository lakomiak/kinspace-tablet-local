package com.adhdfocus.app.data.security

import android.content.Context
import android.os.Build
import com.adhdfocus.app.data.network.ApiConfig
import java.time.Instant

/**
 * Token storage that prefers encrypted preferences on supported devices
 * and gracefully falls back to plain SharedPreferences on older Android versions.
 */
class TokenStorage(context: Context) {
    private val prefs = createPrefs(context)

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

    private fun createPrefs(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val masterKeyClass = Class.forName("androidx.security.crypto.MasterKey")
                val keySchemeClass = Class.forName("androidx.security.crypto.MasterKey\$KeyScheme")
                val builderClass = Class.forName("androidx.security.crypto.MasterKey\$Builder")
                val keySchemeValue = keySchemeClass.getField("AES256_GCM").get(null)
                val builder = builderClass
                    .getConstructor(Context::class.java)
                    .newInstance(context)
                val configuredBuilder = builderClass
                    .getMethod("setKeyScheme", keySchemeClass)
                    .invoke(builder, keySchemeValue)
                val masterKey = builderClass
                    .getMethod("build")
                    .invoke(configuredBuilder)

                val prefsClass = Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
                val keySchemeEnum = Class.forName("androidx.security.crypto.EncryptedSharedPreferences\$PrefKeyEncryptionScheme")
                val valueSchemeEnum = Class.forName("androidx.security.crypto.EncryptedSharedPreferences\$PrefValueEncryptionScheme")
                val keyScheme = keySchemeEnum.getField("AES256_SIV").get(null)
                val valueScheme = valueSchemeEnum.getField("AES256_GCM").get(null)

                prefsClass.getMethod(
                    "create",
                    Context::class.java,
                    String::class.java,
                    masterKeyClass,
                    keySchemeEnum,
                    valueSchemeEnum
                ).invoke(
                    null,
                    context,
                    "auth_tokens",
                    masterKey,
                    keyScheme,
                    valueScheme
                ) as android.content.SharedPreferences
            }.getOrElse {
                context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)
            }
        } else {
            context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)
        }
}
