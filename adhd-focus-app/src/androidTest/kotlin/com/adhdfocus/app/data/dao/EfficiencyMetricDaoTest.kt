package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.EfficiencyMetric
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
 * Unit tests for EfficiencyMetricDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by user, household, and efficiency ranges
 * - Date range queries
 * - Count and aggregation operations
 * - Sorting and ordering
 */
@RunWith(AndroidJUnit4::class)
class EfficiencyMetricDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var efficiencyMetricDao: EfficiencyMetricDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        efficiencyMetricDao = database.efficiencyMetricDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertMetric() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        val retrieved = efficiencyMetricDao.getMetricById("metric-1")

        assertNotNull(retrieved)
        assertEquals(83.33f, retrieved.efficiencyPercentage)
        assertEquals(25, retrieved.actualDurationMinutes)
    }

    @Test
    fun testUpdateMetric() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        val updated = metric.copy(actualDurationMinutes = 20, efficiencyPercentage = 66.67f)
        efficiencyMetricDao.update(updated)

        val retrieved = efficiencyMetricDao.getMetricById("metric-1")
        assertNotNull(retrieved)
        assertEquals(66.67f, retrieved.efficiencyPercentage)
        assertEquals(20, retrieved.actualDurationMinutes)
    }

    @Test
    fun testDeleteMetric() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        efficiencyMetricDao.delete(metric)

        val retrieved = efficiencyMetricDao.getMetricById("metric-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetMetricById() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        val retrieved = efficiencyMetricDao.getMetricById("metric-1")

        assertNotNull(retrieved)
        assertEquals(metric.id, retrieved.id)
        assertEquals(metric.taskId, retrieved.taskId)
    }

    @Test
    fun testGetNonExistentMetric() = runBlocking {
        val retrieved = efficiencyMetricDao.getMetricById("non-existent")
        assertNull(retrieved)
    }
}

    // ==================== Retrieve by Task ====================

    @Test
    fun testGetMetricByTask() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        val retrieved = efficiencyMetricDao.getMetricByTask("task-1")

        assertNotNull(retrieved)
        assertEquals("task-1", retrieved.taskId)
        assertEquals(83.33f, retrieved.efficiencyPercentage)
    }

    @Test
    fun testGetMetricByTaskFlow() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            efficiencyPercentage = 83.33f
        )

        efficiencyMetricDao.insert(metric)
        val retrieved = efficiencyMetricDao.getMetricByTaskFlow("task-1").first()

        assertNotNull(retrieved)
        assertEquals("task-1", retrieved.taskId)
    }

    // ==================== Retrieve by User ====================

    @Test
    fun testGetMetricsByUser() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsByUser("user-1")

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.userId == "user-1" })
    }

    @Test
    fun testGetMetricsByUserFlow() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))

        val metrics = efficiencyMetricDao.getMetricsByUserFlow("user-1").first()

        assertEquals(2, metrics.size)
    }

    // ==================== Retrieve by Household ====================

    @Test
    fun testGetMetricsByHousehold() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-3", householdId = "household-2", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsByHousehold("household-1")

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.householdId == "household-1" })
    }

    @Test
    fun testGetMetricsByHouseholdFlow() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))

        val metrics = efficiencyMetricDao.getMetricsByHouseholdFlow("household-1").first()

        assertEquals(2, metrics.size)
    }

    // ==================== Retrieve All ====================

    @Test
    fun testGetAllMetrics() = runBlocking {
        repeat(3) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-$i", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }

        val allMetrics = efficiencyMetricDao.getAllMetrics()

        assertEquals(3, allMetrics.size)
    }

    @Test
    fun testGetAllMetricsFlow() = runBlocking {
        repeat(3) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-$i", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }

        val allMetrics = efficiencyMetricDao.getAllMetricsFlow().first()

        assertEquals(3, allMetrics.size)
    }
}

    // ==================== Filtering by Efficiency ====================

    @Test
    fun testGetMetricsWithMinEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsWithMinEfficiency("user-1", 80.0f)

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.efficiencyPercentage!! >= 80.0f })
    }

    @Test
    fun testGetMetricsWithMaxEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsWithMaxEfficiency("user-1", 85.0f)

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.efficiencyPercentage!! <= 85.0f })
    }

    @Test
    fun testGetMetricsInEfficiencyRange() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsInEfficiencyRange("user-1", 80.0f, 85.0f)

        assertEquals(1, metrics.size)
        assertEquals(83.33f, metrics[0].efficiencyPercentage)
    }

    // ==================== Sorting ====================

    @Test
    fun testGetTopMetricsByEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val topMetrics = efficiencyMetricDao.getTopMetricsByEfficiency("user-1", 2)

        assertEquals(2, topMetrics.size)
        assertEquals(87.5f, topMetrics[0].efficiencyPercentage)
        assertEquals(83.33f, topMetrics[1].efficiencyPercentage)
    }

    @Test
    fun testGetRecentMetrics() = runBlocking {
        val now = Instant.now()
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f, completedAt = now.minusSeconds(100)))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f, completedAt = now))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f, completedAt = now.minusSeconds(50)))

        val recentMetrics = efficiencyMetricDao.getRecentMetrics("user-1", 2)

        assertEquals(2, recentMetrics.size)
        assertEquals("metric-2", recentMetrics[0].id)
        assertEquals("metric-3", recentMetrics[1].id)
    }

    // ==================== Date Range Queries ====================

    @Test
    fun testGetMetricsInDateRange() = runBlocking {
        val now = Instant.now()
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f, completedAt = now.minusSeconds(86400)))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f, completedAt = now))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f, completedAt = now.plusSeconds(86400)))

        val metrics = efficiencyMetricDao.getMetricsInDateRange("user-1", now.minusSeconds(43200), now.plusSeconds(43200))

        assertEquals(1, metrics.size)
        assertEquals("metric-2", metrics[0].id)
    }

    @Test
    fun testGetHouseholdMetricsInDateRange() = runBlocking {
        val now = Instant.now()
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f, completedAt = now.minusSeconds(86400)))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f, completedAt = now))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-3", householdId = "household-2", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f, completedAt = now))

        val metrics = efficiencyMetricDao.getHouseholdMetricsInDateRange("household-1", now.minusSeconds(43200), now.plusSeconds(43200))

        assertEquals(1, metrics.size)
        assertEquals("metric-2", metrics[0].id)
    }

    // ==================== Aggregation Queries ====================

    @Test
    fun testGetAverageEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 100.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 80.0f))

        val average = efficiencyMetricDao.getAverageEfficiency("user-1")

        assertNotNull(average)
        assertEquals(86.67f, average!!, 0.1f)
    }

    @Test
    fun testGetAverageEfficiencyByHousehold() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 100.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-3", householdId = "household-2", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 80.0f))

        val average = efficiencyMetricDao.getAverageEfficiencyByHousehold("household-1")

        assertNotNull(average)
        assertEquals(90.0f, average!!, 0.1f)
    }

    @Test
    fun testGetMaxEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 100.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 85.0f))

        val max = efficiencyMetricDao.getMaxEfficiency("user-1")

        assertNotNull(max)
        assertEquals(100.0f, max)
    }

    @Test
    fun testGetMinEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 100.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 85.0f))

        val min = efficiencyMetricDao.getMinEfficiency("user-1")

        assertNotNull(min)
        assertEquals(80.0f, min)
    }
}

    // ==================== Count Operations ====================

    @Test
    fun testGetTotalMetricCount() = runBlocking {
        repeat(5) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-$i", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }

        val count = efficiencyMetricDao.getTotalMetricCount()

        assertEquals(5, count)
    }

    @Test
    fun testGetMetricCountByUser() = runBlocking {
        repeat(3) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-other", taskId = "task-other", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))

        val count = efficiencyMetricDao.getMetricCountByUser("user-1")

        assertEquals(3, count)
    }

    @Test
    fun testGetMetricCountByHousehold() = runBlocking {
        repeat(3) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-$i", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-other", taskId = "task-other", userId = "user-other", householdId = "household-2", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 80.0f))

        val count = efficiencyMetricDao.getMetricCountByHousehold("household-1")

        assertEquals(3, count)
    }

    @Test
    fun testGetMetricCountWithMinEfficiency() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val count = efficiencyMetricDao.getMetricCountWithMinEfficiency("user-1", 80.0f)

        assertEquals(2, count)
    }

    @Test
    fun testGetMetricCountInDateRange() = runBlocking {
        val now = Instant.now()
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f, completedAt = now.minusSeconds(86400)))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f, completedAt = now))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f, completedAt = now.plusSeconds(86400)))

        val count = efficiencyMetricDao.getMetricCountInDateRange("user-1", now.minusSeconds(43200), now.plusSeconds(43200))

        assertEquals(1, count)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteMetricById() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.deleteMetricById("metric-1")

        val retrieved = efficiencyMetricDao.getMetricById("metric-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteUserMetrics() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        efficiencyMetricDao.deleteUserMetrics("user-1")

        val user1Metrics = efficiencyMetricDao.getMetricsByUser("user-1")
        val user2Metrics = efficiencyMetricDao.getMetricsByUser("user-2")

        assertEquals(0, user1Metrics.size)
        assertEquals(1, user2Metrics.size)
    }

    @Test
    fun testDeleteHouseholdMetrics() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-3", householdId = "household-2", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        efficiencyMetricDao.deleteHouseholdMetrics("household-1")

        val household1Metrics = efficiencyMetricDao.getMetricsByHousehold("household-1")
        val household2Metrics = efficiencyMetricDao.getMetricsByHousehold("household-2")

        assertEquals(0, household1Metrics.size)
        assertEquals(1, household2Metrics.size)
    }

    @Test
    fun testDeleteTaskMetrics() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        efficiencyMetricDao.deleteTaskMetrics("task-1")

        val task1Metrics = efficiencyMetricDao.getMetricsByTaskIds(listOf("task-1"))
        val task2Metrics = efficiencyMetricDao.getMetricsByTaskIds(listOf("task-2"))

        assertEquals(0, task1Metrics.size)
        assertEquals(1, task2Metrics.size)
    }

    @Test
    fun testDeleteAllMetrics() = runBlocking {
        repeat(5) { i ->
            efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-$i", taskId = "task-$i", userId = "user-$i", householdId = "household-1", estimatedDurationMinutes = 30 + i, actualDurationMinutes = 25 + i, efficiencyPercentage = 80.0f + i))
        }

        efficiencyMetricDao.deleteAllMetrics()

        val count = efficiencyMetricDao.getTotalMetricCount()
        assertEquals(0, count)
    }

    // ==================== Batch Operations ====================

    @Test
    fun testInsertAll() = runBlocking {
        val metrics = listOf(
            EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f),
            EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f),
            EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f)
        )

        efficiencyMetricDao.insertAll(metrics)

        val count = efficiencyMetricDao.getTotalMetricCount()
        assertEquals(3, count)
    }

    @Test
    fun testUpdateAll() = runBlocking {
        val metrics = listOf(
            EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f),
            EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f)
        )

        efficiencyMetricDao.insertAll(metrics)

        val updated = metrics.map { it.copy(efficiencyPercentage = it.efficiencyPercentage!! + 5.0f) }
        efficiencyMetricDao.updateAll(updated)

        val retrieved1 = efficiencyMetricDao.getMetricById("metric-1")
        val retrieved2 = efficiencyMetricDao.getMetricById("metric-2")

        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals(88.33f, retrieved1.efficiencyPercentage!!, 0.1f)
        assertEquals(80.0f, retrieved2.efficiencyPercentage!!, 0.1f)
    }

    @Test
    fun testGetMetricsByUserIds() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-2", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-3", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsByUserIds(listOf("user-1", "user-3"))

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.userId in listOf("user-1", "user-3") })
    }

    @Test
    fun testGetMetricsByTaskIds() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-2", taskId = "task-2", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 20, actualDurationMinutes = 15, efficiencyPercentage = 75.0f))
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-3", taskId = "task-3", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 40, actualDurationMinutes = 35, efficiencyPercentage = 87.5f))

        val metrics = efficiencyMetricDao.getMetricsByTaskIds(listOf("task-1", "task-3"))

        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.taskId in listOf("task-1", "task-3") })
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyResultsForNonExistentUser() = runBlocking {
        efficiencyMetricDao.insert(EfficiencyMetric(id = "metric-1", taskId = "task-1", userId = "user-1", householdId = "household-1", estimatedDurationMinutes = 30, actualDurationMinutes = 25, efficiencyPercentage = 83.33f))

        val metrics = efficiencyMetricDao.getMetricsByUser("non-existent-user")

        assertEquals(0, metrics.size)
    }

    @Test
    fun testMetricWithNullEfficiency() = runBlocking {
        val metric = EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            efficiencyPercentage = null
        )

        efficiencyMetricDao.insert(metric)
        val retrieved = efficiencyMetricDao.getMetricById("metric-1")

        assertNotNull(retrieved)
        assertNull(retrieved.actualDurationMinutes)
        assertNull(retrieved.efficiencyPercentage)
    }

    @Test
    fun testAggregationWithEmptyHousehold() = runBlocking {
        val average = efficiencyMetricDao.getAverageEfficiencyByHousehold("non-existent-household")
        assertNull(average)
    }
}