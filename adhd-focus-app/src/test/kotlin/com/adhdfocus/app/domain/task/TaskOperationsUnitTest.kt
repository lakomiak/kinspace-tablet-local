package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.assertions.throwables.shouldThrow
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
 * Comprehensive Unit Tests for Task Operations
 *
 * Feature: adhd-focus-app
 * Task 4.6: Create unit tests for task operations
 *
 * Tests all task operations:
 * - Create task
 * - Update task
 * - Complete task
 * - Delete task
 * - Start task
 * - Retrieve tasks
 */
class TaskOperationsUnitTest : BehaviorSpec({
    val taskDao = mock<TaskDao>()
    val syncQueueDao = mock<SyncQueueDao>()
    val taskRepository = mock<TaskRepository>()
    val taskManager = TaskManager(taskRepository, taskDao, syncQueueDao)

    Given("TaskManager for task operations") {
        When("creating a task with all fields") {
            Then("should create task with correct values") {
                // Arrange
                whenever(taskDao.insert(any())).thenReturn(1L)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.createTask(
                    title = "Complete Task",
                    description = "Task with all fields",
                    estimatedDurationMinutes = 45,
                    todoGroup = "Morning",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert
                result.title shouldBe "Complete Task"
                result.description shouldBe "Task with all fields"
                result.estimatedDurationMinutes shouldBe 45
                result.todoGroup shouldBe "Morning"
                result.householdId shouldBe "household1"
                result.assignedUserId shouldBe "user1"
                result.status shouldBe TaskStatus.INCOMPLETE
                result.syncStatus shouldBe SyncStatus.PENDING
                result.isDeleted shouldBe false
                result.completedAt shouldBe null
            }
        }

        When("creating a task with minimal fields") {
            Then("should create task with defaults") {
                // Arrange
                whenever(taskDao.insert(any())).thenReturn(1L)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.createTask(
                    title = "Minimal Task",
                    todoGroup = "Evening",
                    householdId = "household1",
                    assignedUserId = "user1"
                )

                // Assert
                result.title shouldBe "Minimal Task"
                result.description shouldBe null
                result.estimatedDurationMinutes shouldBe null
                result.todoGroup shouldBe "Evening"
                result.status shouldBe TaskStatus.INCOMPLETE
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("updating task title") {
            Then("should update only title") {
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
                    title = "Updated Title"
                )

                // Assert
                result.title shouldBe "Updated Title"
                result.todoGroup shouldBe "Morning"
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("updating task description") {
            Then("should update only description") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    description = "Old description",
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
                    description = "New description"
                )

                // Assert
                result.description shouldBe "New description"
                result.title shouldBe "Test Task"
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("updating task estimated duration") {
            Then("should update only duration") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    estimatedDurationMinutes = 30,
                    status = TaskStatus.INCOMPLETE,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(existingTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.updateTask(
                    taskId = "task1",
                    estimatedDurationMinutes = 60
                )

                // Assert
                result.estimatedDurationMinutes shouldBe 60
                result.title shouldBe "Test Task"
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("updating task status") {
            Then("should update status") {
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
                val result = taskManager.updateTask(
                    taskId = "task1",
                    status = TaskStatus.IN_PROGRESS
                )

                // Assert
                result.status shouldBe TaskStatus.IN_PROGRESS
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("updating multiple fields") {
            Then("should update all provided fields") {
                // Arrange
                val existingTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Original Title",
                    description = "Original description",
                    todoGroup = "Morning",
                    estimatedDurationMinutes = 30,
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
                    description = "Updated description",
                    estimatedDurationMinutes = 60
                )

                // Assert
                result.title shouldBe "Updated Title"
                result.description shouldBe "Updated description"
                result.estimatedDurationMinutes shouldBe 60
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("completing an incomplete task") {
            Then("should mark as COMPLETED with timestamp") {
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

                // Assert
                result.status shouldBe TaskStatus.COMPLETED
                result.completedAt shouldNotBe null
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("completing an in-progress task") {
            Then("should mark as COMPLETED with timestamp") {
                // Arrange
                val inProgressTask = Task(
                    id = "task1",
                    householdId = "household1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = TaskStatus.IN_PROGRESS,
                    syncStatus = SyncStatus.SYNCED
                )
                whenever(taskDao.getTaskById("task1")).thenReturn(inProgressTask)
                whenever(taskDao.update(any())).thenReturn(Unit)
                whenever(syncQueueDao.insert(any())).thenReturn(1L)

                // Act
                val result = taskManager.completeTask("task1")

                // Assert
                result.status shouldBe TaskStatus.COMPLETED
                result.completedAt shouldNotBe null
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("deleting an active task") {
            Then("should soft-delete task") {
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

                // Assert
                val taskCaptor = argumentCaptor<Task>()
                verify(taskDao).update(taskCaptor.capture())
                taskCaptor.firstValue.isDeleted shouldBe true
                taskCaptor.firstValue.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("starting an incomplete task") {
            Then("should transition to IN_PROGRESS") {
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

                // Assert
                result.status shouldBe TaskStatus.IN_PROGRESS
                result.syncStatus shouldBe SyncStatus.PENDING
            }
        }

        When("retrieving task by ID") {
            Then("should return task if exists") {
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
                result shouldNotBe null
                result?.id shouldBe "task1"
                result?.title shouldBe "Test Task"
            }
        }

        When("retrieving non-existent task") {
            Then("should return null") {
                // Arrange
                whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

                // Act
                val result = taskManager.getTaskById("nonexistent")

                // Assert
                result shouldBe null
            }
        }

        When("retrieving tasks by household") {
            Then("should return flow of tasks") {
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

                // Assert
                verify(taskDao, times(1)).getTasksByHousehold("household1")
            }
        }

        When("retrieving tasks by user") {
            Then("should return flow of user tasks") {
                // Arrange
                val tasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning"
                    )
                )
                whenever(taskDao.getTasksByUser("user1")).thenReturn(flowOf(tasks))

                // Act
                val result = taskManager.getTasksByUser("user1")

                // Assert
                verify(taskDao, times(1)).getTasksByUser("user1")
            }
        }

        When("retrieving tasks by status") {
            Then("should return flow of tasks with status") {
                // Arrange
                val tasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        status = TaskStatus.INCOMPLETE
                    )
                )
                whenever(taskDao.getTasksByStatus("household1", TaskStatus.INCOMPLETE))
                    .thenReturn(flowOf(tasks))

                // Act
                val result = taskManager.getTasksByStatus("household1", TaskStatus.INCOMPLETE)

                // Assert
                verify(taskDao, times(1)).getTasksByStatus("household1", TaskStatus.INCOMPLETE)
            }
        }

        When("retrieving tasks by todo group") {
            Then("should return flow of tasks in group") {
                // Arrange
                val tasks = listOf(
                    Task(
                        id = "task1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning"
                    )
                )
                whenever(taskDao.getTasksByTodoGroup("household1", "Morning"))
                    .thenReturn(flowOf(tasks))

                // Act
                val result = taskManager.getTasksByTodoGroup("household1", "Morning")

                // Assert
                verify(taskDao, times(1)).getTasksByTodoGroup("household1", "Morning")
            }
        }

        When("retrieving pending sync tasks") {
            Then("should return flow of pending tasks") {
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

        When("creating task with blank ID") {
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

        When("updating non-existent task") {
            Then("should throw IllegalArgumentException") {
                // Arrange
                whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

                // Act & Assert
                shouldThrow<IllegalArgumentException> {
                    taskManager.updateTask(taskId = "nonexistent", title = "New Title")
                }
            }
        }

        When("completing non-existent task") {
            Then("should throw IllegalArgumentException") {
                // Arrange
                whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

                // Act & Assert
                shouldThrow<IllegalArgumentException> {
                    taskManager.completeTask("nonexistent")
                }
            }
        }

        When("deleting non-existent task") {
            Then("should throw IllegalArgumentException") {
                // Arrange
                whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

                // Act & Assert
                shouldThrow<IllegalArgumentException> {
                    taskManager.deleteTask("nonexistent")
                }
            }
        }
    }
})
