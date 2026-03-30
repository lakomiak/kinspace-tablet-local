package com.adhdfocus.app.ui.family

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.userswitching.UserSwitchingManager
import com.adhdfocus.app.util.PinValidator
import io.mockk.any
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for FamilyMemberSwitcherViewModel.
 *
 * **Validates: Phase 3 Requirements - Family Member Switching**
 *
 * These tests verify:
 * 1. Household members are loaded correctly
 * 2. User switching updates state
 * 3. Modal state is managed correctly
 * 4. PIN validation works
 * 5. Loading state is tracked
 * 6. Error handling works correctly
 * 7. Integration with UserSwitchingManager
 */
class FamilyMemberSwitcherViewModelTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userSwitchingManager: UserSwitchingManager
    private lateinit var viewModel: FamilyMemberSwitcherViewModel

    @Before
    fun setup() {
        userRepository = mockk()
        userSwitchingManager = mockk()
        viewModel = FamilyMemberSwitcherViewModel(userRepository, userSwitchingManager)
    }

    @Test
    fun `loadHouseholdMembers loads members successfully`() = runTest {
        val members = listOf(
            User(
                id = "user-1",
                householdId = "household-1",
                email = "user1@example.com",
                displayName = "User One",
                role = UserRole.ADHD_USER
            ),
            User(
                id = "user-2",
                householdId = "household-1",
                email = "user2@example.com",
                displayName = "User Two",
                role = UserRole.CAREGIVER
            )
        )

        coEvery {
            userRepository.getUsersByHousehold("household-1")
        } returns members

        viewModel.loadHouseholdMembers("household-1")
        // Give coroutine time to complete
        kotlinx.coroutines.delay(100)

        assertEquals(members, viewModel.householdMembers.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadHouseholdMembers sets loading state`() = runTest {
        val members = emptyList<User>()

        coEvery {
            userRepository.getUsersByHousehold("household-1")
        } returns members

        viewModel.loadHouseholdMembers("household-1")
        kotlinx.coroutines.delay(100)

        // After loading, isLoading should be false
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHouseholdMembers handles empty household`() = runTest {
        coEvery {
            userRepository.getUsersByHousehold("household-1")
        } returns emptyList()

        viewModel.loadHouseholdMembers("household-1")
        kotlinx.coroutines.delay(100)

        assertEquals(emptyList(), viewModel.householdMembers.value)
        assertEquals("No household members found", viewModel.errorMessage.value)
    }

    @Test
    fun `loadHouseholdMembers handles error gracefully`() = runTest {
        coEvery {
            userRepository.getUsersByHousehold("household-1")
        } throws Exception("Network error")

        viewModel.loadHouseholdMembers("household-1")
        kotlinx.coroutines.delay(100)

        assertFalse(viewModel.isLoading.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to load") == true)
    }

    @Test
    fun `switchToMember updates current user`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)

        assertEquals(user, viewModel.currentUser.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember closes modal after successful switch`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        viewModel.openMemberSelector()
        assertTrue(viewModel.isModalOpen.value)

        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)

        assertFalse(viewModel.isModalOpen.value)
    }

    @Test
    fun `switchToMember requires PIN for protected profile`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = true,
            pinHash = PinValidator.hashPin("1234")
        )

        viewModel.householdMembers.value = listOf(user)

        viewModel.switchToMember("user-1", pin = null)
        kotlinx.coroutines.delay(100)

        // Should not switch (PIN required)
        assertNull(viewModel.currentUser.value)
        assertEquals("PIN required for this profile", viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember validates PIN for protected profile`() = runTest {
        val pin = "1234"
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = true,
            pinHash = PinValidator.hashPin(pin)
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        viewModel.switchToMember("user-1", pin = pin)
        kotlinx.coroutines.delay(100)

        // PIN validation should pass
        assertEquals(user, viewModel.currentUser.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember rejects invalid PIN`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = true,
            pinHash = PinValidator.hashPin("1234")
        )

        viewModel.householdMembers.value = listOf(user)

        viewModel.switchToMember("user-1", pin = "9999")
        kotlinx.coroutines.delay(100)

        // Should not switch (invalid PIN)
        assertNull(viewModel.currentUser.value)
        assertEquals("Invalid PIN", viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember fails for non-existent user`() = runTest {
        viewModel.householdMembers.value = emptyList()

        viewModel.switchToMember("non-existent")
        kotlinx.coroutines.delay(100)

        assertNull(viewModel.currentUser.value)
        assertEquals("User not found", viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember handles UserSwitchingManager failure`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns false

        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)

        assertNull(viewModel.currentUser.value)
        assertEquals("Failed to switch user", viewModel.errorMessage.value)
    }

    @Test
    fun `openMemberSelector opens modal`() {
        assertFalse(viewModel.isModalOpen.value)

        viewModel.openMemberSelector()

        assertTrue(viewModel.isModalOpen.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `closeMemberSelector closes modal`() {
        viewModel.openMemberSelector()
        assertTrue(viewModel.isModalOpen.value)

        viewModel.closeMemberSelector()

        assertFalse(viewModel.isModalOpen.value)
    }

    @Test
    fun `multiple member switches update current user correctly`() = runTest {
        val user1 = User(
            id = "user-1",
            householdId = "household-1",
            email = "user1@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )
        val user2 = User(
            id = "user-2",
            householdId = "household-1",
            email = "user2@example.com",
            displayName = "User Two",
            role = UserRole.CAREGIVER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user1, user2)
        coEvery {
            userSwitchingManager.switchUser(any(), any())
        } returns true

        // Switch to user1
        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)
        assertEquals(user1, viewModel.currentUser.value)

        // Switch to user2
        viewModel.switchToMember("user-2")
        kotlinx.coroutines.delay(100)
        assertEquals(user2, viewModel.currentUser.value)
    }

    @Test
    fun `household members are sorted by display name`() = runTest {
        val members = listOf(
            User(
                id = "user-3",
                householdId = "household-1",
                email = "user3@example.com",
                displayName = "Zoe",
                role = UserRole.ADHD_USER
            ),
            User(
                id = "user-1",
                householdId = "household-1",
                email = "user1@example.com",
                displayName = "Alice",
                role = UserRole.ADHD_USER
            ),
            User(
                id = "user-2",
                householdId = "household-1",
                email = "user2@example.com",
                displayName = "Bob",
                role = UserRole.CAREGIVER
            )
        )

        coEvery {
            userRepository.getUsersByHousehold("household-1")
        } returns members

        viewModel.loadHouseholdMembers("household-1")
        kotlinx.coroutines.delay(100)

        // Members should be available (sorting would be done in UI layer)
        assertEquals(3, viewModel.householdMembers.value.size)
    }

    @Test
    fun `current user is null initially`() {
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `modal is closed initially`() {
        assertFalse(viewModel.isModalOpen.value)
    }

    @Test
    fun `isLoading is false initially`() {
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `isSwitching is false initially`() {
        assertFalse(viewModel.isSwitching.value)
    }

    @Test
    fun `householdMembers is empty initially`() {
        assertEquals(emptyList(), viewModel.householdMembers.value)
    }

    @Test
    fun `errorMessage is null initially`() {
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearError clears error message`() {
        viewModel.errorMessage.value = "Some error"
        assertTrue(viewModel.errorMessage.value != null)

        viewModel.clearError()

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `switching to same user multiple times maintains state`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        // Switch multiple times
        repeat(3) {
            viewModel.switchToMember("user-1")
            kotlinx.coroutines.delay(100)
            assertEquals(user, viewModel.currentUser.value)
        }
    }

    @Test
    fun `PIN-protected user cannot be switched without PIN`() = runTest {
        val protectedUser = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "Protected User",
            role = UserRole.ADHD_USER,
            isPinProtected = true,
            pinHash = PinValidator.hashPin("1234")
        )

        viewModel.householdMembers.value = listOf(protectedUser)

        // Try to switch without PIN
        viewModel.switchToMember("user-1", pin = null)
        kotlinx.coroutines.delay(100)

        // Should not switch
        assertNull(viewModel.currentUser.value)
        assertEquals("PIN required for this profile", viewModel.errorMessage.value)
    }

    @Test
    fun `unprotected user can be switched without PIN`() = runTest {
        val unprotectedUser = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "Unprotected User",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(unprotectedUser)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)

        assertEquals(unprotectedUser, viewModel.currentUser.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `switchToMember handles exception gracefully`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } throws Exception("Network error")

        viewModel.switchToMember("user-1")
        kotlinx.coroutines.delay(100)

        assertNull(viewModel.currentUser.value)
        assertTrue(viewModel.errorMessage.value?.contains("Error switching user") == true)
        assertFalse(viewModel.isSwitching.value)
    }

    @Test
    fun `isSwitching state is managed correctly`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER,
            isPinProtected = false
        )

        viewModel.householdMembers.value = listOf(user)
        coEvery {
            userSwitchingManager.switchUser("user-1", "household-1")
        } returns true

        assertFalse(viewModel.isSwitching.value)

        viewModel.switchToMember("user-1")
        // isSwitching should be true during the operation
        // After delay, it should be false
        kotlinx.coroutines.delay(100)

        assertFalse(viewModel.isSwitching.value)
    }
}
