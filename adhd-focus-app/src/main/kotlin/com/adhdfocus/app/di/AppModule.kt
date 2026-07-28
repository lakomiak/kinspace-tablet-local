package com.adhdfocus.app.di

import android.content.Context
import androidx.room.Room
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.database.DatabaseInitializer
import com.adhdfocus.app.domain.completion.TaskDayCompletionRepository
import com.adhdfocus.app.domain.persistence.DataCleanupScheduler
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import com.adhdfocus.app.domain.persistence.TaskPersistenceManagerImpl
import com.adhdfocus.app.domain.notification.NotificationPreferencesManager
import com.adhdfocus.app.domain.notification.NotificationPreferencesManagerImpl
import com.adhdfocus.app.domain.notification.UpdateNotificationManager
import com.adhdfocus.app.domain.notification.UpdateNotificationManagerImpl
import com.adhdfocus.app.data.repository.PuzzleRepository
import com.adhdfocus.app.domain.gamification.EfficiencyCalculator
import com.adhdfocus.app.data.dao.BadgeDao
import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.dao.PuzzleProgressDao
import com.adhdfocus.app.data.dao.StreakDao
import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDayCompletionDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.dao.TaskSessionMetricDao
import com.adhdfocus.app.data.dao.TokenTransactionDao
import com.adhdfocus.app.data.dao.UserDao
import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.data.dao.UserSwitchingStateDao
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.domain.theme.ThemeManagerImpl
import com.adhdfocus.app.domain.userswitching.UserSwitchingManager
import com.adhdfocus.app.domain.visibility.TodoGroupVisibilityManager
import com.adhdfocus.app.domain.visibility.TodoGroupVisibilityManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAdhdfocusDatabase(
        @ApplicationContext context: Context
    ): AdhdfocusDatabase {
        return Room.databaseBuilder(
            context,
            AdhdfocusDatabase::class.java,
            "adhdfocus_database"
        )
            // Add migration framework for future schema changes
            .addMigrations(*AdhdfocusDatabase.MIGRATIONS)
            // Fallback to destructive migration for development/testing
            // In production, proper migrations should be implemented
            .fallbackToDestructiveMigration()
            // Initialize database with pre-populated data if needed
            .addCallback(DatabaseInitializer.getCallback())
            .build()
    }

    @Singleton
    @Provides
    fun provideUserSwitchingManager(        userSwitchingRepository: com.adhdfocus.app.data.repository.UserSwitchingRepository
    ): UserSwitchingManager {
        return UserSwitchingManager(userSwitchingRepository)
    }

    @Singleton
    @Provides
    fun provideTaskPersistenceManager(
        database: AdhdfocusDatabase
    ): TaskPersistenceManager {
        return TaskPersistenceManagerImpl(database.taskDao())
    }

    @Singleton
    @Provides
    fun provideTaskDayCompletionRepository(
        database: AdhdfocusDatabase
    ): TaskDayCompletionRepository {
        return TaskDayCompletionRepository(database.taskDayCompletionDao())
    }

    @Singleton
    @Provides
    fun providePuzzleRepository(
        database: AdhdfocusDatabase
    ): PuzzleRepository {
        return PuzzleRepository(database.puzzleProgressDao())
    }

    @Singleton
    @Provides
    fun provideDataCleanupScheduler(
        taskPersistenceManager: TaskPersistenceManager
    ): DataCleanupScheduler {
        return DataCleanupScheduler(taskPersistenceManager)
    }

    @Singleton
    @Provides
    fun provideThemeManager(
        userPreferencesManager: com.adhdfocus.app.domain.preferences.UserPreferencesManager
    ): ThemeManager {
        return ThemeManagerImpl(userPreferencesManager)
    }

    @Singleton
    @Provides
    fun provideNotificationPreferencesManager(
        userPreferencesManager: com.adhdfocus.app.domain.preferences.UserPreferencesManager
    ): NotificationPreferencesManager {
        return NotificationPreferencesManagerImpl(userPreferencesManager)
    }

    @Singleton
    @Provides
    fun provideTodoGroupVisibilityManager(
        userPreferencesManager: com.adhdfocus.app.domain.preferences.UserPreferencesManager
    ): TodoGroupVisibilityManager {
        return TodoGroupVisibilityManagerImpl(userPreferencesManager)
    }

    // DAO providers - extracted from database
    @Singleton @Provides fun provideTaskDao(db: AdhdfocusDatabase): TaskDao = db.taskDao()
    @Singleton @Provides fun provideUserDao(db: AdhdfocusDatabase): UserDao = db.userDao()
    @Singleton @Provides fun provideBadgeDao(db: AdhdfocusDatabase): BadgeDao = db.badgeDao()
    @Singleton @Provides fun provideStreakDao(db: AdhdfocusDatabase): StreakDao = db.streakDao()
    @Singleton @Provides fun provideSyncQueueDao(db: AdhdfocusDatabase): SyncQueueDao = db.syncQueueDao()
    @Singleton @Provides fun provideTaskDayCompletionDao(db: AdhdfocusDatabase): TaskDayCompletionDao = db.taskDayCompletionDao()
    @Singleton @Provides fun provideTaskSessionMetricDao(db: AdhdfocusDatabase): TaskSessionMetricDao = db.taskSessionMetricDao()
    @Singleton @Provides fun provideTokenTransactionDao(db: AdhdfocusDatabase): TokenTransactionDao = db.tokenTransactionDao()
    @Singleton @Provides fun provideEfficiencyMetricDao(db: AdhdfocusDatabase): EfficiencyMetricDao = db.efficiencyMetricDao()
    @Singleton @Provides fun providePuzzleProgressDao(db: AdhdfocusDatabase): PuzzleProgressDao = db.puzzleProgressDao()
    @Singleton @Provides fun provideUserPreferencesDao(db: AdhdfocusDatabase): UserPreferencesDao = db.userPreferencesDao()
    @Singleton @Provides fun provideUserSwitchingStateDao(db: AdhdfocusDatabase): UserSwitchingStateDao = db.userSwitchingStateDao()

    @Singleton
    @Provides
    fun provideUpdateNotificationManager(impl: UpdateNotificationManagerImpl): UpdateNotificationManager = impl

    @Singleton
    @Provides
    fun provideEfficiencyCalculator(): EfficiencyCalculator = EfficiencyCalculator()
}
