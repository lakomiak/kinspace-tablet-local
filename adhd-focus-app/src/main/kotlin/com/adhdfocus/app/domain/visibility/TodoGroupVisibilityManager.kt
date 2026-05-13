package com.adhdfocus.app.domain.visibility

import kotlinx.coroutines.flow.StateFlow

/**
 * TodoGroupVisibilityManager handles per-member Todo_Group visibility preferences.
 *
 * Manages:
 * - Getting visible Todo_Groups for a user
 * - Setting visible Todo_Groups for a user
 * - Toggling visibility of a specific Todo_Group
 * - Resetting to defaults (all visible)
 * - Observing visibility changes via StateFlow
 */
interface TodoGroupVisibilityManager {
    /**
     * Gets visible Todo_Groups for a user.
     *
     * @param userId User ID
     * @return List of visible Todo_Group names
     */
    suspend fun getVisibleTodoGroups(userId: String): List<String>

    /**
     * Gets visible Todo_Groups for a user, or defaults if not found.
     *
     * @param userId User ID
     * @return List of visible Todo_Group names (or all groups if not found)
     */
    suspend fun getVisibleTodoGroupsOrDefault(userId: String): List<String>

    /**
     * Sets visible Todo_Groups for a user.
     *
     * @param userId User ID
     * @param groups List of visible Todo_Group names
     * @return True if successful
     */
    suspend fun setVisibleTodoGroups(userId: String, groups: List<String>): Boolean

    /**
     * Toggles visibility of a specific Todo_Group for a user.
     *
     * @param userId User ID
     * @param todoGroup Todo_Group name to toggle
     * @return True if successful
     */
    suspend fun toggleTodoGroupVisibility(userId: String, todoGroup: String): Boolean

    /**
     * Resets visibility to defaults (all visible) for a user.
     *
     * @param userId User ID
     * @return True if successful
     */
    suspend fun resetToDefaults(userId: String): Boolean

    /**
     * Observes visibility changes for a user.
     *
     * @param userId User ID
     * @return StateFlow of visible Todo_Group names
     */
    fun observeVisibleTodoGroups(userId: String): StateFlow<List<String>>

    /**
     * Gets all available Todo_Groups.
     *
     * @return List of all Todo_Group names
     */
    fun getAllTodoGroups(): List<String>

    /**
     * Gets all available Todo_Groups for a user, including any custom categories.
     *
     * @param userId User ID
     * @return List of all Todo_Group names
     */
    suspend fun getAllTodoGroups(userId: String): List<String>
}
