package com.adhdfocus.app.domain.persistence

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TaskPersistenceManagerUnitTest {

    private lateinit var taskDao: TaskDao
    private lateinit var persistenceManager: TaskPersistenceManager

    @Before
    fun setup() {
        taskDao = mockk()
        persistenceManager = TaskPersistenceManagerImpl(taskDao)
    }

    @Test
    fun `saveTask inserts new task when it doesn't exist`() = runTest {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Buy groceries",
            todoGroup = "Errands"
        )

        coEvery { taskDao.getTaskById("task-1") } returns null
        coEvery { taskDao.insert(task) } returns 1L

        persistenceManager.saveTask(task)

        coVerify { taskDao.insert(task) }
    }

    @Test
    fun `saveTask updates existing task`() = runTest {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Buy groceries",
            todoGroup = "Errands"
        )
        val existingTask = task.copy()

        coEvery { taskDao.getTaskById("task-1") } returns existingTask
        coEvery { taskDao.update(task) } returns Unit

        persistenceManager.saveTask(task)

        coVerify { taskDao.update(task) }
    }

    @Test
    fun `saveTask fails with blank householdId`() = runTest {
        val task = Task(
            id = "task-1",
            householdId = "",
            assignedUserId = "user-1",
            title = "Buy groceries",
            todoGroup = "Errands"
        )

        assertFailsWith<IllegalArgumentException> {
            persistenceManager.saveTask(task)
        }
    }

    @Test
    fun `saveTask fails with blank title`() = runTest {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "",
            todoGroup = "Errands"
        )

        assertFailsWith<IllegalArgumentException> {
            persistenceManager.saveTask(task)
        }
    }

    @Test
    fun `saveTasks saves multiple tasks in batch`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning"
            ),
            Task(
                id = "task-2",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 2",
                todoGroup = "Evening"
            )
        )

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        persistenceManager.saveTasks(tasks)

        coVerify(exactly = 2) { taskDao.insert(any()) }
    }

    @Test
    fun `saveTasks fails with empty list`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            persistenceManager.saveTasks(emptyList())
        }
    }

    @Test
    fun `getTasks retrieves all tasks for household`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning"
            )
        )

        coEvery { taskDao.getTasksByHouseholdOnce("household-1") } returns tasks

        val result = persistenceManager.getTasks("household-1")

        assertEquals(tasks, result)
        coVerify { taskDao.getTasksByHouseholdOnce("household-1") }
    }

    @Test
    fun `getTasks fails with blank householdId`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            persistenceManager.getTasks("")
        }
    }

    @Test
    fun `getUserTasks retrieves tasks for user`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning"
            )
        )

        coEvery { taskDao.getUserTasksInDateRange(any(), any(), any()) } returns tasks

        val result = persistenceManager.getUserTasks("user-1")

        assertEquals(tasks, result)
    }

    @Test
    fun `getTasksForDate retrieves tasks for specific date`() = runTest {
        val date = LocalDate.now()
        val tasks = listOf(
            Task(
                id = "task-1",
                householdId = "household-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning",
                createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        coEvery { taskDao.getTasksInDateRange(any(), any(), any()) } returns tasks

        val result = persistenceManager.getTasksForDate("household-1", date)

        assertEquals(tasks, result)
    }

    @Test
    fun `getTaskById retrieves single task`() = runTest {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Task 1",
            todoGroup = "Morning"
        )

        coEvery { taskDao.getTaskById("task-1") } returns task

        val result = persistenceManager.getTaskById("task-1")

        assertEquals(task, result)
    }

    @Test
    fun `getTaskById returns null when task not found`() = runTest {
        coEvery { taskDao.getTaskById("task-1") } returns null

        val result = persistenceManager.getTaskById("task-1")

        assertNull(result)
    }

    @Test
    fun `deleteOldTasks removes tasks older than cutoff`() = runTest {
        coEvery { taskDao.deleteOldSoftDeletedTasks(any()) } returns Unit

        persistenceManager.deleteOldTasks(90)

        coVerify { taskDao.deleteOldSoftDeletedTasks(any()) }
    }

    @Test
    fun `deleteOldTasks fails with non-positive days`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            persistenceManager.deleteOldTasks(0)
        }

        assertFailsWith<IllegalArgumentException> {
            persistenceManager.deleteOldTasks(-1)
        }
    }

    @Test
    fun `getTaskCount returns count for household`() = runTest {
        coEvery { taskDao.getTaskCount("household-1") } returns 5

        val result = persistenceManager.getTaskCount("household-1")

        assertEquals(5, result)
    }

    @Test
    fun `getUserTaskCount returns count for user`() = runTest {
        coEvery { taskDao.getUserTaskCount("user-1") } returns 3

        val result = persistenceManager.getUserTaskCount("user-1")

        assertEquals(3, result)
    }

    @Test
    fun `deleteTask soft deletes task`() = runTest {
        coEvery { taskDao.softDeleteTask("task-1") } returns Unit

        persistenceManager.deleteTask("task-1")

        coVerify { taskDao.softDeleteTask("task-1") }
    }

    @Test
    fun `permanentlyDeleteTask removes task from database`() = runTest {
        coEvery { taskDao.deleteTaskById("task-1") } returns Unit

        persistenceManager.permanentlyDeleteTask("task-1")

        coVerify { taskDao.deleteTaskById("task-1") }
    }

    @Test
    fun `deleteTask fails with blank taskId`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            persistenceManager.deleteTask("")
        }
    }

    @Test
    fun `permanentlyDeleteTask fails with blank taskId`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            persistenceManager.permanentlyDeleteTask("")
        }
    }
}
