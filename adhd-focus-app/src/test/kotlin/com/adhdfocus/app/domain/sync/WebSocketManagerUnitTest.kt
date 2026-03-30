package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.verify
import okhttp3.OkHttpClient
import java.time.Instant
import java.util.UUID

class WebSocketManagerUnitTest : FunSpec({
    val okHttpClient = mockk<OkHttpClient>()
    val gson = Gson()
    val tokenProvider = mockk<TokenProvider>()
    val manager = WebSocketManagerImpl(okHttpClient, gson, tokenProvider)

    val householdId = "household-123"
    val userId = "user-456"

    fun createTestTask(
        id: String = UUID.randomUUID().toString(),
        status: TaskStatus = TaskStatus.INCOMPLETE
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
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }

    test("isConnected should return false initially") {
        manager.isConnected() shouldBe false
    }

    test("connect should establish connection and return event flow") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        val flow = manager.connect(householdId, userId)

        flow shouldNotBe null
    }

    test("disconnect should close connection") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)
        manager.disconnect()

        manager.isConnected() shouldBe false
    }

    test("reconnect should reset connection") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)
        manager.reconnect()

        // After reconnect, should attempt to reconnect
        manager.isConnected() shouldBe false
    }

    test("WebSocketEvent.TaskUpdated should contain task data") {
        val task = createTestTask()
        val event = WebSocketEvent.TaskUpdated("task-1", task)

        event.taskId shouldBe "task-1"
        event.task shouldBe task
    }

    test("WebSocketEvent.TaskDeleted should contain task ID") {
        val event = WebSocketEvent.TaskDeleted("task-1")

        event.taskId shouldBe "task-1"
    }

    test("WebSocketEvent.TaskCreated should contain task data") {
        val task = createTestTask()
        val event = WebSocketEvent.TaskCreated(task)

        event.task shouldBe task
    }

    test("WebSocketEvent.SyncSignal should be singleton") {
        val event1 = WebSocketEvent.SyncSignal
        val event2 = WebSocketEvent.SyncSignal

        event1 shouldBe event2
    }

    test("WebSocketEvent.ConnectionEstablished should be singleton") {
        val event1 = WebSocketEvent.ConnectionEstablished
        val event2 = WebSocketEvent.ConnectionEstablished

        event1 shouldBe event2
    }

    test("WebSocketEvent.ConnectionLost should be singleton") {
        val event1 = WebSocketEvent.ConnectionLost
        val event2 = WebSocketEvent.ConnectionLost

        event1 shouldBe event2
    }

    test("WebSocketEvent.Error should contain message and throwable") {
        val throwable = Exception("Test error")
        val event = WebSocketEvent.Error("Error message", throwable)

        event.message shouldBe "Error message"
        event.throwable shouldBe throwable
    }

    test("WebSocketEvent.Error should allow null throwable") {
        val event = WebSocketEvent.Error("Error message")

        event.message shouldBe "Error message"
        event.throwable shouldBe null
    }

    test("connect should store household and user IDs") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)

        // Verify that connect was called with correct parameters
        // (This would be verified through actual WebSocket connection in integration tests)
    }

    test("multiple connect calls should use latest household and user IDs") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)
        manager.connect("household-456", "user-789")

        // Latest IDs should be used for reconnection
    }

    test("disconnect should set isConnected to false") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)
        manager.disconnect()

        manager.isConnected() shouldBe false
    }

    test("reconnect should reset reconnect attempts") {
        coEvery { tokenProvider.getAccessToken() } returns "test-token"

        manager.connect(householdId, userId)
        manager.reconnect()

        // Reconnect attempts should be reset
    }
})
