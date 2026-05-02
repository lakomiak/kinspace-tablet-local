package com.adhdfocus.app.data.network

import com.adhdfocus.app.data.security.TokenStorage
import com.adhdfocus.app.domain.auth.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock

/**
 * OkHttp interceptor that handles token refresh on 401 responses
 * by delegating to the real AppAuth refresh flow.
 */
class TokenRefreshInterceptor(
    private val tokenStorage: TokenStorage,
    private val authManager: AuthManager
) : Interceptor {
    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // If response is 401 Unauthorized, try to refresh token and retry
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.encodedPath)) {
            lock.lock()
            try {
                if (refreshToken()) {
                    response.close()
                    return retryRequestWithNewToken(chain, originalRequest)
                }
            } finally {
                lock.unlock()
            }
        }

        return response
    }

    private fun refreshToken(): Boolean {
        return try {
            val result = runBlocking { authManager.refreshTokens() }
            result is com.adhdfocus.app.domain.auth.AuthResult.Success &&
                !tokenStorage.getAccessToken().isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun retryRequestWithNewToken(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request
    ): Response {
        val newToken = tokenStorage.getIdToken()
            ?: tokenStorage.getAccessToken()
        return if (newToken != null) {
            val newRequest = originalRequest.newBuilder()
                .header(
                    ApiConfig.Token.HEADER_NAME,
                    "${ApiConfig.Token.BEARER_PREFIX}$newToken"
                )
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    private fun isAuthEndpoint(path: String): Boolean {
        return path.contains("auth/login") ||
                path.contains("auth/refresh") ||
                path.contains("auth/logout")
    }
}
