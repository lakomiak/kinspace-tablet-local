package com.adhdfocus.app.domain.sync

/**
 * Represents the current state of cloud synchronization.
 */
enum class SyncStatus {
    /**
     * No sync operation in progress, no pending changes.
     */
    IDLE,

    /**
     * Sync operation is currently in progress.
     */
    SYNCING,

    /**
     * Last sync completed successfully.
     */
    SYNCED,

    /**
     * Last sync failed with an error.
     */
    ERROR,

    /**
     * Device is offline, cannot sync.
     */
    OFFLINE
}
