package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AffirmationTriggerManager handles affirmation triggers for various events.
 *
 * Provides:
 * - Day completion affirmation trigger
 * - Task completion affirmation trigger
 * - Streak milestone affirmation trigger
 * - Affirmation event flow
 * - Message variety with rotation to avoid repetition
 * - Frequency-based affirmation filtering (1-5 scale)
 *
 * Frequency Scale:
 * - 1 (Rarely): Show affirmations for 20% of task completions
 * - 2 (Infrequently): Show affirmations for 40% of task completions
 * - 3 (Moderate): Show affirmations for 60% of task completions (default)
 * - 4 (Frequently): Show affirmations for 80% of task completions
 * - 5 (Very Frequently): Show affirmations for 100% of task completions
 */
@Singleton
class AffirmationTriggerManager @Inject constructor() {

    private val _affirmationEvent = MutableStateFlow<AffirmationEvent?>(null)
    val affirmationEvent: StateFlow<AffirmationEvent?> = _affirmationEvent

    private var lastDayCompleteTime = 0L
    private var lastTaskCompleteTime = 0L
    
    // Frequency setting (1-5 scale, default 3)
    private var affirmationFrequency = 3

    // Message pools for variety
    private val taskCompleteMessages = listOf(
        "Great job!",
        "You're on a roll!",
        "Awesome work!",
        "Nice one!",
        "Keep it up!",
        "Excellent!",
        "Well done!",
        "You got this!",
        "Fantastic!",
        "Superb!"
    )

    // Streak-aware messages for 3+ day streaks
    private val streakAwareMessages = mapOf(
        3 to listOf(
            "🔥 3-day streak! Keep it going!",
            "🔥 Amazing! 3 days in a row!",
            "🔥 3-day streak! You're unstoppable!"
        ),
        7 to listOf(
            "🏆 Week Warrior! 7 days strong!",
            "🏆 7-day streak! Incredible!",
            "🏆 A full week! You're amazing!"
        ),
        14 to listOf(
            "💪 2-Week Champion! 14 days!",
            "💪 14-day streak! You're crushing it!",
            "💪 Two weeks of consistency!"
        ),
        30 to listOf(
            "🌟 Month Master! 30 days!",
            "🌟 30-day streak! Phenomenal!",
            "🌟 A full month of dedication!"
        ),
        60 to listOf(
            "⭐ 2-Month Legend! 60 days!",
            "⭐ 60-day streak! You're on fire!",
            "⭐ Two months of excellence!"
        ),
        90 to listOf(
            "🚀 3-Month Superstar! 90 days!",
            "🚀 90-day streak! Extraordinary!",
            "🚀 Three months of brilliance!"
        ),
        365 to listOf(
            "👑 Year of Consistency! 365 days!",
            "👑 1-year streak! You're a legend!",
            "👑 A full year of dedication!"
        )
    )

    // Message rotation indices for streak-aware messages
    private val streakAwareIndices = mutableMapOf<Int, Int>()

    private val dayCompleteMessages = listOf(
        "🎉 Perfect day! You crushed it!",
        "🌟 All To Do's complete! Amazing work!",
        "🏆 Day complete! You're unstoppable!",
        "✨ Fantastic! You finished everything!",
        "🚀 Incredible! All tasks done!",
        "💪 You did it! Perfect day!",
        "🎊 Excellent! Day complete!",
        "👑 You're a champion! Day complete!"
    )

    // Message rotation indices to avoid repetition
    private var taskCompleteIndex = 0
    private var dayCompleteIndex = 0

    /**
     * Checks if day is complete and triggers affirmation based on frequency setting.
     *
     * @param tasks Current list of tasks
     * @return True if day complete affirmation was triggered
     */
    fun checkAndTriggerDayCompleteAffirmation(tasks: List<Task>): Boolean {
        if (tasks.isEmpty()) return false

        val allCompleted = tasks.all { it.status == TaskStatus.COMPLETED }

        if (allCompleted) {
            // Check if affirmation should be shown based on frequency setting
            if (!shouldShowAffirmation()) return false

            val currentTime = System.currentTimeMillis()
            // Prevent duplicate triggers within 1 second
            if (currentTime - lastDayCompleteTime > 1000) {
                lastDayCompleteTime = currentTime
                _affirmationEvent.value = AffirmationEvent.DayComplete(
                    message = getDayCompleteMessage(),
                    timestamp = currentTime
                )
                return true
            }
        }

        return false
    }

    /**
     * Checks if task is complete and triggers affirmation based on frequency setting.
     *
     * @param task Completed task
     * @param streakCount Current streak count (optional, for streak-aware affirmations)
     * @return True if task complete affirmation was triggered
     */
    fun checkAndTriggerTaskCompleteAffirmation(task: Task, streakCount: Int = 0): Boolean {
        if (task.status != TaskStatus.COMPLETED) return false

        // Check if affirmation should be shown based on frequency setting
        if (!shouldShowAffirmation()) return false

        val currentTime = System.currentTimeMillis()
        // Prevent duplicate triggers within 500ms
        if (currentTime - lastTaskCompleteTime > 500) {
            lastTaskCompleteTime = currentTime
            _affirmationEvent.value = AffirmationEvent.TaskComplete(
                message = getTaskCompleteMessage(streakCount),
                taskId = task.id,
                timestamp = currentTime,
                streakCount = streakCount
            )
            return true
        }

        return false
    }

