package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.TaskStatus

/**
 * TaskStatusTransitionManager handles valid state transitions for tasks.
 *
 * Enforces the following state machine:
 * - INCOMPLETE → IN_PROGRESS (start task)
 * - INCOMPLETE → COMPLETED (mark complete directly)
 * - IN_PROGRESS → COMPLETED (complete task)
 * - IN_PROGRESS → INCOMPLETE (restart task)
 * - COMPLETED → INCOMPLETE (reopen task)
 *
 * Prevents invalid transitions and provides validation for state changes.
 */
class TaskStatusTransitionManager {
    /**
     * Checks if a transition from one status to another is valid.
     *
     * @param fromStatus Current task status
     * @param toStatus Desired task status
     * @return true if transition is valid, false otherwise
     */
    fun isValidTransition(fromStatus: TaskStatus, toStatus: TaskStatus): Boolean {
        // Same status is always valid (no-op)
        if (fromStatus == toStatus) {
            return true
        }

        return when (fromStatus) {
            TaskStatus.INCOMPLETE -> {
                // From INCOMPLETE, can go to IN_PROGRESS or COMPLETED
                toStatus == TaskStatus.IN_PROGRESS || toStatus == TaskStatus.COMPLETED
            }
            TaskStatus.IN_PROGRESS -> {
                // From IN_PROGRESS, can go to COMPLETED or back to INCOMPLETE
                toStatus == TaskStatus.COMPLETED || toStatus == TaskStatus.INCOMPLETE
            }
            TaskStatus.COMPLETED -> {
                // From COMPLETED, can go back to INCOMPLETE
                toStatus == TaskStatus.INCOMPLETE
            }
        }
    }

    /**
     * Gets a human-readable error message for an invalid transition.
     *
     * @param fromStatus Current task status
     * @param toStatus Desired task status
     * @return Error message describing why the transition is invalid
     */
    fun getTransitionErrorMessage(fromStatus: TaskStatus, toStatus: TaskStatus): String {
        return when {
            fromStatus == toStatus -> "Task is already in $fromStatus status"
            fromStatus == TaskStatus.INCOMPLETE && toStatus == TaskStatus.INCOMPLETE ->
                "Task is already incomplete"
            fromStatus == TaskStatus.IN_PROGRESS && toStatus == TaskStatus.IN_PROGRESS ->
                "Task is already in progress"
            fromStatus == TaskStatus.COMPLETED && toStatus == TaskStatus.COMPLETED ->
                "Task is already completed"
            else -> "Cannot transition from $fromStatus to $toStatus"
        }
    }

    /**
     * Gets all valid next statuses from a given status.
     *
     * @param currentStatus Current task status
     * @return List of valid next statuses
     */
    fun getValidNextStatuses(currentStatus: TaskStatus): List<TaskStatus> {
        return when (currentStatus) {
            TaskStatus.INCOMPLETE -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED)
            TaskStatus.IN_PROGRESS -> listOf(TaskStatus.COMPLETED, TaskStatus.INCOMPLETE)
            TaskStatus.COMPLETED -> listOf(TaskStatus.INCOMPLETE)
        }
    }

    /**
     * Validates a sequence of status transitions.
     *
     * @param transitions List of (fromStatus, toStatus) pairs
     * @return ValidationResult indicating if all transitions are valid
     */
    fun validateTransitionSequence(transitions: List<Pair<TaskStatus, TaskStatus>>): ValidationResult {
        val errors = mutableListOf<String>()

        for ((index, transition) in transitions.withIndex()) {
            val (fromStatus, toStatus) = transition
            if (!isValidTransition(fromStatus, toStatus)) {
                errors.add(
                    "Transition $index: ${getTransitionErrorMessage(fromStatus, toStatus)}"
                )
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
}
