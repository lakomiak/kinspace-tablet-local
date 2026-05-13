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
import com.adhdfocus.app.data.model.TaskDayCompletion
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
import com.adhdfocus.app.data.dao.TaskDayCompletionDao

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
        OfflineUpdateQueueItem::class,
        TaskDayCompletion::class
    ],
    version = 12,
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
    abstract fun taskDayCompletionDao(): TaskDayCompletionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueDate TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks RENAME TO tasks_old")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT NOT NULL,
                        householdId TEXT NOT NULL,
                        assignedUserId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        todoGroup TEXT NOT NULL,
                        estimatedDurationMinutes INTEGER,
                        actualDurationMinutes INTEGER,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        syncStatus TEXT NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        dueDate INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO tasks (
                        id, householdId, assignedUserId, title, description, todoGroup,
                        estimatedDurationMinutes, actualDurationMinutes, status, createdAt,
                        updatedAt, completedAt, syncStatus, isDeleted, dueDate
                    )
                    SELECT
                        id,
                        householdId,
                        assignedUserId,
                        title,
                        description,
                        todoGroup,
                        estimatedDurationMinutes,
                        actualDurationMinutes,
                        status,
                        createdAt,
                        updatedAt,
                        completedAt,
                        syncStatus,
                        isDeleted,
                        CASE
                            WHEN dueDate IS NULL OR TRIM(dueDate) = '' THEN NULL
                            WHEN instr(dueDate, 'T') > 0 THEN CAST(strftime('%s', dueDate) AS INTEGER) * 1000
                            ELSE CAST(strftime('%s', dueDate || 'T00:00:00Z') AS INTEGER) * 1000
                        END
                    FROM tasks_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE tasks_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_householdId ON tasks(householdId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assignedUserId ON tasks(assignedUserId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_syncStatus ON tasks(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_todoGroup ON tasks(todoGroup)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDate ON tasks(dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_createdAt ON tasks(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_updatedAt ON tasks(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isDeleted ON tasks(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_householdId_status ON tasks(householdId, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assignedUserId_status ON tasks(assignedUserId, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_householdId_todoGroup ON tasks(householdId, todoGroup)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assignedUserId_todoGroup ON tasks(assignedUserId, todoGroup)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_householdId_syncStatus ON tasks(householdId, syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assignedUserId_syncStatus ON tasks(assignedUserId, syncStatus)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'once'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedDurationSeconds INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN timerDurationMs INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN settingsPasscodeHash TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN enableTodoEditing INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences RENAME TO user_preferences_old")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        userId TEXT NOT NULL,
                        theme TEXT NOT NULL,
                        visibleTodoGroups TEXT NOT NULL,
                        notificationPreferences TEXT NOT NULL,
                        settingsPasscodeHash TEXT,
                        enableTodoEditing INTEGER NOT NULL,
                        dailyResetTime TEXT NOT NULL,
                        affirmationFrequency INTEGER NOT NULL,
                        enableGamification INTEGER NOT NULL,
                        enableBadges INTEGER NOT NULL,
                        enableStreaks INTEGER NOT NULL,
                        enableEfficiencyMetrics INTEGER NOT NULL,
                        timerDefaultDuration INTEGER NOT NULL,
                        autoLogoutTimeout INTEGER NOT NULL,
                        PRIMARY KEY(userId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO user_preferences (
                        userId, theme, visibleTodoGroups, notificationPreferences, settingsPasscodeHash,
                        enableTodoEditing, dailyResetTime, affirmationFrequency, enableGamification,
                        enableBadges, enableStreaks, enableEfficiencyMetrics, timerDefaultDuration, autoLogoutTimeout
                    )
                    SELECT
                        userId,
                        theme,
                        visibleTodoGroups,
                        notificationPreferences,
                        settingsPasscodeHash,
                        COALESCE(enableTodoEditing, 0),
                        dailyResetTime,
                        affirmationFrequency,
                        enableGamification,
                        enableBadges,
                        enableStreaks,
                        enableEfficiencyMetrics,
                        timerDefaultDuration,
                        autoLogoutTimeout
                    FROM user_preferences_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE user_preferences_old")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_day_completions (
                        householdId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        taskId TEXT NOT NULL,
                        targetDate TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(householdId, userId, taskId, targetDate)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_day_completions_householdId ON task_day_completions(householdId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_day_completions_userId ON task_day_completions(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_day_completions_taskId ON task_day_completions(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_day_completions_targetDate ON task_day_completions(targetDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_day_completions_householdId_userId_targetDate ON task_day_completions(householdId, userId, targetDate)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE badges ADD COLUMN seasonYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE badges
                    SET seasonYear = CASE
                        WHEN seasonYear = 0 THEN CAST(strftime('%Y', datetime(earnedAt / 1000, 'unixepoch')) AS INTEGER)
                        ELSE seasonYear
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN customTodoGroups TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * Migration framework for future schema changes.
         * Add new migrations here as the database schema evolves.
         * Format: MIGRATION_X_Y where X is from version and Y is to version
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12
        )
    }
}
