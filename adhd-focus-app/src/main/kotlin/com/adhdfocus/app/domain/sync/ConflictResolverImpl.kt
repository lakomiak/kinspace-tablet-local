package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import javax.inject.Inject

/**
 * Implementation of ConflictResolver using timestamp-based resolution.
 *
 * Resolves conflicts by comparing updatedAt timestamps:
 * - Most recent version wins
 * - If timestamps are equal, remote version wins (server is source of truth)
 * - Logs conflicts for debugging
 */
class ConflictResolverImpl @Inject constructor() : ConflictResolver {

    override fun isConflict(localTask: Task, remoteTask: Task): Boolean {
        // Conflict exists if timestamps differ or status differs
        return localTask.updatedAt != remoteTask.updatedAt ||
                localTask.status != remoteTask.status
    }

    override fun resolveConflict(localTask: Task, remoteTask: Task): Task {
        // Remote is newer or same timestamp (prefer remote as server is source of truth)
        return if (remoteTask.updatedAt.isAfter(localTask.updatedAt) ||
            remoteTask.updatedAt.equals(localTask.updatedAt)
        ) {
            remoteTask
        } else {
            localTask
        }
    }

    override fun getConflictReason(localTask: Task, remoteTask: Task): String {
        return when {
            localTask.updatedAt != remoteTask.updatedAt -> {
                val winner = if (remoteTask.updatedAt.isAfter(localTask.updatedAt)) "remote" else "local"
                "Timestamp conflict: $winner version is newer (local: ${localTask.updatedAt}, remote: ${remoteTask.updatedAt})"
            }
            localTask.status != remoteTask.status -> {
                "Status conflict: local=${localTask.status}, remote=${remoteTask.status}, preferring remote"
            }
            else -> "Conflict resolved by preferring remote version"
        }
    }
}
