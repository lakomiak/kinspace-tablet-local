package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE householdId = :householdId")
    fun getUsersByHousehold(householdId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE householdId = :householdId")
    suspend fun getUsersByHouseholdOnce(householdId: String): List<User>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        AND role = :role
        ORDER BY displayName ASC
    """)
    fun getUsersByRole(householdId: String, role: UserRole): Flow<List<User>>

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        AND role = :role
        ORDER BY displayName ASC
    """)
    suspend fun getUsersByRoleOnce(householdId: String, role: UserRole): List<User>

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        AND isPinProtected = 1
        ORDER BY displayName ASC
    """)
    fun getPinProtectedUsers(householdId: String): Flow<List<User>>

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        AND isPinProtected = 1
        ORDER BY displayName ASC
    """)
    suspend fun getPinProtectedUsersOnce(householdId: String): List<User>

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        ORDER BY displayName ASC
    """)
    suspend fun getUsersByHouseholdSorted(householdId: String): List<User>

    @Query("""
        SELECT COUNT(*) FROM users 
        WHERE householdId = :householdId
    """)
    suspend fun getUserCountByHousehold(householdId: String): Int

    @Query("""
        SELECT COUNT(*) FROM users 
        WHERE householdId = :householdId 
        AND role = :role
    """)
    suspend fun getUserCountByRole(householdId: String, role: UserRole): Int

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users WHERE householdId = :householdId")
    suspend fun deleteUsersByHousehold(householdId: String)

    @Query("""
        UPDATE users 
        SET isPinProtected = :isPinProtected, pinHash = :pinHash 
        WHERE id = :userId
    """)
    suspend fun updatePinProtection(userId: String, isPinProtected: Boolean, pinHash: String?)

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentUsers(householdId: String, limit: Int): List<User>

    @Query("""
        SELECT * FROM users 
        WHERE householdId = :householdId 
        AND role = :role
        LIMIT 1
    """)
    suspend fun getFirstUserByRole(householdId: String, role: UserRole): User?
}
