package com.adhdfocus.app.domain.userswitching

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.model.UserSwitchingState
import com.adhdfocus.app.data.repository.UserSwitchingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for user switching data isolation and session management.
 *
 * **Validates: Phase 3 Requirements - Family Member Switching**
 *
 * These tests verify:
 * 1. Data isolation between family members
 * 2. Session state management
 * 3. User validation and context
 * 4. Edge cases and error conditions
 */
class UserSwitchingDataIsolationTest {
    private lateinit var userSwitchingRepository: UserSwitchingRepository
    private lateinit var userSwitchingManager: UserSwitchingManager

    @Before
    fun setup() {
        userSwitchingRepository = mockk()
        userSwitchingManager = UserSwitchingManager(userSwitchingRepository)
    }

    @Test
    fun `switching users maintains data isolation - user1 data not accessible to user2`() = runTest {
        val user1 = User(
            id = "user-1",
            householdId = "household-1",
            email = "user1@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val user2 = User(
            id = "user-2",
            householdId = "household-1",
            email = "user2@example.com",
            displayName = "User Two",
            role = UserRole.ADHD_USER
        )

        // Switch to user1
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns user1

        userSwitchingManager.switchUser("user-1", "household-1")
        var currentUser = userSwitchingManager.getCurrentUser()
        assertEquals(user1.id, currentUser?.id)

        // Switch to user2
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns user2

        userSwitchingManager.switchUser("user-2", "household-1")
        currentUser = userSwitchingManager.getCurrentUser()
        assertEquals(user2.id, currentUser?.id)

        // Verify user1 is no longer current
        assertFalse(currentUser?.id == user1.id)
    }

    @Test
    fun `switching users prevents cross-household access`() = runTest {
        val user1 = User(
            id = "user-1",
            householdId = "household-1",
            email = "user1@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val user2 = User(
            id = "user-2",
            householdId = "household-2",
            email = "user2@example.com",
            displayName = "User Two",
            role = UserRole.ADHD_USER
        )

        // Try to switch user from household-1 to user from household-2
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-2", "household-1")
        } returns false

        val result = userSwitchingManager.switchUser("user-2", "household-1")

