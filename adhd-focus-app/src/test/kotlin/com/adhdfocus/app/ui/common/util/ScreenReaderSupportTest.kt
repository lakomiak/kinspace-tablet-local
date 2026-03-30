package com.adhdfocus.app.ui.common.util

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for screen reader support and semantic descriptions.
 */
class ScreenReaderSupportTest {

    @Test
    fun testTaskDescriptionIncomplete() {
        val task = Task(
            id = "1",
            title = "Buy groceries",
            status = TaskStatus.INCOMPLETE,
            estimatedDurationMinutes = 30,
            householdId = "household1",
            todoGroup = "Shopping"
        )
        
        val description = ScreenReaderSupport.getTaskDescription(task)
        assertEquals("Buy groceries, incomplete, estimated 30 minutes", description)
    }

    @Test
    fun testTaskDescriptionInProgress() {
        val task = Task(
            id = "2",
            title = "Do homework",
            status = TaskStatus.IN_PROGRESS,
            estimatedDurationMinutes = 60,
            householdId = "household1",
            todoGroup = "School"
        )
        
        val description = ScreenReaderSupport.getTaskDescription(task)
        assertEquals("Do homework, in progress, estimated 60 minutes", description)
    }

    @Test
    fun testTaskDescriptionCompleted() {
        val task = Task(
            id = "3",
            title = "Clean room",
            status = TaskStatus.COMPLETED,
            estimatedDurationMinutes = 45,
            householdId = "household1",
            todoGroup = "Chores"
        )
        
        val description = ScreenReaderSupport.getTaskDescription(task)
        assertEquals("Clean room, completed, estimated 45 minutes", description)
    }

    @Test
    fun testTaskDescriptionNoEstimatedDuration() {
        val task = Task(
            id = "4",
            title = "Quick task",
            status = TaskStatus.INCOMPLETE,
            estimatedDurationMinutes = 0,
            householdId = "household1",
            todoGroup = "Misc"
        )
        
        val description = ScreenReaderSupport.getTaskDescription(task)
        assertEquals("Quick task, incomplete", description)
    }

    @Test
    fun testStatusDescription() {
        assertEquals("Task is incomplete", ScreenReaderSupport.getStatusDescription(TaskStatus.INCOMPLETE))
        assertEquals("Task is in progress", ScreenReaderSupport.getStatusDescription(TaskStatus.IN_PROGRESS))
        assertEquals("Task is completed", ScreenReaderSupport.getStatusDescription(TaskStatus.COMPLETED))
    }

    @Test
    fun testCompletionDescription() {
        assertEquals("0 of 5 tasks complete, 0 percent", ScreenReaderSupport.getCompletionDescription(0, 5))
        assertEquals("2 of 5 tasks complete, 40 percent", ScreenReaderSupport.getCompletionDescription(2, 5))
        assertEquals("5 of 5 tasks complete, 100 percent", ScreenReaderSupport.getCompletionDescription(5, 5))
    }

    @Test
    fun testCompletionDescriptionZeroTotal() {
        assertEquals("0 of 0 tasks complete, 0 percent", ScreenReaderSupport.getCompletionDescription(0, 0))
    }

    @Test
    fun testStreakDescription() {
        assertEquals("No current streak", ScreenReaderSupport.getStreakDescription(0))
        assertEquals("1 day streak", ScreenReaderSupport.getStreakDescription(1))
        assertEquals("7 day streak", ScreenReaderSupport.getStreakDescription(7))
        assertEquals("30 day streak", ScreenReaderSupport.getStreakDescription(30))
    }

    @Test
    fun testTimerDescription() {
        assertEquals("30 seconds remaining", ScreenReaderSupport.getTimerDescription(30))
        assertEquals("1 minutes 30 seconds remaining", ScreenReaderSupport.getTimerDescription(90))
        assertEquals("5 minutes 0 seconds remaining", ScreenReaderSupport.getTimerDescription(300))
        assertEquals("10 minutes 45 seconds remaining", ScreenReaderSupport.getTimerDescription(645))
    }

    @Test
    fun testBadgeDescriptionEarned() {
        val description = ScreenReaderSupport.getBadgeDescription("First Task", true)
        assertEquals("First Task badge earned", description)
    }

    @Test
    fun testBadgeDescriptionLocked() {
        val description = ScreenReaderSupport.getBadgeDescription("Week Warrior", false)
        assertEquals("Week Warrior badge locked", description)
    }

    @Test
    fun testEfficiencyDescriptionFaster() {
        val description = ScreenReaderSupport.getEfficiencyDescription(120.0)
        assertEquals("Completed 20% faster than estimated", description)
    }

    @Test
    fun testEfficiencyDescriptionSlower() {
        val description = ScreenReaderSupport.getEfficiencyDescription(80.0)
        assertEquals("Completed 20% slower than estimated", description)
    }

    @Test
    fun testEfficiencyDescriptionOnTime() {
        val description = ScreenReaderSupport.getEfficiencyDescription(100.0)
        assertEquals("Completed at estimated time", description)
    }

    @Test
    fun testSyncStatusDescriptionSyncing() {
        val description = ScreenReaderSupport.getSyncStatusDescription(isSyncing = true, isOnline = true)
        assertEquals("Syncing with cloud", description)
    }

    @Test
    fun testSyncStatusDescriptionSynced() {
        val description = ScreenReaderSupport.getSyncStatusDescription(isSyncing = false, isOnline = true)
        assertEquals("Synced", description)
    }

    @Test
    fun testSyncStatusDescriptionOffline() {
        val description = ScreenReaderSupport.getSyncStatusDescription(isSyncing = false, isOnline = false)
        assertEquals("Offline, changes will sync when online", description)
    }

    @Test
    fun testDescriptionsAreNotEmpty() {
        // Verify all descriptions are non-empty
        val task = Task(
            id = "1",
            title = "Test",
            status = TaskStatus.INCOMPLETE,
            estimatedDurationMinutes = 30,
            householdId = "household1",
            todoGroup = "Test"
        )
        
        assertTrue(ScreenReaderSupport.getTaskDescription(task).isNotEmpty())
        assertTrue(ScreenReaderSupport.getStatusDescription(TaskStatus.INCOMPLETE).isNotEmpty())
        assertTrue(ScreenReaderSupport.getCompletionDescription(1, 5).isNotEmpty())
        assertTrue(ScreenReaderSupport.getStreakDescription(5).isNotEmpty())
        assertTrue(ScreenReaderSupport.getTimerDescription(60).isNotEmpty())
        assertTrue(ScreenReaderSupport.getBadgeDescription("Badge", true).isNotEmpty())
        assertTrue(ScreenReaderSupport.getEfficiencyDescription(100.0).isNotEmpty())
        assertTrue(ScreenReaderSupport.getSyncStatusDescription(false, true).isNotEmpty())
    }
}
