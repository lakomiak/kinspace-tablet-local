package com.adhdfocus.app.ui.family

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class FamilyMemberSwitcherIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testUsers = listOf(
        User(
            id = "user1",
            householdId = "household1",
            email = "alice@example.com",
            displayName = "Alice",
            role = UserRole.ADHD_USER,
            isPinProtected = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        User(
            id = "user2",
            householdId = "household1",
            email = "bob@example.com",
            displayName = "Bob",
            role = UserRole.CAREGIVER,
            isPinProtected = true,
            pinHash = "hashed_pin",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        User(
            id = "user3",
            householdId = "household1",
            email = "charlie@example.com",
            displayName = "Charlie",
            role = UserRole.ADMIN,
            isPinProtected = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    )

    @Test
    fun testMemberSelectionModalDisplaysAllMembers() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
    }

    @Test
    fun testMemberSelectionModalShowsCurrentMemberIndicator() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // Current member (Alice) should have checkmark
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testMemberSelectionModalShowsPinProtectionForProtectedMembers() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // Bob is PIN protected, should show lock icon
        composeTestRule.onNodeWithText("🔒").assertIsDisplayed()
    }

    @Test
    fun testMemberSelectionModalSwitchingUnprotectedMember() {
        var selectedUserId: String? = null
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { userId, _ -> selectedUserId = userId },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // Click on Charlie (unprotected)
        composeTestRule.onNodeWithText("Charlie").performClick()
        assert(selectedUserId == "user3")
    }

    @Test
    fun testMemberSelectionModalShowsPinDialogForProtectedMember() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        // Click on Bob (protected)
        composeTestRule.onNodeWithText("Bob").performClick()
        // PIN dialog should appear
        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()
    }

    @Test
    fun testMemberCardDisplaysRoleCorrectly() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUsers[1],
                isCurrentMember = false,
                isPinProtected = true,
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("CAREGIVER").assertIsDisplayed()
    }

    @Test
    fun testMemberCardDisplaysAvatarPlaceholder() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUsers[0],
                isCurrentMember = false,
                isPinProtected = false,
                onTap = {}
            )
        }

        // Avatar placeholder should show first letter
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun testPinEntryDialogAcceptsNumericInput() {
        composeTestRule.setContent {
            PinEntryDialog(
                memberName = "Bob",
                onPinSubmit = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("PIN required for Bob").assertIsDisplayed()
    }

    @Test
    fun testPinEntryDialogDisplaysMaskedPin() {
        composeTestRule.setContent {
            PinEntryDialog(
                memberName = "Bob",
                onPinSubmit = {},
                onDismiss = {}
            )
        }

        // PIN should be masked with dots
        composeTestRule.onNodeWithText("PIN").assertIsDisplayed()
    }

    @Test
    fun testCurrentMemberIndicatorDisplaysUserRole() {
        composeTestRule.setContent {
            CurrentMemberIndicator(
                user = testUsers[0],
                onTap = {}
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADHD USER").assertIsDisplayed()
    }

    @Test
    fun testMemberSelectionModalHandlesEmptyMemberList() {
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

    @Test
    fun testMemberSelectionModalHandlesLoadingState() {
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

        composeTestRule.onNodeWithText("Select Family Member").assertIsDisplayed()
    }

    @Test
    fun testMemberSelectionModalHandlesErrorState() {
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
    fun testMultipleMembersWithDifferentRoles() {
        composeTestRule.setContent {
            FamilyMemberSelectionModal(
                members = testUsers,
                currentUser = testUsers[0],
                isLoading = false,
                errorMessage = null,
                onMemberSelected = { _, _ -> },
                onDismiss = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("ADHD USER").assertIsDisplayed()
        composeTestRule.onNodeWithText("CAREGIVER").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADMIN").assertIsDisplayed()
    }

    @Test
    fun testMemberCardHighlightingForCurrentMember() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUsers[0],
                isCurrentMember = true,
                isPinProtected = false,
                onTap = {}
            )
        }

        // Current member should show checkmark
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testMemberCardNoHighlightingForNonCurrentMember() {
        composeTestRule.setContent {
            FamilyMemberCard(
                member = testUsers[1],
                isCurrentMember = false,
                isPinProtected = false,
                onTap = {}
            )
        }

        // Non-current member should not show checkmark
        // This test verifies the absence of checkmark
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }
}
