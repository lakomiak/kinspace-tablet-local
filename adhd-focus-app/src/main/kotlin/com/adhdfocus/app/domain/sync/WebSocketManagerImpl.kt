package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of WebSocketManager using OkHttp WebSocket.
 *
 * Handles:
 * - WebSocket connection lifecycle
 * - Event emission via Flow
 * - Automatic reconnection with exponential backoff
 * - Connection state tracking
 * - Event parsing and conversion to domain models
 */
class WebSocketManagerImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val tokenProvider: TokenProvider
) : WebSocketManager {

    companion object {
        private const val WS_URL = "wss://calendar-cloud.example.com/ws"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 60000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }

    private val eventFlow = MutableSharedFlow<WebSocketEvent>(replay = 0)
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private var reconnectAttempts = 0
    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null

    override fun connect(householdId: String, userId: String): Flow<WebSocketEvent> {
        currentHouseholdId = householdId
        currentUserId = userId
        reconnectAttempts = 0
        performConnect()
        return eventFlow.asSharedFlow()
    }

    override suspend fun disconnect() {
        isConnected.set(false)
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    override fun isConnected(): Boolean = isConnected.get()

    override suspend fun reconnect() {
        disconnect()
        reconnectAttempts = 0
        performConnect()
    }

    private fun performConnect() {
        try {
            val token = runBlocking { tokenProvider.getAccessToken() }
            val url = "$WS_URL?householdId=${currentHouseholdId}&userId=${currentUserId}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = okHttpClient.newWebSocket(request, WebSocketListener())
        } catch (e: Exception) {
            emitEvent(WebSocketEvent.Error("Failed to connect: ${e.message}", e))
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            emitEvent(WebSocketEvent.Error("Max reconnection attempts reached"))
            return
        }

        val backoffMs = calculateBackoff(reconnectAttempts)
        reconnectAttempts++

        // Schedule reconnection in background
        Thread {
            try {
                Thread.sleep(backoffMs)
                performConnect()
            } catch (e: InterruptedException) {
                // Ignore
            }
        }.start()
    }

    private fun calculateBackoff(attempt: Int): Long {
        val backoff = (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt.toDouble())).toLong()
        return backoff.coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun emitEvent(event: WebSocketEvent) {
        try {
            eventFlow.tryEmit(event)
        } catch (e: Exception) {
            // Flow emission failed, log but continue
        }
    }

    private inner class WebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            isConnected.set(true)
            reconnectAttempts = 0
            emitEvent(WebSocketEvent.ConnectionEstablished)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = gson.fromJson(text, JsonObject::class.java)
                val eventType = json.get("type")?.asString ?: return

                when (eventType) {
                    "task_updated" -> handleTaskUpdated(json)
                    "task_deleted" -> handleTaskDeleted(json)
                    "task_created" -> handleTaskCreated(json)
                    "sync_signal" -> emitEvent(WebSocketEvent.SyncSignal)
                }
            } catch (e: Exception) {
                emitEvent(WebSocketEvent.Error("Failed to parse message: ${e.message}", e))
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            isConnected.set(false)
            emitEvent(WebSocketEvent.ConnectionLost)
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            isConnected.set(false)
            emitEvent(WebSocketEvent.Error("WebSocket failure: ${t.message}", t))
            scheduleReconnect()
        }
    }

    private fun handleTaskUpdated(json: JsonObject) {
        try {
            val taskId = json.get("taskId")?.asString ?: return
            val taskJson = json.get("task")?.asJsonObject ?: return
            val task = parseTask(taskJson)
            emitEvent(WebSocketEvent.TaskUpdated(taskId, task))
        } catch (e: Exception) {
            emitEvent(WebSocketEvent.Error("Failed to parse task update: ${e.message}", e))
        }
    }

    private fun handleTaskDeleted(json: JsonObject) {
        try {
            val taskId = json.get("taskId")?.asString ?: return
            emitEvent(WebSocketEvent.TaskDeleted(taskId))
        } catch (e: Exception) {
            emitEvent(WebSocketEvent.Error("Failed to parse task deletion: ${e.message}", e))
        }
    }

    private fun handleTaskCreated(json: JsonObject) {
        try {
            val taskJson = json.get("task")?.asJsonObject ?: return
            val task = parseTask(taskJson)
            emitEvent(WebSocketEvent.TaskCreated(task))
        } catch (e: Exception) {
            emitEvent(WebSocketEvent.Error("Failed to parse task creation: ${e.message}", e))
        }
    }

    private fun parseTask(json: JsonObject): Task {
        return Task(
            id = json.get("id")?.asString ?: "",
            householdId = json.get("householdId")?.asString ?: "",
            assignedUserId = json.get("assignedUserId")?.asString ?: "",
            title = json.get("title")?.asString ?: "",
            description = json.get("description")?.asString,
            todoGroup = json.get("todoGroup")?.asString ?: "",
            estimatedDurationMinutes = json.get("estimatedDurationMinutes")?.asInt,
            actualDurationMinutes = json.get("actualDurationMinutes")?.asInt,
            status = TaskStatus.valueOf(json.get("status")?.asString ?: "INCOMPLETE"),
            createdAt = Instant.parse(json.get("createdAt")?.asString ?: Instant.now().toString()),
            updatedAt = Instant.parse(json.get("updatedAt")?.asString ?: Instant.now().toString()),
            completedAt = json.get("completedAt")?.asString?.let { Instant.parse(it) },
            syncStatus = SyncStatus.valueOf(json.get("syncStatus")?.asString ?: "PENDING"),
            isDeleted = json.get("isDeleted")?.asBoolean ?: false
        )
    }
}

/**
 * Blocking wrapper for coroutine execution.
 * Used for synchronous context in WebSocket callbacks.
 */
private fun <T> runBlocking(block: suspend () -> T): T {
    var result: T? = null
    var exception: Exception? = null

    val thread = Thread {
        try {
            kotlinx.coroutines.runBlocking {
                result = block()
            }
        } catch (e: Exception) {
            exception = e
        }
    }

    thread.start()
    thread.join()

    exception?.let { throw it }
    return result as T
}
