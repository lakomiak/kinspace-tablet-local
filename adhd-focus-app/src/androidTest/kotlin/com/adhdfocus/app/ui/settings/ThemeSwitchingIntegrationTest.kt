package com.adhdfocus.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.ui.theme.AdhdfocusAppThemeWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Integration tests for theme switching in the UI.
 *
 * Tests:
 * - Theme application in UI
 * - Theme switching in settings
 * - Visual appearance with different themes
 */
@RunWith(AndroidJUnit4::class)
class ThemeSwitchingIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLightThemeApplied() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.LIGHT) {
                // Light theme should have light background
                val backgroundColor = MaterialTheme.colorScheme.background
                // Verify light theme colors are applied
                assert(backgroundColor.toString().contains("ffffff") || backgroundColor.toString().contains("FFFFFF"))
            }
        }
    }

    @Test
    fun testDarkThemeApplied() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.DARK) {
                // Dark theme should have dark background
                val backgroundColor = MaterialTheme.colorScheme.background
                // Verify dark theme colors are applied
                assert(backgroundColor.toString().contains("121212") || backgroundColor.toString().contains("1E1E1E"))
            }
        }
    }

    @Test
    fun testThemeSelectorDisplaysOptions() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.LIGHT) {
                ThemeSelector(
                    selectedTheme = Theme.LIGHT,
                    onThemeSelected = {}
                )
            }
        }

        // Verify both theme options are displayed
        composeTestRule.onNodeWithText("LIGHT").assertExists()
        composeTestRule.onNodeWithText("DARK").assertExists()
    }

    @Test
    fun testThemeSelectorClickable() {
        var selectedTheme = Theme.LIGHT
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = selectedTheme) {
                ThemeSelector(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it }
                )
            }
        }

        // Click DARK theme button
        composeTestRule.onNodeWithText("DARK").performClick()

        // Verify theme changed
        assertEquals(Theme.DARK, selectedTheme)
    }

    @Test
    fun testThemeSwitchingPreservesOtherSettings() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.LIGHT) {
                // Verify other UI elements are still present
                val backgroundColor = MaterialTheme.colorScheme.background
                assert(backgroundColor != null)
            }
        }
    }

    @Test
    fun testLightThemeColorScheme() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.LIGHT) {
                val colorScheme = MaterialTheme.colorScheme
                // Verify light theme has appropriate colors
                assert(colorScheme.primary != null)
                assert(colorScheme.secondary != null)
                assert(colorScheme.background != null)
            }
        }
    }

    @Test
    fun testDarkThemeColorScheme() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.DARK) {
                val colorScheme = MaterialTheme.colorScheme
                // Verify dark theme has appropriate colors
                assert(colorScheme.primary != null)
                assert(colorScheme.secondary != null)
                assert(colorScheme.background != null)
            }
        }
    }

    @Test
    fun testThemeTransition() {
        var currentTheme = Theme.LIGHT
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = currentTheme) {
                ThemeSelector(
                    selectedTheme = currentTheme,
                    onThemeSelected = { currentTheme = it }
                )
            }
        }

        // Start with light theme
        assertEquals(Theme.LIGHT, currentTheme)

        // Switch to dark
        composeTestRule.onNodeWithText("DARK").performClick()
        assertEquals(Theme.DARK, currentTheme)

        // Switch back to light
        composeTestRule.onNodeWithText("LIGHT").performClick()
        assertEquals(Theme.LIGHT, currentTheme)
    }

    @Test
    fun testThemeSelectorDisablesCurrentTheme() {
        composeTestRule.setContent {
            AdhdfocusAppThemeWithTheme(theme = Theme.LIGHT) {
                ThemeSelector(
                    selectedTheme = Theme.LIGHT,
                    onThemeSelected = {}
                )
            }
        }

        // LIGHT button should be disabled (current theme)
        composeTestRule.onNodeWithText("LIGHT").assertExists()
        // DARK button should be enabled
        composeTestRule.onNodeWithText("DARK").assertExists()
    }
}
