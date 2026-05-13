package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/**
 * Retrofit service for household notification/settings blob.
 *
 * Used to sync shared tablet preferences such as custom todo categories.
 */
interface HouseholdNotificationSettingsService {
    @GET("households/{householdId}/notifications")
    fun getHouseholdNotificationSettings(
        @Path("householdId") householdId: String
    ): Call<HouseholdNotificationSettingsResponse>

    @PATCH("households/{householdId}/notifications")
    fun updateHouseholdNotificationSettings(
        @Path("householdId") householdId: String,
        @Body request: HouseholdNotificationSettingsRequest
    ): Call<HouseholdNotificationSettingsResponse>
}
