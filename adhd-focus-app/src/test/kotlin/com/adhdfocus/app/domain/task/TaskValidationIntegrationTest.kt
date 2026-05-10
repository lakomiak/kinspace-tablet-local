package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

/**
 * Integration Tests for Task Validation (Property 5)
 *
 * Feature: adhd-focus-app
 * Property 5: Task Validation
 *
 * These tests verify that the TaskValidator correctly validates tasks
 * and that the TaskManager enforces validation during task operations.
 */
class TaskValidationIntegrationTest : BehaviorSpec({
    val validator = TaskValidator()

    Given("TaskValidator with various task scenarios") {
        When("validating a complete valid task") {
            Then("should pass validation") {
                // Arrange
                val now = Instant.now()
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Complete Task",
                    description = "A complete task with all fields",
                    todoGroup = "Morning",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 25,
                    status = TaskStatus.COMPLETED,
                    createdAt = now,
                    updatedAt = now.plusSeconds(60),
                    completedAt = now.plusSeconds(120)
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe true
                result.getErrors().isEmpty() shouldBe true
            }
        }

        When("validating a task with missing title") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "",
                    todoGroup = "Morning",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Task title cannot be empty"
            }
        }

        When("validating a task with missing householdId") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Household ID cannot be empty"
            }
        }

        When("validating a task with missing assignedUserId") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "",
                    title = "Test Task",
                    todoGroup = "Morning",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Assigned user ID cannot be empty"
            }
        }

        When("validating a task with missing todoGroup") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Todo group cannot be empty"
            }
        }

        When("validating a task with negative estimated duration") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    estimatedDurationMinutes = -30,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Estimated duration cannot be negative"
            }
        }

        When("validating a task with zero estimated duration") {
            Then("should pass validation") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    estimatedDurationMinutes = 0,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe true
            }
        }

        When("validating a task with negative actual duration") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    actualDurationMinutes = -10,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Actual duration cannot be negative"
            }
        }

        When("validating a task with updatedAt before createdAt") {
            Then("should fail with appropriate error message") {
                // Arrange
                val now = Instant.now()
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    createdAt = now,
                    updatedAt = now.minusSeconds(60)
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Updated timestamp cannot be before created timestamp"
            }
        }

        When("validating a completed task without completedAt") {
            Then("should fail with appropriate error message") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.COMPLETED,
                    completedAt = null,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Completed task must have completedAt timestamp"
            }
        }

        When("validating a task with completedAt before createdAt") {
            Then("should fail with appropriate error message") {
                // Arrange
                val now = Instant.now()
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.COMPLETED,
                    createdAt = now,
                    completedAt = now.minusSeconds(60),
                    updatedAt = now
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Completed timestamp cannot be before created timestamp"
            }
        }

        When("validating task creation input with valid fields") {
            Then("should pass validation") {
                // Act
                val result = validator.validateTaskCreationInput(
                    title = "Test Task",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1",
                    estimatedDurationMinutes = 30
                )

                // Assert
                result.isValid() shouldBe true
            }
        }

        When("validating task creation input with blank title") {
            Then("should fail") {
                // Act
                val result = validator.validateTaskCreationInput(
                    title = "",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Task title cannot be empty"
            }
        }

        When("validating task creation input with title exceeding 500 characters") {
            Then("should fail") {
                // Act
                val result = validator.validateTaskCreationInput(
                    title = "a".repeat(501),
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Task title cannot exceed 500 characters"
            }
        }

        When("validating task creation input with duration exceeding 24 hours") {
            Then("should fail") {
                // Act
                val result = validator.validateTaskCreationInput(
                    title = "Test Task",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1",
                    estimatedDurationMinutes = 1441
                )

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Estimated duration cannot exceed 1440 minutes (24 hours)"
            }
        }

        When("validating task update input with valid fields") {
            Then("should pass validation") {
                // Act
                val result = validator.validateTaskUpdateInput(
                    title = "Updated Title",
                    todoGroup = "Afternoon",
                    estimatedDurationMinutes = 45
                )

                // Assert
                result.isValid() shouldBe true
            }
        }

        When("validating task update input with partial updates") {
            Then("should pass validation") {
                // Act
                val result = validator.validateTaskUpdateInput(
                    title = "Updated Title"
                )

                // Assert
                result.isValid() shouldBe true
            }
        }

        When("validating task update input with blank title") {
            Then("should fail") {
                // Act
                val result = validator.validateTaskUpdateInput(title = "")

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Task title cannot be empty"
            }
        }

        When("validating task update input with negative duration") {
            Then("should fail") {
                // Act
                val result = validator.validateTaskUpdateInput(estimatedDurationMinutes = -30)

                // Assert
                result.isValid() shouldBe false
                result.getErrors() shouldContain "Estimated duration cannot be negative"
            }
        }
    }
})
