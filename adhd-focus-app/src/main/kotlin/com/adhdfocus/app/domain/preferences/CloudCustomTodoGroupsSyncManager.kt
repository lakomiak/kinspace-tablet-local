package com.adhdfocus.app.domain.preferences

import com.adhdfocus.app.data.network.HouseholdNotificationSettingsRequest
import com.adhdfocus.app.data.network.HouseholdNotificationSettingsService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CloudCustomTodoGroupsSnapshot(
    val groups: List<String>,
    val fromCloud: Boolean
)

@Singleton
class CloudCustomTodoGroupsSyncManager @Inject constructor(
    private val householdNotificationSettingsService: HouseholdNotificationSettingsService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCustomTodoGroups(householdId: String): CloudCustomTodoGroupsSnapshot {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        return try {
            val response = householdNotificationSettingsService
                .getHouseholdNotificationSettings(householdId)
                .execute()
            if (!response.isSuccessful) {
                return CloudCustomTodoGroupsSnapshot(emptyList(), false)
            }

            val body = response.body()
            val settings = body?.settings.orEmpty()
            val raw = settings["customTodoGroups"].orEmpty()
            val groups = deserializeGroups(raw)
            CloudCustomTodoGroupsSnapshot(
                groups = groups,
                fromCloud = body?.source?.equals("cloud", ignoreCase = true) == true
            )
        } catch (_: Exception) {
            CloudCustomTodoGroupsSnapshot(emptyList(), false)
        }
    }

    suspend fun saveCustomTodoGroups(householdId: String, groups: List<String>): Boolean {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        return try {
            val cleaned = groups.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val response = householdNotificationSettingsService
                .updateHouseholdNotificationSettings(
                    householdId,
                    HouseholdNotificationSettingsRequest(
                        settings = mapOf("customTodoGroups" to json.encodeToString(cleaned))
                    )
                )
                .execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    private fun deserializeGroups(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
