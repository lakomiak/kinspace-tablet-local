package com.adhdfocus.app.domain.visibility

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AppDatabase
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TodoGroupVisibilityManagerIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var manager: TodoGroupVisibilityManager
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userPreferencesManager = UserPreferencesManager(database.userPreferencesDao())
        manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testVisibilityPreferencesPersistedToDatabase() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Afternoon", "Evening")

        manager.setVisibleTodoGroups(userId, groups)
        val result = manager.getVisibleTodoGroups(userId)

        assertEquals(groups.sorted(), result.sorted())
    }

    @Test
    fun testVisibilityPreferencesPersistedIndependently() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Evening")

        manager.setVisibleTodoGroups(userId, groups)
        val result = manager.getVisibleTodoGroups(userId)

        assertEquals(groups.sorted(), result.sorted())
    }

    @Test
    fun testResetToDefaultsPersisted() = runTest {
        val userId = "user1"
        val initialGroups = listOf("Morning")

        manager.setVisibleTodoGroups(userId, initialGroups)
        manager.resetToDefaults(userId)
        val result = manager.getVisibleTodoGroups(userId)

        val expected = listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other")
        assertEquals(expected.sorted(), result.sorted())
    }

    @Test
    fun testDefaultsReturnedWhenNotFound() = runTest {
        val userId = "user1"

        val result = manager.getVisibleTodoGroupsOrDefault(userId)

        val expected = listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other")
        assertEquals(expected.sorted(), result.sorted())
    }

    @Test
    fun testPerMemberVisibilityIsolated() = runTest {
        val userId1 = "user1"
        val userId2 = "user2"
        val groups1 = listOf("Morning", "Afternoon")
        val groups2 = listOf("Evening", "Bedtime")

        manager.setVisibleTodoGroups(userId1, groups1)
        manager.setVisibleTodoGroups(userId2, groups2)

        val result1 = manager.getVisibleTodoGroups(userId1)
        val result2 = manager.getVisibleTodoGroups(userId2)

        assertEquals(groups1.sorted(), result1.sorted())
        assertEquals(groups2.sorted(), result2.sorted())
    }

    @Test
    fun testMultipleUpdatesPreserveLatestState() = runTest {
        val userId = "user1"
        val groups1 = listOf("Morning", "Afternoon")
        val groups2 = listOf("Evening", "Bedtime", "Other")

        manager.setVisibleTodoGroups(userId, groups1)
        manager.setVisibleTodoGroups(userId, groups2)
        val result = manager.getVisibleTodoGroups(userId)

        assertEquals(groups2.sorted(), result.sorted())
    }

    @Test
    fun testStateFlowUpdatesOnPreferenceChange() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Evening")

        val flow = manager.observeVisibleTodoGroups(userId)
        val initialValue = flow.value

        manager.setVisibleTodoGroups(userId, groups)

        assertEquals(groups.sorted(), flow.value.sorted())
    }

    @Test
    fun testToggleVisibilityPersisted() = runTest {
        val userId = "user1"
        val initialGroups = listOf("Morning", "Afternoon", "Evening")

        manager.setVisibleTodoGroups(userId, initialGroups)
        manager.toggleTodoGroupVisibility(userId, "Bedtime")
        val result = manager.getVisibleTodoGroups(userId)

        assertTrue(result.contains("Bedtime"))
        assertEquals(4, result.size)
    }

    @Test
    fun testCannotHideAllGroups() = runTest {
        val userId = "user1"
        val groups = listOf("Morning")

        manager.setVisibleTodoGroups(userId, groups)
        val result = manager.toggleTodoGroupVisibility(userId, "Morning")

        assertFalse(result)
        val remaining = manager.getVisibleTodoGroups(userId)
        assertEquals(groups, remaining)
    }

    @Test
    fun testVisibilityPersistsAcrossAppRestart() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Afternoon", "Evening")

        manager.setVisibleTodoGroups(userId, groups)

        // Simulate app restart by creating new manager instance
        val newManager = TodoGroupVisibilityManagerImpl(userPreferencesManager)
        val result = newManager.getVisibleTodoGroups(userId)

        assertEquals(groups.sorted(), result.sorted())
    }
}
