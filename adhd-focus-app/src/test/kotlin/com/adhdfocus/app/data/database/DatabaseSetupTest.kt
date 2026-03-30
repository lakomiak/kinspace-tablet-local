package com.adhdfocus.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for database setup, initialization, and basic operations.
 * Verifies that the database initializes without errors and all entities are properly registered.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSetupTest {

    private lateinit var database: AdhdfocusDatabase

    @Before
    fun setUp() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        )
            .addCallback(DatabaseInitializer.getCallback())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDatabaseInitializesWithoutErrors() {
        // Verify database is not null and is open
        assertNotNull(database)
        assert(database.isOpen)
    }

    @Test
    fun testAllEntitiesAreRegistered() {
        // Verify all DAOs are accessible
        assertNotNull(database.taskDao())
        assertNotNull(database.userDao())
        assertNotNull(database.affirmationDao())
        assertNotNull(database.badgeDao())
        assertNotNull(database.streakDao())
        assertNotNull(database.efficiencyMetricDao())
    }

    @Test
    fun testTypeConvertersWorkCorrectly() = runBlocking {
        // Test Instant type converter
        val now = Instant.now()
        val user = User(
            id = "test-user",
            householdId = "test-household",
            email = "test@example.com",
            displayName = "Test User",
            createdAt = now,
            updatedAt = now
        )

        database.userDao().insert(user)
        val retrievedUser = database.userDao().getUserById("test-user")

        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser.id)
        assertEquals(user.email, retrievedUser.email)
        // Verify Instant was properly converted and retrieved
        assertEquals(user.createdAt.toEpochMilli(), retrievedUser.createdAt.toEpochMilli())
    }

    @Test
    fun testLocalDateTypeConverter() = runBlocking {
        // Test LocalDate type converter
        val today = LocalDate.now()
        val streak = com.adhdfocus.app.data.model.Streak(
            id = "test-streak",
            userId = "test-user",
            householdId = "test-household",
            currentCount = 5,
            bestCount = 10,
            lastCompletionDate = today,
            startDate = today.minusDays(5)
        )

        database.streakDao().insert(streak)
        val retrievedStreak = database.streakDao().getStreakByUserId("test-user")

        assertNotNull(retrievedStreak)
        assertEquals(streak.id, retrievedStreak.id)
        assertEquals(today, retrievedStreak.lastCompletionDate)
    }

    @Test
    fun testDatabaseCanBeCreatedAndDestroyed() {
        // Create a task
        val task = Task(
            id = "test-task",
            householdId = "test-household",
            assignedUserId = "test-user",
            title = "Test Task",
            todoGroup = "Morning",
            status = TaskStatus.INCOMPLETE
        )

        runBlocking {
            database.taskDao().insert(task)
            val retrieved = database.taskDao().getTaskById("test-task")
            assertNotNull(retrieved)
            assertEquals("Test Task", retrieved.title)
        }

        // Database can be destroyed (closed)
        database.close()
        assert(!database.isOpen)
    }

    @Test
    fun testSchemaVersioning() {
        // Verify database version is set correctly
        assertEquals(1, database.openHelper.readableDatabase.version)
    }

    @Test
    fun testMigrationFrameworkIsReady() {
        // Verify migrations array is accessible
        val migrations = AdhdfocusDatabase.MIGRATIONS
        assertNotNull(migrations)
        // Currently no migrations, but framework is ready for future additions
        assertEquals(0, migrations.size)
    }

    @Test
    fun testPrePopulatedAffirmationsExist() = runBlocking {
        // Verify that default affirmations were populated during database creation
        val affirmations = database.affirmationDao().getAllAffirmations()
        assert(affirmations.isNotEmpty()) { "No affirmations found in database" }

        // Verify we have affirmations of different types
        val taskCompletionAffirmations = affirmations.filter { it.type == AffirmationType.TASK_COMPLETION }
        val dayCompletionAffirmations = affirmations.filter { it.type == AffirmationType.DAY_COMPLETION }
        val streakMilestoneAffirmations = affirmations.filter { it.type == AffirmationType.STREAK_MILESTONE }

        assert(taskCompletionAffirmations.isNotEmpty()) { "No task completion affirmations found" }
        assert(dayCompletionAffirmations.isNotEmpty()) { "No day completion affirmations found" }
        assert(streakMilestoneAffirmations.isNotEmpty()) { "No streak milestone affirmations found" }
    }

    @Test
    fun testDatabaseCanStoreAndRetrieveAllEntityTypes() = runBlocking {
        // Test Task
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning"
        )
        database.taskDao().insert(task)
        assertNotNull(database.taskDao().getTaskById("task-1"))

        // Test User
        val user = User(
            id = "user-1",
            householdId = "household-1",
            email = "test@example.com",
            displayName = "Test User"
        )
        database.userDao().insert(user)
        assertNotNull(database.userDao().getUserById("user-1"))

        // Test Badge
        val badge = com.adhdfocus.app.data.model.Badge(
            id = "badge-1",
            householdId = "household-1",
            userId = "user-1",
            badgeType = "first_task",
            name = "First Task",
            isLocked = false
        )
        database.badgeDao().insert(badge)
        assertNotNull(database.badgeDao().getBadgeById("badge-1"))

        // Test Streak
        val streak = com.adhdfocus.app.data.model.Streak(
            id = "streak-1",
            userId = "user-1",
            householdId = "household-1",
            currentCount = 5
        )
        database.streakDao().insert(streak)
        assertNotNull(database.streakDao().getStreakByUserId("user-1"))

        // Test EfficiencyMetric
        val metric = com.adhdfocus.app.data.model.EfficiencyMetric(
            id = "metric-1",
            taskId = "task-1",
            userId = "user-1",
            householdId = "household-1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25
        )
        database.efficiencyMetricDao().insert(metric)
        assertNotNull(database.efficiencyMetricDao().getMetricById("metric-1"))
    }
}
