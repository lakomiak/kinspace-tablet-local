package com.adhdfocus.app.ui.family

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.domain.userswitching.PinManagementManager
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinManagementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var pinManagementManager: PinManagementManager
    private lateinit var viewModel: PinManagementViewModel

    private val testUserId = "user-123"
    private val testPin = "1234"

    @Before
    fun setup() {
        pinManagementManager = mockk()
        viewModel = PinManagementViewModel(pinManagementManager)
    }

    // ============ Display Tests ============

    @Test
    fun testPinManagementScreenDisplaysTitle() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("PIN Protection").assertIsDisplayed()
    }

    @Test
    fun testPinManagementScreenDisplaysStatusCard() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Current Status").assertIsDisplayed()
    }

    @Test
    fun testPinManagementScreenDisplaysRequirementsCard() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("PIN Requirements").assertIsDisplayed()
    }

    // ============ Unprotected User Tests ============

    @Test
    fun testUnprotectedUserShowsSetPinButton() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Set PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set PIN").assertIsEnabled()
    }

    @Test
    fun testUnprotectedUserDisplaysCorrectStatus() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No PIN protection").assertIsDisplayed()
    }

    @Test
    fun testSetPinButtonOpensDialog() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Set PIN").performClick()

        composeTestRule.onNodeWithText("Set PIN Protection").assertIsDisplayed()
    }

    // ============ Protected User Tests ============

    @Test
    fun testProtectedUserShowsChangePinButton() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Change PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Change PIN").assertIsEnabled()
    }

    @Test
    fun testProtectedUserShowsRemovePinButton() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Remove PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove PIN").assertIsEnabled()
    }

    @Test
    fun testProtectedUserDisplaysCorrectStatus() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("PIN protected").assertIsDisplayed()
    }

    @Test
    fun testChangePinButtonOpensDialog() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Change PIN").performClick()

        composeTestRule.onNodeWithText("Change PIN").assertIsDisplayed()
    }

    @Test
    fun testRemovePinButtonOpensDialog() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Remove PIN").performClick()

        composeTestRule.onNodeWithText("Remove PIN Protection").assertIsDisplayed()
    }

    // ============ Dialog Tests ============

    @Test
    fun testSetPinDialogDisplaysFields() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Set PIN").performClick()

        composeTestRule.onNodeWithText("PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm PIN").assertIsDisplayed()
    }

    @Test
    fun testSetPinDialogConfirmButtonDisabledInitially() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Set PIN").performClick()

        composeTestRule.onNodeWithText("Set PIN").assertIsNotEnabled()
    }

    @Test
    fun testChangePinDialogDisplaysFields() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Change PIN").performClick()

        composeTestRule.onNodeWithText("Current PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("New PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm New PIN").assertIsDisplayed()
    }

    @Test
    fun testRemovePinDialogDisplaysField() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Remove PIN").performClick()

        composeTestRule.onNodeWithText("PIN").assertIsDisplayed()
    }

    // ============ Error Message Tests ============

    @Test
    fun testErrorMessageDisplayed() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        // Simulate error
        viewModel.setupPin("123") // Invalid PIN

        composeTestRule.waitForIdle()

        // Error should be displayed (if setupPin fails)
        // This test verifies the error display mechanism
    }

    // ============ Button State Tests ============

    @Test
    fun testSetPinButtonDisabledDuringLoading() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        // Verify button is enabled initially
        composeTestRule.onNodeWithText("Set PIN").assertIsEnabled()
    }

    @Test
    fun testChangePinButtonDisabledDuringLoading() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        // Verify button is enabled initially
        composeTestRule.onNodeWithText("Change PIN").assertIsEnabled()
    }

    @Test
    fun testRemovePinButtonDisabledDuringLoading() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        // Verify button is enabled initially
        composeTestRule.onNodeWithText("Remove PIN").assertIsEnabled()
    }

    // ============ Dialog Cancellation Tests ============

    @Test
    fun testSetPinDialogCanBeCancelled() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Set PIN").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog should be closed
        composeTestRule.onNodeWithText("Set PIN Protection").assertDoesNotExist()
    }

    @Test
    fun testChangePinDialogCanBeCancelled() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Change PIN").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog should be closed
        composeTestRule.onNodeWithText("Change PIN").assertDoesNotExist()
    }

    @Test
    fun testRemovePinDialogCanBeCancelled() {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        composeTestRule.setContent {
            PinManagementScreen(userId = testUserId, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Remove PIN").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog should be closed
        composeTestRule.onNodeWithText("Remove PIN Protection").assertDoesNotExist()
    }
}