    /**
     * Triggers streak milestone affirmation.
     *
     * Streak milestones always show regardless of frequency setting,
     * as they are special achievements that deserve recognition.
     *
     * @param streakCount Current streak count
     * @return True if milestone affirmation was triggered
     */
    fun checkAndTriggerStreakMilestoneAffirmation(streakCount: Int): Boolean {
        val isMilestone = streakCount in listOf(3, 7, 14, 30, 60, 90, 365)

        if (isMilestone) {
            val currentTime = System.currentTimeMillis()
            _affirmationEvent.value = AffirmationEvent.StreakMilestone(
                message = getStreakMilestoneMessage(streakCount),
                streakCount = streakCount,
                timestamp = currentTime
            )
            return true
        }

        return false
    }

    /**
     * Sets the affirmation frequency.
     *
     * @param frequency Frequency level (1-5)
     * @throws IllegalArgumentException if frequency is not in range 1-5
     */
    fun setAffirmationFrequency(frequency: Int) {
        require(frequency in 1..5) { "Affirmation frequency must be between 1 and 5, got $frequency" }
        this.affirmationFrequency = frequency
    }

    /**
     * Gets the current affirmation frequency.
     *
     * @return Frequency level (1-5)
     */
    fun getAffirmationFrequency(): Int = affirmationFrequency

    /**
     * Determines if an affirmation should be shown based on frequency setting.
     *
     * Frequency mapping:
     * - 1 (Rarely): 20% chance (1 in 5)
     * - 2 (Infrequently): 40% chance (2 in 5)
     * - 3 (Moderate): 60% chance (3 in 5) - default
     * - 4 (Frequently): 80% chance (4 in 5)
     * - 5 (Very Frequently): 100% chance (always show)
     *
     * @return True if affirmation should be shown
     */
    private fun shouldShowAffirmation(): Boolean {
        return when (affirmationFrequency) {
            1 -> kotlin.random.Random.nextInt(5) == 0  // 20% chance
            2 -> kotlin.random.Random.nextInt(5) < 2   // 40% chance
            3 -> kotlin.random.Random.nextInt(5) < 3   // 60% chance
            4 -> kotlin.random.Random.nextInt(5) < 4   // 80% chance
            5 -> true                                   // 100% chance
            else -> true                                // Default to always show
        }
    }

    /**
     * Clears the current affirmation event.
     */
    fun clearAffirmation() {
        _affirmationEvent.value = null
    }

    /**
     * Gets the next day complete message with rotation to avoid repetition.
     */
    private fun getDayCompleteMessage(): String {
        val message = dayCompleteMessages[dayCompleteIndex]
        dayCompleteIndex = (dayCompleteIndex + 1) % dayCompleteMessages.size
        return message
    }

    /**
     * Gets the next task complete message with rotation to avoid repetition.
     * If streak is 3+, returns streak-aware message instead.
     */
    private fun getTaskCompleteMessage(streakCount: Int = 0): String {
        // If streak is 3 or higher, use streak-aware messages
        if (streakCount >= 3) {
            // Find the appropriate streak level
            val streakLevel = when {
                streakCount >= 365 -> 365
                streakCount >= 90 -> 90
                streakCount >= 60 -> 60
                streakCount >= 30 -> 30
                streakCount >= 14 -> 14
                streakCount >= 7 -> 7
                streakCount >= 3 -> 3
                else -> 0
            }

            if (streakLevel > 0) {
                val messages = streakAwareMessages[streakLevel] ?: emptyList()
                if (messages.isNotEmpty()) {
                    val index = streakAwareIndices.getOrDefault(streakLevel, 0)
                    val message = messages[index]
                    streakAwareIndices[streakLevel] = (index + 1) % messages.size
                    return message
                }
            }
        }

        // Fall back to regular task complete messages
        val message = taskCompleteMessages[taskCompleteIndex]
        taskCompleteIndex = (taskCompleteIndex + 1) % taskCompleteMessages.size
        return message
    }

    /**
     * Gets a streak milestone message.
     */
    private fun getStreakMilestoneMessage(streakCount: Int): String {
        return when (streakCount) {
            3 -> "🔥 3-Day Streak! Keep it going!"
            7 -> "🏆 Week Warrior! Amazing consistency!"
            14 -> "💪 2-Week Champion! You're unstoppable!"
            30 -> "🌟 Month Master! Incredible dedication!"
            60 -> "⭐ 2-Month Legend! You're on fire!"
            90 -> "🚀 3-Month Superstar! Phenomenal!"
            365 -> "👑 Year of Consistency! You're a legend!"
            else -> "🔥 $streakCount-Day Streak! Amazing!"
        }
    }

    /**
     * Gets the current affirmation event.
     */
    fun getCurrentAffirmation(): AffirmationEvent? = _affirmationEvent.value
}

/**
 * Sealed class for affirmation events.
 */
sealed class AffirmationEvent {
    abstract val message: String
    abstract val timestamp: Long

    data class TaskComplete(
        override val message: String,
        val taskId: String,
        override val timestamp: Long,
        val streakCount: Int = 0
    ) : AffirmationEvent()

    data class DayComplete(
        override val message: String,
        override val timestamp: Long
    ) : AffirmationEvent()

    data class StreakMilestone(
        override val message: String,
        val streakCount: Int,
        override val timestamp: Long
    ) : AffirmationEvent()
}
