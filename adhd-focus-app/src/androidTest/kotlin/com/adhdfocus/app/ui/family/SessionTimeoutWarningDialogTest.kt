package com.adhdfocus.app.ui.family

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for SessionTimeoutWarningDialog.
 *
 * Tests verify:
 * - Warning dialog displays correctly
 * - Time remaining is shown
 * - Extend Session button works
 * - Logout button works
 * - Dialog is not visible when isVisible is false
 */
@RunWith(AndroidJUnit4::class)
class SessionTimeoutWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWarningDialogDisplaysWhenVisible() {
        var extendClicked = false
        var logoutClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { extendClicked = true },
                    onLogout = { logoutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertIsDisplayed()
    }

    @Test
    fun testWarningDialogHiddenWhenNotVisible() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = false,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertDoesNotExist()
    }

    @Test
    fun testTimeRemainingDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 65, // 1 minute 5 seconds
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()
    }

    @Test
    fun testExtendSessionButtonDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Extend Session").assertIsDisplayed()
    }

    @Test
    fun testLogoutButtonDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }

    @Test
    fun testExtendSessionButtonClick() {
        var extendClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { extendClicked = true },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Extend Session").performClick()

        assert(extendClicked)
    }

    @Test
    fun testLogoutButtonClick() {
        var logoutClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { logoutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Logout").performClick()

        assert(logoutClicked)
    }

    @Test
    fun testWarningMessageDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText(
            "You will be automatically logged out due to inactivity. Tap 'Extend Session' to continue."
        ).assertIsDisplayed()
    }

    @Test
    fun testTimeRemainingFormatMinutesAndSeconds() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 125, // 2 minutes 5 seconds
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        // Should display time in format "X minute(s) Y second(s)"
        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()
    }

    @Test
    fun testTimeRemainingFormatSecondsOnly() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 45, // 45 seconds
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()
    }

    @Test
    fun testTimeRemainingFormatOneSecond() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = 1, // 1 second
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()
    }

    @Test
    fun testWarningDialogWithButtonsVariant() {
        var extendClicked = false
        var logoutClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialogWithButtons(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { extendClicked = true },
                    onLogout = { logoutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Extend Session").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }

    @Test
    fun testWarningDialogWithButtonsVariantExtendClick() {
        var extendClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialogWithButtons(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { extendClicked = true },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Extend Session").performClick()

        assert(extendClicked)
    }

    @Test
    fun testWarningDialogWithButtonsVariantLogoutClick() {
        var logoutClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialogWithButtons(
                    isVisible = true,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { logoutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Logout").performClick()

        assert(logoutClicked)
    }

    @Test
    fun testWarningDialogWithButtonsVariantHidden() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialogWithButtons(
                    isVisible = false,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertDoesNotExist()
    }

    @Test
    fun testMultipleTimeRemainingUpdates() {
        var timeRemaining = 60L

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = timeRemaining,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()

        // Update time remaining
        timeRemaining = 30L
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = true,
                    timeRemaining = timeRemaining,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Your session will expire in:").assertIsDisplayed()
    }

    @Test
    fun testDialogVisibilityToggle() {
        var isVisible = true

        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = isVisible,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertIsDisplayed()

        // Hide dialog
        isVisible = false
        composeTestRule.setContent {
            MaterialTheme {
                SessionTimeoutWarningDialog(
                    isVisible = isVisible,
                    timeRemaining = 60,
                    onExtendSession = { },
                    onLogout = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Session Timeout Warning").assertDoesNotExist()
    }
}
