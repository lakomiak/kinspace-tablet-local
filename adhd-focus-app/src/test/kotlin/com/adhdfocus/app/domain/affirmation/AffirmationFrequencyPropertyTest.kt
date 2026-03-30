package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for affirmation frequency customization.
 *
 * **Validates: Requirements 18, Property: Affirmation Frequency Customization**
 *
 * Tests:
 * - Frequency validation (1-5 range)
 * - Frequency persistence
 * - Affirmation filtering based on frequency
 * - Streak milestones always shown
 * - Frequency changes apply immediately
 */
class AffirmationFrequencyPropertyTest : FunSpec({

    test("Property: Affirmation frequency must be in range 1-5") {
        checkAll(Arb.int()) { frequency ->
            val manager = AffirmationTriggerManager()
            
            if (frequency in 1..5) {
                // Valid frequencies should be accepted
                manager.setAffirmationFrequency(frequency)
                manager.getAffirmationFrequency() shouldBe frequency
            } else {
                // Invalid frequencies should throw
                try {
                    manager.setAffirmationFrequency(frequency)
                    throw AssertionError("Should have thrown for frequency $frequency")
                } catch (e: IllegalArgumentException) {
                    // Expected
                }
            }
        }
    }

    test("Property: Default affirmation frequency is 3") {
        checkAll(Arb.string(minSize = 1, maxSize = 50)) { _ ->
            val manager = AffirmationTriggerManager()
            manager.getAffirmationFrequency() shouldBe 3
        }
    }

    test("Property: Frequency setting persists across multiple calls") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            // Verify persistence across multiple reads
            repeat(10) {
                manager.getAffirmationFrequency() shouldBe frequency
            }
        }
    }

    test("Property: Frequency 5 always shows task completion affirmations") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.string(minSize = 1, maxSize = 100)
        ) { title, group ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(5)
            
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = "Test",
                todoGroup = group,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = null,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = Instant.now(),
                syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                isDeleted = false
            )
            
            // With frequency 5, should always show
            var affirmationCount = 0
            repeat(10) {
                manager.clearAffirmation()
                if (manager.checkAndTriggerTaskCompleteAffirmation(task)) {
                    affirmationCount++
                }
            }
            
            affirmationCount shouldBe 10
        }
    }

    test("Property: Frequency 1 shows task completion affirmations approximately 20% of the time") {
        checkAll(Arb.string(minSize = 1, maxSize = 100)) { title ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(1)
            
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = "Test",
                todoGroup = "Test",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = null,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = Instant.now(),
                syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                isDeleted = false
            )
            
            // With frequency 1, should show approximately 20%
            var affirmationCount = 0
            val trials = 100
            repeat(trials) {
                manager.clearAffirmation()
                if (manager.checkAndTriggerTaskCompleteAffirmation(task)) {
                    affirmationCount++
                }
            }
            
            // Allow 5-35% range for statistical variation
            affirmationCount.shouldBeInRange(5, 35)
        }
    }

    test("Property: Frequency 3 shows task completion affirmations approximately 60% of the time") {
        checkAll(Arb.string(minSize = 1, maxSize = 100)) { title ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(3)
            
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = "Test",
                todoGroup = "Test",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = null,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = Instant.now(),
                syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                isDeleted = false
            )
            
            // With frequency 3, should show approximately 60%
            var affirmationCount = 0
            val trials = 100
            repeat(trials) {
                manager.clearAffirmation()
                if (manager.checkAndTriggerTaskCompleteAffirmation(task)) {
                    affirmationCount++
                }
            }
            
            // Allow 40-80% range for statistical variation
            affirmationCount.shouldBeInRange(40, 80)
        }
    }

    test("Property: Incomplete tasks never trigger affirmations regardless of frequency") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Test",
                description = "Test",
                todoGroup = "Test",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = null,
                status = TaskStatus.INCOMPLETE,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = null,
                syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                isDeleted = false
            )
            
            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
            triggered.shouldBeFalse()
        }
    }

    test("Property: Streak milestones always trigger regardless of frequency") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            val milestones = listOf(3, 7, 14, 30, 60, 90, 365)
            
            milestones.forEach { streak ->
                manager.clearAffirmation()
                val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(streak)
                triggered.shouldBeTrue()
            }
        }
    }

    test("Property: Non-milestone streaks never trigger regardless of frequency") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            val nonMilestones = listOf(1, 2, 4, 5, 6, 8, 15, 29, 31, 59, 61, 89, 91, 364, 366)
            
            nonMilestones.forEach { streak ->
                manager.clearAffirmation()
                val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(streak)
                triggered.shouldBeFalse()
            }
        }
    }

    test("Property: Frequency changes apply immediately to subsequent affirmations") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency1 ->
            val frequency2 = if (frequency1 == 5) 1 else frequency1 + 1
            
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency1)
            manager.getAffirmationFrequency() shouldBe frequency1
            
            manager.setAffirmationFrequency(frequency2)
            manager.getAffirmationFrequency() shouldBe frequency2
        }
    }

    test("Property: Empty task list never triggers day completion affirmation") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            val triggered = manager.checkAndTriggerDayCompleteAffirmation(emptyList())
            triggered.shouldBeFalse()
        }
    }

    test("Property: Partial completion never triggers day completion affirmation") {
        checkAll(Arb.int(min = 1, max = 5)) { frequency ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(frequency)
            
            val tasks = listOf(
                Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household-1",
                    assignedUserId = "user-1",
                    title = "Task 1",
                    description = "Test",
                    todoGroup = "Test",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = null,
                    status = TaskStatus.COMPLETED,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    completedAt = Instant.now(),
                    syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                    isDeleted = false
                ),
                Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household-1",
                    assignedUserId = "user-1",
                    title = "Task 2",
                    description = "Test",
                    todoGroup = "Test",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = null,
                    status = TaskStatus.INCOMPLETE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    completedAt = null,
                    syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                    isDeleted = false
                )
            )
            
            val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)
            triggered.shouldBeFalse()
        }
    }

    test("Property: Frequency 5 always shows day completion affirmations") {
        checkAll(Arb.string(minSize = 1, maxSize = 100)) { title ->
            val manager = AffirmationTriggerManager()
            manager.setAffirmationFrequency(5)
            
            val tasks = listOf(
                Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household-1",
                    assignedUserId = "user-1",
                    title = title,
                    description = "Test",
                    todoGroup = "Test",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = null,
                    status = TaskStatus.COMPLETED,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    completedAt = Instant.now(),
                    syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
                    isDeleted = false
                )
            )
            
            // With frequency 5, should always show
            var affirmationCount = 0
            repeat(10) {
                manager.clearAffirmation()
                if (manager.checkAndTriggerDayCompleteAffirmation(tasks)) {
                    affirmationCount++
                }
            }
            
            affirmationCount shouldBe 10
        }
    }
})
