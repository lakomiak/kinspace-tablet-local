package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
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
import java.time.LocalDate
import java.time.ZoneId
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

            webSocket = okHttpClient.newWebSocket(request, InternalWebSocketListener())
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

    private inner class InternalWebSocketListener : WebSocketListener() {
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
        val durationParts = durationPartsFromJson(json)
        return Task(
            id = json.get("id")?.asString ?: "",
            householdId = json.get("householdId")?.asString ?: "",
            assignedUserId = json.get("assignedUserId")?.asString ?: "",
            title = json.get("title")?.asString ?: "",
            description = json.get("description")?.asString,
            todoGroup = json.get("todoGroup")?.asString ?: "",
            repeatRule = json.get("repeatRule")?.asString?.takeIf { it.isNotBlank() }
                ?: json.get("repeat")?.asString?.takeIf { it.isNotBlank() }
                ?: "once",
            estimatedDurationMinutes = json.get("estimatedDurationMinutes")?.asInt ?: durationParts?.first,
            estimatedDurationSeconds = durationParts?.second?.takeIf { it > 0 },
            timerDurationMs = durationParts?.let { (it.first * 60L + it.second.toLong()) * 1000L },
            actualDurationMinutes = json.get("actualDurationMinutes")?.asInt,
            status = TaskStatus.valueOf(json.get("status")?.asString ?: "INCOMPLETE"),
            dueDate = parseDueDate(json.get("dueDate")?.asString),
            createdAt = Instant.parse(json.get("createdAt")?.asString ?: Instant.now().toString()),
            updatedAt = Instant.parse(json.get("updatedAt")?.asString ?: Instant.now().toString()),
            completedAt = json.get("completedAt")?.asString?.let { Instant.parse(it) },
            syncStatus = SyncStatus.valueOf(json.get("syncStatus")?.asString ?: "PENDING"),
            isDeleted = json.get("isDeleted")?.asBoolean ?: false
        )
    }

    private fun durationPartsFromJson(json: JsonObject): Pair<Int, Int>? {
        val timerObj = json.get("timer")?.takeIf { it.isJsonObject }?.asJsonObject
        val durationMs = timerObj?.get("durationMs")?.takeIf { it.isJsonPrimitive }?.asLong
            ?: json.get("estimatedDurationSeconds")?.takeIf { it.isJsonPrimitive }?.asInt?.let { it * 1000L }
        if (durationMs == null || durationMs <= 0) return null
        val totalSeconds = (durationMs / 1000L).toInt()
        if (totalSeconds <= 0) return null
        return (totalSeconds / 60) to (totalSeconds % 60)
    }

    private fun parseDueDate(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }
            .getOrElse {
                runCatching {
                    LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant()
                }.getOrNull()
            }
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
