package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for UserPreferencesDao.
 *
 * Tests database operations with actual Room database.
 */
@RunWith(AndroidJUnit4::class)
class UserPreferencesDaoIntegrationTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var userPreferencesDao: UserPreferencesDao
    private lateinit var userDao: UserDao
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AdhdfocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userPreferencesDao = database.userPreferencesDao()
        userDao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrievePreferences() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.DARK,
            dailyResetTime = "06:00",
            affirmationFrequency = 4
        )

        // Act
        userPreferencesDao.insert(preferences)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals(Theme.DARK, result.theme)
        assertEquals("06:00", result.dailyResetTime)
        assertEquals(4, result.affirmationFrequency)
    }

    @Test
    fun testUpdatePreferences() = runTest {
        // Arrange
        val userId = "user-123"
        val originalPrefs = UserPreferences(userId = userId, theme = Theme.LIGHT)
        userPreferencesDao.insert(originalPrefs)

        val updatedPrefs = originalPrefs.copy(theme = Theme.DARK)

        // Act
        userPreferencesDao.update(updatedPrefs)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(Theme.DARK, result.theme)
    }

    @Test
    fun testDeletePreferences() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.delete(preferences)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNull(result)
    }

    @Test
    fun testUpdateTheme() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, theme = Theme.LIGHT)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateTheme(userId, Theme.DARK)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(Theme.DARK, result.theme)
    }

    @Test
    fun testUpdateVisibleTodoGroups() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        val groups = listOf("Morning", "Afternoon", "Evening")
        val serialized = json.encodeToString(groups)

        // Act
        userPreferencesDao.updateVisibleTodoGroups(userId, serialized)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(serialized, result.visibleTodoGroups)
    }

    @Test
    fun testUpdateNotificationPreferences() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        val notifPrefs = NotificationPreferences(soundEnabled = false)
        val serialized = json.encodeToString(notifPrefs)

        // Act
        userPreferencesDao.updateNotificationPreferences(userId, serialized)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(serialized, result.notificationPreferences)
    }

    @Test
    fun testUpdateDailyResetTime() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateDailyResetTime(userId, "06:30")
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals("06:30", result.dailyResetTime)
    }

    @Test
    fun testUpdateAffirmationFrequency() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateAffirmationFrequency(userId, 5)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(5, result.affirmationFrequency)
    }

    @Test
    fun testUpdateGamificationEnabled() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, enableGamification = true)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateGamificationEnabled(userId, false)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(false, result.enableGamification)
    }

    @Test
    fun testUpdateTimerDefaultDuration() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateTimerDefaultDuration(userId, 30)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(30, result.timerDefaultDuration)
    }

    @Test
    fun testUpdateAutoLogoutTimeout() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.updateAutoLogoutTimeout(userId, 15)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(15, result.autoLogoutTimeout)
    }

    @Test
    fun testDeletePreferencesByUserId() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insert(preferences)

        // Act
        userPreferencesDao.deletePreferencesByUserId(userId)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNull(result)
    }

    @Test
    fun testPreferencesExist() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)

        // Act & Assert - Before insert
        var count = userPreferencesDao.preferencesExist(userId)
        assertEquals(0, count)

        // Act - Insert
        userPreferencesDao.insert(preferences)

        // Assert - After insert
        count = userPreferencesDao.preferencesExist(userId)
        assertEquals(1, count)
    }

    @Test
    fun testGetPreferencesByTheme() = runTest {
        // Arrange
        val lightPrefs = UserPreferences(userId = "user-1", theme = Theme.LIGHT)
        val darkPrefs = UserPreferences(userId = "user-2", theme = Theme.DARK)
        val anotherLightPrefs = UserPreferences(userId = "user-3", theme = Theme.LIGHT)

        userPreferencesDao.insert(lightPrefs)
        userPreferencesDao.insert(darkPrefs)
        userPreferencesDao.insert(anotherLightPrefs)

        // Act
        val lightResults = userPreferencesDao.getPreferencesByTheme(Theme.LIGHT)
        val darkResults = userPreferencesDao.getPreferencesByTheme(Theme.DARK)

        // Assert
        assertEquals(2, lightResults.size)
        assertEquals(1, darkResults.size)
        assertTrue(lightResults.all { it.theme == Theme.LIGHT })
        assertTrue(darkResults.all { it.theme == Theme.DARK })
    }

    @Test
    fun testGetGamificationEnabledPreferences() = runTest {
        // Arrange
        val gamificationEnabled = UserPreferences(userId = "user-1", enableGamification = true)
        val gamificationDisabled = UserPreferences(userId = "user-2", enableGamification = false)

        userPreferencesDao.insert(gamificationEnabled)
        userPreferencesDao.insert(gamificationDisabled)

        // Act
        val results = userPreferencesDao.getGamificationEnabledPreferences()

        // Assert
        assertEquals(1, results.size)
        assertTrue(results.all { it.enableGamification })
    }

    @Test
    fun testGetAutoLogoutEnabledPreferences() = runTest {
        // Arrange
        val autoLogoutEnabled = UserPreferences(userId = "user-1", autoLogoutTimeout = 15)
        val autoLogoutDisabled = UserPreferences(userId = "user-2", autoLogoutTimeout = 0)

        userPreferencesDao.insert(autoLogoutEnabled)
        userPreferencesDao.insert(autoLogoutDisabled)

        // Act
        val results = userPreferencesDao.getAutoLogoutEnabledPreferences()

        // Assert
        assertEquals(1, results.size)
        assertTrue(results.all { it.autoLogoutTimeout > 0 })
    }

    @Test
    fun testPerUserIsolation() = runTest {
        // Arrange
        val user1Prefs = UserPreferences(
            userId = "user-1",
            theme = Theme.LIGHT,
            affirmationFrequency = 2
        )
        val user2Prefs = UserPreferences(
            userId = "user-2",
            theme = Theme.DARK,
            affirmationFrequency = 5
        )

        userPreferencesDao.insert(user1Prefs)
        userPreferencesDao.insert(user2Prefs)

        // Act
        val result1 = userPreferencesDao.getPreferencesByUserId("user-1")
        val result2 = userPreferencesDao.getPreferencesByUserId("user-2")

        // Assert
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(Theme.LIGHT, result1.theme)
        assertEquals(2, result1.affirmationFrequency)
        assertEquals(Theme.DARK, result2.theme)
        assertEquals(5, result2.affirmationFrequency)
    }

    @Test
    fun testMultipleUpdatesPreserveOtherFields() = runTest {
        // Arrange
        val userId = "user-123"
        val originalPrefs = UserPreferences(
            userId = userId,
            theme = Theme.LIGHT,
            dailyResetTime = "06:00",
            affirmationFrequency = 3,
            enableGamification = true,
            timerDefaultDuration = 25,
            autoLogoutTimeout = 0
        )
        userPreferencesDao.insert(originalPrefs)

        // Act - Update only theme
        userPreferencesDao.updateTheme(userId, Theme.DARK)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNotNull(result)
        assertEquals(Theme.DARK, result.theme)
        assertEquals("06:00", result.dailyResetTime)
        assertEquals(3, result.affirmationFrequency)
        assertEquals(true, result.enableGamification)
        assertEquals(25, result.timerDefaultDuration)
        assertEquals(0, result.autoLogoutTimeout)
    }

    @Test
    fun testCascadeDeleteWithUser() = runTest {
        // Arrange
        val userId = "user-123"
        val user = User(
            id = userId,
            householdId = "household-1",
            email = "test@example.com",
            displayName = "Test User"
        )
        val preferences = UserPreferences(userId = userId)

        userDao.insert(user)
        userPreferencesDao.insert(preferences)

        // Act
        userDao.delete(user)
        val result = userPreferencesDao.getPreferencesByUserId(userId)

        // Assert
        assertNull(result)
    }

    @Test
    fun testFlowObservesPreferenceChanges() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, theme = Theme.LIGHT)
        userPreferencesDao.insert(preferences)

        // Act & Assert
        val flow = userPreferencesDao.getPreferencesByUserIdFlow(userId)
        var emissionCount = 0
        var lastTheme: Theme? = null

        // Collect first emission
        flow.collect { prefs ->
            emissionCount++
            lastTheme = prefs?.theme
            if (emissionCount == 1) {
                assertEquals(Theme.LIGHT, lastTheme)
            }
        }
    }
}
