package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Task Validation (Property 5)
 *
 * Feature: adhd-focus-app
 * Property 5: Task Validation
 *
 * Correctness Property:
 * All created tasks must have valid required fields and maintain data integrity constraints.
 * For any valid task creation input, the resulting task must:
 * 1. Have all required fields populated
 * 2. Have optional fields within acceptable ranges
 * 3. Maintain timestamp consistency (createdAt <= updatedAt, completedAt >= createdAt)
 * 4. Have correct status-timestamp relationships
 */
class TaskValidationPropertyTest : BehaviorSpec({
    val validator = TaskValidator()

    Given("TaskValidator with property-based test generation") {
        When("validating tasks with valid required fields") {
            Then("validation should always succeed") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50)
                ) { title, todoGroup, householdId, userId ->
                    // Arrange
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        todoGroup = todoGroup,
                        householdId = householdId,
                        assignedUserId = userId,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    // Act
                    val result = validator.validateTask(task)

                    // Assert
                    result.isValid() shouldBe true
                    result.getErrors().isEmpty() shouldBe true
                }
            }
        }

        When("validating tasks with positive estimated duration") {
            Then("validation should succeed") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.int(min = 1, max = 1440)
                ) { title, duration ->
                    // Arrange
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        estimatedDurationMinutes = duration,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    // Act
                    val result = validator.validateTask(task)

                    // Assert
                    result.isValid() shouldBe true
                }
            }
        }

        When("validating tasks with non-negative actual duration") {
            Then("validation should succeed") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.int(min = 0, max = 1440)
                ) { title, duration ->
                    // Arrange
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        actualDurationMinutes = duration,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    // Act
                    val result = validator.validateTask(task)

                    // Assert
                    result.isValid() shouldBe true
                }
            }
        }

        When("validating tasks with blank title") {
            Then("validation should fail") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
                result.getErrors().isNotEmpty() shouldBe true
            }
        }

        When("validating tasks with negative estimated duration") {
            Then("validation should fail") {
                checkAll(
                    Arb.int(min = Int.MIN_VALUE, max = 0)
                ) { duration ->
                    // Arrange
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = "Test Task",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        estimatedDurationMinutes = duration,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    // Act
                    val result = validator.validateTask(task)

                    // Assert
                    result.isValid() shouldBe false
                }
            }
        }

        When("validating tasks with negative actual duration") {
            Then("validation should fail") {
                checkAll(
                    Arb.int(min = Int.MIN_VALUE, max = -1)
                ) { duration ->
                    // Arrange
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = "Test Task",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        actualDurationMinutes = duration,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    // Act
                    val result = validator.validateTask(task)

                    // Assert
                    result.isValid() shouldBe false
                }
            }
        }

        When("validating tasks with updatedAt before createdAt") {
            Then("validation should fail") {
                // Arrange
                val now = Instant.now()
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1",
                    createdAt = now,
                    updatedAt = now.minusSeconds(60)
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
            }
        }

        When("validating completed tasks without completedAt timestamp") {
            Then("validation should fail") {
                // Arrange
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1",
                    status = TaskStatus.COMPLETED,
                    completedAt = null,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Act
                val result = validator.validateTask(task)

                // Assert
                result.isValid() shouldBe false
            }
        }

        When("validating task creation input with valid fields") {
            Then("validation should succeed") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50)
                ) { title, todoGroup, householdId, userId ->
                    // Act
                    val result = validator.validateTaskCreationInput(
                        title = title,
                        todoGroup = todoGroup,
                        householdId = householdId,
                        assignedUserId = userId
                    )

                    // Assert
                    result.isValid() shouldBe true
                }
            }
        }

        When("validating task creation input with positive duration") {
            Then("validation should succeed") {
                checkAll(
                    Arb.int(min = 1, max = 1440)
                ) { duration ->
                    // Act
                    val result = validator.validateTaskCreationInput(
                        title = "Test Task",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        estimatedDurationMinutes = duration
                    )

                    // Assert
                    result.isValid() shouldBe true
                }
            }
        }

        When("validating task creation input with duration exceeding 24 hours") {
            Then("validation should fail") {
                checkAll(
                    Arb.int(min = 1441, max = Int.MAX_VALUE)
                ) { duration ->
                    // Act
                    val result = validator.validateTaskCreationInput(
                        title = "Test Task",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        estimatedDurationMinutes = duration
                    )

                    // Assert
                    result.isValid() shouldBe false
                }
            }
        }

        When("validating task update input with valid fields") {
            Then("validation should succeed") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.int(min = 1, max = 1440)
                ) { title, duration ->
                    // Act
                    val result = validator.validateTaskUpdateInput(
                        title = title,
                        estimatedDurationMinutes = duration
                    )

                    // Assert
                    result.isValid() shouldBe true
                }
            }
        }

        When("validating task update input with blank title") {
            Then("validation should fail") {
                // Act
                val result = validator.validateTaskUpdateInput(title = "")

                // Assert
                result.isValid() shouldBe false
            }
        }
    }
})
