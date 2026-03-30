package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.OfflineUpdateQueueDao
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of OfflineUpdateQueue.
 *
 * Manages updates received while offline using Room database persistence.
 * Supports FIFO ordering, conflict detection, and batch operations.
 */
class OfflineUpdateQueueImpl(
    private val dao: OfflineUpdateQueueDao
) : OfflineUpdateQueue {

    override suspend fun addUpdate(
        taskId: String,
        userId: String,
        updateType: UpdateType,
        payload: String
    ): Boolean {
        return try {
            val item = OfflineUpdateQueueItem(
                taskId = taskId,
                userId = userId,
                updateType = updateType,
                payload = payload
            )
            dao.insert(item)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getQueuedUpdates(userId: String): List<OfflineUpdateQueueItem> {
        return dao.getItemsByUserId(userId)
    }

    override suspend fun getUnappliedUpdates(userId: String): List<OfflineUpdateQueueItem> {
        return dao.getUnappliedItemsByUserId(userId)
    }

    override suspend fun removeUpdate(updateId: String): Boolean {
        return try {
            dao.deleteItemById(updateId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun markAsApplied(updateId: String): Boolean {
        return try {
            dao.markAsApplied(updateId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun clearQueue(userId: String): Boolean {
        return try {
            dao.deleteItemsByUserId(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getQueueSize(userId: String): Int {
        return dao.getQueueSize(userId)
    }

    override suspend fun getUnappliedQueueSize(userId: String): Int {
        return dao.getUnappliedQueueSize(userId)
    }

    override fun observeQueueChanges(userId: String): Flow<QueueState> {
        return dao.observeItemsByUserId(userId).map { items ->
            QueueState(
                userId = userId,
                queueSize = items.size,
                unappliedCount = items.count { !it.applied }
            )
        }
    }

    override suspend fun hasQueuedUpdates(userId: String): Boolean {
        return getQueueSize(userId) > 0
    }
}
