package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.AffirmationDao
import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import javax.inject.Inject

/**
 * AffirmationRepository provides data access abstraction for affirmations.
 *
 * Handles:
 * - Affirmation CRUD operations
 * - Affirmation retrieval by type and age level
 * - Affirmation persistence
 */
class AffirmationRepository @Inject constructor(
    private val affirmationDao: AffirmationDao
) {
    /**
     * Gets all affirmations.
     *
     * @return List of all affirmations
     */
    suspend fun getAllAffirmations(): List<Affirmation> {
        return affirmationDao.getAllAffirmations()
    }

    /**
     * Gets affirmations by type.
     *
     * @param type Affirmation type
     * @return List of affirmations of the specified type
     */
    suspend fun getAffirmationsByType(type: AffirmationType): List<Affirmation> {
        return affirmationDao.getAffirmationsByType(type.name)
    }

    /**
     * Gets affirmations by age appropriateness level.
     *
     * @param ageLevel Age appropriateness level (1-5)
     * @return List of affirmations suitable for the age level
     */
    suspend fun getAffirmationsByAgeLevel(ageLevel: Int): List<Affirmation> {
        return affirmationDao.getAffirmationsByAgeLevel(ageLevel)
    }

    /**
     * Gets an affirmation by ID.
     *
     * @param affirmationId Affirmation ID
     * @return Affirmation or null if not found
     */
    suspend fun getAffirmationById(affirmationId: String): Affirmation? {
        return affirmationDao.getAffirmationById(affirmationId)
    }

    /**
     * Saves an affirmation.
     *
     * @param affirmation Affirmation to save
     */
    suspend fun saveAffirmation(affirmation: Affirmation) {
        affirmationDao.insert(affirmation)
    }

    /**
     * Saves multiple affirmations.
     *
     * @param affirmations List of affirmations to save
     */
    suspend fun saveAffirmations(affirmations: List<Affirmation>) {
        affirmationDao.insertAll(affirmations)
    }

    /**
     * Deletes an affirmation.
     *
     * @param affirmationId Affirmation ID
     */
    suspend fun deleteAffirmation(affirmationId: String) {
        affirmationDao.delete(affirmationId)
    }
}
