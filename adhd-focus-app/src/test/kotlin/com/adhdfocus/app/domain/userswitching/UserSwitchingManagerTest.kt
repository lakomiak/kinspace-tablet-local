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

class UserSwitchingManagerTest {
    private lateinit var userSwitchingRepository: UserSwitchingRepository
    private lateinit var userSwitchingManager: UserSwitchingManager

    @Before
    fun setup() {
        userSwitchingRepository = mockk()
        userSwitchingManager = UserSwitchingManager(userSwitchingRepository)
    }

    @Test
    fun `switchUser succeeds when user is valid`() = runTest {
        val userId = "user-1"
        val householdId = "household-1"

        coEvery {
            userSwitchingRepository.validateUserSwitch(userId, householdId)
        } returns true

        coEvery {
            userSwitchingRepository.setCurrentUser(userId, householdId)
        } returns true

        val result = userSwitchingManager.switchUser(userId, householdId)

        assertTrue(result)
        coVerify { userSwitchingRepository.validateUserSwitch(userId, householdId) }
        coVerify { userSwitchingRepository.setCurrentUser(userId, householdId) }
    }

    @Test
    fun `switchUser fails when user validation fails`() = runTest {
        val userId = "invalid-user"
        val householdId = "household-1"

        coEvery {
            userSwitchingRepository.validateUserSwitch(userId, householdId)
        } returns false

        val result = userSwitchingManager.switchUser(userId, householdId)

        assertFalse(result)
        coVerify(exactly = 0) { userSwitchingRepository.setCurrentUser(any(), any()) }
    }

    @Test
    fun `switchUser fails when setCurrentUser fails`() = runTest {
        val userId = "user-1"
        val householdId = "household-1"

        coEvery {
            userSwitchingRepository.validateUserSwitch(userId, householdId)
        } returns true

        coEvery {
            userSwitchingRepository.setCurrentUser(userId, householdId)
        } returns false

        val result = userSwitchingManager.switchUser(userId, householdId)

        assertFalse(result)
    }

    @Test
    fun `getCurrentUser returns current user`() = runTest {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "user@example.com",
            displayName = "User One",
            role = UserRole.ADHD_USER
        )

        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns user

        val result = userSwitchingManager.getCurrentUser()

        assertEquals(user, result)
    }

    @Test
    fun `getCurrentUser returns null when no user is set`() = runTest {
        coEvery {
            userSwitchingRepository.getCurrentUser()
        } returns null

        val result = userSwitchingManager.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun `validateUserSwitch returns true for valid user`() = runTest {
        val userId = "user-1"
        val householdId = "household-1"

        coEvery {
            userSwitchingRepository.validateUserSwitch(userId, householdId)
        } returns true

        val result = userSwitchingManager.validateUserSwitch(userId, householdId)

        assertTrue(result)
    }

    @Test
    fun `validateUserSwitch returns false for invalid user`() = runTest {
        val userId = "invalid-user"
        val householdId = "household-1"

        coEvery {
            userSwitchingRepository.validateUserSwitch(userId, householdId)
        } returns false

        val result = userSwitchingManager.validateUserSwitch(userId, householdId)

        assertFalse(result)
    }

    @Test
    fun `isUserSwitchingEnabled returns true when user is set`() = runTest {
        coEvery {
            userSwitchingRepository.isUserSwitchingEnabled()
        } returns true

        val result = userSwitchingManager.isUserSwitchingEnabled()

        assertTrue(result)
    }

    @Test
    fun `isUserSwitchingEnabled returns false when no user is set`() = runTest {
        coEvery {
            userSwitchingRepository.isUserSwitchingEnabled()
        } returns false

        val result = userSwitchingManager.isUserSwitchingEnabled()

        assertFalse(result)
    }

    @Test
    fun `clearCurrentUser clears the current user`() = runTest {
        coEvery {
            userSwitchingRepository.clearCurrentUser()
        } returns Unit

        userSwitchingManager.clearCurrentUser()

        coVerify { userSwitchingRepository.clearCurrentUser() }
    }

    @Test
    fun `getSessionDuration returns correct duration`() = runTest {
        val duration = 3600000L // 1 hour in milliseconds

        coEvery {
            userSwitchingRepository.getSessionDuration()
        } returns duration

        val result = userSwitchingManager.getSessionDuration()

        assertEquals(duration, result)
    }

    @Test
    fun `getSessionDuration returns 0 when no session`() = runTest {
        coEvery {
            userSwitchingRepository.getSessionDuration()
        } returns 0L

        val result = userSwitchingManager.getSessionDuration()

        assertEquals(0L, result)
    }

    @Test
    fun `getTimeSinceLastSwitch returns correct time`() = runTest {
        val timeSinceSwitch = 1800000L // 30 minutes in milliseconds

        coEvery {
            userSwitchingRepository.getTimeSinceLastSwitch()
        } returns timeSinceSwitch

        val result = userSwitchingManager.getTimeSinceLastSwitch()

        assertEquals(timeSinceSwitch, result)
    }

    @Test
    fun `getTimeSinceLastSwitch returns 0 when no switch recorded`() = runTest {
        coEvery {
            userSwitchingRepository.getTimeSinceLastSwitch()
        } returns 0L

        val result = userSwitchingManager.getTimeSinceLastSwitch()

        assertEquals(0L, result)
    }
}
