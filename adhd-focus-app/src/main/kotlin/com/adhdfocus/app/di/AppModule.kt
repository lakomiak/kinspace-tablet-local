package com.adhdfocus.app.di

import android.content.Context
import androidx.room.Room
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.database.DatabaseInitializer
import com.adhdfocus.app.data.network.ApiConfig
import com.adhdfocus.app.data.network.AuthInterceptor
import com.adhdfocus.app.data.network.AuthService
import com.adhdfocus.app.data.network.SyncService
import com.adhdfocus.app.data.network.TaskService
import com.adhdfocus.app.data.network.TokenRefreshInterceptor
import com.adhdfocus.app.data.security.TokenStorage
import com.adhdfocus.app.domain.auth.AuthManager
import com.adhdfocus.app.domain.persistence.DataCleanupScheduler
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import com.adhdfocus.app.domain.persistence.TaskPersistenceManagerImpl
import com.adhdfocus.app.domain.notification.NotificationPreferencesManager
import com.adhdfocus.app.domain.notification.NotificationPreferencesManagerImpl
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
        tokenStorage: TokenStorage
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(TokenRefreshInterceptor(tokenStorage) { provideRetrofit(tokenStorage) })
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
    fun provideAuthManager(
        authService: AuthService,
        tokenStorage: TokenStorage
    ): AuthManager {
        return AuthManager(authService, tokenStorage)
    }

    @Singleton
    @Provides
    fun provideUserSwitchingManager(
        userSwitchingRepository: com.adhdfocus.app.data.repository.UserSwitchingRepository
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
}
