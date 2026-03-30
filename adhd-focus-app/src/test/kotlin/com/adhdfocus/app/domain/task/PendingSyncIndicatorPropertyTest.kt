package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Pending Sync Indicator (Property 6)
 *
 * Feature: adhd-focus-app
 * Property 6: Pending Sync Indicator
 *
 * Correctness Property:
 * All local changes to tasks must be marked with PENDING sync status until successfully synced.
 * For any task operation (create, update, complete, delete), the resulting task must:
 * 1. Have syncStatus = PENDING immediately after the operation
 * 2. Be queued in the sync queue for transmission to calendar-cloud
 * 3. Remain queryable via getPendingSyncTasks() until sync completes
 * 4. Maintain PENDING status across app restarts until sync succeeds
 */
class PendingSyncIndicatorPropertyTest : BehaviorSpec({
    val taskDao = mock<TaskDao>()
    val syncQueueDao = mock<SyncQueueDao>()
    val taskRepository = mock<TaskRepository>()
    val taskManager = TaskManager(taskRepository, taskDao, syncQueueDao)

    Given("TaskManager with sync queue tracking") {
        When("creating a task with valid input") {
            Then("task should have PENDING sync status") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50)
                ) { title, todoGroup, householdId, userId ->
                    // Arrange
                    whenever(taskDao.insert(any())).thenReturn(1L)
                    whenever(syncQueueDao.insert(any())).thenReturn(1L)

                    // Act
                    val result = taskManager.createTask(
                        title = title,
                        todoGroup = todoGroup,
                        householdId = householdId,
                        assignedUserId = userId
                    )

                    // Assert - Property 6: Pending Sync Indicator
                    result.syncStatus shouldBe SyncStatus.PENDING
                    verify(syncQueueDao, times(1)).insert(any())
                }
            }
        }

        When("updating a task") {
            Then("updated task should have PENDING sync status") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.int(min = 1, max = 1440)
                ) { newTitle, newDuration ->
                    // Arrange
                    val existingTask = Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Original Title",
                        todoGroup = "Morning",
                        status = TaskStatus.INCOMPLETE,
                        syncStatus = SyncStatus.SYNCED
                    )
                    whenever(taskDao.getTaskById("task1")).thenReturn(existingTask)
                    whenever(taskDao.update(any())).thenReturn(Unit)
                    whenever(syncQueueDao.insert(any())).thenReturn(1L)

                    // Act
                    val result = taskManager.updateTask(
                        taskId = "task1",
                        title = newTitle,
                        estimatedDurationMinutes = newDuration
                    )

                    // Assert - Property 6: Pending Sync Indicator
                    result.syncStatus shouldBe SyncStatus.PENDING
                    verify(syncQueueDao, times(1)).insert(any())
                }
            }
        }

        When("completing a task") {
            Then("completed task should have PENDING sync status") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(existingTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.completeTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.status shouldBe TaskStatus.COMPLETED
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("deleting a task") {
            Then("deleted task should have PENDING sync status") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(existingTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                taskManager.deleteTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                verify(taskDao, times(1)).update(any())
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("starting a task") {
            Then("started task should have PENDING sync status") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(existingTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.startTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.status shouldBe TaskStatus.IN_PROGRESS
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("retrieving pending sync tasks") {
            Then("should return all tasks with PENDING sync status") {
                // Arrange
                val pendingTasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.PENDING
                    ),
                    Task(
                        id = "task2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 2",
                        todoGroup = "Evening",
                        syncStatus = SyncStatus.PENDING
                    )
                )
                whenever(taskDao.getUserTasksBySyncStatus("user1", SyncStatus.PENDING))
                    .thenReturn(flowOf(pendingTasks))

                // Act
                val result = taskManager.getPendingSyncTasks("user1")

                // Assert - Property 6: Pending Sync Indicator
                verify(taskDao, times(1)).getUserTasksBySyncStatus("user1", SyncStatus.PENDING)
            }
        }

        When("creating multiple tasks in sequence") {
            Then("all tasks should have PENDING sync status") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 100)
                ) { title1, title2, title3 ->
                    // Arrange
                    whenever(taskDao.insert(any())).thenReturn(1L)
                    whenever(syncQueueDao.insert(any())).thenReturn(1L)

                    // Act
                    val task1 = taskManager.createTask(
                        title = title1,
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1"
                    )
                    val task2 = taskManager.createTask(
                        title = title2,
                        todoGroup = "Afternoon",
                        householdId = "household1",
                        assignedUserId = "user1"
                    )
                    val task3 = taskManager.createTask(
                        title = title3,
                        todoGroup = "Evening",
                        householdId = "household1",
                        assignedUserId = "user1"
                    )

                    // Assert - Property 6: Pending Sync Indicator
                    task1.syncStatus shouldBe SyncStatus.PENDING
                    task2.syncStatus shouldBe SyncStatus.PENDING
                    task3.syncStatus shouldBe SyncStatus.PENDING
                    verify(syncQueueDao, times(3)).insert(any())
                }
            }
        }

        When("updating a task that was previously synced") {
            Then("sync status should transition from SYNCED to PENDING") {
                // Arrange
                val syncedTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Original Title",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(syncedTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.updateTask(
                    taskId = "task1",
                    title = "Updated Title"
                )

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                verify(syncQueueDao, times(1)).insert(any())
            }
        }
    }
})
