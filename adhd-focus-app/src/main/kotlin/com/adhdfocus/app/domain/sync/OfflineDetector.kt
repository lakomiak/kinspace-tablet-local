package com.adhdfocus.app.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Interface for detecting and monitoring offline/online state transitions.
 *
 * Responsibilities:
 * - Monitor network connectivity state
 * - Detect transitions between online and offline states
 * - Emit connectivity state changes via Flow
 * - Provide current connectivity state
 * - Support different network types (WiFi, cellular, etc.)
 * - Handle rapid connectivity changes gracefully
 */
interface OfflineDetector {
    /**
     * Starts monitoring connectivity state.
     * Must be called before observing connectivity changes.
     */
    fun startMonitoring()

    /**
     * Stops monitoring connectivity state.
     * Cleans up resources and stops emitting state changes.
     */
    fun stopMonitoring()

    /**
     * Gets the current connectivity state.
     *
     * @return true if online, false if offline
     */
    fun isOnline(): Boolean

    /**
     * Observes connectivity state changes.
     *
     * Emits true when online, false when offline.
     * Emits immediately with current state on subscription.
     *
     * @return Flow that emits connectivity state changes
     */
    fun observeConnectivityState(): Flow<Boolean>
}
