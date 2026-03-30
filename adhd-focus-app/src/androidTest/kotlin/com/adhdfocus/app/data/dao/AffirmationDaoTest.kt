package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
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
 * Unit tests for AffirmationDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by type, tone, and age appropriateness level
 * - Random affirmation selection
 * - Count operations
 * - Date range queries
 */
@RunWith(AndroidJUnit4::class)
class AffirmationDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var affirmationDao: AffirmationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        affirmationDao = database.affirmationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertAffirmation() = runBlocking {
        val affirmation = Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great job!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )

        affirmationDao.insert(affirmation)
        val retrieved = affirmationDao.getAffirmationById("aff-1")

        assertNotNull(retrieved)
        assertEquals("Great job!", retrieved.message)
        assertEquals(AffirmationType.TASK_COMPLETION, retrieved.type)
    }

    @Test
    fun testUpdateAffirmation() = runBlocking {
        val affirmation = Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Original message",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )

        affirmationDao.insert(affirmation)
        val updated = affirmation.copy(message = "Updated message", tone = AffirmationTone.CELEBRATORY)
        affirmationDao.update(updated)

        val retrieved = affirmationDao.getAffirmationById("aff-1")
        assertNotNull(retrieved)
        assertEquals("Updated message", retrieved.message)
        assertEquals(AffirmationTone.CELEBRATORY, retrieved.tone)
    }

    @Test
    fun testDeleteAffirmation() = runBlocking {
        val affirmation = Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great job!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )

        affirmationDao.insert(affirmation)
        affirmationDao.delete(affirmation)

        val retrieved = affirmationDao.getAffirmationById("aff-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetAffirmationById() = runBlocking {
        val affirmation = Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great job!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )

        affirmationDao.insert(affirmation)
        val retrieved = affirmationDao.getAffirmationById("aff-1")

        assertNotNull(retrieved)
        assertEquals(affirmation.id, retrieved.id)
        assertEquals(affirmation.message, retrieved.message)
    }

    @Test
    fun testGetNonExistentAffirmation() = runBlocking {
        val retrieved = affirmationDao.getAffirmationById("non-existent")
        assertNull(retrieved)
    }

    // ==================== Filtering by Type ====================

    @Test
    fun testGetAffirmationsByType() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.DAY_COMPLETION, message = "Perfect day!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.MOTIVATIONAL, ageAppropriatenessLevel = 3))

        val taskCompletionAffirmations = affirmationDao.getAffirmationsByType(AffirmationType.TASK_COMPLETION)
        val dayCompletionAffirmations = affirmationDao.getAffirmationsByType(AffirmationType.DAY_COMPLETION)

        assertEquals(2, taskCompletionAffirmations.size)
        assertEquals(1, dayCompletionAffirmations.size)
        assertTrue(taskCompletionAffirmations.all { it.type == AffirmationType.TASK_COMPLETION })
    }

    @Test
    fun testGetAffirmationsByTypeFlow() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.DAY_COMPLETION, message = "Perfect day!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))

        val taskCompletionAffirmations = affirmationDao.getAffirmationsByTypeFlow(AffirmationType.TASK_COMPLETION).first()

        assertEquals(1, taskCompletionAffirmations.size)
        assertEquals(AffirmationType.TASK_COMPLETION, taskCompletionAffirmations[0].type)
    }

    // ==================== Filtering by Tone ====================

    @Test
    fun testGetAffirmationsByTone() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.TASK_COMPLETION, message = "Nice!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val encouragingAffirmations = affirmationDao.getAffirmationsByTone(AffirmationTone.ENCOURAGING)
        val celebratoryAffirmations = affirmationDao.getAffirmationsByTone(AffirmationTone.CELEBRATORY)

        assertEquals(2, encouragingAffirmations.size)
        assertEquals(1, celebratoryAffirmations.size)
        assertTrue(encouragingAffirmations.all { it.tone == AffirmationTone.ENCOURAGING })
    }

    @Test
    fun testGetAffirmationsByToneFlow() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))

        val encouragingAffirmations = affirmationDao.getAffirmationsByToneFlow(AffirmationTone.ENCOURAGING).first()

        assertEquals(1, encouragingAffirmations.size)
        assertEquals(AffirmationTone.ENCOURAGING, encouragingAffirmations[0].tone)
    }

    // ==================== Filtering by Type and Tone ====================

    @Test
    fun testGetAffirmationsByTypeAndTone() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.DAY_COMPLETION, message = "Perfect!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val taskCompletionEncouraging = affirmationDao.getAffirmationsByTypeAndTone(AffirmationType.TASK_COMPLETION, AffirmationTone.ENCOURAGING)

        assertEquals(1, taskCompletionEncouraging.size)
        assertEquals(AffirmationType.TASK_COMPLETION, taskCompletionEncouraging[0].type)
        assertEquals(AffirmationTone.ENCOURAGING, taskCompletionEncouraging[0].tone)
    }

    @Test
    fun testGetAffirmationsByTypeAndToneFlow() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))

        val taskCompletionEncouraging = affirmationDao.getAffirmationsByTypeAndToneFlow(AffirmationType.TASK_COMPLETION, AffirmationTone.ENCOURAGING).first()

        assertEquals(1, taskCompletionEncouraging.size)
    }

    // ==================== Filtering by Age Level ====================

    @Test
    fun testGetAffirmationsByAgeLevel() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 1))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.TASK_COMPLETION, message = "Nice!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 5))

        val ageLevelAffirmations = affirmationDao.getAffirmationsByAgeLevel(2, 4)

        assertEquals(1, ageLevelAffirmations.size)
        assertEquals(3, ageLevelAffirmations[0].ageAppropriatenessLevel)
    }

    @Test
    fun testGetAffirmationsByAgeLevelFlow() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 1))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val ageLevelAffirmations = affirmationDao.getAffirmationsByAgeLevelFlow(2, 4).first()

        assertEquals(1, ageLevelAffirmations.size)
    }

    // ==================== Filtering by Type and Age Level ====================

    @Test
    fun testGetAffirmationsByTypeAndAgeLevel() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 1))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.DAY_COMPLETION, message = "Perfect!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val taskCompletionAgeLevel = affirmationDao.getAffirmationsByTypeAndAgeLevel(AffirmationType.TASK_COMPLETION, 2, 4)

        assertEquals(1, taskCompletionAgeLevel.size)
        assertEquals(AffirmationType.TASK_COMPLETION, taskCompletionAgeLevel[0].type)
        assertEquals(3, taskCompletionAgeLevel[0].ageAppropriatenessLevel)
    }

    @Test
    fun testGetAffirmationsByTypeAndAgeLevelFlow() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 1))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val taskCompletionAgeLevel = affirmationDao.getAffirmationsByTypeAndAgeLevelFlow(AffirmationType.TASK_COMPLETION, 2, 4).first()

        assertEquals(1, taskCompletionAgeLevel.size)
    }

    // ==================== Date Range Queries ====================

    @Test
    fun testGetAffirmationsInDateRange() = runBlocking {
        val now = Instant.now()

        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now.minusSeconds(3600)))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.TASK_COMPLETION, message = "Nice!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now.plusSeconds(3600)))

        val affirmationsInRange = affirmationDao.getAffirmationsInDateRange(now.minusSeconds(1800), now.plusSeconds(1800))

        assertEquals(1, affirmationsInRange.size)
        assertEquals("aff-2", affirmationsInRange[0].id)
    }

    @Test
    fun testGetAffirmationsInDateRangeFlow() = runBlocking {
        val now = Instant.now()

        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now.plusSeconds(3600)))

        val affirmationsInRange = affirmationDao.getAffirmationsInDateRangeFlow(now.minusSeconds(1800), now.plusSeconds(1800)).first()

        assertEquals(1, affirmationsInRange.size)
    }

    // ==================== Get All Affirmations ====================

    @Test
    fun testGetAllAffirmations() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val allAffirmations = affirmationDao.getAllAffirmationsOnce()

        assertEquals(3, allAffirmations.size)
    }

    @Test
    fun testGetAllAffirmationsFlow() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val allAffirmations = affirmationDao.getAllAffirmations().first()

        assertEquals(3, allAffirmations.size)
    }

    // ==================== Random Affirmations ====================

    @Test
    fun testGetRandomAffirmations() = runBlocking {
        repeat(5) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val randomAffirmations = affirmationDao.getRandomAffirmations(3)

        assertEquals(3, randomAffirmations.size)
    }

    @Test
    fun testGetRandomAffirmationsByType() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-task-$i", type = AffirmationType.TASK_COMPLETION, message = "Task Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }
        repeat(2) { i ->
            affirmationDao.insert(Affirmation(id = "aff-day-$i", type = AffirmationType.DAY_COMPLETION, message = "Day Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val randomTaskAffirmations = affirmationDao.getRandomAffirmationsByType(AffirmationType.TASK_COMPLETION, 2)

        assertEquals(2, randomTaskAffirmations.size)
        assertTrue(randomTaskAffirmations.all { it.type == AffirmationType.TASK_COMPLETION })
    }

    @Test
    fun testGetRandomAffirmationsByTypeAndAgeLevel() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }
        affirmationDao.insert(Affirmation(id = "aff-adult", type = AffirmationType.TASK_COMPLETION, message = "Adult message", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 5))

        val randomAffirmations = affirmationDao.getRandomAffirmationsByTypeAndAgeLevel(AffirmationType.TASK_COMPLETION, 2, 4, 2)

        assertEquals(2, randomAffirmations.size)
        assertTrue(randomAffirmations.all { it.ageAppropriatenessLevel in 2..4 })
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetAffirmationCount() = runBlocking {
        repeat(5) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val count = affirmationDao.getAffirmationCount()

        assertEquals(5, count)
    }

    @Test
    fun testGetAffirmationCountByType() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-task-$i", type = AffirmationType.TASK_COMPLETION, message = "Task Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }
        repeat(2) { i ->
            affirmationDao.insert(Affirmation(id = "aff-day-$i", type = AffirmationType.DAY_COMPLETION, message = "Day Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        val taskCount = affirmationDao.getAffirmationCountByType(AffirmationType.TASK_COMPLETION)
        val dayCount = affirmationDao.getAffirmationCountByType(AffirmationType.DAY_COMPLETION)

        assertEquals(3, taskCount)
        assertEquals(2, dayCount)
    }

    @Test
    fun testGetAffirmationCountByTone() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-enc-$i", type = AffirmationType.TASK_COMPLETION, message = "Encouraging $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }
        repeat(2) { i ->
            affirmationDao.insert(Affirmation(id = "aff-cel-$i", type = AffirmationType.TASK_COMPLETION, message = "Celebratory $i", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))
        }

        val encouragingCount = affirmationDao.getAffirmationCountByTone(AffirmationTone.ENCOURAGING)
        val celebratoryCount = affirmationDao.getAffirmationCountByTone(AffirmationTone.CELEBRATORY)

        assertEquals(3, encouragingCount)
        assertEquals(2, celebratoryCount)
    }

    @Test
    fun testGetAffirmationCountByTypeAndTone() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Awesome!", tone = AffirmationTone.CELEBRATORY, ageAppropriatenessLevel = 3))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.DAY_COMPLETION, message = "Perfect!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))

        val taskCompletionEncouragingCount = affirmationDao.getAffirmationCountByTypeAndTone(AffirmationType.TASK_COMPLETION, AffirmationTone.ENCOURAGING)

        assertEquals(1, taskCompletionEncouragingCount)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteAffirmationById() = runBlocking {
        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "Great!", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        affirmationDao.deleteAffirmationById("aff-1")

        val retrieved = affirmationDao.getAffirmationById("aff-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteAffirmationsByType() = runBlocking {
        repeat(3) { i ->
            affirmationDao.insert(Affirmation(id = "aff-task-$i", type = AffirmationType.TASK_COMPLETION, message = "Task Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }
        repeat(2) { i ->
            affirmationDao.insert(Affirmation(id = "aff-day-$i", type = AffirmationType.DAY_COMPLETION, message = "Day Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        affirmationDao.deleteAffirmationsByType(AffirmationType.TASK_COMPLETION)

        val remainingCount = affirmationDao.getAffirmationCount()
        assertEquals(2, remainingCount)
    }

    @Test
    fun testDeleteAllAffirmations() = runBlocking {
        repeat(5) { i ->
            affirmationDao.insert(Affirmation(id = "aff-$i", type = AffirmationType.TASK_COMPLETION, message = "Message $i", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3))
        }

        affirmationDao.deleteAllAffirmations()

        val count = affirmationDao.getAffirmationCount()
        assertEquals(0, count)
    }

    // ==================== Validation ====================

    @Test(expected = IllegalArgumentException::class)
    fun testAffirmationValidationRejectsBlankMessage() {
        Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAffirmationValidationRejectsInvalidAgeLevelTooLow() {
        Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAffirmationValidationRejectsInvalidAgeLevelTooHigh() {
        Affirmation(
            id = "aff-1",
            type = AffirmationType.TASK_COMPLETION,
            message = "Great!",
            tone = AffirmationTone.ENCOURAGING,
            ageAppropriatenessLevel = 6
        )
    }

    @Test
    fun testAffirmationValidationAcceptsValidAgeLevel() {
        // Should not throw
        for (level in 1..5) {
            Affirmation(
                id = "aff-$level",
                type = AffirmationType.TASK_COMPLETION,
                message = "Great!",
                tone = AffirmationTone.ENCOURAGING,
                ageAppropriatenessLevel = level
            )
        }
    }

    // ==================== Ordering ====================

    @Test
    fun testAffirmationsOrderedByCreatedAtDescending() = runBlocking {
        val now = Instant.now()

        affirmationDao.insert(Affirmation(id = "aff-1", type = AffirmationType.TASK_COMPLETION, message = "First", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now.minusSeconds(100)))
        affirmationDao.insert(Affirmation(id = "aff-2", type = AffirmationType.TASK_COMPLETION, message = "Second", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now))
        affirmationDao.insert(Affirmation(id = "aff-3", type = AffirmationType.TASK_COMPLETION, message = "Third", tone = AffirmationTone.ENCOURAGING, ageAppropriatenessLevel = 3, createdAt = now.minusSeconds(50)))

        val affirmations = affirmationDao.getAllAffirmationsOnce()

        assertEquals("aff-2", affirmations[0].id)
        assertEquals("aff-3", affirmations[1].id)
        assertEquals("aff-1", affirmations[2].id)
    }
}
