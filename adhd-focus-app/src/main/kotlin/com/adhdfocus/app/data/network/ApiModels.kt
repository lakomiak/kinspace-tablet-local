package com.adhdfocus.app.data.network

import com.google.gson.annotations.SerializedName

/**
 * API request and response models for calendar-cloud integration
 */

// Authentication Models
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("household_id")
    val householdId: String,
    @SerializedName("user_id")
    val userId: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)

data class LogoutRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

// Task Models
data class TaskResponse(
    val id: String,
    @SerializedName("household_id")
    val householdId: String,
    @SerializedName("assigned_user_id")
    val assignedUserId: String,
    val title: String,
    val description: String?,
    @SerializedName("todo_group")
    val todoGroup: String,
    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int?,
    @SerializedName("actual_duration_minutes")
    val actualDurationMinutes: Int?,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("sync_status")
    val syncStatus: String,
    @SerializedName("is_deleted")
    val isDeleted: Boolean
)

data class TasksResponse(
    val tasks: List<TaskResponse>
)

data class CreateTaskRequest(
    val title: String,
    val description: String?,
    @SerializedName("todo_group")
    val todoGroup: String,
    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int?,
    @SerializedName("assigned_user_id")
    val assignedUserId: String
)

data class UpdateTaskRequest(
    val title: String?,
    val description: String?,
    val status: String?,
    @SerializedName("actual_duration_minutes")
    val actualDurationMinutes: Int?,
    @SerializedName("completed_at")
    val completedAt: String?
)

// Sync Models
data class SyncQueueItem(
    @SerializedName("task_id")
    val taskId: String,
    val operation: String,
    val payload: String,
    val timestamp: Long
)

data class BatchSyncRequest(
    val changes: List<SyncQueueItem>
)

data class SyncResponse(
    @SerializedName("synced_count")
    val syncedCount: Int,
    @SerializedName("failed_count")
    val failedCount: Int,
    val conflicts: List<SyncConflict>?
)

data class SyncConflict(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("local_version")
    val localVersion: TaskResponse,
    @SerializedName("remote_version")
    val remoteVersion: TaskResponse
)

// Error Response
data class ErrorResponse(
    val error: String,
    val message: String,
    val code: Int
)


// Household Models
data class HouseholdResponse(
    val id: String,
    val name: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class HouseholdMembersResponse(
    val members: List<HouseholdMemberResponse>
)

data class HouseholdMemberResponse(
    val id: String,
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    val role: String,
    @SerializedName("is_pin_protected")
    val isPinProtected: Boolean
)

data class HouseholdSettingsResponse(
    @SerializedName("household_id")
    val householdId: String,
    val settings: Map<String, String>
)
