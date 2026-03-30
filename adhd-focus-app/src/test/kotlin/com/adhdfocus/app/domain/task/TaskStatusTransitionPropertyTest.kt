package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

/**
 * Property-Based Tests for Task Status Transitions
 *
 * Feature: adhd-focus-app
 * Task 4.3: Implement task status transitions (incomplete → in-progress → completed)
 *
 * Correctness Properties:
 * 1. Valid transitions must be allowed
 * 2. Invalid transitions must be rejected
 * 3. Same-status transitions must be allowed (no-op)
 * 4. Transition validity must be consistent
 * 5. Valid next statuses must be reachable
 */
class TaskStatusTransitionPropertyTest : BehaviorSpec({
    val transitionManager = TaskStatusTransitionManager()

    Given("TaskStatusTransitionManager with property-based generation") {
        When("checking any transition to itself") {
            Then("should always be valid") {
                checkAll(Arb.enum<TaskStatus>()) { status ->
                    val result = transitionManager.isValidTransition(status, status)
                    result shouldBe true
                }
            }
        }

        When("checking valid transitions from INCOMPLETE") {
            Then("should allow IN_PROGRESS and COMPLETED") {
                val validTransitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.INCOMPLETE to TaskStatus.COMPLETED
                )

                for ((from, to) in validTransitions) {
                    transitionManager.isValidTransition(from, to) shouldBe true
                }
            }
        }

        When("checking valid transitions from IN_PROGRESS") {
            Then("should allow COMPLETED and INCOMPLETE") {
                val validTransitions = listOf(
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS to TaskStatus.INCOMPLETE
                )

                for ((from, to) in validTransitions) {
                    transitionManager.isValidTransition(from, to) shouldBe true
                }
            }
        }

        When("checking valid transitions from COMPLETED") {
            Then("should allow INCOMPLETE") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.INCOMPLETE
                )
                result shouldBe true
            }
        }

        When("checking invalid transition from COMPLETED to IN_PROGRESS") {
            Then("should always be invalid") {
                val result = transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS
                )
                result shouldBe false
            }
        }

        When("getting valid next statuses for any status") {
            Then("should return non-empty list") {
                checkAll(Arb.enum<TaskStatus>()) { status ->
                    val result = transitionManager.getValidNextStatuses(status)
                    result.isNotEmpty() shouldBe true
                }
            }
        }

        When("checking if valid next statuses are actually valid transitions") {
            Then("all should be valid transitions") {
                checkAll(Arb.enum<TaskStatus>()) { status ->
                    val validNextStatuses = transitionManager.getValidNextStatuses(status)
                    for (nextStatus in validNextStatuses) {
                        transitionManager.isValidTransition(status, nextStatus) shouldBe true
                    }
                }
            }
        }

        When("validating empty transition sequence") {
            Then("should return Success") {
                val result = transitionManager.validateTransitionSequence(emptyList())
                result.isValid() shouldBe true
            }
        }

        When("validating single valid transition") {
            Then("should return Success") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS
                )
                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe true
            }
        }

        When("validating sequence of valid transitions") {
            Then("should return Success") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.COMPLETED to TaskStatus.INCOMPLETE
                )
                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe true
            }
        }

        When("validating sequence with one invalid transition") {
            Then("should return Failure") {
                val transitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.COMPLETED to TaskStatus.IN_PROGRESS  // Invalid
                )
                val result = transitionManager.validateTransitionSequence(transitions)
                result.isValid() shouldBe false
            }
        }

        When("checking transition consistency") {
            Then("isValidTransition should be consistent across multiple calls") {
                checkAll(
                    Arb.enum<TaskStatus>(),
                    Arb.enum<TaskStatus>()
                ) { from, to ->
                    val result1 = transitionManager.isValidTransition(from, to)
                    val result2 = transitionManager.isValidTransition(from, to)
                    result1 shouldBe result2
                }
            }
        }

        When("checking that invalid transitions are truly invalid") {
            Then("should reject COMPLETED to IN_PROGRESS") {
                transitionManager.isValidTransition(
                    TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS
                ) shouldBe false
            }
        }

        When("checking that valid transitions are truly valid") {
            Then("should accept all documented valid transitions") {
                val validTransitions = listOf(
                    TaskStatus.INCOMPLETE to TaskStatus.IN_PROGRESS,
                    TaskStatus.INCOMPLETE to TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS to TaskStatus.COMPLETED,
                    TaskStatus.IN_PROGRESS to TaskStatus.INCOMPLETE,
                    TaskStatus.COMPLETED to TaskStatus.INCOMPLETE
                )

                for ((from, to) in validTransitions) {
                    transitionManager.isValidTransition(from, to) shouldBe true
                }
            }
        }
    }
})
