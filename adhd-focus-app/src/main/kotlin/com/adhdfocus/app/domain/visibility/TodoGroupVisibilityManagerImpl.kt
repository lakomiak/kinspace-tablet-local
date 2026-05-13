package com.adhdfocus.app.domain.visibility

import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * TodoGroupVisibilityManagerImpl implements Todo_Group visibility management with persistence.
 *
 * Features:
 * - Integration with UserPreferencesManager
 * - In-memory visibility state via MutableStateFlow
 * - Persistence to Room database via UserPreferencesManager
 * - Per-member visibility isolation
 * - Singleton scope for app-wide access
 * - Default visibility: all groups visible
 */
class TodoGroupVisibilityManagerImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : TodoGroupVisibilityManager {
    private val visibilityCache = mutableMapOf<String, MutableStateFlow<List<String>>>()

    companion object {
        private val DEFAULT_TODO_GROUPS = listOf(
            "Morning",
            "Afternoon",
            "Evening",
            "Bedtime",
            "Other"
        )
    }

    override suspend fun getVisibleTodoGroups(userId: String): List<String> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        val prefs = userPreferencesManager.getPreferences(userId) ?: return emptyList()
        return userPreferencesManager.deserializeVisibleTodoGroups(prefs.visibleTodoGroups)
    }

    override suspend fun getVisibleTodoGroupsOrDefault(userId: String): List<String> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        val visible = getVisibleTodoGroups(userId)
        return if (visible.isEmpty()) DEFAULT_TODO_GROUPS else visible
    }

    override suspend fun setVisibleTodoGroups(userId: String, groups: List<String>): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(groups.isNotEmpty()) { "At least one todo group must be visible" }
        validateTodoGroups(groups)

        return try {
            userPreferencesManager.updateVisibleTodoGroups(userId, groups)
            updateCache(userId, groups)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun toggleTodoGroupVisibility(userId: String, todoGroup: String): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(todoGroup.isNotBlank()) { "todoGroup cannot be blank" }
        validateTodoGroups(listOf(todoGroup))

        return try {
            val current = getVisibleTodoGroupsOrDefault(userId).toMutableList()
            if (current.contains(todoGroup)) {
                // Only remove if it won't result in empty list
                if (current.size > 1) {
                    current.remove(todoGroup)
                } else {
                    return false // Cannot hide all groups
                }
            } else {
                current.add(todoGroup)
            }
            setVisibleTodoGroups(userId, current)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resetToDefaults(userId: String): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return setVisibleTodoGroups(userId, DEFAULT_TODO_GROUPS)
    }

    override fun observeVisibleTodoGroups(userId: String): StateFlow<List<String>> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return visibilityCache.getOrPut(userId) {
            MutableStateFlow(DEFAULT_TODO_GROUPS)
        }
    }

    override fun getAllTodoGroups(): List<String> {
        return DEFAULT_TODO_GROUPS
    }

    override suspend fun getAllTodoGroups(userId: String): List<String> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        val customGroups = userPreferencesManager.getCustomTodoGroups(userId)
        return (DEFAULT_TODO_GROUPS + customGroups)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private suspend fun updateCache(userId: String, groups: List<String>) {
        val flow = visibilityCache.getOrPut(userId) {
            MutableStateFlow(DEFAULT_TODO_GROUPS)
        }
        flow.value = groups
    }

    private fun validateTodoGroups(groups: List<String>) {
        groups.forEach { group ->
            require(DEFAULT_TODO_GROUPS.contains(group)) {
                "Invalid todo group: $group. Valid groups are: ${DEFAULT_TODO_GROUPS.joinToString(", ")}"
            }
        }
    }
}
