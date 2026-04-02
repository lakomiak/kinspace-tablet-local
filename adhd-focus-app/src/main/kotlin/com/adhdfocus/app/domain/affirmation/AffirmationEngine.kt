package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import com.adhdfocus.app.data.repository.AffirmationRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * AffirmationEngine delivers positive reinforcement at key moments.
 *
 * Provides:
 * - Task completion affirmations
 * - Day completion affirmations
 * - Streak milestone affirmations
 * - Message variety to avoid repetition
 */
class AffirmationEngine @Inject constructor(
    private val affirmationRepository: AffirmationRepository
) {
    private val random = Random(System.currentTimeMillis())

    /**
     * Gets an affirmation for task completion.
     *
     * @return Random task completion affirmation
     */
    suspend fun getTaskCompletionAffirmation(): Affirmation {
        val affirmations = affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION)
        return if (affirmations.isNotEmpty()) {
            affirmations[random.nextInt(affirmations.size)]
        } else {
            Affirmation(
                id = java.util.UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = AffirmationTone.ENCOURAGING,
                ageAppropriatenessLevel = 3
            )
        }
    }

    /**
     * Gets an affirmation for day completion.
     *
     * @return Random day completion affirmation
     */
    suspend fun getDayCompletionAffirmation(): Affirmation {
        val affirmations = affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION)
        return if (affirmations.isNotEmpty()) {
            affirmations[random.nextInt(affirmations.size)]
        } else {
            Affirmation(
                id = java.util.UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day! You crushed it!",
                tone = AffirmationTone.CELEBRATORY,
                ageAppropriatenessLevel = 4
            )
        }
    }

    /**
     * Gets an affirmation for a streak milestone.
     *
     * @param streakCount Current streak count
     * @return Streak milestone affirmation
     */
    suspend fun getStreakMilestoneAffirmation(streakCount: Int): Affirmation {
        val affirmations = affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE)
        val affirmation = if (affirmations.isNotEmpty()) {
            affirmations[random.nextInt(affirmations.size)]
        } else {
            Affirmation(
                id = java.util.UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "$streakCount-day streak! Keep it up!",
                tone = AffirmationTone.MOTIVATIONAL,
                ageAppropriatenessLevel = 4
            )
        }

        // Customize message with streak count
        return affirmation.copy(
            message = affirmation.message.replace("{streak}", streakCount.toString())
        )
    }

    /**
     * Gets a random affirmation of any type.
     *
     * @return Random affirmation
     */
    suspend fun getRandomAffirmation(): Affirmation {
        val affirmations = affirmationRepository.getAllAffirmations()
        return if (affirmations.isNotEmpty()) {
            affirmations[random.nextInt(affirmations.size)]
        } else {
            Affirmation(
                id = java.util.UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "You're doing great!",
                tone = AffirmationTone.ENCOURAGING,
                ageAppropriatenessLevel = 3
            )
        }
    }

    /**
     * Gets affirmations suitable for a specific age group.
     *
     * @param ageAppropriatenessLevel Age appropriateness level (1-5, where 5 is most mature)
     * @return List of affirmations suitable for the age level
     */
    suspend fun getAffirmationsForAgeLevel(ageAppropriatenessLevel: Int): List<Affirmation> {
        return affirmationRepository.getAffirmationsByAgeLevel(ageAppropriatenessLevel)
    }
}
