package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.SyncQueueDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.SyncQueueItem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class SyncQueueManagerTest : BehaviorSpec({
    val syncQueueDao = mock<SyncQueueDao>()
    val syncQueueManager = SyncQueueManager(syncQueueDao)

    Given("SyncQueueManager with valid dependencies") {
        When("queueing an item with valid input") {
            Then("item should be created and inserted") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50)
                ) { taskId, userId ->
                    // Arrange
                    whenever(syncQueueDao.insert(any())).thenReturn(1L)

                    // Act
                    val result = syncQueueManager.queueItem(
                        taskId = taskId,
                        userId = userId,
                        operation = SyncOperation.CREATE,
                        payload = """{"id":"$taskId","title":"Test"}"""
                    )

                    // Assert
                    result.taskId shouldBe taskId
                    result.userId shouldBe userId
                    result.operation shouldBe SyncOperation.CREATE
                    result.retryCount shouldBe 0
                    verify(syncQueueDao, times(1)).insert(any())
                }
            }
        }

        When("queueing an item with blank taskId") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    syncQueueManager.queueItem(
                        taskId = "",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}"
                    )
                }
            }
        }

        When("queueing an item with blank userId") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    syncQueueManager.queueItem(
                        taskId = "task1",
                        userId = "",
                        operation = SyncOperation.CREATE,
                        payload = "{}"
                    )
                }
            }
        }

        When("queueing an item with blank payload") {
            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    syncQueueManager.queueItem(
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = ""
                    )
                }
            }
        }

        When("getting pending items for a user") {
            Then("should return items in FIFO order") {
                // Arrange
                val items = listOf(
                    SyncQueueItem(
                        id = "item1",
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}",
                        timestamp = Instant.now().minusSeconds(60)
                    ),
                    SyncQueueItem(
                        id = "item2",
                        taskId = "task2",
                        userId = "user1",
                        operation = SyncOperation.UPDATE,
                        payload = "{}",
                        timestamp = Instant.now()
                    )
                )
                whenever(syncQueueDao.getPendingItemsByUserFifo("user1")).thenReturn(items)

                // Act
                val result = syncQueueManager.getPendingItemsByUser("user1")

                // Assert
                result.size shouldBe 2
                result[0].id shouldBe "item1"
                result[1].id shouldBe "item2"
            }
        }

        When("getting all pending items") {
            Then("should return items in FIFO order") {
                // Arrange
                val items = listOf(
                    SyncQueueItem(
                        id = "item1",
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}",
                        timestamp = Instant.now().minusSeconds(60)
                    ),
                    SyncQueueItem(
                        id = "item2",
                        taskId = "task2",
                        userId = "user2",
                        operation = SyncOperation.UPDATE,
                        payload = "{}",
                        timestamp = Instant.now()
                    )
                )
                whenever(syncQueueDao.getAllPendingItemsFifo()).thenReturn(items)

                // Act
                val result = syncQueueManager.getAllPendingItems()

                // Assert
                result.size shouldBe 2
            }
        }

        When("getting pending items by operation") {
            Then("should return items for the specified operation") {
                // Arrange
                val items = listOf(
                    SyncQueueItem(
                        id = "item1",
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}"
                    )
                )
                whenever(syncQueueDao.getPendingItemsByUserAndOperation("user1", SyncOperation.CREATE))
                    .thenReturn(items)

                // Act
                val result = syncQueueManager.getPendingItemsByOperation("user1", SyncOperation.CREATE)

                // Assert
                result.size shouldBe 1
                result[0].operation shouldBe SyncOperation.CREATE
            }
        }

        When("getting retryable items") {
            Then("should return items with retry count below max") {
                // Arrange
                val items = listOf(
                    SyncQueueItem(
                        id = "item1",
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}",
                        retryCount = 2
                    )
                )
                whenever(syncQueueDao.getRetryableItemsByUser("user1", SyncQueueManager.MAX_RETRIES))
                    .thenReturn(items)

                // Act
                val result = syncQueueManager.getRetryableItems("user1")

                // Assert
                result.size shouldBe 1
                result[0].retryCount shouldBe 2
            }
        }

        When("incrementing retry count") {
            Then("should call dao to increment") {
                // Act
                syncQueueManager.incrementRetryCount("item1")

                // Assert
                verify(syncQueueDao, times(1)).incrementRetryCount("item1")
            }
        }

        When("removing an item") {
            Then("should call dao to delete") {
                // Act
                syncQueueManager.removeItem("item1")

                // Assert
                verify(syncQueueDao, times(1)).deleteItemById("item1")
            }
        }

        When("removing items by task") {
            Then("should call dao to delete by task") {
                // Act
                syncQueueManager.removeItemsByTask("task1")

                // Assert
                verify(syncQueueDao, times(1)).deleteItemsByTaskId("task1")
            }
        }

        When("removing items by user") {
            Then("should call dao to delete by user") {
                // Act
                syncQueueManager.removeItemsByUser("user1")

                // Assert
                verify(syncQueueDao, times(1)).deleteItemsByUserId("user1")
            }
        }

        When("removing items by operation") {
            Then("should call dao to delete by operation") {
                // Act
                syncQueueManager.removeItemsByOperation("user1", SyncOperation.CREATE)

                // Assert
                verify(syncQueueDao, times(1)).deleteItemsByUserAndOperation("user1", SyncOperation.CREATE)
            }
        }

        When("getting pending item count") {
            Then("should return count from dao") {
                // Arrange
                whenever(syncQueueDao.getPendingItemCount("user1")).thenReturn(5)

                // Act
                val result = syncQueueManager.getPendingItemCount("user1")

                // Assert
                result shouldBe 5
            }
        }

        When("getting pending item count by operation") {
            Then("should return count for operation") {
                // Arrange
                whenever(syncQueueDao.getPendingItemCountByOperation("user1", SyncOperation.CREATE))
                    .thenReturn(3)

                // Act
                val result = syncQueueManager.getPendingItemCountByOperation("user1", SyncOperation.CREATE)

                // Assert
                result shouldBe 3
            }
        }

        When("getting retryable item count") {
            Then("should return count of retryable items") {
                // Arrange
                whenever(syncQueueDao.getRetryableItemCount("user1", SyncQueueManager.MAX_RETRIES))
                    .thenReturn(2)

                // Act
                val result = syncQueueManager.getRetryableItemCount("user1")

                // Assert
                result shouldBe 2
            }
        }

        When("checking if user has pending items") {
            Then("should return true if count > 0") {
                // Arrange
                whenever(syncQueueDao.getPendingItemCount("user1")).thenReturn(1)

                // Act
                val result = syncQueueManager.hasPendingItems("user1")

                // Assert
                result shouldBe true
            }
        }

        When("checking if user has no pending items") {
            Then("should return false if count == 0") {
                // Arrange
                whenever(syncQueueDao.getPendingItemCount("user1")).thenReturn(0)

                // Act
                val result = syncQueueManager.hasPendingItems("user1")

                // Assert
                result shouldBe false
            }
        }

        When("cleaning up old items") {
            Then("should call dao with cutoff time") {
                // Act
                syncQueueManager.cleanupOldItems("user1", 30)

                // Assert
                verify(syncQueueDao, times(1)).deleteOldItems(any(), any())
            }
        }

        When("clearing all items") {
            Then("should call dao to delete all") {
                // Act
                syncQueueManager.clearAllItems()

                // Assert
                verify(syncQueueDao, times(1)).deleteAllItems()
            }
        }

        When("getting items in time range") {
            Then("should return items in range") {
                // Arrange
                val startTime = Instant.now().minusSeconds(3600)
                val endTime = Instant.now()
                val items = listOf(
                    SyncQueueItem(
                        id = "item1",
                        taskId = "task1",
                        userId = "user1",
                        operation = SyncOperation.CREATE,
                        payload = "{}",
                        timestamp = Instant.now().minusSeconds(1800)
                    )
                )
                whenever(syncQueueDao.getItemsInTimeRange("user1", startTime, endTime))
                    .thenReturn(items)

                // Act
                val result = syncQueueManager.getItemsInTimeRange("user1", startTime, endTime)

                // Assert
                result.size shouldBe 1
            }
        }

        When("getting item by ID") {
            Then("should return item if found") {
                // Arrange
                val item = SyncQueueItem(
                    id = "item1",
                    taskId = "task1",
                    userId = "user1",
                    operation = SyncOperation.CREATE,
                    payload = "{}"
                )
                whenever(syncQueueDao.getItemById("item1")).thenReturn(item)

                // Act
                val result = syncQueueManager.getItemById("item1")

                // Assert
                result shouldNotBe null
                result?.id shouldBe "item1"
            }
        }

        When("updating an item") {
            Then("should call dao to update") {
                // Arrange
                val item = SyncQueueItem(
                    id = "item1",
                    taskId = "task1",
                    userId = "user1",
                    operation = SyncOperation.CREATE,
                    payload = "{}",
                    retryCount = 1
                )

                // Act
                syncQueueManager.updateItem(item)

                // Assert
                verify(syncQueueDao, times(1)).update(item)
            }
        }
    }
})
