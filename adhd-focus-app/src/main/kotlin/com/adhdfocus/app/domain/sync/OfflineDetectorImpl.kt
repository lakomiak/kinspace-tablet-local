package com.adhdfocus.app.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Implementation of OfflineDetector using ConnectivityManager.
 *
 * Monitors network connectivity changes and emits state transitions via Flow.
 * Handles different network types (WiFi, cellular, etc.).
 * Gracefully handles rapid connectivity changes through Flow's distinctUntilChanged.
 */
class OfflineDetectorImpl @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : OfflineDetector {

    private var isMonitoring = false

    override fun startMonitoring() {
        isMonitoring = true
    }

    override fun stopMonitoring() {
        isMonitoring = false
    }

    override fun isOnline(): Boolean {
        return connectivityManager.isOnline()
    }

    override fun observeConnectivityState(): Flow<Boolean> {
        return connectivityManager.observeConnectivity()
            .distinctUntilChanged()
    }
}
