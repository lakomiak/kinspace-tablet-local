package com.adhdfocus.app.di

import android.content.Context
import androidx.room.Room
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.database.DatabaseInitializer
import com.adhdfocus.app.data.network.ApiConfig
import com.adhdfocus.app.data.network.AuthInterceptor
import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.HouseholdNotificationSettingsService
import com.adhdfocus.app.data.network.TokenRefreshInterceptor
import com.adhdfocus.app.data.network.SyncService
import com.adhdfocus.app.data.network.TaskService
import com.adhdfocus.app.data.security.TokenStorage
import com.adhdfocus.app.domain.auth.AuthManager
import com.adhdfocus.app.domain.completion.TaskDayCompletionRepository
import com.adhdfocus.app.domain.persistence.DataCleanupScheduler
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import com.adhdfocus.app.domain.persistence.TaskPersistenceManagerImpl
import com.adhdfocus.app.domain.preferences.CloudCustomTodoGroupsSyncManager
import com.adhdfocus.app.domain.notification.NotificationPreferencesManager
import com.adhdfocus.app.domain.notification.NotificationPreferencesManagerImpl
import com.adhdfocus.app.domain.notification.UpdateNotificationManager
import com.adhdfocus.app.domain.notification.UpdateNotificationManagerImpl
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.sync.CloudSyncManagerImpl
import com.adhdfocus.app.domain.sync.ConflictResolver
import com.adhdfocus.app.domain.sync.ConflictResolverImpl
import com.adhdfocus.app.domain.sync.ConnectivityManager
import com.adhdfocus.app.domain.sync.ConnectivityManagerImpl
import com.adhdfocus.app.domain.sync.RestApiClient
import com.adhdfocus.app.domain.sync.RestApiClientImpl
import com.adhdfocus.app.domain.sync.RetryPolicy
import com.adhdfocus.app.domain.sync.ExponentialBackoffRetryPolicy
import com.adhdfocus.app.domain.sync.TokenProvider
import com.adhdfocus.app.domain.gamification.EfficiencyCalculator
import com.google.gson.Gson
import com.adhdfocus.app.data.dao.BadgeDao
import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.dao.StreakDao
import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDayCompletionDao
import com.adhdfocus.app.data.dao.TaskDao
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    fun provideTokenStorage(
        @ApplicationContext context: Context
    ): TokenStorage {
        return TokenStorage(context)
    }

    @Singleton
    @Provides
    fun provideRetrofit(
        tokenStorage: TokenStorage,
        authManager: AuthManager
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(authManager))
            .addInterceptor(TokenRefreshInterceptor(tokenStorage, authManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Singleton
    @Provides
    fun provideTaskService(retrofit: Retrofit): TaskService {
        return retrofit.create(TaskService::class.java)
    }

    @Singleton
    @Provides
    fun provideSyncService(retrofit: Retrofit): SyncService {
        return retrofit.create(SyncService::class.java)
    }

    @Singleton
    @Provides
    fun provideHouseholdNotificationSettingsService(retrofit: Retrofit): HouseholdNotificationSettingsService {
        return retrofit.create(HouseholdNotificationSettingsService::class.java)
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
    @Singleton @Provides fun provideEfficiencyMetricDao(db: AdhdfocusDatabase): EfficiencyMetricDao = db.efficiencyMetricDao()
    @Singleton @Provides fun provideUserPreferencesDao(db: AdhdfocusDatabase): UserPreferencesDao = db.userPreferencesDao()
    @Singleton @Provides fun provideUserSwitchingStateDao(db: AdhdfocusDatabase): UserSwitchingStateDao = db.userSwitchingStateDao()

    @Singleton
    @Provides
    fun provideUpdateNotificationManager(impl: UpdateNotificationManagerImpl): UpdateNotificationManager = impl

    @Singleton
    @Provides
    fun provideCloudSyncManager(impl: CloudSyncManagerImpl): CloudSyncManager = impl

    @Singleton
    @Provides
    fun provideConflictResolver(impl: ConflictResolverImpl): ConflictResolver = impl

    @Singleton
    @Provides
    fun provideConnectivityManager(impl: ConnectivityManagerImpl): ConnectivityManager = impl

    @Singleton
    @Provides
    fun provideRestApiClient(impl: RestApiClientImpl): RestApiClient = impl

    @Singleton
    @Provides
    fun provideRetryPolicy(): RetryPolicy = ExponentialBackoffRetryPolicy()

    @Singleton
    @Provides
    fun provideEfficiencyCalculator(): EfficiencyCalculator = EfficiencyCalculator()

    @Singleton
    @Provides
    fun provideGson(): Gson = Gson()

    @Singleton
    @Provides
    fun provideTokenProvider(authManager: AuthManager): TokenProvider {
        return object : TokenProvider {
            override suspend fun getAccessToken(): String {
                return authManager.getIdToken()
                    ?: authManager.getValidAccessToken()
                    ?: ""
            }
        }
    }
}
