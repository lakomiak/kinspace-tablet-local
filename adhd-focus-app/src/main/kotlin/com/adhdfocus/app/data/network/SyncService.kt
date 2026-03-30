package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for sync operations
 */
interface SyncService {
    @POST(ApiConfig.Sync.BATCH_SYNC)
    fun batchSync(
        @Path("householdId") householdId: String,
        @Body request: BatchSyncRequest
    ): Call<SyncResponse>

    @GET(ApiConfig.Sync.SYNC_STATUS)
    fun getSyncStatus(@Path("householdId") householdId: String): Call<SyncResponse>
}
