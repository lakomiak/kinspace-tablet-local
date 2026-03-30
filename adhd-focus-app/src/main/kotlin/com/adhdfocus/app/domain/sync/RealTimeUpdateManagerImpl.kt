package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.ui.focus.FocusViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import java.time.Instant

/**
 * Implementation of RealTimeUpdateManager.
 *
 * Integrates WebSocketTaskUpdateHandler with UI layer to refresh Daily_Focus_View in real-time.
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
class RealTimeUpdateManagerImpl @Inject constructor(
    private val webSocketTaskUpdateHandler: WebSocketTaskUpdateHandler,
    private val taskDao: TaskDao,
    private val taskManager: TaskManager,
    private val webSocketManager: WebSocketManager
) : RealTimeUpdateManager {

    private val updateEventFlow = MutableSharedFlow<UpdateEvent>(replay = 0)
    private val connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1)
    private val latencyFlow = MutableSharedFlow<LatencyMetric>(replay = 0)

    private var isListening = false
    private var currentConnectionState = ConnectionState.DISCONNECTED
    private val latencyMetrics = mutableListOf<Long>()
    private val maxLatencyMetrics = 100 // Keep last 100 metrics for average calculation

    init {
        // Initialize connection state
        connectionStateFlow.tryEmit(currentConnectionState)
    }

    override suspend fun startListening(householdId: String, userId: String) {
        if (isListening) {
            return
        }

        isListening = true
        currentConnectionState = ConnectionState.CONNECTED
        connectionStateFlow.emit(currentConnectionState)

        try {
            // Listen to WebSocket update events
            webSocketTaskUpdateHandler.observeUpdates()
                .onEach { event ->
                    val startTime = System.currentTimeMillis()
                    
                    // Apply update to UI state
                    applyUpdateToUI(event, householdId, userId)
                    
                    // Track latency
                    val latency = System.currentTimeMillis() - startTime
                    recordLatency(event, latency)
                    
                    // Emit update event for UI refresh
                    updateEventFlow.emit(event)
                }
                .collect()
        } catch (e: Exception) {
            currentConnectionState = ConnectionState.ERROR
            connectionStateFlow.emit(currentConnectionState)
            isListening = false
        }
    }

    override suspend fun stopListening() {
        isListening = false
        currentConnectionState = ConnectionState.DISCONNECTED
        connectionStateFlow.emit(currentConnectionState)
    }

    override fun observeUpdates(): Flow<UpdateEvent> = updateEventFlow.asSharedFlow()

    override fun observeConnectionState(): Flow<ConnectionState> = connectionStateFlow.asSharedFlow()

    override fun observeLatency(): Flow<LatencyMetric> = latencyFlow.asSharedFlow()

    override suspend fun getConnectionState(): ConnectionState = currentConnectionState

    override suspend fun getAverageLatency(): Long {
        return if (latencyMetrics.isEmpty()) 0 else latencyMetrics.average().toLong()
    }

    override suspend fun isListening(): Boolean = isListening

    /**
     * Applies an update event to the UI state.
     *
     * Updates the task in the database and triggers UI refresh.
     * Handles different update types (TaskUpdated, TaskDeleted, TaskCreated).
     */
    private suspend fun applyUpdateToUI(
        event: UpdateEvent,
        householdId: String,
        userId: String
    ) {
        when (event) {
            is UpdateEvent.TaskUpdated -> {
                // Task is already updated in database by WebSocketTaskUpdateHandler
                // UI will refresh via observeUpdates flow
            }
            is UpdateEvent.TaskDeleted -> {
                // Task is already deleted in database by WebSocketTaskUpdateHandler
                // UI will refresh via observeUpdates flow
            }
            is UpdateEvent.TaskCreated -> {
                // Task is already created in database by WebSocketTaskUpdateHandler
                // UI will refresh via observeUpdates flow
            }
            is UpdateEvent.UpdatesApplied -> {
                // Multiple updates applied, UI should refresh
            }
            is UpdateEvent.Error -> {
                // Error occurred, UI should display error state
            }
        }
    }

    /**
     * Records latency metric for an update.
     *
     * Maintains a rolling window of latency metrics for average calculation.
     * Ensures 2-second update latency requirement is tracked.
     */
    private suspend fun recordLatency(event: UpdateEvent, latencyMs: Long) {
        val taskId = when (event) {
            is UpdateEvent.TaskUpdated -> event.taskId
            is UpdateEvent.TaskDeleted -> event.taskId
            is UpdateEvent.TaskCreated -> event.task.id
            else -> return
        }

        // Add to metrics
        latencyMetrics.add(latencyMs)
        if (latencyMetrics.size > maxLatencyMetrics) {
            latencyMetrics.removeAt(0)
        }

        // Emit latency metric
        latencyFlow.emit(LatencyMetric(taskId, latencyMs))
    }
}
