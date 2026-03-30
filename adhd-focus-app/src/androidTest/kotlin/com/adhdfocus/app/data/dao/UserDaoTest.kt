package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by role, household, and email
 * - Sorting and ordering
 * - PIN protection functionality
 * - Count operations
 * - Household member management
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertUser() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe",
            role = UserRole.ADHD_USER
        )

        userDao.insert(user)
        val retrieved = userDao.getUserById("user-1")

        assertNotNull(retrieved)
        assertEquals("John Doe", retrieved.displayName)
        assertEquals("john@example.com", retrieved.email)
        assertEquals(UserRole.ADHD_USER, retrieved.role)
    }

    @Test
    fun testUpdateUser() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe",
            role = UserRole.ADHD_USER
        )

        userDao.insert(user)
        val updated = user.copy(displayName = "Jane Doe", role = UserRole.CAREGIVER)
        userDao.update(updated)

        val retrieved = userDao.getUserById("user-1")
        assertNotNull(retrieved)
        assertEquals("Jane Doe", retrieved.displayName)
        assertEquals(UserRole.CAREGIVER, retrieved.role)
    }

    @Test
    fun testDeleteUser() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe"
        )

        userDao.insert(user)
        userDao.delete(user)

        val retrieved = userDao.getUserById("user-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetUserById() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe"
        )

        userDao.insert(user)
        val retrieved = userDao.getUserById("user-1")

        assertNotNull(retrieved)
        assertEquals(user.id, retrieved.id)
        assertEquals(user.email, retrieved.email)
    }

    @Test
    fun testGetNonExistentUser() = runBlocking {
        val retrieved = userDao.getUserById("non-existent")
        assertNull(retrieved)
    }

    // ==================== Filtering by Household ====================

    @Test
    fun testGetUsersByHousehold() = runBlocking {
        val household1 = "household-1"
        val household2 = "household-2"

        userDao.insert(User(id = "user-1", householdId = household1, email = "user1@example.com", displayName = "User 1"))
        userDao.insert(User(id = "user-2", householdId = household1, email = "user2@example.com", displayName = "User 2"))
        userDao.insert(User(id = "user-3", householdId = household2, email = "user3@example.com", displayName = "User 3"))

        val household1Users = userDao.getUsersByHousehold(household1).first()
        val household2Users = userDao.getUsersByHousehold(household2).first()

        assertEquals(2, household1Users.size)
        assertEquals(1, household2Users.size)
        assertTrue(household1Users.all { it.householdId == household1 })
    }

    @Test
    fun testGetUsersByHouseholdOnce() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "user1@example.com", displayName = "User 1"))
        userDao.insert(User(id = "user-2", householdId = household, email = "user2@example.com", displayName = "User 2"))

        val users = userDao.getUsersByHouseholdOnce(household)

        assertEquals(2, users.size)
    }

    // ==================== Filtering by Email ====================

    @Test
    fun testGetUserByEmail() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe"
        )

        userDao.insert(user)
        val retrieved = userDao.getUserByEmail("john@example.com")

        assertNotNull(retrieved)
        assertEquals("John Doe", retrieved.displayName)
    }

    @Test
    fun testGetNonExistentUserByEmail() = runBlocking {
        val retrieved = userDao.getUserByEmail("nonexistent@example.com")
        assertNull(retrieved)
    }

    // ==================== Filtering by Role ====================

    @Test
    fun testGetUsersByRole() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))
        userDao.insert(User(id = "user-2", householdId = household, email = "caregiver@example.com", displayName = "Caregiver", role = UserRole.CAREGIVER))
        userDao.insert(User(id = "user-3", householdId = household, email = "adhd2@example.com", displayName = "ADHD User 2", role = UserRole.ADHD_USER))

        val adhdUsers = userDao.getUsersByRole(household, UserRole.ADHD_USER).first()
        val caregiverUsers = userDao.getUsersByRole(household, UserRole.CAREGIVER).first()

        assertEquals(2, adhdUsers.size)
        assertEquals(1, caregiverUsers.size)
        assertTrue(adhdUsers.all { it.role == UserRole.ADHD_USER })
    }

    @Test
    fun testGetUsersByRoleOnce() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))
        userDao.insert(User(id = "user-2", householdId = household, email = "caregiver@example.com", displayName = "Caregiver", role = UserRole.CAREGIVER))

        val adhdUsers = userDao.getUsersByRoleOnce(household, UserRole.ADHD_USER)

        assertEquals(1, adhdUsers.size)
        assertEquals(UserRole.ADHD_USER, adhdUsers[0].role)
    }

    // ==================== PIN Protection ====================

    @Test
    fun testGetPinProtectedUsers() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "user1@example.com", displayName = "User 1", isPinProtected = true, pinHash = "hash1"))
        userDao.insert(User(id = "user-2", householdId = household, email = "user2@example.com", displayName = "User 2", isPinProtected = false))
        userDao.insert(User(id = "user-3", householdId = household, email = "user3@example.com", displayName = "User 3", isPinProtected = true, pinHash = "hash3"))

        val pinProtectedUsers = userDao.getPinProtectedUsers(household).first()

        assertEquals(2, pinProtectedUsers.size)
        assertTrue(pinProtectedUsers.all { it.isPinProtected })
    }

    @Test
    fun testGetPinProtectedUsersOnce() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "user1@example.com", displayName = "User 1", isPinProtected = true, pinHash = "hash1"))
        userDao.insert(User(id = "user-2", householdId = household, email = "user2@example.com", displayName = "User 2", isPinProtected = false))

        val pinProtectedUsers = userDao.getPinProtectedUsersOnce(household)

        assertEquals(1, pinProtectedUsers.size)
        assertTrue(pinProtectedUsers[0].isPinProtected)
    }

    @Test
    fun testUpdatePinProtection() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe",
            isPinProtected = false
        )

        userDao.insert(user)
        userDao.updatePinProtection("user-1", true, "newhash")

        val retrieved = userDao.getUserById("user-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isPinProtected)
        assertEquals("newhash", retrieved.pinHash)
    }

    // ==================== Sorting ====================

    @Test
    fun testGetUsersByHouseholdSorted() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "user1@example.com", displayName = "Zoe"))
        userDao.insert(User(id = "user-2", householdId = household, email = "user2@example.com", displayName = "Alice"))
        userDao.insert(User(id = "user-3", householdId = household, email = "user3@example.com", displayName = "Bob"))

        val sortedUsers = userDao.getUsersByHouseholdSorted(household)

        assertEquals(3, sortedUsers.size)
        assertEquals("Alice", sortedUsers[0].displayName)
        assertEquals("Bob", sortedUsers[1].displayName)
        assertEquals("Zoe", sortedUsers[2].displayName)
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetUserCountByHousehold() = runBlocking {
        val household1 = "household-1"
        val household2 = "household-2"

        repeat(3) { i ->
            userDao.insert(User(id = "user-h1-$i", householdId = household1, email = "user$i@example.com", displayName = "User $i"))
        }
        repeat(2) { i ->
            userDao.insert(User(id = "user-h2-$i", householdId = household2, email = "user$i@example.com", displayName = "User $i"))
        }

        val household1Count = userDao.getUserCountByHousehold(household1)
        val household2Count = userDao.getUserCountByHousehold(household2)

        assertEquals(3, household1Count)
        assertEquals(2, household2Count)
    }

    @Test
    fun testGetUserCountByRole() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))
        userDao.insert(User(id = "user-2", householdId = household, email = "caregiver@example.com", displayName = "Caregiver", role = UserRole.CAREGIVER))
        userDao.insert(User(id = "user-3", householdId = household, email = "adhd2@example.com", displayName = "ADHD User 2", role = UserRole.ADHD_USER))

        val adhdCount = userDao.getUserCountByRole(household, UserRole.ADHD_USER)
        val caregiverCount = userDao.getUserCountByRole(household, UserRole.CAREGIVER)

        assertEquals(2, adhdCount)
        assertEquals(1, caregiverCount)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteUserById() = runBlocking {
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = "John Doe"
        )

        userDao.insert(user)
        userDao.deleteUserById("user-1")

        val retrieved = userDao.getUserById("user-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteUsersByHousehold() = runBlocking {
        val household = "household-1"

        repeat(3) { i ->
            userDao.insert(User(id = "user-$i", householdId = household, email = "user$i@example.com", displayName = "User $i"))
        }

        userDao.deleteUsersByHousehold(household)

        val users = userDao.getUsersByHouseholdOnce(household)
        assertEquals(0, users.size)
    }

    // ==================== Recent Users ====================

    @Test
    fun testGetRecentUsers() = runBlocking {
        val household = "household-1"

        repeat(5) { i ->
            userDao.insert(User(id = "user-$i", householdId = household, email = "user$i@example.com", displayName = "User $i"))
        }

        val recentUsers = userDao.getRecentUsers(household, 3)

        assertEquals(3, recentUsers.size)
    }

    // ==================== First User by Role ====================

    @Test
    fun testGetFirstUserByRole() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))
        userDao.insert(User(id = "user-2", householdId = household, email = "caregiver@example.com", displayName = "Caregiver", role = UserRole.CAREGIVER))

        val firstAdhd = userDao.getFirstUserByRole(household, UserRole.ADHD_USER)
        val firstCaregiver = userDao.getFirstUserByRole(household, UserRole.CAREGIVER)

        assertNotNull(firstAdhd)
        assertNotNull(firstCaregiver)
        assertEquals(UserRole.ADHD_USER, firstAdhd.role)
        assertEquals(UserRole.CAREGIVER, firstCaregiver.role)
    }

    @Test
    fun testGetFirstUserByRoleNotFound() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))

        val firstAdmin = userDao.getFirstUserByRole(household, UserRole.ADMIN)

        assertNull(firstAdmin)
    }

    // ==================== Validation ====================

    @Test(expected = IllegalArgumentException::class)
    fun testUserValidationRejectsBlankHouseholdId() {
        User(
            id = "user-1",
            householdId = "",
            email = "john@example.com",
            displayName = "John Doe"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUserValidationRejectsBlankEmail() {
        User(
            id = "user-1",
            householdId = "household-1",
            email = "",
            displayName = "John Doe"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUserValidationRejectsBlankDisplayName() {
        User(
            id = "user-1",
            householdId = "household-1",
            email = "john@example.com",
            displayName = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUserValidationRejectsInvalidEmail() {
        User(
            id = "user-1",
            householdId = "household-1",
            email = "invalid-email",
            displayName = "John Doe"
        )
    }

    // ==================== Multiple Households ====================

    @Test
    fun testUsersIsolatedByHousehold() = runBlocking {
        val household1 = "household-1"
        val household2 = "household-2"

        userDao.insert(User(id = "user-1", householdId = household1, email = "user1@example.com", displayName = "User 1"))
        userDao.insert(User(id = "user-2", householdId = household2, email = "user2@example.com", displayName = "User 2"))

        val household1Users = userDao.getUsersByHouseholdOnce(household1)
        val household2Users = userDao.getUsersByHouseholdOnce(household2)

        assertEquals(1, household1Users.size)
        assertEquals(1, household2Users.size)
        assertEquals("user-1", household1Users[0].id)
        assertEquals("user-2", household2Users[0].id)
    }

    // ==================== Role Distribution ====================

    @Test
    fun testMultipleRolesInHousehold() = runBlocking {
        val household = "household-1"

        userDao.insert(User(id = "user-1", householdId = household, email = "adhd@example.com", displayName = "ADHD User", role = UserRole.ADHD_USER))
        userDao.insert(User(id = "user-2", householdId = household, email = "caregiver@example.com", displayName = "Caregiver", role = UserRole.CAREGIVER))
        userDao.insert(User(id = "user-3", householdId = household, email = "admin@example.com", displayName = "Admin", role = UserRole.ADMIN))

        val adhdUsers = userDao.getUsersByRoleOnce(household, UserRole.ADHD_USER)
        val caregiverUsers = userDao.getUsersByRoleOnce(household, UserRole.CAREGIVER)
        val adminUsers = userDao.getUsersByRoleOnce(household, UserRole.ADMIN)

        assertEquals(1, adhdUsers.size)
        assertEquals(1, caregiverUsers.size)
        assertEquals(1, adminUsers.size)
    }
}
