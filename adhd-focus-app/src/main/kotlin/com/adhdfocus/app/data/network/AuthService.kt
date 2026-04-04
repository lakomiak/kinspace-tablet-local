package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Legacy auth service - no longer used. Auth is handled by AppAuth + Cognito.
 * Kept to avoid breaking other references; all methods are no-ops.
 */
interface AuthService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST("auth/logout")
    fun logout(@Body request: LogoutRequest): Call<Unit>
}
