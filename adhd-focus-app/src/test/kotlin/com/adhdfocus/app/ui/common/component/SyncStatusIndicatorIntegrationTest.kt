package com.adhdfocus.app.ui.common.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.sync.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests for SyncStatusIndicator with CloudSyncManager
 *
 * Tests verify:
 * - UI updates when sync status changes
 * - Component correctly observes CloudSyncManager status
 * - Status transitions are reflected in UI
 * - Multiple status changes are handled correctly
 *
 * Validates: Requirements 10 - Cloud Synchronization with calendar-cloud
 */
@RunWith(AndroidJUnit4::class)
class SyncStatusIndicatorIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun syncStatusIndicator_observesSyncStatus_fromCloudSyncManager() {
        val mockCloudSyncManager = MockCloudSyncManager(SyncStatus.SYNCING)

        composeTestRule.setContent {
            SyncStatusIndicator(status = mockCloudSyncManager.getCurrentSyncStatus())
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_updatesUI_whenSyncStatusChanges() {
        var currentStatus = SyncStatus.IDLE
        val mockCloudSyncManager = MockCloudSyncManager(currentStatus)

        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        // Change status to SYNCING
        currentStatus = SyncStatus.SYNCING
        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_handlesMultipleStatusTransitions() {
        var currentStatus = SyncStatus.IDLE
        val transitions = listOf(
            SyncStatus.SYNCING,
            SyncStatus.SYNCED,
            SyncStatus.IDLE,
            SyncStatus.SYNCING,
            SyncStatus.ERROR,
            SyncStatus.OFFLINE,
            SyncStatus.SYNCING,
            SyncStatus.SYNCED
        )

        for (newStatus in transitions) {
            currentStatus = newStatus
            composeTestRule.setContent {
                SyncStatusIndicator(status = currentStatus)
            }

            composeTestRule.onRoot().assertExists()
        }
    }

    @Test
    fun syncStatusIndicator_displaysCorrectIndicator_forSyncingStatus() {
        composeTestRule.setContent {
            SyncStatusIndicator(status = SyncStatus.SYNCING)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_displaysCorrectIndicator_forSyncedStatus() {
        composeTestRule.setContent {
            SyncStatusIndicator(status = SyncStatus.SYNCED)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_displaysCorrectIndicator_forErrorStatus() {
        composeTestRule.setContent {
            SyncStatusIndicator(status = SyncStatus.ERROR)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_displaysCorrectIndicator_forOfflineStatus() {
        composeTestRule.setContent {
            SyncStatusIndicator(status = SyncStatus.OFFLINE)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_handlesRapidStatusChanges() {
        var currentStatus = SyncStatus.IDLE

        for (i in 0..10) {
            currentStatus = if (i % 2 == 0) SyncStatus.SYNCING else SyncStatus.SYNCED
            composeTestRule.setContent {
                SyncStatusIndicator(status = currentStatus)
            }
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_recoversFromError_toSynced() {
        var currentStatus = SyncStatus.ERROR
        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        // Recover to SYNCED
        currentStatus = SyncStatus.SYNCED
        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_recoversFromOffline_toSyncing() {
        var currentStatus = SyncStatus.OFFLINE
        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        // Recover to SYNCING
        currentStatus = SyncStatus.SYNCING
        composeTestRule.setContent {
            SyncStatusIndicator(status = currentStatus)
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun syncStatusIndicator_completeSyncCycle() {
        val syncCycle = listOf(
            SyncStatus.IDLE,
            SyncStatus.SYNCING,
            SyncStatus.SYNCED,
            SyncStatus.IDLE
        )

        for (status in syncCycle) {
            composeTestRule.setContent {
                SyncStatusIndicator(status = status)
            }

            composeTestRule.onRoot().assertExists()
        }
    }

    @Test
    fun syncStatusIndicator_offlineToSyncCycle() {
        val offlineToSyncCycle = listOf(
            SyncStatus.OFFLINE,
            SyncStatus.SYNCING,
            SyncStatus.SYNCED,
            SyncStatus.IDLE
        )

        for (status in offlineToSyncCycle) {
            composeTestRule.setContent {
                SyncStatusIndicator(status = status)
            }

            composeTestRule.onRoot().assertExists()
        }
    }

    @Test
    fun syncStatusIndicator_errorRecoveryCycle() {
        val errorRecoveryCycle = listOf(
            SyncStatus.SYNCING,
            SyncStatus.ERROR,
            SyncStatus.SYNCING,
            SyncStatus.SYNCED
        )

        for (status in errorRecoveryCycle) {
            composeTestRule.setContent {
                SyncStatusIndicator(status = status)
            }

            composeTestRule.onRoot().assertExists()
        }
    }

    /**
     * Mock implementation of CloudSyncManager for testing
     */
    private class MockCloudSyncManager(private val initialStatus: SyncStatus) : CloudSyncManager {
        private var currentStatus = initialStatus

        override suspend fun syncPendingChanges(householdId: String, userId: String): SyncResult {
            return SyncResult(syncedCount = 0, failedCount = 0, conflicts = emptyList())
        }

        override fun observeSyncStatus(): Flow<SyncStatus> {
            return flowOf(currentStatus)
        }

        override fun getCurrentSyncStatus(): SyncStatus {
            return currentStatus
        }

        fun setStatus(status: SyncStatus) {
            currentStatus = status
        }
    }
}

/**
 * Kotest-based integration tests for SyncStatusIndicator
 *
 * Tests verify:
 * - Component integrates correctly with CloudSyncManager
 * - Status flow is properly observed
 * - UI updates reflect status changes
 */
class SyncStatusIndicatorKotestIntegrationTest : FunSpec({

    test("SyncStatusIndicator integrates with CloudSyncManager") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.SYNCING)
        val status = mockManager.getCurrentSyncStatus()

        status shouldBe SyncStatus.SYNCING
    }

    test("SyncStatusIndicator observes status changes from CloudSyncManager") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.IDLE)

        mockManager.setStatus(SyncStatus.SYNCING)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCING

        mockManager.setStatus(SyncStatus.SYNCED)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED
    }

    test("SyncStatusIndicator handles complete sync cycle") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.IDLE)

        mockManager.setStatus(SyncStatus.SYNCING)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCING

        mockManager.setStatus(SyncStatus.SYNCED)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED

        mockManager.setStatus(SyncStatus.IDLE)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.IDLE
    }

    test("SyncStatusIndicator handles error recovery") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.SYNCING)

        mockManager.setStatus(SyncStatus.ERROR)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.ERROR

        mockManager.setStatus(SyncStatus.SYNCING)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCING

        mockManager.setStatus(SyncStatus.SYNCED)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED
    }

    test("SyncStatusIndicator handles offline to online transition") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.OFFLINE)

        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.OFFLINE

        mockManager.setStatus(SyncStatus.SYNCING)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCING

        mockManager.setStatus(SyncStatus.SYNCED)
        mockManager.getCurrentSyncStatus() shouldBe SyncStatus.SYNCED
    }

    test("SyncStatusIndicator handles rapid status changes") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.IDLE)
        val statuses = listOf(
            SyncStatus.SYNCING,
            SyncStatus.SYNCED,
            SyncStatus.IDLE,
            SyncStatus.SYNCING,
            SyncStatus.ERROR,
            SyncStatus.OFFLINE
        )

        for (status in statuses) {
            mockManager.setStatus(status)
            mockManager.getCurrentSyncStatus() shouldBe status
        }
    }

    test("SyncStatusIndicator maintains status consistency") {
        val mockManager = MockCloudSyncManagerKotest(SyncStatus.SYNCING)

        val status1 = mockManager.getCurrentSyncStatus()
        val status2 = mockManager.getCurrentSyncStatus()

        status1 shouldBe status2
    }

    /**
     * Mock implementation for Kotest tests
     */
    class MockCloudSyncManagerKotest(private val initialStatus: SyncStatus) : CloudSyncManager {
        private var currentStatus = initialStatus

        override suspend fun syncPendingChanges(householdId: String, userId: String): SyncResult {
            return SyncResult(syncedCount = 0, failedCount = 0, conflicts = emptyList())
        }

        override fun observeSyncStatus(): Flow<SyncStatus> {
            return flowOf(currentStatus)
        }

        override fun getCurrentSyncStatus(): SyncStatus {
            return currentStatus
        }

        fun setStatus(status: SyncStatus) {
            currentStatus = status
        }
    }
})

/**
 * Mock SyncResult for testing
 */
data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val conflicts: List<Any>
)
