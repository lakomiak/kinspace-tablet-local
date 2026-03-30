package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for authentication endpoints
 */
interface AuthService {
    @POST(ApiConfig.Auth.LOGIN)
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST(ApiConfig.Auth.REFRESH)
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST(ApiConfig.Auth.LOGOUT)
    fun logout(@Body request: LogoutRequest): Call<Unit>
}
