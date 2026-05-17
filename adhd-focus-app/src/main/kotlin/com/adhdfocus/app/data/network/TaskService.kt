package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for task management endpoints
 */
interface TaskService {
    @GET(ApiConfig.Tasks.GET_TASKS)
    fun getTasks(
        @Path("householdId") householdId: String,
        @Query("date") date: String? = null,
        @Query("familyMemberId") familyMemberId: String? = null
    ): Call<TodosResponse>

    @POST(ApiConfig.Tasks.CREATE_TASK)
    fun createTask(
        @Path("householdId") householdId: String,
        @Body request: CreateTaskRequest
    ): Call<TodoEnvelope>

    @PATCH(ApiConfig.Tasks.UPDATE_TASK)
    fun updateTask(
        @Path("householdId") householdId: String,
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): Call<TodoEnvelope>

    @PATCH(ApiConfig.Tasks.UPDATE_TASK)
    fun syncDayCompletion(
        @Path("householdId") householdId: String,
        @Path("taskId") taskId: String,
        @Body request: DayCompletionSyncRequest
    ): Call<DayCompletionEnvelope>

    @DELETE(ApiConfig.Tasks.DELETE_TASK)
    fun deleteTask(
        @Path("householdId") householdId: String,
        @Path("taskId") taskId: String
    ): Call<Unit>
}
