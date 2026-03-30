package com.adhdfocus.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for managing sync status display.
 *
 * Responsibilities:
 * - Observe sync status from CloudSyncManager
 * - Expose sync status as StateFlow for UI consumption
 * - Handle status transitions smoothly
 *
 * Validates: Requirements 10 - Cloud Synchronization with calendar-cloud
 */
@HiltViewModel
class SyncStatusIndicatorViewModel @Inject constructor(
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    /**
     * Current sync status as a StateFlow.
     * Emits immediately with current status on subscription.
     * Updates whenever sync status changes.
     */
    val syncStatus: Flow<SyncStatus> = cloudSyncManager.observeSyncStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = SyncStatus.IDLE
        )
}
