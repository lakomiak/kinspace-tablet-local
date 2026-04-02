package com.adhdfocus.app.domain.sync

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean = true,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val conflicts: List<SyncConflict> = emptyList(),
    val errorMessage: String? = null
)
