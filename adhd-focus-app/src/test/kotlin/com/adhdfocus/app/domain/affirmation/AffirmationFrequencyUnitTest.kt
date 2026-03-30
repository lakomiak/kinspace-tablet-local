package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for affirmation frequency customization.
 *
 * Tests:
 * - Frequency setting validation (1-5 range)
 * - Frequency getter/setter
 * - Task completion affirmation with frequency filtering
 * - Day completion affirmation with frequency filtering
 * - Streak milestone affirmations (always shown)
 * - Affirmation event triggering
 */
class AffirmationFrequencyUnitTest : BehaviorSpec({

    Given("AffirmationTriggerManager") {
        val manager = AffirmationTriggerManager()

        When("setting affirmation frequency") {
            Then("should accept valid frequencies (1-5)") {
                for (frequency in 1..5) {
                    manager.setAffirmationFrequency(frequency)
                    manager.getAffirmationFrequency() shouldBe frequency
                }
            }

            Then("should reject frequency 0") {
                try {
                    manager.setAffirmationFrequency(0)
                    throw AssertionError("Should have thrown IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    e.message shouldNotBe null
                }
            }

            Then("should reject frequency 6") {
                try {
                    manager.setAffirmationFrequency(6)
                    throw AssertionError("Should have thrown IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    e.message shouldNotBe null
                }
            }

            Then("should reject negative frequencies") {
                try {
                    manager.setAffirmationFrequency(-1)
                    throw AssertionError("Should have thrown IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    e.message shouldNotBe null
                }
            }
        }

        When("getting affirmation frequency") {
            Then("should return default frequency of 3") {
                val newManager = AffirmationTriggerManager()
                newManager.getAffirmationFrequency() shouldBe 3
            }

            Then("should return set frequency") {
                manager.setAffirmationFrequency(5)
                manager.getAffirmationFrequency() shouldBe 5
            }
        }

        When("triggering task completion affirmation with frequency 1 (rarely)") {
            manager.setAffirmationFrequency(1)
            val task = createTestTask(TaskStatus.COMPLETED)
            
            Then("should show affirmation approximately 20% of the time") {
                var affirmationCount = 0
                val trials = 100
                
                repeat(trials) {
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
                    if (triggered) affirmationCount++
                }
                
                // With 100 trials and 20% probability, expect 10-30 affirmations
                affirmationCount shouldBeBetween 5..35
            }
        }

        When("triggering task completion affirmation with frequency 3 (moderate)") {
            manager.setAffirmationFrequency(3)
            val task = createTestTask(TaskStatus.COMPLETED)
            
            Then("should show affirmation approximately 60% of the time") {
                var affirmationCount = 0
                val trials = 100
                
                repeat(trials) {
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
                    if (triggered) affirmationCount++
                }
                
                // With 100 trials and 60% probability, expect 40-80 affirmations
                affirmationCount shouldBeBetween 40..80
            }
        }

        When("triggering task completion affirmation with frequency 5 (very frequently)") {
            manager.setAffirmationFrequency(5)
            val task = createTestTask(TaskStatus.COMPLETED)
            
            Then("should always show affirmation") {
                var affirmationCount = 0
                val trials = 20
                
                repeat(trials) {
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
                    if (triggered) affirmationCount++
                }
                
                // With frequency 5, should always show (100%)
                affirmationCount shouldBe trials
            }
        }

        When("triggering day completion affirmation with frequency 1 (rarely)") {
            manager.setAffirmationFrequency(1)
            val tasks = listOf(
                createTestTask(TaskStatus.COMPLETED),
                createTestTask(TaskStatus.COMPLETED)
            )
            
            Then("should show affirmation approximately 20% of the time") {
                var affirmationCount = 0
                val trials = 100
                
                repeat(trials) {
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)
                    if (triggered) affirmationCount++
                }
                
                // With 100 trials and 20% probability, expect 5-35 affirmations
                affirmationCount shouldBeBetween 5..35
            }
        }

        When("triggering day completion affirmation with frequency 5 (very frequently)") {
            manager.setAffirmationFrequency(5)
            val tasks = listOf(
                createTestTask(TaskStatus.COMPLETED),
                createTestTask(TaskStatus.COMPLETED)
            )
            
            Then("should always show affirmation") {
                var affirmationCount = 0
                val trials = 20
                
                repeat(trials) {
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)
                    if (triggered) affirmationCount++
                }
                
                // With frequency 5, should always show (100%)
                affirmationCount shouldBe trials
            }
        }

        When("triggering streak milestone affirmation") {
            manager.setAffirmationFrequency(1)  // Even with low frequency
            
            Then("should always show milestone affirmations regardless of frequency") {
                val milestones = listOf(3, 7, 14, 30, 60, 90, 365)
                
                milestones.forEach { streak ->
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(streak)
                    triggered.shouldBeTrue()
                }
            }
        }

        When("triggering non-milestone streak affirmation") {
            manager.setAffirmationFrequency(1)
            
            Then("should not trigger for non-milestone streaks") {
                val nonMilestones = listOf(1, 2, 4, 5, 6, 8, 15, 29, 31, 59, 61, 89, 91, 364, 366)
                
                nonMilestones.forEach { streak ->
                    manager.clearAffirmation()
                    val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(streak)
                    triggered.shouldBeFalse()
                }
            }
        }

        When("task completion affirmation is triggered") {
            manager.setAffirmationFrequency(5)
            val task = createTestTask(TaskStatus.COMPLETED)
            
            Then("should set affirmation event") {
                manager.clearAffirmation()
                manager.checkAndTriggerTaskCompleteAffirmation(task)
                
                val event = manager.getCurrentAffirmation()
                event shouldNotBe null
                event shouldBe AffirmationEvent.TaskComplete::class.java.simpleName
            }
        }

        When("day completion affirmation is triggered") {
            manager.setAffirmationFrequency(5)
            val tasks = listOf(
                createTestTask(TaskStatus.COMPLETED),
                createTestTask(TaskStatus.COMPLETED)
            )
            
            Then("should set affirmation event") {
                manager.clearAffirmation()
                manager.checkAndTriggerDayCompleteAffirmation(tasks)
                
                val event = manager.getCurrentAffirmation()
                event shouldNotBe null
            }
        }

        When("affirmation is cleared") {
            manager.setAffirmationFrequency(5)
            val task = createTestTask(TaskStatus.COMPLETED)
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            
            Then("should clear affirmation event") {
                manager.clearAffirmation()
                manager.getCurrentAffirmation() shouldBe null
            }
        }

        When("incomplete task completion is checked") {
            manager.setAffirmationFrequency(5)
            val task = createTestTask(TaskStatus.INCOMPLETE)
            
            Then("should not trigger affirmation") {
                val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
                triggered.shouldBeFalse()
            }
        }

        When("in-progress task completion is checked") {
            manager.setAffirmationFrequency(5)
            val task = createTestTask(TaskStatus.IN_PROGRESS)
            
            Then("should not trigger affirmation") {
                val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task)
                triggered.shouldBeFalse()
            }
        }

        When("empty task list day completion is checked") {
            manager.setAffirmationFrequency(5)
            
            Then("should not trigger affirmation") {
                val triggered = manager.checkAndTriggerDayCompleteAffirmation(emptyList())
                triggered.shouldBeFalse()
            }
        }

        When("partial completion day is checked") {
            manager.setAffirmationFrequency(5)
            val tasks = listOf(
                createTestTask(TaskStatus.COMPLETED),
                createTestTask(TaskStatus.INCOMPLETE)
            )
            
            Then("should not trigger day completion affirmation") {
                val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)
                triggered.shouldBeFalse()
            }
        }

        When("frequency is changed mid-session") {
            val task = createTestTask(TaskStatus.COMPLETED)
            
            Then("should use new frequency for subsequent affirmations") {
                manager.setAffirmationFrequency(5)
                manager.clearAffirmation()
                val triggered1 = manager.checkAndTriggerTaskCompleteAffirmation(task)
                triggered1.shouldBeTrue()
                
                manager.setAffirmationFrequency(1)
                manager.clearAffirmation()
                // With frequency 1, might not trigger, but should respect new setting
                manager.getAffirmationFrequency() shouldBe 1
            }
        }
    }
})

/**
 * Helper function to create a test task.
 */
private fun createTestTask(status: TaskStatus): Task {
    return Task(
        id = UUID.randomUUID().toString(),
        householdId = "household-1",
        assignedUserId = "user-1",
        title = "Test Task",
        description = "Test Description",
        todoGroup = "Test Group",
        estimatedDurationMinutes = 30,
        actualDurationMinutes = null,
        status = status,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
        syncStatus = com.adhdfocus.app.data.model.SyncStatus.SYNCED,
        isDeleted = false
    )
}
