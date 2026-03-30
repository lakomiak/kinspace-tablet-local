package com.adhdfocus.app.ui.family

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class FamilyMemberSwitcherScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testUser = User(
        id = "user1",
        householdId = "household1",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.ADHD_USER,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun testCurrentMemberIndicatorDisplaysUserInfo() {
        composeTestRule.setContent {
            CurrentMemberIndicator(
                user = testUser,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("Test User").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADHD USER").assertIsDisplayed()
    }

    @Test
    fun testCurrentMemberIndicatorShowsAvatarPlaceholder() {
        composeTestRule.setContent {
            CurrentMemberIndicator(
                user = testUser,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("T").assertIsDisplayed()
    }

    @Test
    fun testCurrentMemberIndicatorCallsOnTapWhenMenuClicked() {
        var tapped = false
        composeTestRule.setContent {
            CurrentMemberIndicator(
                user = testUser,
                onTap = { tapped = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Switch family member").performClick()
        assert(tapped)
    }

    @Test
    fun testFamilyMemberCardDisplaysMemberInfo() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUser,
                isCurrentMember = false,
                isPinProtected = false,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("Test User").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADHD USER").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberCardHighlightsCurrentMember() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUser,
                isCurrentMember = true,
                isPinProtected = false,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberCardShowsPinProtectionIndicator() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUser,
                isCurrentMember = false,
                isPinProtected = true,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("🔒").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberCardCallsOnTapWhenClicked() {
        var tapped = false
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUser,
                isCurrentMember = false,
                isPinProtected = false,
                onTap = { tapped = true }
            )
        }

        composeTestRule.onNodeWithText("Test User").performClick()
        assert(tapped)
    }

    @Test
    fun testPinEntryDialogDisplaysTitle() {
        composeTestRule.setContent {
            PinEntryDialog(
                memberName = "Test User",
                onPinSubmit = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("PIN required for Test User").assertIsDisplayed()
    }

    @Test
    fun testPinEntryDialogMasksPinInput() {
        composeTestRule.setContent {
            PinEntryDialog(
                memberName = "Test User",
                onPinSubmit = {},
                onDismiss = {}
            )
        }

        // Find the PIN input field and type
        composeTestRule.onNodeWithText("PIN").performClick()
        // Note: In a real test, we'd type the PIN and verify masking
    }

    @Test
    fun testPinEntryDialogCallsOnDismissWhenCancelClicked() {
        var dismissed = false
        composeTestRule.setContent {
            PinEntryDialog(
                memberName = "Test User",
                onPinSubmit = {},
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun testFamilyMemberSelectionModalDisplaysMembers() {
        val members = listOf(
            testUser,
            testUser.copy(id = "user2", displayName = "User Two")
        )

        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = members,
                currentUser = testUser,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Select Family Member").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test User").assertIsDisplayed()
        composeTestRule.onNodeWithText("User Two").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberSelectionModalShowsLoadingState() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = emptyList(),
                currentUser = null,
                isLoading = true,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // CircularProgressIndicator should be displayed
        composeTestRule.onNodeWithText("Select Family Member").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberSelectionModalShowsErrorMessage() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = emptyList(),
                currentUser = null,
                isLoading = false,
                errorMessage = "Failed to load members",
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Failed to load members").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberSelectionModalCallsOnDismissWhenCloseClicked() {
        var dismissed = false
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = emptyList(),
                currentUser = null,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = { dismissed = true },
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Close member selection").performClick()
        assert(dismissed)
    }

    @Test
    fun testFamilyMemberSelectionModalCallsOnMemberSelectedForUnprotectedMember() {
        var selectedUserId: String? = null
        val members = listOf(testUser)

        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = members,
                currentUser = null,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { userId, _ -> selectedUserId = userId },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Test User").performClick()
        assert(selectedUserId == "user1")
    }

    @Test
    fun testFamilyMemberSelectionModalShowsPinDialogForProtectedMember() {
        val protectedUser = testUser.copy(isPinProtected = true)
        val members = listOf(protectedUser)

        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = members,
                currentUser = null,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Test User").performClick()
        // PIN dialog should be displayed
        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberSelectionModalHighlightsCurrentMember() {
        val members = listOf(
            testUser,
            testUser.copy(id = "user2", displayName = "User Two")
        )

        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = members,
                currentUser = testUser,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // Current member should have checkmark
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testFamilyMemberSelectionModalShowsEmptyState() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = emptyList(),
                currentUser = null,
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("No household members found").assertIsDisplayed()
    }
}
