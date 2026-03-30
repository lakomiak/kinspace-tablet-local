package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.domain.affirmation.AffirmationEvent
import com.adhdfocus.app.domain.affirmation.AffirmationTriggerManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.UUID

/**
 * Integration Tests for Task Completion with Affirmation Triggering
 *
 * Tests the integration between TaskManager.completeTask() and AffirmationTriggerManager.
 * Verifies that affirmations are triggered when tasks are completed.
 *
 * Correctness Properties:
 * - Property 18: Affirmation on Task Completion - Affirmation is triggered when task is completed
 */
class TaskCompletionAffirmationIntegrationTest : FunSpec({

    fun createMockTaskDao(): TaskDao {
        return object : TaskDao {
            private val tasks = mutableMapOf<String, Task>()

            override suspend fun insert(task: Task) {
                tasks[task.id] = task
            }

            override suspend fun update(task: Task) {
                tasks[task.id] = task
            }

            override suspend fun delete(task: Task) {
                tasks.remove(task.id)
            }

            override suspend fun getTaskById(id: String): Task? = tasks[id]

            override fun getTasksByHousehold(householdId: String) = throw NotImplementedError()
            override fun getTasksByUser(userId: String) = throw NotImplementedError()
            override fun getTasksByStatus(householdId: String, status: TaskStatus) = throw NotImplementedError()
            override fun getTasksByTodoGroup(householdId: String, todoGroup: String) = throw NotImplementedError()
            override fun getUserTasksBySyncStatus(userId: String, syncStatus: SyncStatus) = throw NotImplementedError()
        }
    }

    fun createMockSyncQueueDao(): SyncQueueDao {
        return object : SyncQueueDao {
            override suspend fun insert(item: com.adhdfocus.app.data.model.SyncQueueItem) {}
            override suspend fun update(item: com.adhdfocus.app.data.model.SyncQueueItem) {}
            override suspend fun delete(item: com.adhdfocus.app.data.model.SyncQueueItem) {}
            override suspend fun getById(id: String) = throw NotImplementedError()
            override fun getAll() = throw NotImplementedError()
            override fun getByUserId(userId: String) = throw NotImplementedError()
            override fun getByStatus(status: String) = throw NotImplementedError()
        }
    }

    fun createMockTaskRepository(): TaskRepository {
        return object : TaskRepository {
            override fun getTasksByHousehold(householdId: String) = throw NotImplementedError()
            override fun getTasksByUser(userId: String) = throw NotImplementedError()
            override suspend fun getTaskById(id: String) = throw NotImplementedError()
        }
    }

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        householdId: String = "household-1",
        assignedUserId: String = "user-1"
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = null,
            todoGroup = "Work",
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = TaskStatus.INCOMPLETE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    test("Task completion triggers affirmation") {
        runTest {
            val taskDao = createMockTaskDao()
            val syncQueueDao = createMockSyncQueueDao()
            val taskRepository = createMockTaskRepository()
            val affirmationManager = AffirmationTriggerManager()

            val taskManager = TaskManager(
                taskRepository = taskRepository,
                taskDao = taskDao,
                syncQueueDao = syncQueueDao,
                affirmationTriggerManager = affirmationManager
            )

            val task = createTask(id = "task-1", title = "Complete this task")
            taskDao.insert(task)

            // Complete the task
            val completedTask = taskManager.completeTask("task-1")

            // Verify task is completed
            completedTask.status shouldBe TaskStatus.COMPLETED
            completedTask.completedAt shouldNotBe null

            // Verify affirmation was triggered
            val affirmation = affirmationManager.affirmationEvent.value
            affirmation shouldNotBe null
            affirmation shouldBe is AffirmationEvent.TaskComplete
        }
    }

    test("Task completion affirmation includes task ID") {
        runTest {
            val taskDao = createMockTaskDao()
            val syncQueueDao = createMockSyncQueueDao()
            val taskRepository = createMockTaskRepository()
            val affirmationManager = AffirmationTriggerManager()

            val taskManager = TaskManager(
                taskRepository = taskRepository,
                taskDao = taskDao,
                syncQueueDao = syncQueueDao,
                affirmationTriggerManager = affirmationManager
            )

            val taskId = "task-123"
            val task = createTask(id = taskId, title = "Important task")
            taskDao.insert(task)

            // Complete the task
            taskManager.completeTask(taskId)

            // Verify affirmation includes correct task ID
            val affirmation = affirmationManager.affirmationEvent.value as? AffirmationEvent.TaskComplete
            affirmation?.taskId shouldBe taskId
        }
    }

    test("Task completion affirmation has non-empty message") {
        runTest {
            val taskDao = createMockTaskDao()
            val syncQueueDao = createMockSyncQueueDao()
            val taskRepository = createMockTaskRepository()
            val affirmationManager = AffirmationTriggerManager()

            val taskManager = TaskManager(
                taskRepository = taskRepository,
                taskDao = taskDao,
                syncQueueDao = syncQueueDao,
                affirmationTriggerManager = affirmationManager
            )

            val task = createTask(id = "task-1")
            taskDao.insert(task)

            // Complete the task
            taskManager.completeTask("task-1")

            // Verify affirmation has message
            val affirmation = affirmationManager.affirmationEvent.value as? AffirmationEvent.TaskComplete
            affirmation?.message?.isNotEmpty() shouldBe true
        }
    }

    test("Multiple task completions trigger multiple affirmations") {
        runTest {
            val taskDao = createMockTaskDao()
            val syncQueueDao = createMockSyncQueueDao()
            val taskRepository = createMockTaskRepository()
            val affirmationManager = AffirmationTriggerManager()

            val taskManager = TaskManager(
                taskRepository = taskRepository,
                taskDao = taskDao,
                syncQueueDao = syncQueueDao,
                affirmationTriggerManager = affirmationManager
            )

            // Complete first task
            val task1 = createTask(id = "task-1")
            taskDao.insert(task1)
            taskManager.completeTask("task-1")
            val affirmation1 = affirmationManager.affirmationEvent.value
            affirmation1 shouldBe is AffirmationEvent.TaskComplete

            // Clear and complete second task
            affirmationManager.clearAffirmation()
            val task2 = createTask(id = "task-2")
            taskDao.insert(task2)
            taskManager.completeTask("task-2")
            val affirmation2 = affirmationManager.affirmationEvent.value
            affirmation2 shouldBe is AffirmationEvent.TaskComplete
        }
    }

    test("Task completion marks task with PENDING sync status") {
        runTest {
            val taskDao = createMockTaskDao()
            val syncQueueDao = createMockSyncQueueDao()
            val taskRepository = createMockTaskRepository()
            val affirmationManager = AffirmationTriggerManager()

            val taskManager = TaskManager(
                taskRepository = taskRepository,
                taskDao = taskDao,
                syncQueueDao = syncQueueDao,
                affirmationTriggerManager = affirmationManager
            )

            val task = createTask(id = "task-1")
            taskDao.insert(task)

            // Complete the task
            val completedTask = taskManager.completeTask("task-1")

            // Verify sync status is PENDING
            completedTask.syncStatus shouldBe SyncStatus.PENDING
        }
    }
})
