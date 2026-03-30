package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Task Status Transitions
 *
 * Feature: adhd-focus-app
 * Task 4.3: Implement task status transitions (incomplete → in-progress → completed)
 *
 * Verifies that tasks can only transition between valid states:
 * - INCOMPLETE → IN_PROGRESS (start task)
 * - INCOMPLETE → COMPLETED (mark complete directly)
 * - IN_PROGRESS → COMPLETED (complete task)
 * - IN_PROGRESS → INCOMPLETE (restart task)
 * - COMPLETED → INCOMPLETE (reopen task)
 */
class TaskStatusTransitionTest : BehaviorSpec({
    val transitionManager = TaskStatusTransitionManager()

    Given("TaskStatusTransitionManager") {
        When("checking valid transition from INCOMPLETE to IN_PROGRESS") {
            Then("should return true") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.INCOMPLETE,
                    TaskStatus.IN_PROGRESS
                )
                result shouldBe true
            }
        }

        When("checking valid transition from INCOMPLETE to COMPLETED") {
            Then("should return true") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.INCOMPLETE,
                    TaskStatus.COMPLETED
                )
                result shouldBe true
            }
        }

        When("checking valid transition from IN_PROGRESS to COMPLETED") {
            Then("should return true") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.COMPLETED
                )
                result shouldBe true
            }
        }

        When("checking valid transition from IN_PROGRESS to INCOMPLETE") {
            Then("should return true") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.INCOMPLETE
                )
                result shouldBe true
            }
        }

        When("checking valid transition from COMPLETED to INCOMPLETE") {
            Then("should return true") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.INCOMPLETE
                )
                result shouldBe true
            }
        }

        When("checking same status transition") {
            Then("should return true (no-op)") {
                transitionManager.isValidTransition(
                    TaskStatus.INCOMPLETE,
                    TaskStatus.INCOMPLETE
                ) shouldBe true

                transitionManager.isValidTransition(
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS
                ) shouldBe true

                transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED
                ) shouldBe true
            }
        }

        When("checking invalid transition from INCOMPLETE to INCOMPLETE") {
            Then("should return true (same status)") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.INCOMPLETE,
                    TaskStatus.INCOMPLETE
                )
                result shouldBe true
            }
        }

        When("checking invalid transition from COMPLETED to IN_PROGRESS") {
            Then("should return false") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS
                )
                result shouldBe false
            }
        }

        When("checking invalid transition from COMPLETED to COMPLETED") {
            Then("should return true (same status)") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED
                )
                result shouldBe true
            }
        }

        When("getting valid next statuses from INCOMPLETE") {
            Then("should return IN_PROGRESS and COMPLETED") {
                val result = transitionManager.getValidNextStatuses(TaskStatus.INCOMPLETE)
                result shouldContain TaskStatus.IN_PROGRESS
                result shouldContain TaskStatus.COMPLETED
                result.size shouldBe 2
            }
        }

        When("getting valid next statuses from IN_PROGRESS") {
            Then("should return COMPLETED and INCOMPLETE") {
                val result = transitionManager.getValidNextStatuses(TaskStatus.IN_PROGRESS)
                result shouldContain TaskStatus.COMPLETED
                result shouldContain TaskStatus.INCOMPLETE
                result.size shouldBe 2
            }
        }

        When("getting valid next statuses from COMPLETED") {
            Then("should return INCOMPLETE") {
                val result = transitionManager.getValidNextStatuses(TaskStatus.COMPLETED)
                result shouldContain TaskStatus.INCOMPLETE
                result.size shouldBe 1
            }
        }

        When("validating a valid transition sequence") {
            Then("should return Success") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED to TaskStatus.INCOMPLETE,
                    TaskStatus.INCOMPLETE to TaskStatus.COMPLETED
                )

                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe true
            }
        }

        When("validating a sequence with invalid transition") {
            Then("should return Failure with error message") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED to TaskStatus.IN_PROGRESS  // Invalid!
                )

                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe false
                result.getErrors().isNotEmpty() shouldBe true
            }
        }

        When("getting error message for invalid transition") {
            Then("should return descriptive message") {
                val message = transitionManager.getTransitionErrorMessage(
                    TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS
                )
                message shouldBe "Cannot transition from COMPLETED to IN_PROGRESS"
            }
        }

        When("validating a complex valid transition sequence") {
            Then("should handle multiple state changes correctly") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.INCOMPLETE,
                    TaskStatus.INCOMPLETE to TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED to TaskStatus.INCOMPLETE,
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED
                )

                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe true
            }
        }

        When("validating a sequence with multiple invalid transitions") {
            Then("should report all errors") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED to TaskStatus.IN_PROGRESS,  // Invalid
                    TaskStatus.COMPLETED to TaskStatus.COMPLETED,    // Valid (same)
                    TaskStatus.COMPLETED to TaskStatus.IN_PROGRESS   // Invalid
                )

                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe false
                result.getErrors().size shouldBe 2
            }
        }
    }
})
