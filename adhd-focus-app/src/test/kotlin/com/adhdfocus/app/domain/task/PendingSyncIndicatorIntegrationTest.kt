package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncQueueItem
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Integration Tests for Pending Sync Indicator (Property 6)
 *
 * Feature: adhd-focus-app
 * Property 6: Pending Sync Indicator
 *
 * Verifies that all local changes to tasks are marked with PENDING sync status
 * and are properly queued for synchronization with calendar-cloud.
 */
class PendingSyncIndicatorIntegrationTest : BehaviorSpec({
    val taskDao = mock<TaskDao>()
    val syncQueueDao = mock<SyncQueueDao>()
    val taskRepository = mock<TaskRepository>()
    val taskManager = TaskManager(taskRepository, taskDao, syncQueueDao)

    Given("TaskManager with sync queue integration") {
        When("creating a new task") {
            Then("task should have PENDING sync status and be queued") {
                // Arrange
                whenever(taskDao.insert(any())).thenReturn(1L)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.createTask(
                    title = "New Task",
                    description = "Task description",
                    estimatedDurationMinutes = 30,
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.title shouldBe "New Task"
                result.status shouldBe TaskStatus.INCOMPLETE

                // Verify sync queue was called
                val queueCaptor = argumentCaptor<SyncQueueItem>()
                verify(syncQueueDao, times(1)).insert(queueCaptor.capture())
                val queuedItem = queueCaptor.firstValue
                queuedItem.taskId shouldBe result.id
                queuedItem.operation shouldBe SyncOperation.CREATE
                queuedItem.userId shouldBe "user1"
            }
        }

        When("updating a previously synced task") {
            Then("task should transition from SYNCED to PENDING") {
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
                result.title shouldBe "Updated Title"

                // Verify sync queue was called with UPDATE operation
                val queueCaptor = argumentCaptor<SyncQueueItem>()
                verify(syncQueueDao, times(1)).insert(queueCaptor.capture())
                queueCaptor.firstValue.operation shouldBe SyncOperation.UPDATE
            }
        }

        When("completing a task") {
            Then("task should have PENDING sync status and be queued with UPDATE operation") {
                // Arrange
                val incompleteTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(incompleteTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.completeTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.status shouldBe TaskStatus.COMPLETED
                result.completedAt shouldNotBe null

                // Verify sync queue was called with UPDATE operation
                val queueCaptor = argumentCaptor<SyncQueueItem>()
                verify(syncQueueDao, times(1)).insert(queueCaptor.capture())
                queueCaptor.firstValue.operation shouldBe SyncOperation.UPDATE
            }
        }

        When("deleting a task") {
            Then("task should have PENDING sync status and be queued with DELETE operation") {
                // Arrange
                val activeTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(activeTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                taskManager.deleteTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                val taskCaptor = argumentCaptor<Task>()
                verify(taskDao).update(taskCaptor.capture())
                taskCaptor.firstValue.syncStatus shouldBe SyncStatus.PENDING
                taskCaptor.firstValue.isDeleted shouldBe true

                // Verify sync queue was called with DELETE operation
                val queueCaptor = argumentCaptor<SyncQueueItem>()
                verify(syncQueueDao, times(1)).insert(queueCaptor.capture())
                queueCaptor.firstValue.operation shouldBe SyncOperation.DELETE
            }
        }

        When("starting a task") {
            Then("task should have PENDING sync status and be queued") {
                // Arrange
                val incompleteTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(incompleteTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.startTask("task1")

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.status shouldBe TaskStatus.IN_PROGRESS

                // Verify sync queue was called
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("retrieving pending sync tasks for a user") {
            Then("should return all tasks with PENDING sync status") {
                // Arrange
                val pendingTasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Pending Task 1",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.PENDING
                    ),
                    Task(
                        id = "task2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Pending Task 2",
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

        When("creating multiple tasks in rapid succession") {
            Then("all should have PENDING sync status and be queued") {
                // Arrange
                whenever(taskDao.insert(any())).thenReturn(1L)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val task1 = taskManager.createTask(
                    title = "Task 1",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )
                val task2 = taskManager.createTask(
                    title = "Task 2",
                    todoGroup = "Afternoon",
                    householdId = "household1",
                    assignedUserId = "user1"
                )
                val task3 = taskManager.createTask(
                    title = "Task 3",
                    todoGroup = "Evening",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert - Property 6: Pending Sync Indicator
                task1.syncStatus shouldBe SyncStatus.PENDING
                task2.syncStatus shouldBe SyncStatus.PENDING
                task3.syncStatus shouldBe SyncStatus.PENDING

                // Verify all were queued
                verify(syncQueueDao, times(3)).insert(any())
            }
        }

        When("updating a task that has pending changes") {
            Then("should maintain PENDING sync status") {
                // Arrange
                val pendingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Original Title",
                    todoGroup = "Morning",
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.PENDING
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(pendingTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.updateTask(
                    taskId = "task1",
                    title = "Updated Title"
                )

                // Assert - Property 6: Pending Sync Indicator
                result.syncStatus shouldBe SyncStatus.PENDING
                result.title shouldBe "Updated Title"

                // Verify sync queue was called again
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("completing a task that was just created") {
            Then("both operations should be queued with PENDING status") {
                // Arrange
                whenever(taskDao.insert(any())).thenReturn(1L)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Create task
                val createdTask = taskManager.createTask(
                    title = "New Task",
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Now complete it
                whenever(taskDao.getTaskById(createdTask.id)).thenReturn(createdTask)
                whenever(taskDao.update(any())).thenReturn(Unit)

                val completedTask = taskManager.completeTask(createdTask.id)

                // Assert - Property 6: Pending Sync Indicator
                completedTask.syncStatus shouldBe SyncStatus.PENDING
                completedTask.status shouldBe TaskStatus.COMPLETED

                // Verify both operations were queued
                verify(syncQueueDao, times(2)).insert(any())
            }
        }
    }
})
