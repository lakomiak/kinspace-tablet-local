package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service for household endpoints
 */
interface HouseholdService {
    @GET("api/households/{householdId}")
    fun getHousehold(@Path("householdId") householdId: String): Call<HouseholdResponse>

    @GET("api/households/{householdId}/members")
    fun getHouseholdMembers(@Path("householdId") householdId: String): Call<HouseholdMembersResponse>

    @GET("api/households/{householdId}/settings")
    fun getHouseholdSettings(@Path("householdId") householdId: String): Call<HouseholdSettingsResponse>
}
