package com.adhdfocus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.model.AffirmationTone
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AffirmationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(affirmation: Affirmation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(affirmations: List<Affirmation>)

    @Update
    suspend fun update(affirmation: Affirmation)

    @Delete
    suspend fun delete(affirmation: Affirmation)

    @Query("SELECT * FROM affirmations WHERE id = :affirmationId")
    suspend fun getAffirmationById(affirmationId: String): Affirmation?

    @Query("SELECT * FROM affirmations WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getAffirmationsByType(type: AffirmationType): List<Affirmation>

    @Query("SELECT * FROM affirmations WHERE type = :type ORDER BY createdAt DESC")
    fun getAffirmationsByTypeFlow(type: AffirmationType): Flow<List<Affirmation>>

    @Query("SELECT * FROM affirmations WHERE tone = :tone ORDER BY createdAt DESC")
    suspend fun getAffirmationsByTone(tone: AffirmationTone): List<Affirmation>

    @Query("SELECT * FROM affirmations WHERE tone = :tone ORDER BY createdAt DESC")
    fun getAffirmationsByToneFlow(tone: AffirmationTone): Flow<List<Affirmation>>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type AND tone = :tone 
        ORDER BY createdAt DESC
    """)
    suspend fun getAffirmationsByTypeAndTone(type: AffirmationType, tone: AffirmationTone): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type AND tone = :tone 
        ORDER BY createdAt DESC
    """)
    fun getAffirmationsByTypeAndToneFlow(type: AffirmationType, tone: AffirmationTone): Flow<List<Affirmation>>

    @Query("""
        SELECT * FROM affirmations 
        WHERE ageAppropriatenessLevel >= :minLevel 
        AND ageAppropriatenessLevel <= :maxLevel
        ORDER BY createdAt DESC
    """)
    suspend fun getAffirmationsByAgeLevel(minLevel: Int, maxLevel: Int): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE ageAppropriatenessLevel >= :minLevel 
        AND ageAppropriatenessLevel <= :maxLevel
        ORDER BY createdAt DESC
    """)
    fun getAffirmationsByAgeLevelFlow(minLevel: Int, maxLevel: Int): Flow<List<Affirmation>>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type 
        AND ageAppropriatenessLevel >= :minLevel 
        AND ageAppropriatenessLevel <= :maxLevel
        ORDER BY createdAt DESC
    """)
    suspend fun getAffirmationsByTypeAndAgeLevel(
        type: AffirmationType,
        minLevel: Int,
        maxLevel: Int
    ): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type 
        AND ageAppropriatenessLevel >= :minLevel 
        AND ageAppropriatenessLevel <= :maxLevel
        ORDER BY createdAt DESC
    """)
    fun getAffirmationsByTypeAndAgeLevelFlow(
        type: AffirmationType,
        minLevel: Int,
        maxLevel: Int
    ): Flow<List<Affirmation>>

    @Query("""
        SELECT * FROM affirmations 
        WHERE createdAt >= :startTime 
        AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    suspend fun getAffirmationsInDateRange(startTime: Instant, endTime: Instant): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE createdAt >= :startTime 
        AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    fun getAffirmationsInDateRangeFlow(startTime: Instant, endTime: Instant): Flow<List<Affirmation>>

    @Query("SELECT * FROM affirmations ORDER BY createdAt DESC")
    fun getAllAffirmations(): Flow<List<Affirmation>>

    @Query("SELECT * FROM affirmations ORDER BY createdAt DESC")
    suspend fun getAllAffirmationsOnce(): List<Affirmation>

    @Query("SELECT * FROM affirmations ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomAffirmations(limit: Int): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type 
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomAffirmationsByType(type: AffirmationType, limit: Int): List<Affirmation>

    @Query("""
        SELECT * FROM affirmations 
        WHERE type = :type 
        AND ageAppropriatenessLevel >= :minLevel 
        AND ageAppropriatenessLevel <= :maxLevel
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomAffirmationsByTypeAndAgeLevel(
        type: AffirmationType,
        minLevel: Int,
        maxLevel: Int,
        limit: Int
    ): List<Affirmation>

    @Query("SELECT COUNT(*) FROM affirmations")
    suspend fun getAffirmationCount(): Int

    @Query("SELECT COUNT(*) FROM affirmations WHERE type = :type")
    suspend fun getAffirmationCountByType(type: AffirmationType): Int

    @Query("SELECT COUNT(*) FROM affirmations WHERE tone = :tone")
    suspend fun getAffirmationCountByTone(tone: AffirmationTone): Int

    @Query("""
        SELECT COUNT(*) FROM affirmations 
        WHERE type = :type AND tone = :tone
    """)
    suspend fun getAffirmationCountByTypeAndTone(type: AffirmationType, tone: AffirmationTone): Int

    @Query("DELETE FROM affirmations WHERE id = :affirmationId")
    suspend fun deleteAffirmationById(affirmationId: String)

    @Query("DELETE FROM affirmations WHERE type = :type")
    suspend fun deleteAffirmationsByType(type: AffirmationType)

    @Query("DELETE FROM affirmations")
    suspend fun deleteAllAffirmations()
}
