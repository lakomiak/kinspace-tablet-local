package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.data.model.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserPreferencesDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Individual preference updates
 * - Filtering by theme and settings
 * - Preference existence checks
 */
@RunWith(AndroidJUnit4::class)
class UserPreferencesDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var preferencesDao: UserPreferencesDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        preferencesDao = database.userPreferencesDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertPreferences() = runBlocking {
        val prefs = UserPreferences(
            userId = "user-1",
            theme = Theme.DARK,
            affirmationFrequency = 4,
            timerDefaultDuration = 30
        )

        preferencesDao.insert(prefs)
        val retrieved = preferencesDao.getPreferencesByUserId("user-1")

        assertNotNull(retrieved)
        assertEquals(Theme.DARK, retrieved.theme)
        assertEquals(4, retrieved.affirmationFrequency)
        assertEquals(30, retrieved.timerDefaultDuration)
    }

    @Test
    fun testUpdatePreferences() = runBlocking {
        val prefs = UserPreferences(
            userId = "user-1",
            theme = Theme.LIGHT,
            affirmationFrequency = 3
        )

        preferencesDao.insert(prefs)
        val updated = prefs.copy(theme = Theme.DARK, affirmationFrequency = 5)
        preferencesDao.update(updated)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(Theme.DARK, retrieved.theme)
        assertEquals(5, retrieved.affirmationFrequency)
    }

    @Test
    fun testDeletePreferences() = runBlocking {
        val prefs = UserPreferences(userId = "user-1")

        preferencesDao.insert(prefs)
        preferencesDao.delete(prefs)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetPreferencesByUserId() = runBlocking {
        val prefs = UserPreferences(
            userId = "user-1",
            theme = Theme.DARK,
            affirmationFrequency = 4
        )

        preferencesDao.insert(prefs)
        val retrieved = preferencesDao.getPreferencesByUserId("user-1")

        assertNotNull(retrieved)
        assertEquals("user-1", retrieved.userId)
        assertEquals(Theme.DARK, retrieved.theme)
    }

    @Test
    fun testGetNonExistentPreferences() = runBlocking {
        val retrieved = preferencesDao.getPreferencesByUserId("non-existent")
        assertNull(retrieved)
    }

    @Test
    fun testGetPreferencesByUserIdFlow() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", theme = Theme.DARK)

        preferencesDao.insert(prefs)
        val retrieved = preferencesDao.getPreferencesByUserIdFlow("user-1").first()

        assertNotNull(retrieved)
        assertEquals(Theme.DARK, retrieved.theme)
    }

    // ==================== Individual Updates ====================

    @Test
    fun testUpdateTheme() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", theme = Theme.LIGHT)

        preferencesDao.insert(prefs)
        preferencesDao.updateTheme("user-1", Theme.DARK)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(Theme.DARK, retrieved.theme)
    }

    @Test
    fun testUpdateAffirmationFrequency() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", affirmationFrequency = 3)

        preferencesDao.insert(prefs)
        preferencesDao.updateAffirmationFrequency("user-1", 5)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(5, retrieved.affirmationFrequency)
    }

    @Test
    fun testUpdateGamificationEnabled() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", enableGamification = true)

        preferencesDao.insert(prefs)
        preferencesDao.updateGamificationEnabled("user-1", false)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(false, retrieved.enableGamification)
    }

    @Test
    fun testUpdateTimerDefaultDuration() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", timerDefaultDuration = 25)

        preferencesDao.insert(prefs)
        preferencesDao.updateTimerDefaultDuration("user-1", 45)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(45, retrieved.timerDefaultDuration)
    }

    @Test
    fun testUpdateDailyResetTime() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", dailyResetTime = "00:00")

        preferencesDao.insert(prefs)
        preferencesDao.updateDailyResetTime("user-1", "06:00")

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals("06:00", retrieved.dailyResetTime)
    }

    @Test
    fun testUpdateAutoLogoutTimeout() = runBlocking {
        val prefs = UserPreferences(userId = "user-1", autoLogoutTimeout = 0)

        preferencesDao.insert(prefs)
        preferencesDao.updateAutoLogoutTimeout("user-1", 300)

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNotNull(retrieved)
        assertEquals(300, retrieved.autoLogoutTimeout)
    }

    // ==================== Filtering by Theme ====================

    @Test
    fun testGetPreferencesByTheme() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1", theme = Theme.LIGHT))
        preferencesDao.insert(UserPreferences(userId = "user-2", theme = Theme.DARK))
        preferencesDao.insert(UserPreferences(userId = "user-3", theme = Theme.DARK))

        val darkThemePrefs = preferencesDao.getPreferencesByTheme(Theme.DARK)
        val lightThemePrefs = preferencesDao.getPreferencesByTheme(Theme.LIGHT)

        assertEquals(2, darkThemePrefs.size)
        assertEquals(1, lightThemePrefs.size)
        assertTrue(darkThemePrefs.all { it.theme == Theme.DARK })
    }

    // ==================== Filtering by Gamification ====================

    @Test
    fun testGetGamificationEnabledPreferences() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1", enableGamification = true))
        preferencesDao.insert(UserPreferences(userId = "user-2", enableGamification = false))
        preferencesDao.insert(UserPreferences(userId = "user-3", enableGamification = true))

        val gamificationEnabled = preferencesDao.getGamificationEnabledPreferences()

        assertEquals(2, gamificationEnabled.size)
        assertTrue(gamificationEnabled.all { it.enableGamification })
    }

    // ==================== Filtering by Auto-Logout ====================

    @Test
    fun testGetAutoLogoutEnabledPreferences() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1", autoLogoutTimeout = 0))
        preferencesDao.insert(UserPreferences(userId = "user-2", autoLogoutTimeout = 300))
        preferencesDao.insert(UserPreferences(userId = "user-3", autoLogoutTimeout = 600))

        val autoLogoutEnabled = preferencesDao.getAutoLogoutEnabledPreferences()

        assertEquals(2, autoLogoutEnabled.size)
        assertTrue(autoLogoutEnabled.all { it.autoLogoutTimeout > 0 })
    }

    // ==================== Existence Checks ====================

    @Test
    fun testPreferencesExist() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1"))

        val exists = preferencesDao.preferencesExist("user-1")
        val notExists = preferencesDao.preferencesExist("user-2")

        assertEquals(1, exists)
        assertEquals(0, notExists)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeletePreferencesByUserId() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1"))

        preferencesDao.deletePreferencesByUserId("user-1")

        val retrieved = preferencesDao.getPreferencesByUserId("user-1")
        assertNull(retrieved)
    }

    // ==================== Default Values ====================

    @Test
    fun testDefaultPreferencesValues() = runBlocking {
        val prefs = UserPreferences(userId = "user-1")

        preferencesDao.insert(prefs)
        val retrieved = preferencesDao.getPreferencesByUserId("user-1")

        assertNotNull(retrieved)
        assertEquals(Theme.LIGHT, retrieved.theme)
        assertEquals(3, retrieved.affirmationFrequency)
        assertEquals(true, retrieved.enableGamification)
        assertEquals(25, retrieved.timerDefaultDuration)
        assertEquals(0, retrieved.autoLogoutTimeout)
        assertEquals("00:00", retrieved.dailyResetTime)
    }

    // ==================== Multiple Users ====================

    @Test
    fun testMultipleUsersPreferences() = runBlocking {
        preferencesDao.insert(UserPreferences(userId = "user-1", theme = Theme.LIGHT, affirmationFrequency = 3))
        preferencesDao.insert(UserPreferences(userId = "user-2", theme = Theme.DARK, affirmationFrequency = 5))
        preferencesDao.insert(UserPreferences(userId = "user-3", theme = Theme.LIGHT, affirmationFrequency = 4))

        val user1Prefs = preferencesDao.getPreferencesByUserId("user-1")
        val user2Prefs = preferencesDao.getPreferencesByUserId("user-2")
        val user3Prefs = preferencesDao.getPreferencesByUserId("user-3")

        assertNotNull(user1Prefs)
        assertNotNull(user2Prefs)
        assertNotNull(user3Prefs)
        assertEquals(Theme.LIGHT, user1Prefs.theme)
        assertEquals(Theme.DARK, user2Prefs.theme)
        assertEquals(Theme.LIGHT, user3Prefs.theme)
    }

    // ==================== Validation ====================

    @Test(expected = IllegalArgumentException::class)
    fun testPreferencesValidationRejectsBlankUserId() {
        UserPreferences(userId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPreferencesValidationRejectsInvalidAffirmationFrequency() {
        UserPreferences(userId = "user-1", affirmationFrequency = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPreferencesValidationRejectsNegativeTimerDuration() {
        UserPreferences(userId = "user-1", timerDefaultDuration = -5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPreferencesValidationRejectsNegativeAutoLogoutTimeout() {
        UserPreferences(userId = "user-1", autoLogoutTimeout = -1)
    }

    // ==================== Preference Ranges ====================

    @Test
    fun testAffirmationFrequencyRange() = runBlocking {
        for (freq in 1..5) {
            preferencesDao.insert(UserPreferences(userId = "user-$freq", affirmationFrequency = freq))
        }

        for (freq in 1..5) {
            val prefs = preferencesDao.getPreferencesByUserId("user-$freq")
            assertNotNull(prefs)
            assertEquals(freq, prefs.affirmationFrequency)
        }
    }

    @Test
    fun testTimerDurationVariations() = runBlocking {
        val durations = listOf(5, 15, 25, 45, 60)

        durations.forEachIndexed { index, duration ->
            preferencesDao.insert(UserPreferences(userId = "user-$index", timerDefaultDuration = duration))
        }

        durations.forEachIndexed { index, duration ->
            val prefs = preferencesDao.getPreferencesByUserId("user-$index")
            assertNotNull(prefs)
            assertEquals(duration, prefs.timerDefaultDuration)
        }
    }
}
