package com.adhdfocus.app.domain.theme

import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ThemeManagerImpl implements theme management with persistence.
 *
 * Features:
 * - Per-member theme preferences
 * - Theme state management
 * - Dynamic theme switching
 * - Theme persistence via UserPreferencesManager
 */
@Singleton
class ThemeManagerImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ThemeManager {

    private val _currentTheme = MutableStateFlow(Theme.LIGHT)
    override val currentTheme: StateFlow<Theme> = _currentTheme

    override fun getCurrentTheme(): Theme = _currentTheme.value

    override suspend fun setTheme(theme: Theme, userId: String) {
        require(userId.isNotBlank()) { "userId cannot be blank" }

        // Update in-memory state
        _currentTheme.value = theme

        // Persist to database
        userPreferencesManager.updateTheme(userId, theme)
    }

    override suspend fun loadThemeForUser(userId: String) {
        require(userId.isNotBlank()) { "userId cannot be blank" }

        val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
        _currentTheme.value = preferences.theme
    }

    override suspend fun resetThemeToDefault(userId: String) {
        require(userId.isNotBlank()) { "userId cannot be blank" }

        setTheme(Theme.LIGHT, userId)
    }
}
