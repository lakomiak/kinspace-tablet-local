package com.adhdfocus.app.data.network

import android.util.Base64
import android.util.Log
import com.adhdfocus.app.domain.auth.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

/**
 * OkHttp interceptor that adds authentication tokens to requests.
 *
 * The tablet app now prefers the ID token for cloud API calls because the
 * mobile app can successfully read todos with that token shape, and the cloud
 * route is currently rejecting the access token on this device.
 */
class AuthInterceptor(private val authManager: AuthManager) : Interceptor {
    private val tag = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (isAuthEndpoint(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { buildPreferredToken() }
        if (token.isBlank()) {
            Log.w(tag, "no auth token available for path=${originalRequest.url.encodedPath}")
            return chain.proceed(originalRequest)
        }

        Log.d(
            tag,
            "attaching bearer token length=${token.length} path=${originalRequest.url.encodedPath} claims=${describeJwt(token)}"
        )

        val authenticatedRequest = originalRequest.newBuilder()
            .header(
                ApiConfig.Token.HEADER_NAME,
                "${ApiConfig.Token.BEARER_PREFIX}$token"
            )
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private suspend fun buildPreferredToken(): String {
        val idToken = authManager.getIdToken()?.trim().orEmpty()
        if (idToken.isNotBlank()) {
            Log.d(tag, "buildPreferredToken using id token length=${idToken.length}")
            return idToken
        }

        val accessToken = authManager.getValidAccessToken()?.trim().orEmpty()
        if (accessToken.isNotBlank()) {
            Log.d(tag, "buildPreferredToken using access token length=${accessToken.length}")
            return accessToken
        }

        return ""
    }

    private fun describeJwt(token: String): String = runCatching {
        val parts = token.split(".")
        if (parts.size < 2) return "invalid-jwt"
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        )
        val json = JSONObject(payload)
        buildString {
            append("iss=").append(json.optString("iss"))
            append(" aud=").append(json.optString("aud"))
            append(" token_use=").append(json.optString("token_use"))
            append(" scope=").append(json.optString("scope"))
            append(" username=").append(json.optString("cognito:username"))
            append(" exp=").append(json.optLong("exp"))
        }
    }.getOrElse { "decode-error:${it.message}" }

    private fun isAuthEndpoint(path: String): Boolean {
        return path.contains("auth/login") ||
            path.contains("auth/refresh") ||
            path.contains("auth/logout")
    }
}
