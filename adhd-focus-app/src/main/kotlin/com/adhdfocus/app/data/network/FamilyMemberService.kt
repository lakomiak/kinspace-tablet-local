package com.adhdfocus.app.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface FamilyMemberService {
    @GET("households/{householdId}/family-members")
    suspend fun listFamilyMembers(
        @Path("householdId") householdId: String
    ): HouseholdMembersResponse
}
