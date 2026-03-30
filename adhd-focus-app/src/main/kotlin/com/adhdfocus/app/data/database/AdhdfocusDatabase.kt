package com.adhdfocus.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.data.model.UserSwitchingState
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.model.Streak
import com.adhdfocus.app.data.model.EfficiencyMetric
import com.adhdfocus.app.data.model.SyncQueueItem
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.dao.UserDao
import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.data.dao.UserSwitchingStateDao
import com.adhdfocus.app.data.dao.AffirmationDao
import com.adhdfocus.app.data.dao.BadgeDao
import com.adhdfocus.app.data.dao.StreakDao
import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.OfflineUpdateQueueDao

@Database(
    entities = [
        Task::class,
        User::class,
        UserPreferences::class,
        UserSwitchingState::class,
        Affirmation::class,
        Badge::class,
        Streak::class,
        EfficiencyMetric::class,
        SyncQueueItem::class,
        OfflineUpdateQueueItem::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AdhdfocusDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun userSwitchingStateDao(): UserSwitchingStateDao
    abstract fun affirmationDao(): AffirmationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun streakDao(): StreakDao
    abstract fun efficiencyMetricDao(): EfficiencyMetricDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun offlineUpdateQueueDao(): OfflineUpdateQueueDao

    companion object {
        /**
         * Migration framework for future schema changes.
         * Add new migrations here as the database schema evolves.
         * Format: MIGRATION_X_Y where X is from version and Y is to version
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            // Example migration structure for future use:
            // MIGRATION_1_2
        )
    }
}
