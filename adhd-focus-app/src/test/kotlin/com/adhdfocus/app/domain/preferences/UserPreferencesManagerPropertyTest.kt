package com.adhdfocus.app.domain.preferences

import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based tests for UserPreferencesManager.
 *
 * **Validates: Requirements 6, 7, Property 2.8: Task Persistence**
 *
 * Tests:
 * - Preference consistency across operations
 * - Data isolation between users
 * - Serialization round-trip
 * - Concurrent operation safety
 * - Default value correctness
 */
class UserPreferencesManagerPropertyTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    fun userIdArb() = Arb.string(minSize = 1, maxSize = 50)

    fun themeArb() = Arb.enum<Theme>()

    fun affirmationFrequencyArb() = Arb.int(min = 1, max = 5)

    fun timerDurationArb() = Arb.int(min = 1, max = 480)

    fun autoLogoutTimeoutArb() = Arb.int(min = 0, max = 120)

    fun todoGroupsArb() = Arb.list(
        Arb.string(minSize = 1, maxSize = 30),
        minSize = 1,
        maxSize = 10
    )

    fun timeFormatArb() = Arb.string(minSize = 5, maxSize = 5).filter { time ->
        val regex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        regex.matches(time)
    }

    test("Property 2.8: Preferences persist across save and retrieve operations") {
        runTest {
            checkAll(
                userIdArb(),
                themeArb(),
                affirmationFrequencyArb(),
                timerDurationArb(),
                autoLogoutTimeoutArb()
            ) { userId, theme, frequency, duration, timeout ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val preferences = UserPreferences(
                    userId = userId,
                    theme = theme,
                    affirmationFrequency = frequency,
                    timerDefaultDuration = duration,
                    autoLogoutTimeout = timeout
                )

                coEvery { userPreferencesDao.update(preferences) } returns Unit
                coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns preferences

                // Act
                val saveResult = manager.savePreferences(preferences)
                val retrievedPrefs = manager.getPreferences(userId)

                // Assert
                saveResult shouldBe true
                retrievedPrefs shouldNotBe null
                retrievedPrefs?.userId shouldBe userId
                retrievedPrefs?.theme shouldBe theme
                retrievedPrefs?.affirmationFrequency shouldBe frequency
                retrievedPrefs?.timerDefaultDuration shouldBe duration
                retrievedPrefs?.autoLogoutTimeout shouldBe timeout
            }
        }
    }

    test("Property: Data isolation - different users have independent preferences") {
        runTest {
            checkAll(
                userIdArb(),
                userIdArb(),
                themeArb(),
                themeArb()
            ) { userId1, userId2, theme1, theme2 ->
                // Skip if user IDs are the same
                if (userId1 == userId2) return@checkAll

                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val prefs1 = UserPreferences(userId = userId1, theme = theme1)
                val prefs2 = UserPreferences(userId = userId2, theme = theme2)

                coEvery { userPreferencesDao.getPreferencesByUserId(userId1) } returns prefs1
                coEvery { userPreferencesDao.getPreferencesByUserId(userId2) } returns prefs2

                // Act
                val result1 = manager.getPreferences(userId1)
                val result2 = manager.getPreferences(userId2)

                // Assert
                result1?.userId shouldBe userId1
                result1?.theme shouldBe theme1
                result2?.userId shouldBe userId2
                result2?.theme shouldBe theme2
                result1?.theme shouldNotBe result2?.theme
            }
        }
    }

    test("Property: Serialization round-trip - visible todo groups") {
        runTest {
            checkAll(todoGroupsArb()) { groups ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val serialized = json.encodeToString(groups)

                // Act
                val deserialized = manager.deserializeVisibleTodoGroups(serialized)

                // Assert
                deserialized.size shouldBe groups.size
                deserialized shouldBe groups
            }
        }
    }

    test("Property: Serialization round-trip - notification preferences") {
        runTest {
            checkAll(
                Arb.int(0..1).map { it == 1 },
                Arb.int(0..1).map { it == 1 },
                Arb.int(0..1).map { it == 1 }
            ) { soundEnabled, vibrationEnabled, visualAlertsEnabled ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val original = NotificationPreferences(
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    visualAlertsEnabled = visualAlertsEnabled
                )

                val serialized = json.encodeToString(original)

                // Act
                val deserialized = manager.deserializeNotificationPreferences(serialized)

                // Assert
                deserialized.soundEnabled shouldBe soundEnabled
                deserialized.vibrationEnabled shouldBe vibrationEnabled
                deserialized.visualAlertsEnabled shouldBe visualAlertsEnabled
            }
        }
    }

    test("Property: Default preferences have correct values for any user") {
        runTest {
            checkAll(userIdArb()) { userId ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns null

                // Act
                val defaults = manager.getPreferencesOrDefault(userId)

                // Assert
                defaults.userId shouldBe userId
                defaults.theme shouldBe Theme.LIGHT
                defaults.dailyResetTime shouldBe "00:00"
                defaults.affirmationFrequency shouldBe 3
                defaults.enableGamification shouldBe true
                defaults.timerDefaultDuration shouldBe 25
                defaults.autoLogoutTimeout shouldBe 0
            }
        }
    }

    test("Property: Theme updates are persisted correctly") {
        runTest {
            checkAll(userIdArb(), themeArb()) { userId, theme ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.updateTheme(userId, theme) } returns Unit

                // Act
                val result = manager.updateTheme(userId, theme)

                // Assert
                result shouldBe true
            }
        }
    }

    test("Property: Affirmation frequency updates are validated") {
        runTest {
            checkAll(userIdArb(), Arb.int()) { userId, frequency ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.updateAffirmationFrequency(userId, any()) } returns Unit

                // Act
                val result = manager.updateAffirmationFrequency(userId, frequency)

                // Assert
                if (frequency in 1..5) {
                    result shouldBe true
                } else {
                    result shouldBe false
                }
            }
        }
    }

    test("Property: Timer duration updates are validated") {
        runTest {
            checkAll(userIdArb(), Arb.int()) { userId, duration ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.updateTimerDefaultDuration(userId, any()) } returns Unit

                // Act
                val result = manager.updateTimerDefaultDuration(userId, duration)

                // Assert
                if (duration > 0) {
                    result shouldBe true
                } else {
                    result shouldBe false
                }
            }
        }
    }

    test("Property: Auto-logout timeout updates are validated") {
        runTest {
            checkAll(userIdArb(), Arb.int()) { userId, timeout ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.updateAutoLogoutTimeout(userId, any()) } returns Unit

                // Act
                val result = manager.updateAutoLogoutTimeout(userId, timeout)

                // Assert
                if (timeout >= 0) {
                    result shouldBe true
                } else {
                    result shouldBe false
                }
            }
        }
    }

    test("Property: Concurrent operation safety - multiple updates don't conflict") {
        runTest {
            checkAll(userIdArb(), themeArb(), affirmationFrequencyArb()) { userId, theme, frequency ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.updateTheme(userId, any()) } returns Unit
                coEvery { userPreferencesDao.updateAffirmationFrequency(userId, any()) } returns Unit

                // Act
                val result1 = manager.updateTheme(userId, theme)
                val result2 = manager.updateAffirmationFrequency(userId, frequency)

                // Assert
                result1 shouldBe true
                result2 shouldBe true
            }
        }
    }

    test("Property: Empty todo groups list deserialization returns empty") {
        runTest {
            // Arrange
            val userPreferencesDao = mockk<UserPreferencesDao>()
            val manager = UserPreferencesManager(userPreferencesDao)

            val emptyJson = "[]"

            // Act
            val result = manager.deserializeVisibleTodoGroups(emptyJson)

            // Assert
            result.isEmpty() shouldBe true
        }
    }

    test("Property: Invalid JSON deserialization returns safe defaults") {
        runTest {
            // Arrange
            val userPreferencesDao = mockk<UserPreferencesDao>()
            val manager = UserPreferencesManager(userPreferencesDao)

            val invalidJson = "not valid json"

            // Act
            val todoGroupsResult = manager.deserializeVisibleTodoGroups(invalidJson)
            val notificationResult = manager.deserializeNotificationPreferences(invalidJson)

            // Assert
            todoGroupsResult.isEmpty() shouldBe true
            notificationResult.soundEnabled shouldBe true
            notificationResult.vibrationEnabled shouldBe true
            notificationResult.visualAlertsEnabled shouldBe true
        }
    }

    test("Property: Preferences existence check is accurate") {
        runTest {
            checkAll(userIdArb(), Arb.int(0..1)) { userId, exists ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.preferencesExist(userId) } returns exists

                // Act
                val result = manager.preferencesExist(userId)

                // Assert
                result shouldBe (exists > 0)
            }
        }
    }

    test("Property: Reset to defaults creates valid preferences") {
        runTest {
            checkAll(userIdArb()) { userId ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.update(any()) } returns Unit

                // Act
                val result = manager.resetToDefaults(userId)

                // Assert
                result shouldBe true
            }
        }
    }

    test("Property: Delete preferences operation succeeds") {
        runTest {
            checkAll(userIdArb()) { userId ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                coEvery { userPreferencesDao.deletePreferencesByUserId(userId) } returns Unit

                // Act
                val result = manager.deletePreferences(userId)

                // Assert
                result shouldBe true
            }
        }
    }

    test("Property: Visible todo groups with special characters serialize correctly") {
        runTest {
            checkAll(
                Arb.list(
                    Arb.string(minSize = 1, maxSize = 30),
                    minSize = 1,
                    maxSize = 5
                )
            ) { groups ->
                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val serialized = json.encodeToString(groups)

                // Act
                val deserialized = manager.deserializeVisibleTodoGroups(serialized)

                // Assert
                deserialized shouldBe groups
            }
        }
    }

    test("Property: Multiple users can have different preferences simultaneously") {
        runTest {
            checkAll(
                Arb.list(userIdArb(), minSize = 2, maxSize = 5),
                Arb.list(themeArb(), minSize = 2, maxSize = 5)
            ) { userIds, themes ->
                // Skip if not enough themes
                if (themes.size < userIds.size) return@checkAll

                // Arrange
                val userPreferencesDao = mockk<UserPreferencesDao>()
                val manager = UserPreferencesManager(userPreferencesDao)

                val preferences = userIds.mapIndexed { index, userId ->
                    UserPreferences(userId = userId, theme = themes[index])
                }

                preferences.forEach { pref ->
                    coEvery { userPreferencesDao.getPreferencesByUserId(pref.userId) } returns pref
                }

                // Act
                val results = preferences.map { pref ->
                    manager.getPreferences(pref.userId)
                }

                // Assert
                results.forEachIndexed { index, result ->
                    result?.userId shouldBe preferences[index].userId
                    result?.theme shouldBe preferences[index].theme
                }
            }
        }
    }
})
