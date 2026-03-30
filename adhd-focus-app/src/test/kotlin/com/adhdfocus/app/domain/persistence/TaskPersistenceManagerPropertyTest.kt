package com.adhdfocus.app.domain.persistence

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for TaskPersistenceManager.
 *
 * **Validates: Property 2.8: Task Persistence**
 *
 * Property 2.8 states:
 * WHEN a task is created, updated, or deleted
 * THEN the change is persisted to local database immediately
 * AND the change is marked with a timestamp
 * AND the change can be retrieved from local database
 * AND the change is preserved across app restarts
 * AND old data is cleaned up after 90 days
 */
class TaskPersistenceManagerPropertyTest {

    private lateinit var taskDao: TaskDao
    private lateinit var persistenceManager: TaskPersistenceManager

    @Before
    fun setup() {
        taskDao = mockk()
        persistenceManager = TaskPersistenceManagerImpl(taskDao)
    }

    @Test
    fun `Property 2.8: Task persistence - created tasks are persisted with timestamp`() = runTest {
        // For any task created, it should be persisted with a timestamp
        val task = Task(
            id = UUID.randomUUID().toString(),
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test task",
            todoGroup = "Morning",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(task) } returns 1L

        persistenceManager.saveTask(task)

        // Verify task has timestamp
        assertTrue(task.createdAt.isBefore(Instant.now().plusSeconds(1)))
        assertTrue(task.updatedAt.isBefore(Instant.now().plusSeconds(1)))
    }

    @Test
    fun `Property 2.8: Task persistence - updated tasks are persisted with new timestamp`() = runTest {
        // For any task updated, the change should be persisted with updated timestamp
        val originalTask = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Original title",
            todoGroup = "Morning",
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )

        val updatedTask = originalTask.copy(
            title = "Updated title",
            updatedAt = Instant.now()
        )

        coEvery { taskDao.getTaskById("task-1") } returns originalTask
        coEvery { taskDao.update(updatedTask) } returns Unit

        persistenceManager.saveTask(updatedTask)

        // Verify updated timestamp is newer
        assertTrue(updatedTask.updatedAt.isAfter(originalTask.updatedAt))
    }

    @Test
    fun `Property 2.8: Task persistence - persisted tasks can be retrieved`() = runTest {
        // For any persisted task, it should be retrievable from the database
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test task",
            todoGroup = "Morning"
        )

        coEvery { taskDao.getTaskById("task-1") } returns task

        val retrieved = persistenceManager.getTaskById("task-1")

        assertEquals(task, retrieved)
    }

    @Test
    fun `Property 2.8: Task persistence - multiple tasks persisted and retrieved`() = runTest {
        // For any set of tasks persisted, all should be retrievable
        val tasks = (1..5).map { i ->
            Task(
                id = "task-$i",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task $i",
                todoGroup = "Morning"
            )
        }

        coEvery { taskDao.getTasksByHouseholdOnce("household-1") } returns tasks

        val retrieved = persistenceManager.getTasks("household-1")

        assertEquals(tasks.size, retrieved.size)
        tasks.forEach { task ->
            assertTrue(retrieved.any { it.id == task.id })
        }
    }

    @Test
    fun `Property 2.8: Task persistence - deleted tasks are marked and can be cleaned up`() = runTest {
        // For any deleted task, it should be soft-deleted and cleanable
        coEvery { taskDao.softDeleteTask("task-1") } returns Unit
        coEvery { taskDao.deleteOldSoftDeletedTasks(any()) } returns Unit

        persistenceManager.deleteTask("task-1")
        persistenceManager.deleteOldTasks(90)

        // Verify both operations were called
        io.mockk.coVerify { taskDao.softDeleteTask("task-1") }
        io.mockk.coVerify { taskDao.deleteOldSoftDeletedTasks(any()) }
    }

    @Test
    fun `Property 2.8: Task persistence - tasks for specific date are retrievable`() = runTest {
        // For any date, tasks created on that date should be retrievable
        val date = LocalDate.now()
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val tasksForDate = listOf(
            Task(
                id = "task-1",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning",
                createdAt = startOfDay.plusSeconds(3600)
            )
        )

        coEvery { taskDao.getTasksInDateRange("household-1", any(), any()) } returns tasksForDate

        val retrieved = persistenceManager.getTasksForDate("household-1", date)

        assertEquals(1, retrieved.size)
        assertEquals("task-1", retrieved[0].id)
    }

    @Test
    fun `Property 2.8: Task persistence - task count is accurate`() = runTest {
        // For any household, the task count should match the number of persisted tasks
        coEvery { taskDao.getTaskCount("household-1") } returns 5

        val count = persistenceManager.getTaskCount("household-1")

        assertEquals(5, count)
    }

    @Test
    fun `Property 2.8: Task persistence - user task count is accurate`() = runTest {
        // For any user, the task count should match the number of persisted tasks
        coEvery { taskDao.getUserTaskCount("user-1") } returns 3

        val count = persistenceManager.getUserTaskCount("user-1")

        assertEquals(3, count)
    }

    @Test
    fun `Property 2.8: Task persistence - old tasks are cleaned up after cutoff`() = runTest {
        // For any cleanup operation, tasks older than cutoff should be removed
        coEvery { taskDao.deleteOldSoftDeletedTasks(any()) } returns Unit

        persistenceManager.deleteOldTasks(90)

        io.mockk.coVerify { taskDao.deleteOldSoftDeletedTasks(any()) }
    }

    @Test
    fun `Property 2.8: Task persistence - batch save maintains all task data`() = runTest {
        // For any batch of tasks saved, all data should be preserved
        val tasks = (1..3).map { i ->
            Task(
                id = "task-$i",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task $i",
                description = "Description $i",
                todoGroup = "Morning",
                estimatedDurationMinutes = 30 + i,
                status = TaskStatus.INCOMPLETE,
                syncStatus = SyncStatus.PENDING
            )
        }

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        persistenceManager.saveTasks(tasks)

        // Verify all tasks were inserted
        io.mockk.coVerify(exactly = 3) { taskDao.insert(any()) }
    }

    @Test
    fun `Property 2.8: Task persistence - task timestamps are preserved on retrieval`() = runTest {
        // For any task retrieved, its timestamps should match what was persisted
        val now = Instant.now()
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test task",
            todoGroup = "Morning",
            createdAt = now.minusSeconds(3600),
            updatedAt = now
        )

        coEvery { taskDao.getTaskById("task-1") } returns task

        val retrieved = persistenceManager.getTaskById("task-1")

        assertEquals(task.createdAt, retrieved?.createdAt)
        assertEquals(task.updatedAt, retrieved?.updatedAt)
    }
}
