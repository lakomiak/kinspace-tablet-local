package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.Task
import java.time.Instant

/**
 * TaskValidator validates task data for correctness and completeness.
 *
 * Validates:
 * - Required fields are present and non-empty
 * - Field values are within acceptable ranges
 * - Task data integrity
 *
 * Correctness Property 5: Task Validation
 * - All created tasks must have valid required fields
 * - All optional fields must be within acceptable ranges
 * - Task data must maintain integrity constraints
 */
class TaskValidator {
    /**
     * Validates a task object for correctness.
     *
     * @param task Task to validate
     * @return ValidationResult with success status and error messages
     */
    fun validateTask(task: Task): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate required fields
        if (task.id.isBlank()) {
            errors.add("Task ID cannot be empty")
        }
        if (task.title.isBlank()) {
            errors.add("Task title cannot be empty")
        }
        if (task.householdId.isBlank()) {
            errors.add("Household ID cannot be empty")
        }
        if (task.assignedUserId.isBlank()) {
            errors.add("Assigned user ID cannot be empty")
        }
        if (task.todoGroup.isBlank()) {
            errors.add("Todo group cannot be empty")
        }

        // Validate optional fields
        if (task.estimatedDurationMinutes != null && task.estimatedDurationMinutes < 0) {
            errors.add("Estimated duration cannot be negative")
        }
        if (task.actualDurationMinutes != null && task.actualDurationMinutes < 0) {
            errors.add("Actual duration cannot be negative")
        }

        // Validate timestamps
        if (task.createdAt.isAfter(Instant.now().plusSeconds(60))) {
            errors.add("Created timestamp cannot be in the future")
        }
        if (task.updatedAt.isBefore(task.createdAt)) {
            errors.add("Updated timestamp cannot be before created timestamp")
        }
        if (task.completedAt != null && task.completedAt.isBefore(task.createdAt)) {
            errors.add("Completed timestamp cannot be before created timestamp")
        }

        // Validate status consistency
        if (task.status.name == "COMPLETED" && task.completedAt == null) {
            errors.add("Completed task must have completedAt timestamp")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates task creation input.
     *
     * @param title Task title
     * @param todoGroup Todo group
     * @param householdId Household ID
     * @param assignedUserId Assigned user ID
     * @param estimatedDurationMinutes Estimated duration (optional)
     * @return ValidationResult with success status and error messages
     */
    fun validateTaskCreationInput(
        title: String,
        todoGroup: String,
        householdId: String,
        assignedUserId: String,
        estimatedDurationMinutes: Int? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (title.isBlank()) {
            errors.add("Task title cannot be empty")
        }
        if (title.length > 500) {
            errors.add("Task title cannot exceed 500 characters")
        }
        if (todoGroup.isBlank()) {
            errors.add("Todo group cannot be empty")
        }
        if (householdId.isBlank()) {
            errors.add("Household ID cannot be empty")
        }
        if (assignedUserId.isBlank()) {
            errors.add("Assigned user ID cannot be empty")
        }
        if (estimatedDurationMinutes != null && estimatedDurationMinutes < 0) {
            errors.add("Estimated duration cannot be negative if provided")
        }
        if (estimatedDurationMinutes != null && estimatedDurationMinutes > 1440) {
            errors.add("Estimated duration cannot exceed 1440 minutes (24 hours)")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates task update input.
     *
     * @param title New title (optional)
     * @param todoGroup New todo group (optional)
     * @param estimatedDurationMinutes New estimated duration (optional)
     * @return ValidationResult with success status and error messages
     */
    fun validateTaskUpdateInput(
        title: String? = null,
        todoGroup: String? = null,
        estimatedDurationMinutes: Int? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (title != null) {
            if (title.isBlank()) {
                errors.add("Task title cannot be empty")
            }
            if (title.length > 500) {
                errors.add("Task title cannot exceed 500 characters")
            }
        }

        if (todoGroup != null && todoGroup.isBlank()) {
            errors.add("Todo group cannot be empty")
        }

        if (estimatedDurationMinutes != null) {
            if (estimatedDurationMinutes < 0) {
                errors.add("Estimated duration cannot be negative")
            }
            if (estimatedDurationMinutes > 1440) {
                errors.add("Estimated duration cannot exceed 1440 minutes (24 hours)")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
}

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errorList: List<String>) : ValidationResult()

    fun isValid(): Boolean = this is Success
    fun getErrors(): List<String> = if (this is Failure) errorList else emptyList()
}
