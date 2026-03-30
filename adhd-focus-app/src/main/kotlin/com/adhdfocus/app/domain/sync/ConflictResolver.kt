package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task

/**
 * Interface for resolving sync conflicts between local and remote task versions.
 *
 * Responsibilities:
 * - Detect conflicts (different updatedAt or status)
 * - Resolve conflicts by comparing timestamps
 * - Provide conflict reason for logging
 */
interface ConflictResolver {
    /**
     * Checks if a conflict exists between local and remote versions.
     *
     * @param localTask Local task version
     * @param remoteTask Remote task version
     * @return true if conflict exists (different updatedAt or status)
     */
    fun isConflict(localTask: Task, remoteTask: Task): Boolean

    /**
     * Resolves a conflict by comparing timestamps.
     *
     * @param localTask Local task version
     * @param remoteTask Remote task version
     * @return Winning version (most recent by updatedAt, or remote if equal)
     */
    fun resolveConflict(localTask: Task, remoteTask: Task): Task

    /**
     * Gets the reason for a conflict.
     *
     * @param localTask Local task version
     * @param remoteTask Remote task version
     * @return Human-readable conflict reason
     */
    fun getConflictReason(localTask: Task, remoteTask: Task): String
}
