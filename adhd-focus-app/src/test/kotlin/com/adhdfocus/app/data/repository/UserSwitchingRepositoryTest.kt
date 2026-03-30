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

class UserSwitchingRepositoryTest {
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
    fun `getCurrentUserState returns state when set`() = runTest {
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            lastSwitchTime = Instant.now()
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
    fun `getCurrentUser returns user when state is set`() = runTest {
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
    fun `getCurrentUser returns null when state is not set`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun `setCurrentUser succeeds for valid user`() = runTest {
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
    fun `setCurrentUser fails when user does not exist`() = runTest {
        coEvery {
            userDao.getUserById("invalid-user")
        } returns null

        val result = userSwitchingRepository.setCurrentUser("invalid-user", "household-1")

        assertFalse(result)
        coVerify(exactly = 0) { userSwitchingStateDao.insert(any()) }
    }

    @Test
    fun `setCurrentUser fails when user belongs to different household`() = runTest {
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

        val result = userSwitchingRepository.setCurrentUser("user-1", "household-1")

        assertFalse(result)
        coVerify(exactly = 0) { userSwitchingStateDao.insert(any()) }
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
    fun `clearCurrentUser clears the state`() = runTest {
        coEvery {
            userSwitchingStateDao.clearCurrentUser()
        } returns Unit

        userSwitchingRepository.clearCurrentUser()

        coVerify { userSwitchingStateDao.clearCurrentUser() }
    }

    @Test
    fun `validateUserSwitch returns true for valid user`() = runTest {
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
    fun `validateUserSwitch returns false when user does not exist`() = runTest {
        coEvery {
            userDao.getUserById("invalid-user")
        } returns null

        val result = userSwitchingRepository.validateUserSwitch("invalid-user", "household-1")

        assertFalse(result)
    }

    @Test
    fun `validateUserSwitch returns false when user belongs to different household`() = runTest {
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
    fun `getSessionDuration returns correct duration`() = runTest {
        val now = Instant.now()
        val sessionStart = now.minusSeconds(3600) // 1 hour ago
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            sessionStartTime = sessionStart
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val result = userSwitchingRepository.getSessionDuration()

        // Allow some tolerance for test execution time
        assertTrue(result in 3599000..3601000)
    }

    @Test
    fun `getSessionDuration returns 0 when no state`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.getSessionDuration()

        assertEquals(0L, result)
    }

    @Test
    fun `getTimeSinceLastSwitch returns correct time`() = runTest {
        val now = Instant.now()
        val lastSwitch = now.minusSeconds(1800) // 30 minutes ago
        val state = UserSwitchingState(
            userId = "user-1",
            householdId = "household-1",
            lastSwitchTime = lastSwitch
        )

        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns state

        val result = userSwitchingRepository.getTimeSinceLastSwitch()

        // Allow some tolerance for test execution time
        assertTrue(result in 1799000..1801000)
    }

    @Test
    fun `getTimeSinceLastSwitch returns 0 when no state`() = runTest {
        coEvery {
            userSwitchingStateDao.getCurrentUserState()
        } returns null

        val result = userSwitchingRepository.getTimeSinceLastSwitch()

        assertEquals(0L, result)
    }
}
