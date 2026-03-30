package com.adhdfocus.app.domain.visibility

import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoGroupVisibilityManagerUnitTest {
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var manager: TodoGroupVisibilityManager
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)
    }

    @Test
    fun testGetVisibleTodoGroupsReturnsGroups() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Afternoon")
        val serialized = json.encodeToString(groups)
        val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

        coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns groups

        val result = manager.getVisibleTodoGroups(userId)

        assertEquals(groups, result)
    }

    @Test
    fun testGetVisibleTodoGroupsReturnsEmptyWhenNotFound() = runTest {
        val userId = "user1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null

        val result = manager.getVisibleTodoGroups(userId)

        assertEquals(emptyList(), result)
    }

    @Test
    fun testGetVisibleTodoGroupsOrDefaultReturnsExisting() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Afternoon")
        val serialized = json.encodeToString(groups)
        val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

        coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns groups

        val result = manager.getVisibleTodoGroupsOrDefault(userId)

        assertEquals(groups, result)
    }

    @Test
    fun testGetVisibleTodoGroupsOrDefaultReturnsDefaultWhenNotFound() = runTest {
        val userId = "user1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null

        val result = manager.getVisibleTodoGroupsOrDefault(userId)

        assertEquals(listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other"), result)
    }

    @Test
    fun testSetVisibleTodoGroupsUpdatesGroups() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Evening")

        coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, groups) } returns true

        val result = manager.setVisibleTodoGroups(userId, groups)

        assertTrue(result)
        coVerify { userPreferencesManager.updateVisibleTodoGroups(userId, groups) }
    }

    @Test
    fun testSetVisibleTodoGroupsReturnsFalseOnFailure() = runTest {
        val userId = "user1"
        val groups = listOf("Morning", "Evening")

        coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, groups) } returns false

        val result = manager.setVisibleTodoGroups(userId, groups)

        assertFalse(result)
    }

    @Test
    fun testToggleTodoGroupVisibilityAddsGroup() = runTest {
        val userId = "user1"
        val currentGroups = listOf("Morning", "Afternoon")
        val serialized = json.encodeToString(currentGroups)
        val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

        coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns currentGroups
        coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

        val result = manager.toggleTodoGroupVisibility(userId, "Evening")

        assertTrue(result)
        coVerify { userPreferencesManager.updateVisibleTodoGroups(userId, match { it.contains("Evening") }) }
    }

    @Test
    fun testToggleTodoGroupVisibilityRemovesGroup() = runTest {
        val userId = "user1"
        val currentGroups = listOf("Morning", "Afternoon", "Evening")
        val serialized = json.encodeToString(currentGroups)
        val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

        coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns currentGroups
        coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

        val result = manager.toggleTodoGroupVisibility(userId, "Evening")

        assertTrue(result)
        coVerify { userPreferencesManager.updateVisibleTodoGroups(userId, match { !it.contains("Evening") }) }
    }

    @Test
    fun testToggleTodoGroupVisibilityReturnsFalseWhenOnlyOneGroupVisible() = runTest {
        val userId = "user1"
        val currentGroups = listOf("Morning")
        val serialized = json.encodeToString(currentGroups)
        val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

        coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns currentGroups

        val result = manager.toggleTodoGroupVisibility(userId, "Morning")

        assertFalse(result)
    }

    @Test
    fun testResetToDefaultsResetsAllGroups() = runTest {
        val userId = "user1"

        coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

        val result = manager.resetToDefaults(userId)

        assertTrue(result)
        coVerify {
            userPreferencesManager.updateVisibleTodoGroups(
                userId,
                listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other")
            )
        }
    }

    @Test
    fun testObserveVisibleTodoGroupsReturnsStateFlow() = runTest {
        val userId = "user1"

        val flow = manager.observeVisibleTodoGroups(userId)

        assertEquals(listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other"), flow.value)
    }

    @Test
    fun testObserveVisibleTodoGroupsReturnsSameFlowForSameUser() = runTest {
        val userId = "user1"

        val flow1 = manager.observeVisibleTodoGroups(userId)
        val flow2 = manager.observeVisibleTodoGroups(userId)

        assertTrue(flow1 === flow2)
    }

    @Test
    fun testGetVisibleTodoGroupsWithBlankUserIdThrowsException() = runTest {
        try {
            manager.getVisibleTodoGroups("")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    @Test
    fun testSetVisibleTodoGroupsWithBlankUserIdThrowsException() = runTest {
        try {
            manager.setVisibleTodoGroups("", listOf("Morning"))
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    @Test
    fun testSetVisibleTodoGroupsWithEmptyGroupsThrowsException() = runTest {
        try {
            manager.setVisibleTodoGroups("user1", emptyList())
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("At least one todo group must be visible") == true)
        }
    }

    @Test
    fun testSetVisibleTodoGroupsWithInvalidGroupThrowsException() = runTest {
        try {
            manager.setVisibleTodoGroups("user1", listOf("InvalidGroup"))
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid todo group") == true)
        }
    }

    @Test
    fun testGetAllTodoGroupsReturnsAllGroups() {
        val result = manager.getAllTodoGroups()

        assertEquals(listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other"), result)
    }
}
