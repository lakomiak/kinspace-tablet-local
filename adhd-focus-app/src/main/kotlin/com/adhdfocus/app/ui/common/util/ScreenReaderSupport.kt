package com.adhdfocus.app.ui.common.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import java.time.Duration

/**
 * Utility functions for screen reader support and semantic descriptions.
 * Provides accessible content descriptions for all UI elements.
 */
object ScreenReaderSupport {

    /**
     * Generates accessible description for a To Do.
     * @param task The To Do to describe
     * @return Accessible description string
     */
    fun getTaskDescription(task: Task): String {
        val statusText = when (task.status) {
            TaskStatus.INCOMPLETE -> "incomplete"
            TaskStatus.IN_PROGRESS -> "in progress"
            TaskStatus.COMPLETED -> "completed"
        }

        val durationText = when {
            (task.timerDurationMs ?: 0L) > 0L -> {
                val totalSeconds = Duration.ofMillis(task.timerDurationMs!!).seconds.toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                when {
                    minutes > 0 && seconds > 0 -> ", estimated $minutes minutes $seconds seconds"
                    minutes > 0 -> ", estimated $minutes minutes"
                    else -> ", estimated $seconds seconds"
                }
            }
            (task.estimatedDurationMinutes ?: 0) > 0 && (task.estimatedDurationSeconds ?: 0) > 0 ->
                ", estimated ${task.estimatedDurationMinutes} minutes ${task.estimatedDurationSeconds} seconds"
            (task.estimatedDurationMinutes ?: 0) > 0 ->
                ", estimated ${task.estimatedDurationMinutes} minutes"
            (task.estimatedDurationSeconds ?: 0) > 0 ->
                ", estimated ${task.estimatedDurationSeconds} seconds"
            else -> ""
        }

        return "${task.title}, $statusText$durationText"
    }

    /**
     * Generates accessible description for To Do status.
     * @param status The To Do status
     * @return Accessible status description
     */
    fun getStatusDescription(status: TaskStatus): String {
        return when (status) {
            TaskStatus.INCOMPLETE -> "To Do is incomplete"
            TaskStatus.IN_PROGRESS -> "To Do is in progress"
            TaskStatus.COMPLETED -> "To Do is completed"
        }
    }

    /**
     * Generates accessible description for completion percentage.
     * @param completed Number of completed To Do's
     * @param total Total number of To Do's
     * @return Accessible percentage description
     */
    fun getCompletionDescription(completed: Int, total: Int): String {
        val percentage = if (total > 0) (completed * 100) / total else 0
        return "$completed of $total To Do's complete, $percentage percent"
    }

    /**
     * Generates accessible description for streak.
     * @param streakCount Current streak count
     * @return Accessible streak description
     */
    fun getStreakDescription(streakCount: Int): String {
        return when {
            streakCount == 0 -> "No current streak"
            streakCount == 1 -> "1 day streak"
            else -> "$streakCount day streak"
        }
    }

    /**
     * Generates accessible description for timer.
     * @param remainingSeconds Remaining time in seconds
     * @return Accessible timer description
     */
    fun getTimerDescription(remainingSeconds: Long): String {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return when {
            minutes > 0 -> "$minutes minutes $seconds seconds remaining"
            else -> "$seconds seconds remaining"
        }
    }

    /**
     * Generates accessible description for badge.
     * @param badgeName Name of the badge
     * @param isEarned Whether the badge has been earned
     * @return Accessible badge description
     */
    fun getBadgeDescription(badgeName: String, isEarned: Boolean): String {
        return if (isEarned) {
            "$badgeName badge earned"
        } else {
            "$badgeName badge locked"
        }
    }

    /**
     * Generates accessible description for efficiency metric.
     * @param efficiency Efficiency percentage
     * @return Accessible efficiency description
     */
    fun getEfficiencyDescription(efficiency: Double): String {
        return when {
            efficiency > 100 -> "Completed ${(efficiency - 100).toInt()}% faster than estimated"
            efficiency < 100 -> "Completed ${(100 - efficiency).toInt()}% slower than estimated"
            else -> "Completed at estimated time"
        }
    }

    /**
     * Generates accessible description for sync status.
     * @param isSyncing Whether currently syncing
     * @param isOnline Whether online
     * @return Accessible sync status description
     */
    fun getSyncStatusDescription(isSyncing: Boolean, isOnline: Boolean): String {
        return when {
            isSyncing -> "Syncing with cloud"
            isOnline -> "Synced"
            else -> "Offline, changes will sync when online"
        }
    }
}

/**
 * Applies semantic content description to a modifier.
 * @param description The content description for screen readers
 * @return Modified Modifier with semantic description
 */
fun Modifier.screenReaderDescription(description: String): Modifier {
    return this.semantics {
        contentDescription = description
    }
}

/**
 * Applies semantic role and description to a modifier.
 * @param role The semantic role (Button, Checkbox, etc.)
 * @param description The content description for screen readers
 * @return Modified Modifier with semantic role and description
 */
fun Modifier.screenReaderRole(role: Role, description: String): Modifier {
    return this.semantics {
        this.role = role
        contentDescription = description
    }
}

/**
 * Applies semantic state description to a modifier.
 * @param stateDescription The state description for screen readers
 * @return Modified Modifier with semantic state description
 */
fun Modifier.screenReaderState(stateDescription: String): Modifier {
    return this.semantics {
        this.stateDescription = stateDescription
    }
}

/**
 * Applies both content and state descriptions to a modifier.
 * @param contentDescription The content description
 * @param stateDescription The state description
 * @return Modified Modifier with both descriptions
 */
fun Modifier.screenReaderDescriptions(
    contentDescription: String,
    stateDescription: String
): Modifier {
    return this.semantics {
        this.contentDescription = contentDescription
        this.stateDescription = stateDescription
    }
}
