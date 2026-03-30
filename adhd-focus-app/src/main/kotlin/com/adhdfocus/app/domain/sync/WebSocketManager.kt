package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for WebSocket connection management with calendar-cloud.
 *
 * Responsibilities:
 * - Establish and maintain WebSocket connection
 * - Emit WebSocket events (task updates, sync signals, connection state)
 * - Handle connection lifecycle (connect, disconnect, reconnect)
 * - Implement automatic reconnection with exponential backoff
 * - Queue events received while offline
 */
interface WebSocketManager {
    /**
     * Establishes a WebSocket connection to calendar-cloud.
     *
     * @param householdId Household ID
     * @param userId User ID
     * @return Flow of WebSocketEvent emitted by the connection
     */
    fun connect(householdId: String, userId: String): Flow<WebSocketEvent>

    /**
     * Disconnects the WebSocket connection.
     */
    suspend fun disconnect()

    /**
     * Checks if the WebSocket is currently connected.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean

    /**
     * Manually triggers a reconnection attempt.
     */
    suspend fun reconnect()
}

/**
 * Sealed class representing different WebSocket events.
 */
sealed class WebSocketEvent {
    /**
     * Emitted when a task is updated by another user.
     */
    data class TaskUpdated(val taskId: String, val task: Task) : WebSocketEvent()

    /**
     * Emitted when a task is deleted by another user.
     */
    data class TaskDeleted(val taskId: String) : WebSocketEvent()

    /**
     * Emitted when a task is created by another user.
     */
    data class TaskCreated(val task: Task) : WebSocketEvent()

    /**
     * Emitted when calendar-cloud sends a sync signal.
     * Indicates that updates are available and should be fetched.
     */
    object SyncSignal : WebSocketEvent()

    /**
     * Emitted when WebSocket connection is established.
     */
    object ConnectionEstablished : WebSocketEvent()

    /**
     * Emitted when WebSocket connection is lost.
     */
    object ConnectionLost : WebSocketEvent()

    /**
     * Emitted when an error occurs on the WebSocket.
     */
    data class Error(val message: String, val throwable: Throwable? = null) : WebSocketEvent()
}
