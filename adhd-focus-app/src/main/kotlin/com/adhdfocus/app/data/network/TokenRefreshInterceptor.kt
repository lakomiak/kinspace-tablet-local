package com.adhdfocus.app.data.network

import com.adhdfocus.app.data.security.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * OkHttp interceptor that handles token refresh on 401 responses
 * Implements automatic retry logic with token refresh
 */
class TokenRefreshInterceptor(
    private val tokenStorage: TokenStorage,
    private val retrofitProvider: () -> Retrofit
) : Interceptor {
    private val lock = ReentrantReadWriteLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // If response is 401 Unauthorized, try to refresh token and retry
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.encodedPath)) {
            lock.writeLock().lock()
            try {
                // Double-check pattern: verify token wasn't already refreshed by another thread
                val currentToken = tokenStorage.getAccessToken()
                val originalToken = originalRequest.header(ApiConfig.Token.HEADER_NAME)
                    ?.removePrefix("${ApiConfig.Token.BEARER_PREFIX}")

                if (currentToken != originalToken) {
                    // Token was already refreshed, retry with new token
                    response.close()
                    return retryRequestWithNewToken(chain, originalRequest)
                }

                // Attempt to refresh token
                if (refreshToken()) {
                    response.close()
                    return retryRequestWithNewToken(chain, originalRequest)
                }
            } finally {
                lock.writeLock().unlock()
            }
        }

        return response
    }

    private fun refreshToken(): Boolean {
        return try {
            val refreshToken = tokenStorage.getRefreshToken() ?: return false
            val retrofit = retrofitProvider()
            val authService = retrofit.create(AuthService::class.java)

            val refreshRequest = RefreshTokenRequest(refreshToken)
            val refreshResponse = authService.refreshToken(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                val newTokens = refreshResponse.body()
                if (newTokens != null) {
                    tokenStorage.saveAccessToken(newTokens.accessToken)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun retryRequestWithNewToken(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request
    ): Response {
        val newToken = tokenStorage.getAccessToken()
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
