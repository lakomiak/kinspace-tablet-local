package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.Badge
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
 * Unit tests for BadgeDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by user, household, and badge type
 * - Earned vs locked badge filtering
 * - Date range queries
 * - Count operations
 * - Sorting and ordering
 */
@RunWith(AndroidJUnit4::class)
class BadgeDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var badgeDao: BadgeDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        badgeDao = database.badgeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertBadge() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task Complete",
            description = "Completed your first task",
            isLocked = false
        )

        badgeDao.insert(badge)
        val retrieved = badgeDao.getBadgeById("badge-1")

        assertNotNull(retrieved)
        assertEquals("First Task Complete", retrieved.name)
        assertEquals("FIRST_TASK", retrieved.badgeType)
        assertEquals(false, retrieved.isLocked)
    }

    @Test
    fun testUpdateBadge() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task Complete",
            isLocked = false
        )

        badgeDao.insert(badge)
        val updated = badge.copy(name = "Updated Name", progress = 50)
        badgeDao.update(updated)

        val retrieved = badgeDao.getBadgeById("badge-1")
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
        assertEquals(50, retrieved.progress)
    }

    @Test
    fun testDeleteBadge() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task Complete",
            isLocked = false
        )

        badgeDao.insert(badge)
        badgeDao.delete(badge)

        val retrieved = badgeDao.getBadgeById("badge-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetBadgeById() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task Complete",
            isLocked = false
        )

        badgeDao.insert(badge)
        val retrieved = badgeDao.getBadgeById("badge-1")

        assertNotNull(retrieved)
        assertEquals(badge.id, retrieved.id)
        assertEquals(badge.name, retrieved.name)
    }

    @Test
    fun testGetNonExistentBadge() = runBlocking {
        val retrieved = badgeDao.getBadgeById("non-existent")
        assertNull(retrieved)
    }
}


    // ==================== Filtering by User ====================

    @Test
    fun testGetEarnedBadgesByUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val earnedBadges = badgeDao.getEarnedBadgesByUserOnce("user-1")

        assertEquals(1, earnedBadges.size)
        assertEquals("badge-1", earnedBadges[0].id)
        assertTrue(earnedBadges.all { !it.isLocked })
    }

    @Test
    fun testGetEarnedBadgesByUserFlow() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))

        val earnedBadges = badgeDao.getEarnedBadgesByUser("user-1").first()

        assertEquals(1, earnedBadges.size)
        assertEquals(false, earnedBadges[0].isLocked)
    }

    @Test
    fun testGetLockedBadgesByUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true))

        val lockedBadges = badgeDao.getLockedBadgesByUserOnce("user-1")

        assertEquals(2, lockedBadges.size)
        assertTrue(lockedBadges.all { it.isLocked })
    }

    @Test
    fun testGetLockedBadgesByUserFlow() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = true))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))

        val lockedBadges = badgeDao.getLockedBadgesByUser("user-1").first()

        assertEquals(1, lockedBadges.size)
        assertEquals(true, lockedBadges[0].isLocked)
    }

    @Test
    fun testGetBadgesByUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val userBadges = badgeDao.getBadgesByUserOnce("user-1")

        assertEquals(2, userBadges.size)
        assertTrue(userBadges.all { it.userId == "user-1" })
    }

    @Test
    fun testGetBadgesByUserFlow() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))

        val userBadges = badgeDao.getBadgesByUser("user-1").first()

        assertEquals(2, userBadges.size)
    }

    // ==================== Filtering by Household ====================

    @Test
    fun testGetEarnedBadgesByHousehold() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-2", userId = "user-3", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val earnedBadges = badgeDao.getEarnedBadgesByHousehold("household-1")

        assertEquals(2, earnedBadges.size)
        assertTrue(earnedBadges.all { it.householdId == "household-1" && !it.isLocked })
    }

    @Test
    fun testGetLockedBadgesByHousehold() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = true))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = false))

        val lockedBadges = badgeDao.getLockedBadgesByHousehold("household-1")

        assertEquals(2, lockedBadges.size)
        assertTrue(lockedBadges.all { it.householdId == "household-1" && it.isLocked })
    }

    // ==================== Filtering by Badge Type ====================

    @Test
    fun testGetBadgesByType() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))

        val firstTaskBadges = badgeDao.getBadgesByType("FIRST_TASK")

        assertEquals(2, firstTaskBadges.size)
        assertTrue(firstTaskBadges.all { it.badgeType == "FIRST_TASK" })
    }

    @Test
    fun testGetBadgesByTypeFlow() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))

        val firstTaskBadges = badgeDao.getBadgesByTypeFlow("FIRST_TASK").first()

        assertEquals(1, firstTaskBadges.size)
        assertEquals("FIRST_TASK", firstTaskBadges[0].badgeType)
    }

    @Test
    fun testGetBadgesByUserAndType() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val userFirstTaskBadges = badgeDao.getBadgesByUserAndType("user-1", "FIRST_TASK")

        assertEquals(1, userFirstTaskBadges.size)
        assertEquals("user-1", userFirstTaskBadges[0].userId)
        assertEquals("FIRST_TASK", userFirstTaskBadges[0].badgeType)
    }

    // ==================== Date Range Queries ====================

    @Test
    fun testGetBadgesInDateRange() = runBlocking {
        val now = Instant.now()

        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false, earnedAt = now.minusSeconds(3600)))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false, earnedAt = now))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = false, earnedAt = now.plusSeconds(3600)))

        val badgesInRange = badgeDao.getBadgesInDateRange(now.minusSeconds(1800), now.plusSeconds(1800))

        assertEquals(1, badgesInRange.size)
        assertEquals("badge-2", badgesInRange[0].id)
    }

    @Test
    fun testGetUserBadgesInDateRange() = runBlocking {
        val now = Instant.now()

        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false, earnedAt = now.minusSeconds(3600)))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false, earnedAt = now))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false, earnedAt = now))

        val userBadgesInRange = badgeDao.getUserBadgesInDateRange("user-1", now.minusSeconds(1800), now.plusSeconds(1800))

        assertEquals(1, userBadgesInRange.size)
        assertEquals("user-1", userBadgesInRange[0].userId)
        assertEquals("badge-2", userBadgesInRange[0].id)
    }


    // ==================== Get All Badges ====================

    @Test
    fun testGetAllBadges() = runBlocking {
        repeat(3) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }

        val allBadges = badgeDao.getAllBadges()

        assertEquals(3, allBadges.size)
    }

    @Test
    fun testGetAllBadgesFlow() = runBlocking {
        repeat(3) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }

        val allBadges = badgeDao.getAllBadgesFlow().first()

        assertEquals(3, allBadges.size)
    }

    @Test
    fun testGetAllEarnedBadges() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = false))

        val earnedBadges = badgeDao.getAllEarnedBadges()

        assertEquals(2, earnedBadges.size)
        assertTrue(earnedBadges.all { !it.isLocked })
    }

    @Test
    fun testGetAllLockedBadges() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true))

        val lockedBadges = badgeDao.getAllLockedBadges()

        assertEquals(2, lockedBadges.size)
        assertTrue(lockedBadges.all { it.isLocked })
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetBadgeCountByUser() = runBlocking {
        repeat(3) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }
        badgeDao.insert(Badge(id = "badge-other", householdId = "household-1", userId = "user-2", badgeType = "TYPE_OTHER", name = "Other Badge", isLocked = false))

        val count = badgeDao.getBadgeCountByUser("user-1")

        assertEquals(3, count)
    }

    @Test
    fun testGetEarnedBadgeCountByUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true))

        val count = badgeDao.getEarnedBadgeCountByUser("user-1")

        assertEquals(2, count)
    }

    @Test
    fun testGetLockedBadgeCountByUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true))

        val count = badgeDao.getLockedBadgeCountByUser("user-1")

        assertEquals(2, count)
    }

    @Test
    fun testGetBadgeCountByHousehold() = runBlocking {
        repeat(3) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }
        badgeDao.insert(Badge(id = "badge-other", householdId = "household-2", userId = "user-2", badgeType = "TYPE_OTHER", name = "Other Badge", isLocked = false))

        val count = badgeDao.getBadgeCountByHousehold("household-1")

        assertEquals(3, count)
    }

    @Test
    fun testGetEarnedBadgeCountByHousehold() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true))

        val count = badgeDao.getEarnedBadgeCountByHousehold("household-1")

        assertEquals(2, count)
    }

    @Test
    fun testGetBadgeCountByType() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))

        val count = badgeDao.getBadgeCountByType("FIRST_TASK")

        assertEquals(2, count)
    }

    @Test
    fun testGetTotalBadgeCount() = runBlocking {
        repeat(5) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }

        val count = badgeDao.getTotalBadgeCount()

        assertEquals(5, count)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteBadgeById() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.deleteBadgeById("badge-1")

        val retrieved = badgeDao.getBadgeById("badge-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteUserBadges() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-2", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        badgeDao.deleteUserBadges("user-1")

        val user1Badges = badgeDao.getBadgesByUserOnce("user-1")
        val user2Badges = badgeDao.getBadgesByUserOnce("user-2")

        assertEquals(0, user1Badges.size)
        assertEquals(1, user2Badges.size)
    }

    @Test
    fun testDeleteUserBadgesByType() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        badgeDao.deleteUserBadgesByType("user-1", "FIRST_TASK")

        val remainingBadges = badgeDao.getBadgesByUserOnce("user-1")

        assertEquals(1, remainingBadges.size)
        assertEquals("FIVE_TASKS", remainingBadges[0].badgeType)
    }

    @Test
    fun testDeleteHouseholdBadges() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-2", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-2", userId = "user-3", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        badgeDao.deleteHouseholdBadges("household-1")

        val household1Badges = badgeDao.getEarnedBadgesByHousehold("household-1")
        val household2Badges = badgeDao.getEarnedBadgesByHousehold("household-2")

        assertEquals(0, household1Badges.size)
        assertEquals(1, household2Badges.size)
    }

    @Test
    fun testDeleteAllBadges() = runBlocking {
        repeat(5) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false))
        }

        badgeDao.deleteAllBadges()

        val count = badgeDao.getTotalBadgeCount()
        assertEquals(0, count)
    }

    // ==================== Recent and Progress Queries ====================

    @Test
    fun testGetRecentEarnedBadges() = runBlocking {
        val now = Instant.now()

        repeat(5) { i ->
            badgeDao.insert(Badge(id = "badge-$i", householdId = "household-1", userId = "user-1", badgeType = "TYPE_$i", name = "Badge $i", isLocked = false, earnedAt = now.minusSeconds((5 - i) * 100L)))
        }

        val recentBadges = badgeDao.getRecentEarnedBadges("user-1", 3)

        assertEquals(3, recentBadges.size)
    }

    @Test
    fun testGetBadgesWithProgress() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false, progress = null))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = true, progress = 50))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = true, progress = 75))

        val badgesWithProgress = badgeDao.getBadgesWithProgress("user-1")

        assertEquals(2, badgesWithProgress.size)
        assertTrue(badgesWithProgress.all { it.progress != null })
    }

    // ==================== Ordering ====================

    @Test
    fun testEarnedBadgesOrderedByEarnedAtDescending() = runBlocking {
        val now = Instant.now()

        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false, earnedAt = now.minusSeconds(100)))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Five Tasks", isLocked = false, earnedAt = now))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Week Warrior", isLocked = false, earnedAt = now.minusSeconds(50)))

        val badges = badgeDao.getEarnedBadgesByUserOnce("user-1")

        assertEquals("badge-2", badges[0].id)
        assertEquals("badge-3", badges[1].id)
        assertEquals("badge-1", badges[2].id)
    }

    @Test
    fun testLockedBadgesOrderedByNameAscending() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "Zebra Badge", isLocked = true))
        badgeDao.insert(Badge(id = "badge-2", householdId = "household-1", userId = "user-1", badgeType = "FIVE_TASKS", name = "Apple Badge", isLocked = true))
        badgeDao.insert(Badge(id = "badge-3", householdId = "household-1", userId = "user-1", badgeType = "WEEK_WARRIOR", name = "Mango Badge", isLocked = true))

        val badges = badgeDao.getLockedBadgesByUserOnce("user-1")

        assertEquals("Apple Badge", badges[0].name)
        assertEquals("Mango Badge", badges[1].name)
        assertEquals("Zebra Badge", badges[2].name)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyResultsForNonExistentUser() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val badges = badgeDao.getBadgesByUserOnce("non-existent-user")

        assertEquals(0, badges.size)
    }

    @Test
    fun testEmptyResultsForNonExistentType() = runBlocking {
        badgeDao.insert(Badge(id = "badge-1", householdId = "household-1", userId = "user-1", badgeType = "FIRST_TASK", name = "First Task", isLocked = false))

        val badges = badgeDao.getBadgesByType("NON_EXISTENT_TYPE")

        assertEquals(0, badges.size)
    }

    @Test
    fun testBadgeWithNullProgress() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task",
            isLocked = false,
            progress = null
        )

        badgeDao.insert(badge)
        val retrieved = badgeDao.getBadgeById("badge-1")

        assertNotNull(retrieved)
        assertNull(retrieved.progress)
    }

    @Test
    fun testBadgeWithNullDescription() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task",
            description = null,
            isLocked = false
        )

        badgeDao.insert(badge)
        val retrieved = badgeDao.getBadgeById("badge-1")

        assertNotNull(retrieved)
        assertNull(retrieved.description)
    }

    @Test
    fun testBadgeWithNullIconUrl() = runBlocking {
        val badge = Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "FIRST_TASK",
            name = "First Task",
            iconUrl = null,
            isLocked = false
        )

        badgeDao.insert(badge)
        val retrieved = badgeDao.getBadgeById("badge-1")

        assertNotNull(retrieved)
        assertNull(retrieved.iconUrl)
    }
}
