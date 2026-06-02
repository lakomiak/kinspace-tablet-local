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
import com.adhdfocus.app.data.model.PuzzleProgress
import com.adhdfocus.app.data.model.TaskSessionMetric
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
import com.adhdfocus.app.data.dao.PuzzleProgressDao
import com.adhdfocus.app.data.dao.TaskSessionMetricDao
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
        TaskDayCompletion::class,
        PuzzleProgress::class,
        TaskSessionMetric::class
    ],
    version = 18,
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
    abstract fun puzzleProgressDao(): PuzzleProgressDao
    abstract fun taskSessionMetricDao(): TaskSessionMetricDao

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

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue_new (
                        id TEXT NOT NULL,
                        taskId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO sync_queue_new (id, taskId, userId, operation, payload, timestamp, retryCount)
                    SELECT id, taskId, userId, operation, payload, timestamp, retryCount
                    FROM sync_queue
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE sync_queue")
                db.execSQL("ALTER TABLE sync_queue_new RENAME TO sync_queue")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_taskId ON sync_queue(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_userId ON sync_queue(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_operation ON sync_queue(operation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_timestamp ON sync_queue(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_retryCount ON sync_queue(retryCount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_userId_timestamp ON sync_queue(userId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_operation_timestamp ON sync_queue(operation, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_retryCount_timestamp ON sync_queue(retryCount, timestamp)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_update_queue_new (
                        id TEXT NOT NULL,
                        taskId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        updateType TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        applied INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO offline_update_queue_new (id, taskId, userId, updateType, payload, timestamp, applied)
                    SELECT id, taskId, userId, updateType, payload, timestamp, applied
                    FROM offline_update_queue
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE offline_update_queue")
                db.execSQL("ALTER TABLE offline_update_queue_new RENAME TO offline_update_queue")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_update_queue_taskId ON offline_update_queue(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_update_queue_userId ON offline_update_queue(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_update_queue_timestamp ON offline_update_queue(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_update_queue_userId_timestamp ON offline_update_queue(userId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_update_queue_applied ON offline_update_queue(applied)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN configuredDurationSeconds INTEGER")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN actualDurationSeconds INTEGER")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN totalPausedSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN pauseCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN resetCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN timerStartedAt INTEGER")
                db.execSQL("ALTER TABLE efficiency_metrics ADD COLUMN timerStoppedAt INTEGER")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN puzzleAgeBand TEXT NOT NULL DEFAULT '5-6'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS puzzle_progress (
                        id TEXT NOT NULL,
                        householdId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        ageBandKey TEXT NOT NULL,
                        cycleIndex INTEGER NOT NULL,
                        puzzleKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        totalPieces INTEGER NOT NULL,
                        piecesUnlocked INTEGER NOT NULL,
                        lastCompletedDay TEXT,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_puzzle_progress_householdId ON puzzle_progress(householdId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_puzzle_progress_userId ON puzzle_progress(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_puzzle_progress_ageBandKey ON puzzle_progress(ageBandKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_puzzle_progress_householdId_userId_ageBandKey ON puzzle_progress(householdId, userId, ageBandKey)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_puzzle_progress_householdId_userId_ageBandKey_cycleIndex ON puzzle_progress(householdId, userId, ageBandKey, cycleIndex)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN birthDate TEXT")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_session_metrics (
                        id TEXT NOT NULL,
                        taskId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        householdId TEXT NOT NULL,
                        configuredDurationSeconds INTEGER,
                        activeDurationSeconds INTEGER NOT NULL,
                        totalPausedSeconds INTEGER NOT NULL,
                        pauseCount INTEGER NOT NULL,
                        resetCount INTEGER NOT NULL,
                        timerStartedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL,
                        outcome TEXT NOT NULL,
                        completedTask INTEGER NOT NULL,
                        completedAfterTimerEnded INTEGER NOT NULL,
                        stoppedBeforeTimerEnded INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_session_metrics_taskId ON task_session_metrics(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_session_metrics_userId ON task_session_metrics(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_session_metrics_householdId ON task_session_metrics(householdId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_session_metrics_endedAt ON task_session_metrics(endedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_task_session_user_ended_at ON task_session_metrics(userId, endedAt)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_puzzle_progress_ageBandKey ON puzzle_progress(ageBandKey)")
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
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18
        )
    }
}
