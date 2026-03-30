package com.adhdfocus.app.data.network

import com.adhdfocus.app.data.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds authentication tokens to requests
 * Automatically includes the access token in the Authorization header
 */
class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding token to auth endpoints
        if (isAuthEndpoint(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenStorage.getAccessToken()
        if (accessToken != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header(
                    ApiConfig.Token.HEADER_NAME,
                    "${ApiConfig.Token.BEARER_PREFIX}$accessToken"
                )
                .build()
            return chain.proceed(authenticatedRequest)
        }

        return chain.proceed(originalRequest)
    }

    private fun isAuthEndpoint(path: String): Boolean {
        return path.contains("auth/login") ||
                path.contains("auth/refresh") ||
                path.contains("auth/logout")
    }
}
