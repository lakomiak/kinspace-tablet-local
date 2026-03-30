package com.adhdfocus.app.domain.persistence

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataCleanupScheduler handles automatic cleanup of old task data.
 *
 * Responsibilities:
 * - Schedule daily cleanup of tasks older than 90 days
 * - Run cleanup in background without blocking UI
 * - Log cleanup results
 * - Handle cleanup errors gracefully
 *
 * This scheduler implements the cleanup mechanism specified in
 * Requirement 12: Data Persistence and Offline Capability.
 */
@Singleton
class DataCleanupScheduler @Inject constructor(
    private val taskPersistenceManager: TaskPersistenceManager
) {

    companion object {
        private const val TAG = "DataCleanupScheduler"
        const val CLEANUP_CUTOFF_DAYS = 90
        const val CLEANUP_INTERVAL_HOURS = 24L
    }

    private var isScheduled = false

    /**
     * Starts the cleanup scheduler.
     * Cleanup runs daily at the specified interval.
     */
    fun startScheduler() {
        if (isScheduled) {
            Log.d(TAG, "Scheduler already running")
            return
        }

        isScheduled = true
        Log.d(TAG, "Starting data cleanup scheduler")

        CoroutineScope(Dispatchers.Default).launch {
            while (isScheduled) {
                try {
                    performCleanup()
                    delay(CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000) // Convert hours to milliseconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup cycle", e)
                    // Continue scheduling despite error
                    delay(CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000)
                }
            }
        }
    }

    /**
     * Stops the cleanup scheduler.
     */
    fun stopScheduler() {
        isScheduled = false
        Log.d(TAG, "Stopping data cleanup scheduler")
    }

    /**
     * Performs a single cleanup operation.
     * Deletes tasks older than CLEANUP_CUTOFF_DAYS.
     */
    suspend fun performCleanup() {
        try {
            Log.d(TAG, "Starting cleanup of tasks older than $CLEANUP_CUTOFF_DAYS days")

            val deletedCount = taskPersistenceManager.deleteOldTasks(CLEANUP_CUTOFF_DAYS)

            Log.d(TAG, "Cleanup completed. Deleted $deletedCount old tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            throw e
        }
    }

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if scheduler is running, false otherwise
     */
    fun isRunning(): Boolean = isScheduled
}
