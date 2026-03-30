package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.UserDao
import com.adhdfocus.app.data.dao.UserSwitchingStateDao
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.model.UserSwitchingState
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
 * Integration tests for UserSwitchingRepository.
 *
 * **Validates: Phase 3 Requirements - Family Member Switching**
 *
 * These tests verify:
 * 1. Repository correctly manages user switching state
 * 2. Session tracking works correctly
 * 3. User validation is enforced
 * 4. State transitions are atomic and consistent
 */
class UserSwitchingRepositoryIntegrationTest {
    private lateinit var userSwitchingStateDao: UserSwitchingStateDao
    private lateinit var userDao: UserDao
    private lateinit var userSwitchingRepository: UserSwitchingRepository

    @Before
    fun setup() {
        userSwitchingStateDao = mockk()
        userDao = mockk()
        userSwitchingRepository = UserSwitchingRepository(userSwitchingStateDao, userDao)
    }

    @Test
    fun `setCurrentUser creates new state when none exists`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        coEvery {
            userSwitchingStateDao.insert(any())
        } returns 1L

        val result = userSwitchingRepository.setCurrentUser("user-1", "household-1")

        assertTrue(result)
        coVerify { userSwitchingStateDao.insert(any()) }
    }

    @Test
    fun `setCurrentUser updates existing state`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val existingState = UserSwitchingState(
            userId = "user-2",
            householdId = "household-1"
        )

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns existingState

        coEvery {
            userSwitchingStateDao.update(any())
        } returns Unit

        val result = userSwitchingRepository.setCurrentUser("user-1", "household-1")

        assertTrue(result)
        coVerify { userSwitchingStateDao.update(any()) }
    }

    @Test
    fun `setCurrentUser preserves session start time on update`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val sessionStart = Instant.now().minusSeconds(3600)
        val existingState = UserSwitchingState(
            userId = "user-2",
            householdId = "household-1",
            sessionStartTime = sessionStart
        )

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns existingState

        var capturedState: UserSwitchingState? = null
        coEvery {
            userSwitchingStateDao.update(any())
        } answers {
            capturedState = firstArg()
        }

        userSwitchingRepository.setCurrentUser("user-1", "household-1")

        // Session start time should be preserved
        assertEquals(sessionStart, capturedState?.sessionStartTime)
    }

    @Test
    fun `getCurrentUser returns user when state exists`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1"
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        val result = userSwitchingRepository.getCurrentUser()

        assertEquals(user, result)
    }

    @Test
    fun `getCurrentUser returns null when state does not exist`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun `getCurrentUser returns null when user not found`() = runTest {
        val state = UserSwitchingState(
            userId = "non-existent",
            householdId = "household-1"
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        coEvery {
            userDao.getUserById("non-existent")
        } returns null

        val result = userSwitchingRepository.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun `validateUserSwitch returns true for valid user in household`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        val result = userSwitchingRepository.validateUserSwitch("user-1", "household-1")

        assertTrue(result)
    }

    @Test
    fun `validateUserSwitch returns false for user in different household`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-2",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userDao.getUserById("user-1")
        } returns user

        val result = userSwitchingRepository.validateUserSwitch("user-1", "household-1")

        assertFalse(result)
    }

    @Test
    fun `validateUserSwitch returns false for non-existent user`() = runTest {
        coEvery {
            userDao.getUserById("non-existent")
        } returns null

        val result = userSwitchingRepository.validateUserSwitch("non-existent", "household-1")

        assertFalse(result)
    }

    @Test
    fun `isUserSwitchingEnabled returns true when state exists`() = runTest {
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1"
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val result = userSwitchingRepository.isUserSwitchingEnabled()

        assertTrue(result)
    }

    @Test
    fun `isUserSwitchingEnabled returns false when state does not exist`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.isUserSwitchingEnabled()

        assertFalse(result)
    }

    @Test
    fun `getSessionDuration calculates correctly`() = runTest {
        val sessionStart = Instant.now().minusSeconds(3600)
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            sessionStartTime = sessionStart
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val duration = userSwitchingRepository.getSessionDuration()

        // Should be approximately 1 hour (3600000 ms)
        assertTrue(duration in 3599000..3601000)
    }

    @Test
    fun `getSessionDuration returns 0 when no state`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val duration = userSwitchingRepository.getSessionDuration()

        assertEquals(0L, duration)
    }

    @Test
    fun `getTimeSinceLastSwitch calculates correctly`() = runTest {
        val lastSwitch = Instant.now().minusSeconds(1800)
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            lastSwitchTime = lastSwitch
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val timeSinceSwitch = userSwitchingRepository.getTimeSinceLastSwitch()

        // Should be approximately 30 minutes (1800000 ms)
        assertTrue(timeSinceSwitch in 1799000..1801000)
    }

    @Test
    fun `getTimeSinceLastSwitch returns 0 when no state`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val timeSinceSwitch = userSwitchingRepository.getTimeSinceLastSwitch()

        assertEquals(0L, timeSinceSwitch)
    }

    @Test
    fun `clearCurrentUser removes state`() = runTest {
        coEvery {
            userSwitchingStateDao.clearCurrentUser()
        } returns Unit

        userSwitchingRepository.clearCurrentUser()

        coVerify { userSwitchingStateDao.clearCurrentUser() }
    }

    @Test
    fun `getCurrentUserState returns state when set`() = runTest {
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1"
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val result = userSwitchingRepository.getCurrentUserState()

        assertEquals(state, result)
    }

    @Test
    fun `getCurrentUserState returns null when not set`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.getCurrentUserState()

        assertNull(result)
    }

    @Test
    fun `switching users updates last switch time`() = runTest {
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

        // First switch
        coEvery {
            userDao.getUserById("user-1")
        } returns user1
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null
        coEvery {
            userSwitchingStateDao.insert(any())
        } returns 1L

        userSwitchingRepository.setCurrentUser("user-1", "household-1")

        // Second switch
        val existingState = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            lastSwitchTime = Instant.now().minusSeconds(60)
        )
        coEvery {
            userDao.getUserById("user-2")
        } returns user2
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns existingState

        var capturedState: UserSwitchingState? = null
        coEvery {
            userSwitchingStateDao.update(any())
        } answers {
            capturedState = firstArg()
        }

        userSwitchingRepository.setCurrentUser("user-2", "household-1")

        // Last switch time should be updated
        assertTrue(capturedState?.lastSwitchTime?.isAfter(existingState.lastSwitchTime) ?: false)
    }
}
