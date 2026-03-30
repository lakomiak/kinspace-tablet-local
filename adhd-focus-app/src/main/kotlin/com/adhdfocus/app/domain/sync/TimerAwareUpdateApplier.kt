package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Interface for applying updates without interrupting active timers.
 *
 * Responsibilities:
 * - Detect active timer state
 * - Queue updates when timer is active
 * - Apply queued updates when timer completes
 * - Provide visual feedback for queued updates
 * - Maintain timer state during update application
 * - Support multiple concurrent updates
 * - Handle timer cancellation gracefully
 *
 * Correctness Properties:
 * - Property 2.4: Task updates should not interrupt active timers
 * - Property 11: Real-Time Updates - Updates should be applied without disrupting user focus
 */
interface TimerAwareUpdateApplier {
    /**
     * Applies an update, checking timer state first.
     *
     * If timer is active, queues the update for later application.
     * If timer is inactive, applies the update immediately.
     *
     * @param event The update event to apply
     * @return UpdateResult indicating success or queuing
     */
    suspend fun applyUpdate(event: UpdateEvent): UpdateResult

    /**
     * Queues an update for later application.
     *
     * Called when timer is active to defer update application.
     *
     * @param event The update event to queue
     * @return true if queued successfully, false otherwise
     */
    suspend fun queueUpdate(event: UpdateEvent): Boolean

    /**
     * Applies all queued updates.
     *
     * Called when timer completes to apply deferred updates.
     * Updates are applied in FIFO order.
     *
     * @return UpdateResult indicating success and count of applied updates
     */
    suspend fun applyQueuedUpdates(): UpdateResult

    /**
     * Gets the count of queued updates.
     *
     * @return Number of updates currently queued
     */
    suspend fun getQueuedUpdateCount(): Int

    /**
     * Observes queued updates for UI feedback.
     *
     * Emits QueuedUpdateEvent whenever updates are queued or applied.
     *
     * @return Flow of QueuedUpdateEvent
     */
    fun observeQueuedUpdates(): Flow<QueuedUpdateEvent>

    /**
     * Checks if timer is currently active.
     *
     * @return true if timer is running, false otherwise
     */
    suspend fun isTimerActive(): Boolean

    /**
     * Sets the timer active state.
     *
     * Called by TimerViewModel when timer starts/stops.
     *
     * @param active true if timer is running, false otherwise
     */
    suspend fun setTimerActive(active: Boolean)

    /**
     * Clears all queued updates.
     *
     * Called when timer is cancelled to discard deferred updates.
     *
     * @return true if cleared successfully, false otherwise
     */
    suspend fun clearQueuedUpdates(): Boolean
}

/**
 * Represents an event related to queued updates.
 */
sealed class QueuedUpdateEvent {
    /**
     * Emitted when an update is queued due to active timer.
     */
    data class UpdateQueued(val taskId: String, val queueSize: Int) : QueuedUpdateEvent()

    /**
     * Emitted when queued updates are applied.
     */
    data class UpdatesApplied(val count: Int) : QueuedUpdateEvent()

    /**
     * Emitted when queued updates are cleared.
     */
    data class UpdatesCleared(val count: Int) : QueuedUpdateEvent()
}
