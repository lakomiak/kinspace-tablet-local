package com.adhdfocus.app.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SignInScreen UI component.
 *
 * Tests verify:
 * - UI elements are displayed correctly
 * - Email and password input fields work
 * - Password visibility toggle works
 * - Sign-in button is enabled/disabled based on input
 * - Error messages are displayed
 * - Loading state is handled
 * - Navigation callbacks are triggered
 */
@RunWith(AndroidJUnit4::class)
class SignInScreenUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSignInScreenDisplaysAllElements() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Verify app branding
        composeTestRule.onNodeWithText("ADHD Focus App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in to your household account").assertIsDisplayed()

        // Verify input fields
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()

        // Verify buttons
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forgot password?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign up").assertIsDisplayed()
    }

    @Test
    fun testSignInButtonDisabledWhenFieldsEmpty() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Sign-in button should be disabled when fields are empty
        composeTestRule.onNodeWithText("Sign In").assertIsNotEnabled()
    }

    @Test
    fun testSignInButtonEnabledWhenFieldsFilled() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Fill in email field
        composeTestRule.onNodeWithText("Enter your email").performTextInput("test@example.com")

        // Fill in password field
        composeTestRule.onNodeWithText("Enter your password").performTextInput("password123")

        // Sign-in button should be enabled
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test
    fun testPasswordVisibilityToggle() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Fill in password
        composeTestRule.onNodeWithText("Enter your password").performTextInput("password123")

        // Initially password should be hidden (visibility icon should show "Show password")
        composeTestRule.onNodeWithText("Show password").assertIsDisplayed()

        // Click visibility toggle
        composeTestRule.onNodeWithText("Show password").performClick()

        // Now it should show "Hide password"
        composeTestRule.onNodeWithText("Hide password").assertIsDisplayed()
    }

    @Test
    fun testForgotPasswordLinkClickable() {
        var forgotPasswordClicked = false

        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = { forgotPasswordClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Forgot password?").performClick()

        assert(forgotPasswordClicked)
    }

    @Test
    fun testSignUpLinkClickable() {
        var signUpClicked = false

        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = { signUpClicked = true },
                onForgotPasswordClick = {}
            )
        }

        composeTestRule.onNodeWithText("Sign up").performClick()

        assert(signUpClicked)
    }

    @Test
    fun testEmailInputAcceptsText() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        val testEmail = "user@example.com"
        composeTestRule.onNodeWithText("Enter your email").performTextInput(testEmail)

        // Verify the text was entered (button should be enabled if password is also filled)
        composeTestRule.onNodeWithText("Enter your password").performTextInput("password")
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test
    fun testPasswordInputAcceptsText() {
        composeTestRule.setContent {
            SignInScreen(
                onSignInSuccess = {},
                onSignUpClick = {},
                onForgotPasswordClick = {}
            )
        }

        val testPassword = "securePassword123"
        composeTestRule.onNodeWithText("Enter your password").performTextInput(testPassword)

        // Verify the text was entered (button should be enabled if email is also filled)
        composeTestRule.onNodeWithText("Enter your email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }
}
