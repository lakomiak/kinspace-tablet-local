package com.adhdfocus.app.util

/**
 * Application-wide constants.
 */
object Constants {
    // API Configuration
    const val API_BASE_URL = "https://api.calendar-cloud.example.com"
    const val WEBSOCKET_URL = "wss://ws.calendar-cloud.example.com"

    // Database
    const val DATABASE_NAME = "adhdfocus.db"
    const val DATABASE_VERSION = 1

    // Sync Configuration
    const val SYNC_RETRY_MAX_ATTEMPTS = 5
    const val SYNC_RETRY_INITIAL_DELAY_MS = 1000L
    const val SYNC_RETRY_MAX_DELAY_MS = 30000L

    // Timer Configuration
    const val TIMER_WARNING_THRESHOLD_50_PERCENT = 0.5f
    const val TIMER_WARNING_THRESHOLD_90_PERCENT = 0.9f

    // Affirmation Configuration
    const val AFFIRMATION_DISPLAY_DURATION_MS = 3000L

    // Streak Configuration
    const val STREAK_MILESTONE_3_DAYS = 3
    const val STREAK_MILESTONE_7_DAYS = 7
    const val STREAK_MILESTONE_14_DAYS = 14
    const val STREAK_MILESTONE_30_DAYS = 30

    // Data Retention
    const val TASK_HISTORY_RETENTION_DAYS = 30
    const val TASK_CLEANUP_THRESHOLD_DAYS = 90

    // Performance
    const val DAILY_FOCUS_VIEW_LOAD_TIME_TARGET_MS = 1000L
    const val SCROLL_PERFORMANCE_TARGET_FPS = 60
    const val BUTTON_TAP_FEEDBACK_TARGET_MS = 100L

    // Shared Preferences Keys
    const val PREF_CURRENT_USER_ID = "current_user_id"
    const val PREF_CURRENT_HOUSEHOLD_ID = "current_household_id"
    const val PREF_THEME = "theme"
    const val PREF_LAST_SYNC_TIME = "last_sync_time"
}
