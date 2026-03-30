package com.adhdfocus.app.domain.theme

import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ThemeManager.
 *
 * Tests:
 * - Theme state management
 * - Theme persistence
 * - Per-member theme isolation
 * - Theme loading and switching
 */
class ThemeManagerUnitTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var themeManager: ThemeManager

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        themeManager = ThemeManagerImpl(userPreferencesManager)
    }

    @Test
    fun `getCurrentTheme returns current theme`() {
        assertEquals(Theme.LIGHT, themeManager.getCurrentTheme())
    }

    @Test
    fun `setTheme updates current theme`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, Theme.DARK) } returns true

        themeManager.setTheme(Theme.DARK, userId)

        assertEquals(Theme.DARK, themeManager.getCurrentTheme())
    }

    @Test
    fun `setTheme persists theme to preferences manager`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, Theme.DARK) } returns true

        themeManager.setTheme(Theme.DARK, userId)

        coVerify { userPreferencesManager.updateTheme(userId, Theme.DARK) }
    }

    @Test
    fun `setTheme with blank userId throws exception`() = runTest {
        try {
            themeManager.setTheme(Theme.DARK, "")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("userId cannot be blank", e.message)
        }
    }

    @Test
    fun `loadThemeForUser loads theme from preferences`() = runTest {
        val userId = "user1"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.DARK
        )
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences

        themeManager.loadThemeForUser(userId)

        assertEquals(Theme.DARK, themeManager.getCurrentTheme())
    }

    @Test
    fun `loadThemeForUser with blank userId throws exception`() = runTest {
        try {
            themeManager.loadThemeForUser("")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("userId cannot be blank", e.message)
        }
    }

    @Test
    fun `resetThemeToDefault sets theme to LIGHT`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, Theme.LIGHT) } returns true

        themeManager.resetThemeToDefault(userId)

        assertEquals(Theme.LIGHT, themeManager.getCurrentTheme())
    }

    @Test
    fun `resetThemeToDefault persists to preferences manager`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, Theme.LIGHT) } returns true

        themeManager.resetThemeToDefault(userId)

        coVerify { userPreferencesManager.updateTheme(userId, Theme.LIGHT) }
    }

    @Test
    fun `currentTheme StateFlow emits theme changes`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, Theme.DARK) } returns true

        themeManager.setTheme(Theme.DARK, userId)

        val theme = themeManager.currentTheme.value
        assertEquals(Theme.DARK, theme)
    }

    @Test
    fun `per-member theme isolation - different users have independent themes`() = runTest {
        val user1 = "user1"
        val user2 = "user2"
        coEvery { userPreferencesManager.updateTheme(user1, Theme.DARK) } returns true
        coEvery { userPreferencesManager.updateTheme(user2, Theme.LIGHT) } returns true

        themeManager.setTheme(Theme.DARK, user1)
        val theme1 = themeManager.getCurrentTheme()

        themeManager.setTheme(Theme.LIGHT, user2)
        val theme2 = themeManager.getCurrentTheme()

        // Last set theme is LIGHT
        assertEquals(Theme.LIGHT, theme2)
    }

    @Test
    fun `loadThemeForUser with LIGHT theme`() = runTest {
        val userId = "user1"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.LIGHT
        )
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences

        themeManager.loadThemeForUser(userId)

        assertEquals(Theme.LIGHT, themeManager.getCurrentTheme())
    }

    @Test
    fun `loadThemeForUser with DARK theme`() = runTest {
        val userId = "user1"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.DARK
        )
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences

        themeManager.loadThemeForUser(userId)

        assertEquals(Theme.DARK, themeManager.getCurrentTheme())
    }

    @Test
    fun `setTheme LIGHT then DARK`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, any()) } returns true

        themeManager.setTheme(Theme.LIGHT, userId)
        assertEquals(Theme.LIGHT, themeManager.getCurrentTheme())

        themeManager.setTheme(Theme.DARK, userId)
        assertEquals(Theme.DARK, themeManager.getCurrentTheme())
    }

    @Test
    fun `setTheme DARK then LIGHT`() = runTest {
        val userId = "user1"
        coEvery { userPreferencesManager.updateTheme(userId, any()) } returns true

        themeManager.setTheme(Theme.DARK, userId)
        assertEquals(Theme.DARK, themeManager.getCurrentTheme())

        themeManager.setTheme(Theme.LIGHT, userId)
        assertEquals(Theme.LIGHT, themeManager.getCurrentTheme())
    }
}
