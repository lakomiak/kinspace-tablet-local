package com.adhdfocus.app.domain.theme

import com.adhdfocus.app.data.model.Theme
import kotlinx.coroutines.flow.StateFlow

/**
 * ThemeManager manages theme state and application-wide theme switching.
 *
 * Responsibilities:
 * - Get current theme
 * - Set theme and persist preference
 * - Observe theme changes
 * - Apply theme to app
 * - Support system theme detection
 */
interface ThemeManager {
    /**
     * Current theme as a StateFlow.
     */
    val currentTheme: StateFlow<Theme>

    /**
     * Gets the current theme.
     *
     * @return Current theme
     */
    fun getCurrentTheme(): Theme

    /**
     * Sets the theme and persists the preference.
     *
     * @param theme Theme to set
     * @param userId User ID for per-member theme preference
     */
    suspend fun setTheme(theme: Theme, userId: String)

    /**
     * Loads theme preference for a user.
     *
     * @param userId User ID
     */
    suspend fun loadThemeForUser(userId: String)

    /**
     * Resets theme to default (LIGHT).
     *
     * @param userId User ID
     */
    suspend fun resetThemeToDefault(userId: String)
}
