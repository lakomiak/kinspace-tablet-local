package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.network.TaskService
import com.adhdfocus.app.data.network.SyncService
import com.adhdfocus.app.data.network.TaskResponse
import com.adhdfocus.app.data.network.TasksResponse
import com.adhdfocus.app.data.network.CreateTaskRequest
import com.adhdfocus.app.data.network.UpdateTaskRequest
import com.adhdfocus.app.data.network.BatchSyncRequest
import com.adhdfocus.app.data.network.SyncResponse
import com.adhdfocus.app.data.network.SyncConflict as ApiSyncConflict
import com.google.gson.Gson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import retrofit2.Call
import retrofit2.Response
import java.time.Instant
import java.util.UUID

class RestApiClientUnitTest : FunSpec({
    val taskService = mockk<TaskService>()
    val syncService = mockk<SyncService>()
    val gson = Gson()
    val tokenProvider = mockk<TokenProvider>()
    val client = RestApiClientImpl(taskService, syncService, gson, tokenProvider)

    val householdId = "household-123"
    val userId = "user-456"
    val taskId = UUID.randomUUID().toString()

    fun createTestTask(
        id: String = taskId,
        status: TaskStatus = TaskStatus.INCOMPLETE,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = "Test Task",
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    fun createTaskResponse(
        id: String = taskId,
        status: String = "INCOMPLETE",
        syncStatus: String = "PENDING"
    ): TaskResponse {
        val now = Instant.now().toString()
        return TaskResponse(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = "Test Task",
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    test("createTask should send request and return created task") {
        val task = createTestTask()
        val response = createTaskResponse()
        val call = mockk<Call<TaskResponse>>()

        coEvery { taskService.createTask(householdId, any()) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val result = client.createTask(householdId, task)

        result.id shouldBe taskId
        result.title shouldBe "Test Task"
        result.householdId shouldBe householdId
        verify { taskService.createTask(householdId, any()) }
    }

    test("updateTask should send request and return updated task") {
        val taskId = "task-789"
        val response = createTaskResponse(id = taskId, status = "COMPLETED")
        val call = mockk<Call<TaskResponse>>()

        coEvery { taskService.updateTask(householdId, taskId, any()) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val updates = mapOf(
            "status" to TaskStatus.COMPLETED,
            "actualDurationMinutes" to 25
        )
        val result = client.updateTask(householdId, taskId, updates)

        result.id shouldBe taskId
        result.status shouldBe TaskStatus.COMPLETED
        verify { taskService.updateTask(householdId, taskId, any()) }
    }

    test("deleteTask should send delete request") {
        val taskId = "task-delete"
        val call = mockk<Call<Unit>>()

        coEvery { taskService.deleteTask(householdId, taskId) } returns call
        coEvery { call.execute() } returns Response.success(Unit)

        client.deleteTask(householdId, taskId)

        verify { taskService.deleteTask(householdId, taskId) }
    }

    test("fetchTasks should return list of tasks") {
        val task1 = createTaskResponse(id = "task-1")
        val task2 = createTaskResponse(id = "task-2")
        val response = TasksResponse(tasks = listOf(task1, task2))
        val call = mockk<Call<TasksResponse>>()

        coEvery { taskService.getTasks(householdId) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val result = client.fetchTasks(householdId)

        result shouldHaveSize 2
        result[0].id shouldBe "task-1"
        result[1].id shouldBe "task-2"
    }

    test("batchSync should send changes and return sync result") {
        val change1 = SyncChange(
            taskId = "task-1",
            operation = SyncOperation.CREATE,
            payload = "{}",
            timestamp = System.currentTimeMillis()
        )
        val change2 = SyncChange(
            taskId = "task-2",
            operation = SyncOperation.UPDATE,
            payload = "{}",
            timestamp = System.currentTimeMillis()
        )

        val syncResponse = SyncResponse(
            syncedCount = 2,
            failedCount = 0,
            conflicts = null
        )
        val call = mockk<Call<SyncResponse>>()

        coEvery { syncService.batchSync(householdId, any()) } returns call
        coEvery { call.execute() } returns Response.success(syncResponse)

        val result = client.batchSync(householdId, listOf(change1, change2))

        result.syncedCount shouldBe 2
        result.failedCount shouldBe 0
        result.conflicts shouldHaveSize 0
    }

    test("createTask should throw ApiException on error response") {
        val task = createTestTask()
        val call = mockk<Call<TaskResponse>>()

        coEvery { taskService.createTask(householdId, any()) } returns call
        coEvery { call.execute() } returns Response.error(400, mockk())

        try {
            client.createTask(householdId, task)
            throw AssertionError("Should have thrown ApiException")
        } catch (e: ApiException) {
            e.code shouldBe 400
        }
    }

    test("fetchTasks should handle empty response") {
        val response = TasksResponse(tasks = emptyList())
        val call = mockk<Call<TasksResponse>>()

        coEvery { taskService.getTasks(householdId) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val result = client.fetchTasks(householdId)

        result shouldHaveSize 0
    }

    test("batchSync should handle conflicts") {
        val localTask = createTaskResponse(id = "task-conflict", status = "COMPLETED")
        val remoteTask = createTaskResponse(id = "task-conflict", status = "INCOMPLETE")
        val conflict = ApiSyncConflict(
            taskId = "task-conflict",
            localVersion = localTask,
            remoteVersion = remoteTask
        )

        val syncResponse = SyncResponse(
            syncedCount = 0,
            failedCount = 1,
            conflicts = listOf(conflict)
        )
        val call = mockk<Call<SyncResponse>>()

        coEvery { syncService.batchSync(householdId, any()) } returns call
        coEvery { call.execute() } returns Response.success(syncResponse)

        val change = SyncChange(
            taskId = "task-conflict",
            operation = SyncOperation.UPDATE,
            payload = "{}",
            timestamp = System.currentTimeMillis()
        )
        val result = client.batchSync(householdId, listOf(change))

        result.syncedCount shouldBe 0
        result.failedCount shouldBe 1
        result.conflicts shouldHaveSize 1
        result.conflicts[0].taskId shouldBe "task-conflict"
    }

    test("updateTask should handle null values in updates map") {
        val taskId = "task-update"
        val response = createTaskResponse(id = taskId)
        val call = mockk<Call<TaskResponse>>()

        coEvery { taskService.updateTask(householdId, taskId, any()) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val updates = mapOf(
            "title" to null,
            "description" to "Updated description"
        )
        val result = client.updateTask(householdId, taskId, updates)

        result.id shouldBe taskId
        verify { taskService.updateTask(householdId, taskId, any()) }
    }

    test("createTask should convert task status correctly") {
        val task = createTestTask(status = TaskStatus.IN_PROGRESS)
        val response = createTaskResponse(status = "IN_PROGRESS")
        val call = mockk<Call<TaskResponse>>()

        coEvery { taskService.createTask(householdId, any()) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val result = client.createTask(householdId, task)

        result.status shouldBe TaskStatus.IN_PROGRESS
    }

    test("fetchTasks should convert sync status correctly") {
        val response = TasksResponse(
            tasks = listOf(
                createTaskResponse(syncStatus = "SYNCED"),
                createTaskResponse(syncStatus = "CONFLICT")
            )
        )
        val call = mockk<Call<TasksResponse>>()

        coEvery { taskService.getTasks(householdId) } returns call
        coEvery { call.execute() } returns Response.success(response)

        val result = client.fetchTasks(householdId)

        result[0].syncStatus shouldBe SyncStatus.SYNCED
        result[1].syncStatus shouldBe SyncStatus.CONFLICT
    }
})
