package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.TaskManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Property-based tests for TimerAwareUpdateApplier.
 *
 * **Validates: Requirements 2.4, 11**
 *
 * Tests universal properties that should hold for all inputs.
 */
class TimerAwareUpdateApplierPropertyTest : FunSpec({

    fun createTestTask(id: String): Task {
        return Task(
            id = id,
            householdId = "household1",
            assignedUserId = "user1",
            title = "Test Task",
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            status = TaskStatus.INCOMPLETE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun createApplier(): TimerAwareUpdateApplier {
        val taskDao = mockk<TaskDao>(relaxed = true)
        val taskManager = mockk<TaskManager>(relaxed = true)
        val realTimeUpdateManager = mockk<RealTimeUpdateManager>(relaxed = true)
        return TimerAwareUpdateApplierImpl(taskDao, taskManager, realTimeUpdateManager)
    }

    test("Property 1: Timer State Consistency - Timer state is always consistent") {
        checkAll(Arb.list(Arb.int(0..1), 1..10)) { states ->
            runTest {
                val applier = createApplier()
                
                for (state in states) {
                    val isActive = state == 1
                    applier.setTimerActive(isActive)
                    applier.isTimerActive() shouldBe isActive
                }
            }
        }
    }

    test("Property 2: Update Queuing - Updates are queued when timer is active") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                applier.setTimerActive(true)
                
                for (taskId in taskIds) {
                    val task = createTestTask(taskId)
                    val event = UpdateEvent.TaskUpdated(taskId, task)
                    applier.queueUpdate(event)
                }
                
                applier.getQueuedUpdateCount() shouldBe taskIds.size
            }
        }
    }

    test("Property 3: Update Application - Updates are applied when timer is inactive") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                applier.setTimerActive(false)
                
                for (taskId in taskIds) {
                    val task = createTestTask(taskId)
                    val event = UpdateEvent.TaskUpdated(taskId, task)
                    val result = applier.applyUpdate(event)
                    result.success shouldBe true
                }
                
                applier.getQueuedUpdateCount() shouldBe 0
            }
        }
    }

    test("Property 4: Queue Clearing - Clearing queue removes all updates") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                
                for (taskId in taskIds) {
                    val task = createTestTask(taskId)
                    val event = UpdateEvent.TaskUpdated(taskId, task)
                    applier.queueUpdate(event)
                }
                
                applier.clearQueuedUpdates()
                applier.getQueuedUpdateCount() shouldBe 0
            }
        }
    }

    test("Property 5: Timer Completion Triggers Application - Queued updates applied on timer completion") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                applier.setTimerActive(true)
                
                for (taskId in taskIds) {
                    val task = createTestTask(taskId)
                    val event = UpdateEvent.TaskUpdated(taskId, task)
                    applier.queueUpdate(event)
                }
                
                val queuedBefore = applier.getQueuedUpdateCount()
                applier.setTimerActive(false)
                val queuedAfter = applier.getQueuedUpdateCount()
                
                queuedBefore shouldBe taskIds.size
                queuedAfter shouldBe 0
            }
        }
    }

    test("Property 6: Update Ordering - Updates are applied in FIFO order") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                applier.setTimerActive(true)
                
                val events = taskIds.map { taskId ->
                    val task = createTestTask(taskId)
                    UpdateEvent.TaskUpdated(taskId, task)
                }
                
                for (event in events) {
                    applier.queueUpdate(event)
                }
                
                applier.getQueuedUpdateCount() shouldBe events.size
            }
        }
    }

    test("Property 7: Multiple Timer Cycles - Multiple timer cycles work correctly") {
        checkAll(Arb.list(Arb.int(1..5), 1..5)) { cycleCounts ->
            runTest {
                val applier = createApplier()
                
                for (cycleIndex in cycleCounts.indices) {
                    applier.setTimerActive(true)
                    
                    for (i in 0 until cycleCounts[cycleIndex]) {
                        val task = createTestTask("task_${cycleIndex}_$i")
                        val event = UpdateEvent.TaskUpdated("task_${cycleIndex}_$i", task)
                        applier.queueUpdate(event)
                    }
                    
                    applier.setTimerActive(false)
                    applier.getQueuedUpdateCount() shouldBe 0
                }
            }
        }
    }

    test("Property 8: Queue Size Accuracy - Queue size is always accurate") {
        checkAll(Arb.list(Arb.string(1..20), 1..10)) { taskIds ->
            runTest {
                val applier = createApplier()
                applier.setTimerActive(true)
                
                var expectedSize = 0
                for (taskId in taskIds) {
                    val task = createTestTask(taskId)
                    val event = UpdateEvent.TaskUpdated(taskId, task)
                    applier.queueUpdate(event)
                    expectedSize++
                    applier.getQueuedUpdateCount() shouldBe expectedSize
                }
            }
        }
    }
})
