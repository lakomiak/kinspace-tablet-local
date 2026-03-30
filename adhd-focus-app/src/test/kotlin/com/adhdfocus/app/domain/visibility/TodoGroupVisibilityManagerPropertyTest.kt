package com.adhdfocus.app.domain.visibility

import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based tests for TodoGroupVisibilityManager
 *
 * **Validates: Requirements 10, 6, Property 3: Task Organization**
 */
class TodoGroupVisibilityManagerPropertyTest : FunSpec({
    val json = Json { ignoreUnknownKeys = true }
    val allGroups = listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other")

    fun userIdArb() = Arb.string(minSize = 1, maxSize = 50)

    fun visibleGroupsArb() = Arb.list(
        Arb.string(minSize = 1, maxSize = 20),
        range = 1..5
    ).map { groups ->
        groups.filter { it in allGroups }.ifEmpty { listOf("Morning") }
    }

    test("Property 3: Task Organization - Visibility consistency") {
        runTest {
            checkAll(userIdArb(), visibleGroupsArb()) { userId, groups ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                val serialized = json.encodeToString(groups)
                val prefs = UserPreferences(userId = userId, visibleTodoGroups = serialized)

                coEvery { userPreferencesManager.getPreferences(userId) } returns prefs
                coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized) } returns groups
                coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

                val result = manager.getVisibleTodoGroups(userId)

                result.shouldContainExactlyInAnyOrder(groups)
            }
        }
    }

    test("Property: Per-member visibility isolation") {
        runTest {
            checkAll(userIdArb(), userIdArb(), visibleGroupsArb(), visibleGroupsArb()) { userId1, userId2, groups1, groups2 ->
                if (userId1 != userId2) {
                    val userPreferencesManager = mockk<UserPreferencesManager>()
                    val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                    val serialized1 = json.encodeToString(groups1)
                    val serialized2 = json.encodeToString(groups2)
                    val prefs1 = UserPreferences(userId = userId1, visibleTodoGroups = serialized1)
                    val prefs2 = UserPreferences(userId = userId2, visibleTodoGroups = serialized2)

                    coEvery { userPreferencesManager.getPreferences(userId1) } returns prefs1
                    coEvery { userPreferencesManager.getPreferences(userId2) } returns prefs2
                    coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized1) } returns groups1
                    coEvery { userPreferencesManager.deserializeVisibleTodoGroups(serialized2) } returns groups2

                    val result1 = manager.getVisibleTodoGroups(userId1)
                    val result2 = manager.getVisibleTodoGroups(userId2)

                    result1.shouldContainExactlyInAnyOrder(groups1)
                    result2.shouldContainExactlyInAnyOrder(groups2)
                }
            }
        }
    }

    test("Property: State consistency after multiple updates") {
        runTest {
            checkAll(userIdArb(), visibleGroupsArb()) { userId, groups ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

                manager.setVisibleTodoGroups(userId, groups)
                val flow = manager.observeVisibleTodoGroups(userId)

                flow.value.shouldContainExactlyInAnyOrder(groups)
            }
        }
    }

    test("Property: Reset to default always sets all groups") {
        runTest {
            checkAll(userIdArb()) { userId ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

                manager.resetToDefaults(userId)
                val flow = manager.observeVisibleTodoGroups(userId)

                flow.value.shouldContainExactlyInAnyOrder(allGroups)
            }
        }
    }

    test("Property: Individual field updates preserve other fields") {
        runTest {
            checkAll(userIdArb(), visibleGroupsArb()) { userId, initialGroups ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

                manager.setVisibleTodoGroups(userId, initialGroups)
                val groupToToggle = allGroups.first { it !in initialGroups }
                manager.toggleTodoGroupVisibility(userId, groupToToggle)

                val flow = manager.observeVisibleTodoGroups(userId)
                flow.value.shouldContainExactlyInAnyOrder(initialGroups + groupToToggle)
            }
        }
    }

    test("Property: Default preferences correctness") {
        runTest {
            checkAll(userIdArb()) { userId ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.getPreferences(userId) } returns null

                val result = manager.getVisibleTodoGroupsOrDefault(userId)

                result.shouldContainExactlyInAnyOrder(allGroups)
            }
        }
    }

    test("Property: StateFlow emissions consistency") {
        runTest {
            checkAll(userIdArb()) { userId ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                val flow1 = manager.observeVisibleTodoGroups(userId)
                val flow2 = manager.observeVisibleTodoGroups(userId)

                (flow1 === flow2).shouldBe(true)
                flow1.value.shouldContainExactlyInAnyOrder(allGroups)
            }
        }
    }

    test("Property: All visibility combinations valid") {
        runTest {
            checkAll(userIdArb()) { userId ->
                val userPreferencesManager = mockk<UserPreferencesManager>()
                val manager = TodoGroupVisibilityManagerImpl(userPreferencesManager)

                coEvery { userPreferencesManager.updateVisibleTodoGroups(userId, any()) } returns true

                // Test all single-group combinations
                for (group in allGroups) {
                    val result = manager.setVisibleTodoGroups(userId, listOf(group))
                    result.shouldBe(true)
                }

                // Test all two-group combinations
                for (i in allGroups.indices) {
                    for (j in i + 1 until allGroups.size) {
                        val result = manager.setVisibleTodoGroups(userId, listOf(allGroups[i], allGroups[j]))
                        result.shouldBe(true)
                    }
                }
            }
        }
    }
})
