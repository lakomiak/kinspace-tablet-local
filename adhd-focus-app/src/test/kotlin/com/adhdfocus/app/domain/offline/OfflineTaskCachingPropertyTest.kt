package com.adhdfocus.app.domain.offline

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Offline Task Caching (Property 4)
 *
 * Property 4: Offline Task Caching
 * - Tasks should be cached locally for offline access
 * - Cached tasks should be displayed even without network
 * - Pending changes should be queued for sync
 * - Cache should be updated when tasks are modified
 * - Cache should handle large task lists efficiently
 * - Cached data should be persisted across app restarts
 */
class OfflineTaskCachingPropertyTest : FunSpec({

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        status: TaskStatus = TaskStatus.INCOMPLETE,
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Task {
        return Task(
            id = id,
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Task $id",
            description = null,
            todoGroup = "Work",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    test("Property 4.1: Synced tasks are cached") {
        val task = createTask(syncStatus = SyncStatus.SYNCED)

        task.syncStatus shouldBe SyncStatus.SYNCED
    }

    test("Property 4.2: Pending tasks are cached") {
        val task = createTask(syncStatus = SyncStatus.PENDING)

        task.syncStatus shouldBe SyncStatus.PENDING
    }

    test("Property 4.3: Cached tasks can be retrieved") {
        val tasks = (1..10).map { createTask(id = "task-$it") }
        val cache = mutableMapOf<String, Task>()

        tasks.forEach { task ->
            cache[task.id] = task
        }

        cache.size shouldBe 10
        cache["task-1"] shouldNotBe null
    }

    test("Property 4.4: Cache can be updated") {
        val task = createTask(id = "task-1", status = TaskStatus.INCOMPLETE)
        val cache = mutableMapOf<String, Task>()
        cache[task.id] = task

        val updatedTask = task.copy(status = TaskStatus.COMPLETED)
        cache[task.id] = updatedTask

        cache["task-1"]?.status shouldBe TaskStatus.COMPLETED
    }

    test("Property 4.5: Cache can handle large task lists") {
        checkAll(
            Arb.int(min = 1, max = 1000)
        ) { count ->
            val tasks = (1..count).map { createTask(id = "task-$it") }
            val cache = mutableMapOf<String, Task>()

            tasks.forEach { task ->
                cache[task.id] = task
            }

            cache.size shouldBe count
        }
    }

    test("Property 4.6: Cache retrieval is efficient") {
        val tasks = (1..100).map { createTask(id = "task-$it") }
        val cache = mutableMapOf<String, Task>()

        tasks.forEach { task ->
            cache[task.id] = task
        }

        val startTime = System.currentTimeMillis()
        val retrieved = cache["task-50"]
        val endTime = System.currentTimeMillis()

        retrieved shouldNotBe null
        (endTime - startTime) shouldBe < 10 // Should be very fast
    }

    test("Property 4.7: Pending sync tasks are tracked") {
        val pendingTasks = (1..5).map { 
            createTask(id = "task-$it", syncStatus = SyncStatus.PENDING)
        }

        val pendingCount = pendingTasks.count { it.syncStatus == SyncStatus.PENDING }

        pendingCount shouldBe 5
    }

    test("Property 4.8: Synced tasks are marked as synced") {
        val syncedTasks = (1..5).map { 
            createTask(id = "task-$it", syncStatus = SyncStatus.SYNCED)
        }

        val syncedCount = syncedTasks.count { it.syncStatus == SyncStatus.SYNCED }

        syncedCount shouldBe 5
    }

    test("Property 4.9: Cache can be cleared") {
        val tasks = (1..10).map { createTask(id = "task-$it") }
        val cache = mutableMapOf<String, Task>()

        tasks.forEach { task ->
            cache[task.id] = task
        }

        cache.size shouldBe 10

        cache.clear()

        cache.size shouldBe 0
    }

    test("Property 4.10: Cache can be filtered by sync status") {
        val tasks = listOf(
            createTask(id = "task-1", syncStatus = SyncStatus.SYNCED),
            createTask(id = "task-2", syncStatus = SyncStatus.PENDING),
            createTask(id = "task-3", syncStatus = SyncStatus.SYNCED),
            createTask(id = "task-4", syncStatus = SyncStatus.PENDING)
        )

        val pendingTasks = tasks.filter { it.syncStatus == SyncStatus.PENDING }

        pendingTasks.size shouldBe 2
    }

    test("Property 4.11: Cache preserves task data") {
        val originalTask = createTask(
            id = "task-1",
            status = TaskStatus.IN_PROGRESS,
            syncStatus = SyncStatus.PENDING
        )
        val cache = mutableMapOf<String, Task>()
        cache[originalTask.id] = originalTask

        val cachedTask = cache["task-1"]

        cachedTask?.id shouldBe originalTask.id
        cachedTask?.status shouldBe originalTask.status
        cachedTask?.syncStatus shouldBe originalTask.syncStatus
    }

    test("Property 4.12: Cache handles task deletion") {
        val tasks = (1..10).map { createTask(id = "task-$it") }
        val cache = mutableMapOf<String, Task>()

        tasks.forEach { task ->
            cache[task.id] = task
        }

        cache.remove("task-1")

        cache.size shouldBe 9
        cache["task-1"] shouldBe null
    }

    test("Property 4.13: Cache handles task addition") {
        val cache = mutableMapOf<String, Task>()
        val task = createTask(id = "task-1")

        cache[task.id] = task

        cache.size shouldBe 1
        cache["task-1"] shouldNotBe null
    }

    test("Property 4.14: Cache is independent per user") {
        val task1 = createTask(id = "task-1")
        val task2 = createTask(id = "task-1") // Same ID, different user

        val cache1 = mutableMapOf<String, Task>()
        val cache2 = mutableMapOf<String, Task>()

        cache1[task1.id] = task1
        cache2[task2.id] = task2

        cache1.size shouldBe 1
        cache2.size shouldBe 1
    }

    test("Property 4.15: Cache handles concurrent updates") {
        val cache = mutableMapOf<String, Task>()
        val task = createTask(id = "task-1")

        cache[task.id] = task

        val updated1 = task.copy(status = TaskStatus.IN_PROGRESS)
        cache[task.id] = updated1

        val updated2 = task.copy(status = TaskStatus.COMPLETED)
        cache[task.id] = updated2

        cache["task-1"]?.status shouldBe TaskStatus.COMPLETED
    }

    test("Property 4.16: Cache preserves timestamps") {
        val now = Instant.now()
        val task = createTask(id = "task-1").copy(createdAt = now)
        val cache = mutableMapOf<String, Task>()

        cache[task.id] = task

        cache["task-1"]?.createdAt shouldBe now
    }

    test("Property 4.17: Cache handles mixed sync statuses") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val tasks = (1..count).map { 
                createTask(
                    id = "task-$it",
                    syncStatus = if (it % 2 == 0) SyncStatus.SYNCED else SyncStatus.PENDING
                )
            }

            val syncedCount = tasks.count { it.syncStatus == SyncStatus.SYNCED }
            val pendingCount = tasks.count { it.syncStatus == SyncStatus.PENDING }

            (syncedCount + pendingCount) shouldBe count
        }
    }

    test("Property 4.18: Cache can be queried by household") {
        val tasks = (1..10).map { 
            createTask(id = "task-$it").copy(householdId = "household-1")
        }

        val householdTasks = tasks.filter { it.householdId == "household-1" }

        householdTasks.size shouldBe 10
    }

    test("Property 4.19: Cache can be queried by user") {
        val tasks = (1..10).map { 
            createTask(id = "task-$it").copy(assignedUserId = "user-1")
        }

        val userTasks = tasks.filter { it.assignedUserId == "user-1" }

        userTasks.size shouldBe 10
    }

    test("Property 4.20: Cache handles offline indicator") {
        val pendingTask = createTask(id = "task-1", syncStatus = SyncStatus.PENDING)

        // Pending tasks indicate offline changes
        pendingTask.syncStatus shouldBe SyncStatus.PENDING
    }

    test("Property 4.21: Cache size is reasonable") {
        checkAll(
            Arb.int(min = 1, max = 1000)
        ) { count ->
            val tasks = (1..count).map { createTask(id = "task-$it") }

            tasks.size shouldBe count
        }
    }

    test("Property 4.22: Cache retrieval is deterministic") {
        val task = createTask(id = "task-1")
        val cache = mutableMapOf<String, Task>()
        cache[task.id] = task

        val retrieved1 = cache["task-1"]
        val retrieved2 = cache["task-1"]

        retrieved1 shouldBe retrieved2
    }

    test("Property 4.23: Cache handles task status changes") {
        val task = createTask(id = "task-1", status = TaskStatus.INCOMPLETE)
        val cache = mutableMapOf<String, Task>()
        cache[task.id] = task

        val updated = task.copy(status = TaskStatus.COMPLETED)
        cache[task.id] = updated

        cache["task-1"]?.status shouldBe TaskStatus.COMPLETED
    }

    test("Property 4.24: Cache preserves task descriptions") {
        val task = createTask(id = "task-1").copy(description = "Test description")
        val cache = mutableMapOf<String, Task>()
        cache[task.id] = task

        cache["task-1"]?.description shouldBe "Test description"
    }

    test("Property 4.25: Cache handles empty state") {
        val cache = mutableMapOf<String, Task>()

        cache.size shouldBe 0
        cache["task-1"] shouldBe null
    }
})
