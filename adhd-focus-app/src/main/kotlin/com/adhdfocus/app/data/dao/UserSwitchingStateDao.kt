package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.UserSwitchingState
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSwitchingStateDao {
    @Insert
    suspend fun insert(state: UserSwitchingState): Long

    @Update
    suspend fun update(state: UserSwitchingState)

    @Delete
    suspend fun delete(state: UserSwitchingState)

    @Query("SELECT * FROM current_user WHERE id = 'current_user'")
    suspend fun getCurrentUserState(): UserSwitchingState?

    @Query("SELECT * FROM current_user WHERE id = 'current_user'")
    fun getCurrentUserStateFlow(): Flow<UserSwitchingState?>

    @Query("DELETE FROM current_user WHERE id = 'current_user'")
    suspend fun clearCurrentUser()

    @Query("""
        UPDATE current_user 
        SET userId = :userId, lastSwitchTime = :lastSwitchTime 
        WHERE id = 'current_user'
    """)
    suspend fun updateCurrentUser(userId: String, lastSwitchTime: Long)
}
