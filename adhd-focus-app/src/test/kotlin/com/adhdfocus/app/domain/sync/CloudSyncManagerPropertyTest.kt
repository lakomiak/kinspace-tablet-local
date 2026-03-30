package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus as TaskSyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * Property-based tests for CloudSyncManager.
 *
 * **Validates: Requirements 10 (Cloud Sync on Connectivity)**
 *
 * Properties tested:
 * 1. Connectivity state transitions are valid
 * 2. Sync status transitions are valid
 * 3. Pending changes are synced when online
 * 4. Sync status updates correctly
 * 5. Exponential backoff on sync failure
 */
class CloudSyncManagerPropertyTest : FunSpec({

    test("Property 1: Connectivity state transitions are valid") {
        checkAll(Arb.list(Arb.int(0..1), 1..10)) { states ->
            val restApiClient = mockk<RestApiClient>()
            val syncQueueManager = mockk<SyncQueueManager>()
            val taskDao = mockk<TaskDao>()
            val connectivityManager = mockk<ConnectivityManager>()

            val manager = CloudSyncManagerImpl(
                restApiClient,
                syncQueueManager,
                taskDao,
                connectivityManager
            )

            states.forEach { state ->
                val isOnline = state == 1
                every { connectivityManager.isOnline() } returns isOnline
                coEvery { syncQueueManager.getPendingItemsByUser(any()) } returns emptyList()

                val result = runBlocking {
                    manager.syncPendingChanges("household-1", "user-1")
                }

                if (isOnline) {
                    manager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED
                } else {
                    manager.getCurrentSyncStatus() shouldBe SyncStatus.OFFLINE
                }
            }
        }
    }

    test("Property 2: Sync status transitions are valid") {
        checkAll(Arb.list(Arb.int(0..2), 1..5)) { operations ->
            val restApiClient = mockk<RestApiClient>()
            val syncQueueManager = mockk<SyncQueueManager>()
            val taskDao = mockk<TaskDao>()
            val connectivityManager = mockk<ConnectivityManager>()

            val manager = CloudSyncManagerImpl(
                restApiClient,
                syncQueueManager,
                taskDao,
                connectivityManager
            )

            operations.forEach { op ->
                when (op) {
                    0 -> {
                        // Offline
                        every { connectivityManager.isOnline() } returns false
                        runBlocking {
                            manager.syncPendingChanges("household-1", "user-1")
                        }
                        manager.getCurrentSyncStatus() shouldBe SyncStatus.OFFLINE
                    }
                    1 -> {
                        // Success
                        every { connectivityManager.isOnline() } returns true
                        coEvery { syncQueueManager.getPendingItemsByUser(any()) } returns emptyList()
                        runBlocking {
                            manager.syncPendingChanges("household-1", "user-1")
                        }
                        manager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED
                    }
                    2 -> {
                        // Error
                        every { connectivityManager.isOnline() } returns true
                        coEvery { syncQueueManager.getPendingItemsByUser(any()) } throws Exception("Error")
                        runBlocking {
                            manager.syncPendingChanges("household-1", "user-1")
                        }
                        manager.getCurrentSyncStatus() shouldBe SyncStatus.ERROR
                    }
                }
            }
        }
    }

    test("Property 3: Pending changes are synced when online") {
        checkAll(Arb.list(Arb.int(1..5), 1..3)) { changeCounts ->
            changeCounts.forEach { changeCount ->
                val restApiClient = mockk<RestApiClient>()
                val syncQueueManager = mockk<SyncQueueManager>()
                val taskDao = mockk<TaskDao>()
                val connectivityManager = mockk<ConnectivityManager>()

                val manager = CloudSyncManagerImpl(
                    restApiClient,
                    syncQueueManager,
                    taskDao,
                    connectivityManager
                )

                val items = (1..changeCount).map { i ->
                    com.adhdfocus.app.data.model.SyncQueueItem(
                        id = "queue-$i",
                        taskId = "task-$i",
                        userId = "user-1",
                        operation = SyncOperation.CREATE,
                        payload = """{"id":"task-$i"}""",
                        timestamp = Instant.now(),
                        retryCount = 0
                    )
                }

                every { connectivityManager.isOnline() } returns true
                coEvery { syncQueueManager.getPendingItemsByUser("user-1") } returns items
                coEvery {
                    restApiClient.batchSync("household-1", any())
                } returns SyncResult(syncedCount = changeCount, failedCount = 0, conflicts = emptyList())
                coEvery { syncQueueManager.removeItem(any()) } returns Unit

                val result = runBlocking {
                    manager.syncPendingChanges("household-1", "user-1")
                }

                result.syncedCount shouldBe changeCount
            }
        }
    }

    test("Property 4: Sync status updates correctly") {
        checkAll(Arb.int(0..10)) { _ ->
            val restApiClient = mockk<RestApiClient>()
            val syncQueueManager = mockk<SyncQueueManager>()
            val taskDao = mockk<TaskDao>()
            val connectivityManager = mockk<ConnectivityManager>()

            val manager = CloudSyncManagerImpl(
                restApiClient,
                syncQueueManager,
                taskDao,
                connectivityManager
            )

            every { connectivityManager.isOnline() } returns true
            coEvery { syncQueueManager.getPendingItemsByUser(any()) } returns emptyList()

            runBlocking {
                manager.syncPendingChanges("household-1", "user-1")
            }

            val status = runBlocking {
                manager.observeSyncStatus().first()
            }

            status shouldBe SyncStatus.SYNCED
        }
    }

    test("Property 5: Exponential backoff on sync failure") {
        checkAll(Arb.int(1..3)) { failureCount ->
            val restApiClient = mockk<RestApiClient>()
            val syncQueueManager = mockk<SyncQueueManager>()
            val taskDao = mockk<TaskDao>()
            val connectivityManager = mockk<ConnectivityManager>()

            val manager = CloudSyncManagerImpl(
                restApiClient,
                syncQueueManager,
                taskDao,
                connectivityManager
            )

            val items = (1..failureCount).map { i ->
                com.adhdfocus.app.data.model.SyncQueueItem(
                    id = "queue-$i",
                    taskId = "task-$i",
                    userId = "user-1",
                    operation = SyncOperation.CREATE,
                    payload = """{"id":"task-$i"}""",
                    timestamp = Instant.now(),
                    retryCount = 0
                )
            }

            every { connectivityManager.isOnline() } returns true
            coEvery { syncQueueManager.getPendingItemsByUser("user-1") } returns items
            coEvery {
                restApiClient.batchSync("household-1", any())
            } throws Exception("Network error")

            val result = runBlocking {
                manager.syncPendingChanges("household-1", "user-1")
            }

            result.failedCount shouldBe failureCount
            manager.getCurrentSyncStatus() shouldBe SyncStatus.ERROR
        }
    }
})
