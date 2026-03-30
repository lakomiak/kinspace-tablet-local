package com.adhdfocus.app.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Interface for monitoring network connectivity state.
 *
 * Responsibilities:
 * - Monitor network connectivity changes
 * - Emit connectivity state changes via Flow
 * - Provide current connectivity status
 */
interface ConnectivityManager {
    /**
     * Observes connectivity state changes.
     *
     * Emits true when online, false when offline.
     * Emits immediately with current state on subscription.
     *
     * @return Flow that emits connectivity state changes
     */
    fun observeConnectivity(): Flow<Boolean>

    /**
     * Gets the current connectivity status.
     *
     * @return true if online, false if offline
     */
    fun isOnline(): Boolean
}
