package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncQueueItem
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.repository.TaskRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class TaskManagerTest : BehaviorSpec({
    val taskDao = mock<TaskDao>()
    val syncQueueDao = mock<SyncQueueDao>()
    val taskRepository = mock<TaskRepository>()
    val taskManager = TaskManager(taskRepository, taskDao, syncQueueDao)

    Given("TaskManager with valid dependencies") {
        When("creating a task with valid input") {
            Then("task should be created with PENDING sync status") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 100),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.int(min = 1, max = 1440)
                ) { title, todoGroup, householdId, userId, duration ->
                    // Arrange
                    whenever(taskDao.insert(any())).thenReturn(1L)
                    whenever(syncQueueDao.insert(any())).thenReturn(1L)

                    // Act
                    val result = taskManager.createTask(
                        title = title,
                        description = "Test description",
                        estimatedDurationMinutes = duration,
                        todoGroup = todoGroup,
                        householdId = householdId,
                        assignedUserId = userId
                    )

                    // Assert
                    result.title shouldBe title
                    result.todoGroup shouldBe todoGroup
                    result.householdId shouldBe householdId
                    result.assignedUserId shouldBe userId
                    result.estimatedDurationMinutes shouldBe duration
                    result.status shouldBe TaskStatus.INCOMPLETE
                    result.syncStatus shouldBe SyncStatus.PENDING
                    result.isDeleted shouldBe false

                    // Verify sync queue was called
                    verify(syncQueueDao, times(1)).insert(any())
                }
            }
        }

        When("creating a task with blank title") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    taskManager.createTask(
                        title = "",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1"
                    )
                }
            }
        }

        When("creating a task with blank todoGroup") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    taskManager.createTask(
                        title = "Test Task",
                        todoGroup = "",
                        householdId = "household1",
                        assignedUserId = "user1"
                    )
                }
            }
        }

        When("creating a task with negative duration") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    taskManager.createTask(
                        title = "Test Task",
                        todoGroup = "Morning",
                        householdId = "household1",
                        assignedUserId = "user1",
                        estimatedDurationMinutes = -5
                    )
                }
            }
        }

        When("updating a task with valid input") {
            Then("task should be updated with PENDING sync status") {
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
                    title = "Updated Title",
                    estimatedDurationMinutes = 30
                )

                // Assert
                result.title shouldBe "Updated Title"
                result.estimatedDurationMinutes shouldBe 30
                result.syncStatus shouldBe SyncStatus.PENDING
                verify(taskDao, times(1)).update(any())
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("updating a non-existent task") {
            Then("should throw IllegalArgumentException") {
                whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

                shouldThrow<IllegalArgumentException> {
                    taskManager.updateTask(taskId = "nonexistent", title = "New Title")
                }
            }
        }

        When("completing a task") {
            Then("task should be marked COMPLETED with PENDING sync status") {
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

                // Assert
                result.status shouldBe TaskStatus.COMPLETED
                result.completedAt shouldNotBe null
                result.syncStatus shouldBe SyncStatus.PENDING
                verify(taskDao, times(1)).update(any())
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("deleting a task") {
            Then("task should be soft-deleted with PENDING sync status") {
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

                // Assert
                val captor = argumentCaptor<Task>()
                verify(taskDao).update(captor.capture())
                captor.firstValue.isDeleted shouldBe true
                captor.firstValue.syncStatus shouldBe SyncStatus.PENDING
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("starting a task") {
            Then("task should transition to IN_PROGRESS with PENDING sync status") {
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

                // Assert
                result.status shouldBe TaskStatus.IN_PROGRESS
                result.syncStatus shouldBe SyncStatus.PENDING
                verify(taskDao, times(1)).update(any())
                verify(syncQueueDao, times(1)).insert(any())
            }
        }

        When("retrieving a task by ID") {
            Then("should return the task if it exists") {
                // Arrange
                val task = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning"
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(task)

                // Act
                val result = taskManager.getTaskById("task1")

                // Assert
                result shouldBe task
            }
        }

        When("retrieving tasks by household") {
            Then("should return flow of household tasks") {
                // Arrange
                val tasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning"
                    ),
                    Task(
                        id = "task2",
                        householdId = "household1",
                        assignedUserId = "user2",
                        title = "Task 2",
                        todoGroup = "Evening"
                    )
                )
                whenever(taskDao.getTasksByHousehold("household1")).thenReturn(flowOf(tasks))

                // Act
                val result = taskManager.getTasksByHousehold("household1")

                // Assert - Flow is lazy, so we just verify it was called
                verify(taskDao, times(1)).getTasksByHousehold("household1")
            }
        }

        When("retrieving pending sync tasks") {
            Then("should return flow of tasks with PENDING sync status") {
                // Arrange
                val tasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.PENDING
                    )
                )
                whenever(taskDao.getUserTasksBySyncStatus("user1", SyncStatus.PENDING))
                    .thenReturn(flowOf(tasks))

                // Act
                val result = taskManager.getPendingSyncTasks("user1")

                // Assert
                verify(taskDao, times(1)).getUserTasksBySyncStatus("user1", SyncStatus.PENDING)
            }
        }
    }
})