        assertFalse(result)
        coVerify(exactly = 0) { userSwitchingRepository.setCurrentUser("user-2", "household-1") }
    }

    @Test
    fun `session state is isolated per user`() = runTest {
        val user1 = User(
            id = "user-1",
            householdId = "household-1",
            email = "user1@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val user2 = User(
            id = "user-2",
            householdId = "household-1",
            email = "user2@example.com",
            displayName = "User Two",
            role = UserRole.ADHD_USER
        )

        val now = Instant.now()
        val sessionStart1 = now.minusSeconds(3600) // 1 hour ago
        val sessionStart2 = now.minusSeconds(600) // 10 minutes ago

        // User1 session
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            sessionStartTime = sessionStart1
        )

        userSwitchingManager.switchUser("user-1", "household-1")
        val duration1 = userSwitchingManager.getSessionDuration()
        assertTrue(duration1 > 3599000) // Should be close to 1 hour

        // User2 session
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns UserSwitchingState(
            userId = "user-2",
            householdId = "household-1",
            sessionStartTime = sessionStart2
        )

        userSwitchingManager.switchUser("user-2", "household-1")
        val duration2 = userSwitchingManager.getSessionDuration()
        assertTrue(duration2 < 1000000) // Should be close to 10 minutes
        assertTrue(duration2 < duration1) // User2 session should be shorter
    }

    @Test
    fun `null user ID prevents switch`() = runTest {
        coEvery {
            userSwitchingRepository.validateUserSwitch("", "household-1")
        } returns false

        val result = userSwitchingManager.switchUser("", "household-1")

        assertFalse(result)
    }

    @Test
    fun `null household ID prevents switch`() = runTest {
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "")
        } returns false

        val result = userSwitchingManager.switchUser("user-1", "")

        assertFalse(result)
    }

    @Test
    fun `switching to non-existent user fails`() = runTest {
        coEvery {
            userSwitchingRepository.validateUserSwitch("non-existent-user", "household-1")
        } returns false

        val result = userSwitchingManager.switchUser("non-existent-user", "household-1")

        assertFalse(result)
    }

    @Test
    fun `session duration increases over time`() = runTest {
        val sessionStart = Instant.now().minusSeconds(100)

        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            sessionStartTime = sessionStart
        )

        val duration1 = userSwitchingManager.getSessionDuration()

        // Simulate time passing
        Thread.sleep(100)

        val duration2 = userSwitchingManager.getSessionDuration()

        assertTrue(duration2 >= duration1)
    }

    @Test
    fun `time since last switch is accurate`() = runTest {
        val lastSwitch = Instant.now().minusSeconds(60)

        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            lastSwitchTime = lastSwitch
        )

        val timeSinceSwitch = userSwitchingManager.getTimeSinceLastSwitch()

        assertTrue(timeSinceSwitch in 59000..61000) // Allow 1 second tolerance
    }

    @Test
    fun `clearing user prevents access to previous user data`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns user

        userSwitchingManager.switchUser("user-1", "household-1")
        var currentUser = userSwitchingManager.getCurrentUser()
        assertEquals(user.id, currentUser?.id)

        // Clear current user
        coEvery {
            userSwitchingRepository.clearCurrentUser()
        } returns Unit
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns null

        userSwitchingManager.clearCurrentUser()
        currentUser = userSwitchingManager.getCurrentUser()
        assertNull(currentUser)
    }

    @Test
    fun `multiple rapid switches maintain correct state`() = runTest {
        val users = listOf(
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
                role = UserRole.ADHD_USER
            ),
            User(
                id = "user-3",
                householdId = "household-1",
                email = "user3@example.com",
                displayName = "User Three",
                role = UserRole.CAREGIVER
            )
        )

        users.forEach { user ->
            coEvery {
                userSwitchingRepository.validateUserSwitch(user.id, "household-1")
            } returns true
            coEvery {
                userSwitchingRepository.setCurrentUser(user.id, "household-1")
            } returns true
            coEvery {
                userSwitchingRepository.getCurrentUser()
            } returns user

            val result = userSwitchingManager.switchUser(user.id, "household-1")
            assertTrue(result)

            val currentUser = userSwitchingManager.getCurrentUser()
            assertEquals(user.id, currentUser?.id)
        }
    }

    @Test
    fun `user role is preserved across switches`() = runTest {
        val adhd_user = User(
            id = "user-1",
            householdId = "household-1",
            email = "adhd@example.com",
            displayName = "ADHD User",
            role = UserRole.ADHD_USER
        )
        val caregiver = User(
            id = "user-2",
            householdId = "household-1",
            email = "caregiver@example.com",
            displayName = "Caregiver",
            role = UserRole.CAREGIVER
        )

        // Switch to ADHD user
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns adhd_user

        userSwitchingManager.switchUser("user-1", "household-1")
        var currentUser = userSwitchingManager.getCurrentUser()
        assertEquals(UserRole.ADHD_USER, currentUser?.role)

        // Switch to caregiver
        coEvery {
            userSwitchingRepository.validateUserSwitch("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-2", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns caregiver

        userSwitchingManager.switchUser("user-2", "household-1")
        currentUser = userSwitchingManager.getCurrentUser()
        assertEquals(UserRole.CAREGIVER, currentUser?.role)
    }

    @Test
    fun `household context is maintained across multiple switches`() = runTest {
        val household1Users = listOf(
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
                role = UserRole.ADHD_USER
            )
        )

        household1Users.forEach { user ->
            coEvery {
                userSwitchingRepository.validateUserSwitch(user.id, "household-1")
            } returns true
            coEvery {
                userSwitchingRepository.setCurrentUser(user.id, "household-1")
            } returns true
            coEvery {
                userSwitchingRepository.getCurrentUser()
            } returns user

            userSwitchingManager.switchUser(user.id, "household-1")
            val currentUser = userSwitchingManager.getCurrentUser()
            assertEquals("household-1", currentUser?.householdId)
        }
    }

    @Test
    fun `session state is cleared when user is cleared`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userSwitchingRepository.validateUserSwitch("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.setCurrentUser("user-1", "household-1")
        } returns true
        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns UserSwitchingState(
            userId = "user-1",
            householdId = "household-1"
        )

        userSwitchingManager.switchUser("user-1", "household-1")
        var sessionDuration = userSwitchingManager.getSessionDuration()
        assertTrue(sessionDuration >= 0)

        // Clear user
        coEvery {
            userSwitchingRepository.clearCurrentUser()
        } returns Unit
        coEvery {
            userSwitchingRepository.getCurrentUserState()
        } returns null

        userSwitchingManager.clearCurrentUser()
        sessionDuration = userSwitchingManager.getSessionDuration()
        assertEquals(0L, sessionDuration)
    }
}
