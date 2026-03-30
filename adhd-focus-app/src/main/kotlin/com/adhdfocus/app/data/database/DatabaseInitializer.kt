package com.adhdfocus.app.data.database

import androidx.room.RoomDatabase
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationTone
import com.adhdfocus.app.data.model.AffirmationType

/**
 * Handles database initialization with pre-populated data and backup/recovery support.
 * This class manages the initial setup of the database with default affirmations
 * and provides utilities for backup and recovery operations.
 */
object DatabaseInitializer {

    /**
     * Get the Room database callback for initialization.
     * This callback is executed when the database is first created.
     */
    fun getCallback(): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate default affirmations on database creation
                populateDefaultAffirmations(db)
            }
        }
    }

    /**
     * Populate default affirmations into the database.
     * These affirmations are used for task completion, day completion, and streak milestones.
     */
    private fun populateDefaultAffirmations(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Task completion affirmations
        val taskCompletionAffirmations = listOf(
            "Great job!",
            "You're on a roll!",
            "Awesome work!",
            "Nice one!",
            "Keep it up!",
            "Excellent!",
            "You got this!",
            "Fantastic!",
            "Well done!",
            "Crushing it!"
        )

        // Day completion affirmations
        val dayCompletionAffirmations = listOf(
            "Perfect day! You crushed it!",
            "All tasks done! Amazing work!",
            "You completed everything! Fantastic!",
            "100% complete! You're a superstar!",
            "Day complete! You're unstoppable!"
        )

        // Streak milestone affirmations
        val streakMilestoneAffirmations = listOf(
            "3-day streak! Keep it up!",
            "7-day streak! You're on fire!",
            "14-day streak! Incredible!",
            "30-day streak! You're a legend!",
            "Streak milestone! Keep going!"
        )

        // Insert task completion affirmations
        taskCompletionAffirmations.forEachIndexed { index, message ->
            val sql = """
                INSERT INTO affirmations (id, type, message, tone, ageAppropriatenessLevel, createdAt)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            db.execSQL(
                sql,
                arrayOf(
                    "affirmation_task_$index",
                    AffirmationType.TASK_COMPLETION.name,
                    message,
                    AffirmationTone.ENCOURAGING.name,
                    3,
                    System.currentTimeMillis()
                )
            )
        }

        // Insert day completion affirmations
        dayCompletionAffirmations.forEachIndexed { index, message ->
            val sql = """
                INSERT INTO affirmations (id, type, message, tone, ageAppropriatenessLevel, createdAt)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            db.execSQL(
                sql,
                arrayOf(
                    "affirmation_day_$index",
                    AffirmationType.DAY_COMPLETION.name,
                    message,
                    AffirmationTone.CELEBRATORY.name,
                    3,
                    System.currentTimeMillis()
                )
            )
        }

        // Insert streak milestone affirmations
        streakMilestoneAffirmations.forEachIndexed { index, message ->
            val sql = """
                INSERT INTO affirmations (id, type, message, tone, ageAppropriatenessLevel, createdAt)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            db.execSQL(
                sql,
                arrayOf(
                    "affirmation_streak_$index",
                    AffirmationType.STREAK_MILESTONE.name,
                    message,
                    AffirmationTone.MOTIVATIONAL.name,
                    3,
                    System.currentTimeMillis()
                )
            )
        }
    }
}
