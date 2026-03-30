package com.adhdfocus.app.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit service for task management endpoints
 */
interface TaskService {
    @GET(ApiConfig.Tasks.GET_TASKS)
    fun getTasks(@Path("householdId") householdId: String): Call<TasksResponse>

    @POST(ApiConfig.Tasks.CREATE_TASK)
    fun createTask(
        @Path("householdId") householdId: String,
        @Body request: CreateTaskRequest
    ): Call<TaskResponse>

    @PUT(ApiConfig.Tasks.UPDATE_TASK)
    fun updateTask(
        @Path("householdId") householdId: String,
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): Call<TaskResponse>

    @DELETE(ApiConfig.Tasks.DELETE_TASK)
    fun deleteTask(
        @Path("householdId") householdId: String,
        @Path("taskId") taskId: String
    ): Call<Unit>
}
