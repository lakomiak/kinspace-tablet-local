package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.data.model.Theme
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Insert
    suspend fun insert(preferences: UserPreferences): Long

    @Update
    suspend fun update(preferences: UserPreferences)

    @Delete
    suspend fun delete(preferences: UserPreferences)

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getPreferencesByUserId(userId: String): UserPreferences?

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getPreferencesByUserIdFlow(userId: String): Flow<UserPreferences?>

    @Query("""
        UPDATE user_preferences 
        SET theme = :theme 
        WHERE userId = :userId
    """)
    suspend fun updateTheme(userId: String, theme: Theme)

    @Query("""
        UPDATE user_preferences 
        SET visibleTodoGroups = :visibleTodoGroups 
        WHERE userId = :userId
    """)
    suspend fun updateVisibleTodoGroups(userId: String, visibleTodoGroups: String)

    @Query("""
        UPDATE user_preferences 
        SET notificationPreferences = :notificationPreferences 
        WHERE userId = :userId
    """)
    suspend fun updateNotificationPreferences(userId: String, notificationPreferences: String)

    @Query("""
        UPDATE user_preferences 
        SET dailyResetTime = :dailyResetTime 
        WHERE userId = :userId
    """)
    suspend fun updateDailyResetTime(userId: String, dailyResetTime: String)

    @Query("""
        UPDATE user_preferences 
        SET affirmationFrequency = :affirmationFrequency 
        WHERE userId = :userId
    """)
    suspend fun updateAffirmationFrequency(userId: String, affirmationFrequency: Int)

    @Query("""
        UPDATE user_preferences 
        SET enableGamification = :enableGamification 
        WHERE userId = :userId
    """)
    suspend fun updateGamificationEnabled(userId: String, enableGamification: Boolean)

    @Query("""
        UPDATE user_preferences 
        SET timerDefaultDuration = :timerDefaultDuration 
        WHERE userId = :userId
    """)
    suspend fun updateTimerDefaultDuration(userId: String, timerDefaultDuration: Int)

    @Query("""
        UPDATE user_preferences 
        SET autoLogoutTimeout = :autoLogoutTimeout 
        WHERE userId = :userId
    """)
    suspend fun updateAutoLogoutTimeout(userId: String, autoLogoutTimeout: Int)

    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun deletePreferencesByUserId(userId: String)

    @Query("""
        SELECT COUNT(*) FROM user_preferences 
        WHERE userId = :userId
    """)
    suspend fun preferencesExist(userId: String): Int

    @Query("""
        SELECT * FROM user_preferences 
        WHERE theme = :theme
    """)
    suspend fun getPreferencesByTheme(theme: Theme): List<UserPreferences>

    @Query("""
        SELECT * FROM user_preferences 
        WHERE enableGamification = 1
    """)
    suspend fun getGamificationEnabledPreferences(): List<UserPreferences>

    @Query("""
        SELECT * FROM user_preferences 
        WHERE autoLogoutTimeout > 0
    """)
    suspend fun getAutoLogoutEnabledPreferences(): List<UserPreferences>
}
