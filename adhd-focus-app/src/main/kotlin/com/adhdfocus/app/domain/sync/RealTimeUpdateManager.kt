package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing real-time updates from WebSocket and applying them to UI state.
 *
 * Responsibilities:
 * - Listen to WebSocket update events from WebSocketTaskUpdateHandler
 * - Apply updates to local tasks and UI state
 * - Trigger Daily_Focus_View refresh on update events
 * - Track update latency to ensure 2-second requirement
 * - Handle connection state changes
 * - Provide visual feedback for updates
 * - Maintain update consistency without interrupting active timers
 *
 * Correctness Properties:
 * - Property 8: Remote Update Application - Updates received via WebSocket are applied to local tasks and UI refreshes
 */
interface RealTimeUpdateManager {
    /**
     * Starts listening to WebSocket updates and applying them to UI state.
     *
     * Establishes connection to WebSocket update stream and begins processing
     * incoming updates. Updates are applied to local tasks and UI state is refreshed.
     *
     * @param householdId Household ID to listen for updates
     * @param userId User ID to listen for updates
     */
    suspend fun startListening(householdId: String, userId: String)

    /**
     * Stops listening to WebSocket updates.
     *
     * Closes the update stream and stops processing incoming updates.
     */
    suspend fun stopListening()

    /**
     * Observes update events for UI refresh.
     *
     * Emits UpdateEvent whenever a remote update is applied.
     * Used by UI layer to refresh task display in real-time.
     *
     * @return Flow of UpdateEvent emitted when updates are applied
     */
    fun observeUpdates(): Flow<UpdateEvent>

    /**
     * Observes connection state changes.
     *
     * Emits connection state whenever it changes (online/offline).
     * Used by UI layer to display sync status indicator.
     *
     * @return Flow of ConnectionState emitted when connection state changes
     */
    fun observeConnectionState(): Flow<ConnectionState>

    /**
     * Observes update latency metrics.
     *
     * Emits latency measurements for each applied update.
     * Used to track compliance with 2-second update requirement.
     *
     * @return Flow of LatencyMetric emitted when updates are applied
     */
    fun observeLatency(): Flow<LatencyMetric>

    /**
     * Gets the current connection state.
     *
     * @return Current ConnectionState
     */
    suspend fun getConnectionState(): ConnectionState

    /**
     * Gets the average update latency in milliseconds.
     *
     * @return Average latency or 0 if no updates applied yet
     */
    suspend fun getAverageLatency(): Long

    /**
     * Checks if currently listening to updates.
     *
     * @return true if listening, false otherwise
     */
    suspend fun isListening(): Boolean
}

/**
 * Represents the connection state for real-time updates.
 */
enum class ConnectionState {
    /**
     * Connected and receiving updates.
     */
    CONNECTED,

    /**
     * Disconnected, updates will be queued.
     */
    DISCONNECTED,

    /**
     * Attempting to reconnect.
     */
    RECONNECTING,

    /**
     * Connection error occurred.
     */
    ERROR
}

/**
 * Represents a latency measurement for an update.
 */
data class LatencyMetric(
    val taskId: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
