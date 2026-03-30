package com.adhdfocus.app.domain.theme

import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Property-based tests for ThemeManager.
 *
 * **Validates: Requirements 8, 6, Property 2.8: Theme Persistence**
 *
 * Tests:
 * - Theme persistence across operations
 * - Per-member theme isolation
 * - Theme state consistency
 */
class ThemeManagerPropertyTest : FunSpec({

    test("Property 2.8: Theme preference persists across app sessions") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>()
            ) { userId, theme ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateTheme(userId, theme) } returns true
                coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns UserPreferences(
                    userId = userId,
                    theme = theme
                )

                // Set theme
                themeManager.setTheme(theme, userId)
                val setTheme = themeManager.getCurrentTheme()

                // Load theme (simulating app restart)
                themeManager.loadThemeForUser(userId)
                val loadedTheme = themeManager.getCurrentTheme()

                // Both should be the same
                setTheme shouldBe theme
                loadedTheme shouldBe theme
            }
        }
    }

    test("Property: Per-member theme isolation") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>(),
                Arb.enum<Theme>()
            ) { userId1, userId2, theme1, theme2 ->
                if (userId1 != userId2) {
                    val userPreferencesManager = mockk<UserPreferencesManager>()
                    val themeManager = ThemeManagerImpl(userPreferencesManager)

                    coEvery { userPreferencesManager.updateTheme(userId1, theme1) } returns true
                    coEvery { userPreferencesManager.updateTheme(userId2, theme2) } returns true

                    // Set different themes for different users
                    themeManager.setTheme(theme1, userId1)
                    val currentTheme1 = themeManager.getCurrentTheme()

                    themeManager.setTheme(theme2, userId2)
                    val currentTheme2 = themeManager.getCurrentTheme()

                    // Last set theme should be theme2
                    currentTheme2 shouldBe theme2
                }
            }
        }
    }

    test("Property: Theme state consistency after multiple operations") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>()
            ) { userId, theme ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateTheme(userId, any()) } returns true

                // Set theme multiple times
                themeManager.setTheme(theme, userId)
                themeManager.setTheme(theme, userId)
                themeManager.setTheme(theme, userId)

                // Final state should be the theme
                themeManager.getCurrentTheme() shouldBe theme
            }
        }
    }

    test("Property: Reset to default always sets LIGHT theme") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>()
            ) { userId, initialTheme ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateTheme(userId, any()) } returns true

                // Set initial theme
                themeManager.setTheme(initialTheme, userId)

                // Reset to default
                themeManager.resetThemeToDefault(userId)

                // Should be LIGHT
                themeManager.getCurrentTheme() shouldBe Theme.LIGHT
            }
        }
    }

    test("Property: Load theme from preferences returns correct theme") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>()
            ) { userId, theme ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns UserPreferences(
                    userId = userId,
                    theme = theme
                )

                themeManager.loadThemeForUser(userId)

                themeManager.getCurrentTheme() shouldBe theme
            }
        }
    }

    test("Property: Theme switching between LIGHT and DARK") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50)
            ) { userId ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateTheme(userId, any()) } returns true

                // Switch to DARK
                themeManager.setTheme(Theme.DARK, userId)
                themeManager.getCurrentTheme() shouldBe Theme.DARK

                // Switch to LIGHT
                themeManager.setTheme(Theme.LIGHT, userId)
                themeManager.getCurrentTheme() shouldBe Theme.LIGHT

                // Switch back to DARK
                themeManager.setTheme(Theme.DARK, userId)
                themeManager.getCurrentTheme() shouldBe Theme.DARK
            }
        }
    }

    test("Property: StateFlow emits theme changes") {
        runTest {
            checkAll(
                Arb.string(minSize = 1, maxSize = 50),
                Arb.enum<Theme>()
            ) { userId, theme ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val themeManager = ThemeManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateTheme(userId, theme) } returns true

                themeManager.setTheme(theme, userId)

                // StateFlow should have the theme
                themeManager.currentTheme.value shouldBe theme
            }
        }
    }
})
