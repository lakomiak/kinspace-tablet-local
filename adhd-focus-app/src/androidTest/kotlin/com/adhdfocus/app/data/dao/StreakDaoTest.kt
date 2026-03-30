package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.Streak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for StreakDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by user, household, and streak status
 * - Date range queries
 * - Count and aggregation operations
 * - Sorting and ordering
 */
@RunWith(AndroidJUnit4::class)
class StreakDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var streakDao: StreakDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        streakDao = database.streakDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertStreak() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10,
            lastCompletionDate = LocalDate.now(),
            startDate = LocalDate.now().minusDays(5)
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakById("streak-1")

        assertNotNull(retrieved)
        assertEquals(5, retrieved.currentCount)
        assertEquals(10, retrieved.bestCount)
    }

    @Test
    fun testUpdateStreak() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10
        )

        streakDao.insert(streak)
        val updated = streak.copy(currentCount = 7, bestCount = 12)
        streakDao.update(updated)

        val retrieved = streakDao.getStreakById("streak-1")
        assertNotNull(retrieved)
        assertEquals(7, retrieved.currentCount)
        assertEquals(12, retrieved.bestCount)
    }

    @Test
    fun testDeleteStreak() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10
        )

        streakDao.insert(streak)
        streakDao.delete(streak)

        val retrieved = streakDao.getStreakById("streak-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetStreakById() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakById("streak-1")

        assertNotNull(retrieved)
        assertEquals(streak.id, retrieved.id)
        assertEquals(streak.userId, retrieved.userId)
    }

    @Test
    fun testGetNonExistentStreak() = runBlocking {
        val retrieved = streakDao.getStreakById("non-existent")
        assertNull(retrieved)
    }
}


    // ==================== Retrieve by User ====================

    @Test
    fun testGetStreakByUser() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakByUser("user-1")

        assertNotNull(retrieved)
        assertEquals("user-1", retrieved.userId)
        assertEquals(5, retrieved.currentCount)
    }

    @Test
    fun testGetStreakByUserFlow() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakByUserFlow("user-1").first()

        assertNotNull(retrieved)
        assertEquals("user-1", retrieved.userId)
    }

    @Test
    fun testGetStreakByNonExistentUser() = runBlocking {
        val retrieved = streakDao.getStreakByUser("non-existent-user")
        assertNull(retrieved)
    }

    // ==================== Retrieve by Household ====================

    @Test
    fun testGetStreaksByHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-2", currentCount = 7, bestCount = 12))

        val streaks = streakDao.getStreaksByHousehold("household-1")

        assertEquals(2, streaks.size)
        assertTrue(streaks.all { it.householdId == "household-1" })
    }

    @Test
    fun testGetStreaksByHouseholdFlow() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))

        val streaks = streakDao.getStreaksByHouseholdFlow("household-1").first()

        assertEquals(2, streaks.size)
    }

    // ==================== Retrieve All ====================

    @Test
    fun testGetAllStreaks() = runBlocking {
        repeat(3) { i ->
            streakDao.insert(Streak(id = "streak-$i", userId = "user-$i", householdId = "household-1", currentCount = i + 1, bestCount = i + 5))
        }

        val allStreaks = streakDao.getAllStreaks()

        assertEquals(3, allStreaks.size)
    }

    @Test
    fun testGetAllStreaksFlow() = runBlocking {
        repeat(3) { i ->
            streakDao.insert(Streak(id = "streak-$i", userId = "user-$i", householdId = "household-1", currentCount = i + 1, bestCount = i + 5))
        }

        val allStreaks = streakDao.getAllStreaksFlow().first()

        assertEquals(3, allStreaks.size)
    }

    // ==================== Active vs Inactive Streaks ====================

    @Test
    fun testGetActiveStreaks() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 0, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 3, bestCount = 12))

        val activeStreaks = streakDao.getActiveStreaks()

        assertEquals(2, activeStreaks.size)
        assertTrue(activeStreaks.all { it.currentCount > 0 })
    }

    @Test
    fun testGetInactiveStreaks() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 0, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 0, bestCount = 12))

        val inactiveStreaks = streakDao.getInactiveStreaks()

        assertEquals(2, inactiveStreaks.size)
        assertTrue(inactiveStreaks.all { it.currentCount == 0 })
    }

    // ==================== Filtering by Count ====================

    @Test
    fun testGetStreaksWithMinBestCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15))

        val streaks = streakDao.getStreaksWithMinBestCount(10)

        assertEquals(2, streaks.size)
        assertTrue(streaks.all { it.bestCount >= 10 })
    }

    @Test
    fun testGetStreaksWithMinCurrentCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15))

        val streaks = streakDao.getStreaksWithMinCurrentCount(5)

        assertEquals(2, streaks.size)
        assertTrue(streaks.all { it.currentCount >= 5 })
    }

    // ==================== Top Streaks ====================

    @Test
    fun testGetTopStreaksByCurrentCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 3, bestCount = 15))

        val topStreaks = streakDao.getTopStreaksByCurrentCount(2)

        assertEquals(2, topStreaks.size)
        assertEquals(10, topStreaks[0].currentCount)
        assertEquals(5, topStreaks[1].currentCount)
    }

    @Test
    fun testGetTopStreaksByBestCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 20))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 3, bestCount = 15))

        val topStreaks = streakDao.getTopStreaksByBestCount(2)

        assertEquals(2, topStreaks.size)
        assertEquals(20, topStreaks[0].bestCount)
        assertEquals(15, topStreaks[1].bestCount)
    }

    @Test
    fun testGetRecentlyUpdatedStreaks() = runBlocking {
        val now = java.time.Instant.now()
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10, updatedAt = now.minusSeconds(100)))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 20, updatedAt = now))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 3, bestCount = 15, updatedAt = now.minusSeconds(50)))

        val recentStreaks = streakDao.getRecentlyUpdatedStreaks(2)

        assertEquals(2, recentStreaks.size)
        assertEquals("streak-2", recentStreaks[0].id)
        assertEquals("streak-3", recentStreaks[1].id)
    }


    // ==================== Date Range Queries ====================

    @Test
    fun testGetStreaksCompletedInDateRange() = runBlocking {
        val today = LocalDate.now()
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10, lastCompletionDate = today.minusDays(5)))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8, lastCompletionDate = today))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15, lastCompletionDate = today.plusDays(5)))

        val streaks = streakDao.getStreaksCompletedInDateRange(today.minusDays(2), today.plusDays(2))

        assertEquals(1, streaks.size)
        assertEquals("streak-2", streaks[0].id)
    }

    @Test
    fun testGetStreaksStartedInDateRange() = runBlocking {
        val today = LocalDate.now()
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10, startDate = today.minusDays(10)))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8, startDate = today))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15, startDate = today.plusDays(5)))

        val streaks = streakDao.getStreaksStartedInDateRange(today.minusDays(2), today.plusDays(2))

        assertEquals(1, streaks.size)
        assertEquals("streak-2", streaks[0].id)
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetTotalStreakCount() = runBlocking {
        repeat(5) { i ->
            streakDao.insert(Streak(id = "streak-$i", userId = "user-$i", householdId = "household-1", currentCount = i + 1, bestCount = i + 5))
        }

        val count = streakDao.getTotalStreakCount()

        assertEquals(5, count)
    }

    @Test
    fun testGetStreakCountByHousehold() = runBlocking {
        repeat(3) { i ->
            streakDao.insert(Streak(id = "streak-$i", userId = "user-$i", householdId = "household-1", currentCount = i + 1, bestCount = i + 5))
        }
        streakDao.insert(Streak(id = "streak-other", userId = "user-other", householdId = "household-2", currentCount = 5, bestCount = 10))

        val count = streakDao.getStreakCountByHousehold("household-1")

        assertEquals(3, count)
    }

    @Test
    fun testGetActiveStreakCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 0, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 3, bestCount = 12))

        val count = streakDao.getActiveStreakCount()

        assertEquals(2, count)
    }

    @Test
    fun testGetInactiveStreakCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 0, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 0, bestCount = 12))

        val count = streakDao.getInactiveStreakCount()

        assertEquals(2, count)
    }

    @Test
    fun testGetStreakCountWithMinBestCount() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15))

        val count = streakDao.getStreakCountWithMinBestCount(10)

        assertEquals(2, count)
    }

    // ==================== Aggregation Queries ====================

    @Test
    fun testGetAverageCurrentCountByHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 15, bestCount = 12))

        val average = streakDao.getAverageCurrentCountByHousehold("household-1")

        assertNotNull(average)
        assertEquals(10.0, average)
    }

    @Test
    fun testGetAverageBestCountByHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 20))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 15, bestCount = 30))

        val average = streakDao.getAverageBestCountByHousehold("household-1")

        assertNotNull(average)
        assertEquals(20.0, average)
    }

    @Test
    fun testGetMaxBestCountByHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 20))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 15, bestCount = 30))

        val max = streakDao.getMaxBestCountByHousehold("household-1")

        assertNotNull(max)
        assertEquals(30, max)
    }

    @Test
    fun testGetMaxCurrentCountByHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 10, bestCount = 20))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 15, bestCount = 30))

        val max = streakDao.getMaxCurrentCountByHousehold("household-1")

        assertNotNull(max)
        assertEquals(15, max)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteStreakById() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.deleteStreakById("streak-1")

        val retrieved = streakDao.getStreakById("streak-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteUserStreaks() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-1", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-2", householdId = "household-1", currentCount = 7, bestCount = 12))

        streakDao.deleteUserStreaks("user-1")

        val user1Streaks = streakDao.getStreakByUser("user-1")
        val user2Streaks = streakDao.getStreakByUser("user-2")

        assertNull(user1Streaks)
        assertNotNull(user2Streaks)
    }

    @Test
    fun testDeleteHouseholdStreaks() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-2", currentCount = 7, bestCount = 12))

        streakDao.deleteHouseholdStreaks("household-1")

        val household1Streaks = streakDao.getStreaksByHousehold("household-1")
        val household2Streaks = streakDao.getStreaksByHousehold("household-2")

        assertEquals(0, household1Streaks.size)
        assertEquals(1, household2Streaks.size)
    }

    @Test
    fun testDeleteInactiveStreaks() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 0, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 0, bestCount = 12))

        streakDao.deleteInactiveStreaks()

        val activeStreaks = streakDao.getActiveStreaks()
        val inactiveStreaks = streakDao.getInactiveStreaks()

        assertEquals(1, activeStreaks.size)
        assertEquals(0, inactiveStreaks.size)
    }

    @Test
    fun testDeleteAllStreaks() = runBlocking {
        repeat(5) { i ->
            streakDao.insert(Streak(id = "streak-$i", userId = "user-$i", householdId = "household-1", currentCount = i + 1, bestCount = i + 5))
        }

        streakDao.deleteAllStreaks()

        val count = streakDao.getTotalStreakCount()
        assertEquals(0, count)
    }

    // ==================== Batch Operations ====================

    @Test
    fun testInsertAll() = runBlocking {
        val streaks = listOf(
            Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10),
            Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8),
            Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15)
        )

        streakDao.insertAll(streaks)

        val count = streakDao.getTotalStreakCount()
        assertEquals(3, count)
    }

    @Test
    fun testUpdateAll() = runBlocking {
        val streaks = listOf(
            Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10),
            Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8)
        )

        streakDao.insertAll(streaks)

        val updated = streaks.map { it.copy(currentCount = it.currentCount + 1) }
        streakDao.updateAll(updated)

        val retrieved1 = streakDao.getStreakById("streak-1")
        val retrieved2 = streakDao.getStreakById("streak-2")

        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals(6, retrieved1.currentCount)
        assertEquals(4, retrieved2.currentCount)
    }

    @Test
    fun testGetStreaksByUserIds() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))
        streakDao.insert(Streak(id = "streak-3", userId = "user-3", householdId = "household-1", currentCount = 7, bestCount = 15))

        val streaks = streakDao.getStreaksByUserIds(listOf("user-1", "user-3"))

        assertEquals(2, streaks.size)
        assertTrue(streaks.all { it.userId in listOf("user-1", "user-3") })
    }

    @Test
    fun testGetStreaksByUserIdsFlow() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))
        streakDao.insert(Streak(id = "streak-2", userId = "user-2", householdId = "household-1", currentCount = 3, bestCount = 8))

        val streaks = streakDao.getStreaksByUserIdsFlow(listOf("user-1", "user-2")).first()

        assertEquals(2, streaks.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyResultsForNonExistentHousehold() = runBlocking {
        streakDao.insert(Streak(id = "streak-1", userId = "user-1", householdId = "household-1", currentCount = 5, bestCount = 10))

        val streaks = streakDao.getStreaksByHousehold("non-existent-household")

        assertEquals(0, streaks.size)
    }

    @Test
    fun testStreakWithNullDates() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5,
            bestCount = 10,
            lastCompletionDate = null,
            startDate = null
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakById("streak-1")

        assertNotNull(retrieved)
        assertNull(retrieved.lastCompletionDate)
        assertNull(retrieved.startDate)
    }

    @Test
    fun testStreakWithZeroCounts() = runBlocking {
        val streak = Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 0,
            bestCount = 0
        )

        streakDao.insert(streak)
        val retrieved = streakDao.getStreakById("streak-1")

        assertNotNull(retrieved)
        assertEquals(0, retrieved.currentCount)
        assertEquals(0, retrieved.bestCount)
    }

    @Test
    fun testAggregationWithEmptyHousehold() = runBlocking {
        val average = streakDao.getAverageCurrentCountByHousehold("non-existent-household")
        assertNull(average)
    }
}
