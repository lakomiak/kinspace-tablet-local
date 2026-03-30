package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TaskDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Filtering by status, todo group, sync status
 * - Sorting and ordering
 * - Soft delete functionality
 * - Count operations
 * - Date range queries
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertTask() = runBlocking {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning"
        )

        taskDao.insert(task)
        val retrieved = taskDao.getTaskById("task-1")

        assertNotNull(retrieved)
        assertEquals("Test Task", retrieved.title)
        assertEquals("Morning", retrieved.todoGroup)
    }

    @Test
    fun testUpdateTask() = runBlocking {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Original Title",
            todoGroup = "Morning"
        )

        taskDao.insert(task)
        val updated = task.copy(title = "Updated Title", status = TaskStatus.COMPLETED)
        taskDao.update(updated)

        val retrieved = taskDao.getTaskById("task-1")
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved.title)
        assertEquals(TaskStatus.COMPLETED, retrieved.status)
    }

    @Test
    fun testDeleteTask() = runBlocking {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning"
        )

        taskDao.insert(task)
        taskDao.delete(task)

        val retrieved = taskDao.getTaskById("task-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetTaskById() = runBlocking {
        val task = Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning"
        )

        taskDao.insert(task)
        val retrieved = taskDao.getTaskById("task-1")

        assertNotNull(retrieved)
        assertEquals(task.id, retrieved.id)
        assertEquals(task.title, retrieved.title)
    }

    @Test
    fun testGetNonExistentTask() = runBlocking {
        val retrieved = taskDao.getTaskById("non-existent")
        assertNull(retrieved)
    }

    // ==================== Filtering by Status ====================

    @Test
    fun testGetTasksByStatus() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        // Insert tasks with different statuses
        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))

        val incompleteTasks = taskDao.getTasksByStatus(household, TaskStatus.INCOMPLETE).first()
        val completedTasks = taskDao.getTasksByStatus(household, TaskStatus.COMPLETED).first()

        assertEquals(2, incompleteTasks.size)
        assertEquals(1, completedTasks.size)
        assertTrue(completedTasks.all { it.status == TaskStatus.COMPLETED })
    }

    @Test
    fun testGetUserTasksByStatus() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user1, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user2, title = "Task 3", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))

        val user1IncompleteTasks = taskDao.getUserTasksByStatus(user1, TaskStatus.INCOMPLETE).first()
        val user2IncompleteTasks = taskDao.getUserTasksByStatus(user2, TaskStatus.INCOMPLETE).first()

        assertEquals(1, user1IncompleteTasks.size)
        assertEquals(1, user2IncompleteTasks.size)
        assertEquals("user-1", user1IncompleteTasks[0].assignedUserId)
        assertEquals("user-2", user2IncompleteTasks[0].assignedUserId)
    }

    // ==================== Filtering by Todo Group ====================

    @Test
    fun testGetTasksByTodoGroup() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning"))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Afternoon"))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning"))

        val morningTasks = taskDao.getTasksByTodoGroup(household, "Morning").first()
        val afternoonTasks = taskDao.getTasksByTodoGroup(household, "Afternoon").first()

        assertEquals(2, morningTasks.size)
        assertEquals(1, afternoonTasks.size)
        assertTrue(morningTasks.all { it.todoGroup == "Morning" })
    }

    @Test
    fun testGetUserTasksByTodoGroup() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning"))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user1, title = "Task 2", todoGroup = "Afternoon"))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user2, title = "Task 3", todoGroup = "Morning"))

        val user1MorningTasks = taskDao.getUserTasksByTodoGroup(user1, "Morning").first()
        val user2MorningTasks = taskDao.getUserTasksByTodoGroup(user2, "Morning").first()

        assertEquals(1, user1MorningTasks.size)
        assertEquals(1, user2MorningTasks.size)
        assertEquals("user-1", user1MorningTasks[0].assignedUserId)
        assertEquals("user-2", user2MorningTasks[0].assignedUserId)
    }

    // ==================== Filtering by Status and Todo Group ====================

    @Test
    fun testGetTasksByStatusAndGroup() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Afternoon", status = TaskStatus.INCOMPLETE))

        val morningIncompleteTasks = taskDao.getTasksByStatusAndGroup(household, TaskStatus.INCOMPLETE, "Morning").first()

        assertEquals(1, morningIncompleteTasks.size)
        assertEquals("Morning", morningIncompleteTasks[0].todoGroup)
        assertEquals(TaskStatus.INCOMPLETE, morningIncompleteTasks[0].status)
    }

    @Test
    fun testGetUserTasksByStatusAndGroup() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user1, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user2, title = "Task 3", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))

        val user1MorningIncompleteTasks = taskDao.getUserTasksByStatusAndGroup(user1, TaskStatus.INCOMPLETE, "Morning").first()
        val user2MorningIncompleteTasks = taskDao.getUserTasksByStatusAndGroup(user2, TaskStatus.INCOMPLETE, "Morning").first()

        assertEquals(1, user1MorningIncompleteTasks.size)
        assertEquals(1, user2MorningIncompleteTasks.size)
    }

    // ==================== Filtering by Sync Status ====================

    @Test
    fun testGetTasksBySyncStatus() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", syncStatus = SyncStatus.SYNCED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))

        val pendingTasks = taskDao.getTasksBySyncStatus(household, SyncStatus.PENDING).first()
        val syncedTasks = taskDao.getTasksBySyncStatus(household, SyncStatus.SYNCED).first()

        assertEquals(2, pendingTasks.size)
        assertEquals(1, syncedTasks.size)
        assertTrue(pendingTasks.all { it.syncStatus == SyncStatus.PENDING })
    }

    @Test
    fun testGetUserTasksBySyncStatus() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user1, title = "Task 2", todoGroup = "Morning", syncStatus = SyncStatus.SYNCED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user2, title = "Task 3", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))

        val user1PendingTasks = taskDao.getUserTasksBySyncStatus(user1, SyncStatus.PENDING).first()
        val user2PendingTasks = taskDao.getUserTasksBySyncStatus(user2, SyncStatus.PENDING).first()

        assertEquals(1, user1PendingTasks.size)
        assertEquals(1, user2PendingTasks.size)
    }

    // ==================== Recent Tasks ====================

    @Test
    fun testGetRecentTasks() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        repeat(5) { i ->
            taskDao.insert(Task(id = "task-$i", householdId = household, assignedUserId = user, title = "Task $i", todoGroup = "Morning"))
        }

        val recentTasks = taskDao.getRecentTasks(household, 3)

        assertEquals(3, recentTasks.size)
    }

    @Test
    fun testGetUserRecentTasks() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        repeat(3) { i ->
            taskDao.insert(Task(id = "task-u1-$i", householdId = household, assignedUserId = user1, title = "Task $i", todoGroup = "Morning"))
        }
        repeat(2) { i ->
            taskDao.insert(Task(id = "task-u2-$i", householdId = household, assignedUserId = user2, title = "Task $i", todoGroup = "Morning"))
        }

        val user1RecentTasks = taskDao.getUserRecentTasks(user1, 2)
        val user2RecentTasks = taskDao.getUserRecentTasks(user2, 2)

        assertEquals(2, user1RecentTasks.size)
        assertEquals(2, user2RecentTasks.size)
    }

    // ==================== Date Range Queries ====================

    @Test
    fun testGetTasksInDateRange() = runBlocking {
        val household = "household-1"
        val user = "user-1"
        val now = Instant.now()

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", createdAt = now.minusSeconds(3600)))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", createdAt = now))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", createdAt = now.plusSeconds(3600)))

        val tasksInRange = taskDao.getTasksInDateRange(household, now.minusSeconds(1800), now.plusSeconds(1800))

        assertEquals(1, tasksInRange.size)
        assertEquals("task-2", tasksInRange[0].id)
    }

    @Test
    fun testGetUserTasksInDateRange() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"
        val now = Instant.now()

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning", createdAt = now))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user2, title = "Task 2", todoGroup = "Morning", createdAt = now))

        val user1TasksInRange = taskDao.getUserTasksInDateRange(user1, now.minusSeconds(1800), now.plusSeconds(1800))
        val user2TasksInRange = taskDao.getUserTasksInDateRange(user2, now.minusSeconds(1800), now.plusSeconds(1800))

        assertEquals(1, user1TasksInRange.size)
        assertEquals(1, user2TasksInRange.size)
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetTaskCount() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        repeat(5) { i ->
            taskDao.insert(Task(id = "task-$i", householdId = household, assignedUserId = user, title = "Task $i", todoGroup = "Morning"))
        }

        val count = taskDao.getTaskCount(household)
        assertEquals(5, count)
    }

    @Test
    fun testGetUserTaskCount() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        repeat(3) { i ->
            taskDao.insert(Task(id = "task-u1-$i", householdId = household, assignedUserId = user1, title = "Task $i", todoGroup = "Morning"))
        }
        repeat(2) { i ->
            taskDao.insert(Task(id = "task-u2-$i", householdId = household, assignedUserId = user2, title = "Task $i", todoGroup = "Morning"))
        }

        val user1Count = taskDao.getUserTaskCount(user1)
        val user2Count = taskDao.getUserTaskCount(user2)

        assertEquals(3, user1Count)
        assertEquals(2, user2Count)
    }

    @Test
    fun testGetTaskCountByStatus() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))

        val incompleteCount = taskDao.getTaskCountByStatus(household, TaskStatus.INCOMPLETE)
        val completedCount = taskDao.getTaskCountByStatus(household, TaskStatus.COMPLETED)

        assertEquals(2, incompleteCount)
        assertEquals(1, completedCount)
    }

    @Test
    fun testGetUserTaskCountByStatus() = runBlocking {
        val household = "household-1"
        val user1 = "user-1"
        val user2 = "user-2"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user1, title = "Task 1", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user1, title = "Task 2", todoGroup = "Morning", status = TaskStatus.COMPLETED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user2, title = "Task 3", todoGroup = "Morning", status = TaskStatus.INCOMPLETE))

        val user1IncompleteCount = taskDao.getUserTaskCountByStatus(user1, TaskStatus.INCOMPLETE)
        val user2IncompleteCount = taskDao.getUserTaskCountByStatus(user2, TaskStatus.INCOMPLETE)

        assertEquals(1, user1IncompleteCount)
        assertEquals(1, user2IncompleteCount)
    }

    @Test
    fun testGetPendingSyncTaskCount() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", syncStatus = SyncStatus.SYNCED))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", syncStatus = SyncStatus.PENDING))

        val pendingCount = taskDao.getPendingSyncTaskCount(household, SyncStatus.PENDING)
        val syncedCount = taskDao.getPendingSyncTaskCount(household, SyncStatus.SYNCED)

        assertEquals(2, pendingCount)
        assertEquals(1, syncedCount)
    }

    // ==================== Soft Delete Operations ====================

    @Test
    fun testSoftDeleteTask() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning"))
        taskDao.softDeleteTask("task-1")

        val retrieved = taskDao.getTaskById("task-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isDeleted)
    }

    @Test
    fun testSoftDeletedTasksNotIncludedInQueries() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning"))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning"))

        taskDao.softDeleteTask("task-1")

        val tasks = taskDao.getTasksByHouseholdOnce(household)
        assertEquals(1, tasks.size)
        assertEquals("task-2", tasks[0].id)
    }

    @Test
    fun testSoftDeleteAllHouseholdTasks() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        repeat(3) { i ->
            taskDao.insert(Task(id = "task-$i", householdId = household, assignedUserId = user, title = "Task $i", todoGroup = "Morning"))
        }

        taskDao.softDeleteAllHouseholdTasks(household)

        val tasks = taskDao.getTasksByHouseholdOnce(household)
        assertEquals(0, tasks.size)
    }

    @Test
    fun testDeleteOldSoftDeletedTasks() = runBlocking {
        val household = "household-1"
        val user = "user-1"
        val now = Instant.now()

        val oldTask = Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", updatedAt = now.minusSeconds(86400 * 100))
        val newTask = Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", updatedAt = now)

        taskDao.insert(oldTask)
        taskDao.insert(newTask)

        taskDao.softDeleteTask("task-1")
        taskDao.softDeleteTask("task-2")

        val cutoffTime = now.minusSeconds(86400 * 50)
        taskDao.deleteOldSoftDeletedTasks(cutoffTime)

        val allTasks = database.taskDao().getTasksByHouseholdOnce(household)
        assertEquals(1, allTasks.size)
        assertEquals("task-2", allTasks[0].id)
    }

    // ==================== Soft Delete Exclusion ====================

    @Test
    fun testGetTasksByHouseholdExcludesSoftDeleted() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning"))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning"))

        taskDao.softDeleteTask("task-1")

        val tasks = taskDao.getTasksByHousehold(household).first()
        assertEquals(1, tasks.size)
        assertEquals("task-2", tasks[0].id)
    }

    @Test
    fun testGetTasksByUserExcludesSoftDeleted() = runBlocking {
        val household = "household-1"
        val user = "user-1"

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning"))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning"))

        taskDao.softDeleteTask("task-1")

        val tasks = taskDao.getTasksByUser(user).first()
        assertEquals(1, tasks.size)
        assertEquals("task-2", tasks[0].id)
    }

    // ==================== Ordering ====================

    @Test
    fun testTasksOrderedByCreatedAtDescending() = runBlocking {
        val household = "household-1"
        val user = "user-1"
        val now = Instant.now()

        taskDao.insert(Task(id = "task-1", householdId = household, assignedUserId = user, title = "Task 1", todoGroup = "Morning", createdAt = now.minusSeconds(100)))
        taskDao.insert(Task(id = "task-2", householdId = household, assignedUserId = user, title = "Task 2", todoGroup = "Morning", createdAt = now))
        taskDao.insert(Task(id = "task-3", householdId = household, assignedUserId = user, title = "Task 3", todoGroup = "Morning", createdAt = now.minusSeconds(50)))

        val tasks = taskDao.getTasksByHouseholdOnce(household)

        assertEquals("task-2", tasks[0].id)
        assertEquals("task-3", tasks[1].id)
        assertEquals("task-1", tasks[2].id)
    }

    // ==================== Validation ====================

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsBlankHouseholdId() {
        Task(
            id = "task-1",
            householdId = "",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsBlankAssignedUserId() {
        Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "",
            title = "Test Task",
            todoGroup = "Morning"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsBlankTitle() {
        Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "",
            todoGroup = "Morning"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsBlankTodoGroup() {
        Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsNegativeEstimatedDuration() {
        Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning",
            estimatedDurationMinutes = -5
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTaskValidationRejectsNegativeActualDuration() {
        Task(
            id = "task-1",
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Test Task",
            todoGroup = "Morning",
            actualDurationMinutes = -5
        )
    }
}
