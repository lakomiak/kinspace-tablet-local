package com.adhdfocus.app.ui.common.component

import com.adhdfocus.app.domain.affirmation.AffirmationEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Unit Tests for AffirmationDisplay Component
 *
 * Tests verify:
 * - AffirmationEvent creation and properties
 * - Message content validation
 * - Timestamp handling
 * - Task ID tracking
 * - Streak count tracking
 *
 * Correctness Properties:
 * - Property 18: Affirmation on Task Completion - Affirmation events are created correctly
 * - Property 20: Affirmation Display Duration - Affirmation events have valid timestamps
 */
class AffirmationDisplayUnitTest : FunSpec({

    // ============ Task Complete Affirmation Tests ============

    test("TaskComplete affirmation has required fields") {
        val message = "Great job!"
        val taskId = "task-1"
        val timestamp = System.currentTimeMillis()

        val affirmation = AffirmationEvent.TaskComplete(
            message = message,
            taskId = taskId,
            timestamp = timestamp
        )

        affirmation.message shouldBe message
        affirmation.taskId shouldBe taskId
        affirmation.timestamp shouldBe timestamp
    }

    test("TaskComplete affirmation with default streak count") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.streakCount shouldBe 0
    }

    test("TaskComplete affirmation with custom streak count") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis(),
            streakCount = 5
        )

        affirmation.streakCount shouldBe 5
    }

    test("TaskComplete affirmation message is non-empty") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { message ->
            val affirmation = AffirmationEvent.TaskComplete(
                message = message,
                taskId = "task-1",
                timestamp = System.currentTimeMillis()
            )

            affirmation.message.isNotEmpty() shouldBe true
        }
    }

    test("TaskComplete affirmation task ID is non-empty") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50)
        ) { taskId ->
            val affirmation = AffirmationEvent.TaskComplete(
                message = "Great job!",
                taskId = taskId,
                timestamp = System.currentTimeMillis()
            )

            affirmation.taskId.isNotEmpty() shouldBe true
        }
    }

    test("TaskComplete affirmation timestamp is valid") {
        val beforeCreation = System.currentTimeMillis()
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )
        val afterCreation = System.currentTimeMillis()

        affirmation.timestamp shouldBe >= beforeCreation
        affirmation.timestamp shouldBe <= afterCreation + 100
    }

    test("TaskComplete affirmation with various streak counts") {
        checkAll(
            Arb.int(min = 0, max = 365)
        ) { streakCount ->
            val affirmation = AffirmationEvent.TaskComplete(
                message = "Great job!",
                taskId = "task-1",
                timestamp = System.currentTimeMillis(),
                streakCount = streakCount
            )

            affirmation.streakCount shouldBe streakCount
        }
    }

    // ============ Day Complete Affirmation Tests ============

    test("DayComplete affirmation has required fields") {
        val message = "Perfect day! You crushed it!"
        val timestamp = System.currentTimeMillis()

        val affirmation = AffirmationEvent.DayComplete(
            message = message,
            timestamp = timestamp
        )

        affirmation.message shouldBe message
        affirmation.timestamp shouldBe timestamp
    }

    test("DayComplete affirmation message is non-empty") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { message ->
            val affirmation = AffirmationEvent.DayComplete(
                message = message,
                timestamp = System.currentTimeMillis()
            )

            affirmation.message.isNotEmpty() shouldBe true
        }
    }

    test("DayComplete affirmation timestamp is valid") {
        val beforeCreation = System.currentTimeMillis()
        val affirmation = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = System.currentTimeMillis()
        )
        val afterCreation = System.currentTimeMillis()

        affirmation.timestamp shouldBe >= beforeCreation
        affirmation.timestamp shouldBe <= afterCreation + 100
    }

    // ============ Streak Milestone Affirmation Tests ============

    test("StreakMilestone affirmation has required fields") {
        val message = "3-Day Streak! Keep it going!"
        val streakCount = 3
        val timestamp = System.currentTimeMillis()

        val affirmation = AffirmationEvent.StreakMilestone(
            message = message,
            streakCount = streakCount,
            timestamp = timestamp
        )

        affirmation.message shouldBe message
        affirmation.streakCount shouldBe streakCount
        affirmation.timestamp shouldBe timestamp
    }

    test("StreakMilestone affirmation message is non-empty") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { message ->
            val affirmation = AffirmationEvent.StreakMilestone(
                message = message,
                streakCount = 3,
                timestamp = System.currentTimeMillis()
            )

            affirmation.message.isNotEmpty() shouldBe true
        }
    }

    test("StreakMilestone affirmation with various streak counts") {
        checkAll(
            Arb.int(min = 1, max = 365)
        ) { streakCount ->
            val affirmation = AffirmationEvent.StreakMilestone(
                message = "Streak milestone!",
                streakCount = streakCount,
                timestamp = System.currentTimeMillis()
            )

            affirmation.streakCount shouldBe streakCount
        }
    }

    test("StreakMilestone affirmation timestamp is valid") {
        val beforeCreation = System.currentTimeMillis()
        val affirmation = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )
        val afterCreation = System.currentTimeMillis()

        affirmation.timestamp shouldBe >= beforeCreation
        affirmation.timestamp shouldBe <= afterCreation + 100
    }

    // ============ Affirmation Type Tests ============

    test("TaskComplete is instance of AffirmationEvent") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation shouldBe is AffirmationEvent.TaskComplete
    }

    test("DayComplete is instance of AffirmationEvent") {
        val affirmation = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = System.currentTimeMillis()
        )

        affirmation shouldBe is AffirmationEvent.DayComplete
    }

    test("StreakMilestone is instance of AffirmationEvent") {
        val affirmation = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )

        affirmation shouldBe is AffirmationEvent.StreakMilestone
    }

    // ============ Message Content Tests ============

    test("Affirmation messages can contain special characters") {
        val specialMessage = "Great job! 🎉 You're awesome! ⭐"
        val affirmation = AffirmationEvent.TaskComplete(
            message = specialMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.message shouldBe specialMessage
    }

    test("Affirmation messages can contain unicode characters") {
        val unicodeMessage = "Excellent! 你好 مرحبا"
        val affirmation = AffirmationEvent.TaskComplete(
            message = unicodeMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.message shouldBe unicodeMessage
    }

    test("Affirmation messages can contain newlines") {
        val multilineMessage = "Great job!\nYou're awesome!"
        val affirmation = AffirmationEvent.TaskComplete(
            message = multilineMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.message shouldBe multilineMessage
    }

    test("Affirmation messages can be very long") {
        val longMessage = "You've done an amazing job completing this task! Keep up the fantastic work and maintain your momentum! This is a very long affirmation message to test that the component can handle extended text."
        val affirmation = AffirmationEvent.TaskComplete(
            message = longMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.message shouldBe longMessage
    }

    test("Affirmation messages can be very short") {
        val shortMessage = "Yes!"
        val affirmation = AffirmationEvent.TaskComplete(
            message = shortMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.message shouldBe shortMessage
    }

    // ============ Timestamp Tests ============

    test("Affirmation timestamp is non-zero") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.timestamp shouldNotBe 0L
    }

    test("Affirmation timestamp is positive") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.timestamp shouldBe > 0L
    }

    test("Affirmation timestamp is recent") {
        val currentTime = System.currentTimeMillis()
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = currentTime
        )

        affirmation.timestamp shouldBe <= currentTime + 100
    }

    // ============ Task ID Tests ============

    test("TaskComplete affirmation task ID is preserved") {
        val taskIds = listOf("task-1", "task-abc", "task-xyz", "123", "abc-def-ghi")

        for (taskId in taskIds) {
            val affirmation = AffirmationEvent.TaskComplete(
                message = "Great job!",
                taskId = taskId,
                timestamp = System.currentTimeMillis()
            )

            affirmation.taskId shouldBe taskId
        }
    }

    // ============ Streak Count Tests ============

    test("StreakMilestone affirmation streak count is preserved") {
        val streakCounts = listOf(1, 3, 7, 14, 30, 100, 365)

        for (streakCount in streakCounts) {
            val affirmation = AffirmationEvent.StreakMilestone(
                message = "Streak milestone!",
                streakCount = streakCount,
                timestamp = System.currentTimeMillis()
            )

            affirmation.streakCount shouldBe streakCount
        }
    }

    test("TaskComplete affirmation streak count is preserved") {
        val streakCounts = listOf(0, 1, 3, 7, 14, 30, 100)

        for (streakCount in streakCounts) {
            val affirmation = AffirmationEvent.TaskComplete(
                message = "Great job!",
                taskId = "task-1",
                timestamp = System.currentTimeMillis(),
                streakCount = streakCount
            )

            affirmation.streakCount shouldBe streakCount
        }
    }

    // ============ Equality Tests ============

    test("TaskComplete affirmations with same data are equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = timestamp
        )

        affirmation1 shouldBe affirmation2
    }

    test("DayComplete affirmations with same data are equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = timestamp
        )

        affirmation1 shouldBe affirmation2
    }

    test("StreakMilestone affirmations with same data are equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = timestamp
        )

        affirmation1 shouldBe affirmation2
    }

    test("TaskComplete affirmations with different messages are not equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.TaskComplete(
            message = "Awesome work!",
            taskId = "task-1",
            timestamp = timestamp
        )

        affirmation1 shouldNotBe affirmation2
    }

    test("TaskComplete affirmations with different task IDs are not equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-2",
            timestamp = timestamp
        )

        affirmation1 shouldNotBe affirmation2
    }

    test("StreakMilestone affirmations with different streak counts are not equal") {
        val timestamp = System.currentTimeMillis()
        val affirmation1 = AffirmationEvent.StreakMilestone(
            message = "Streak!",
            streakCount = 3,
            timestamp = timestamp
        )
        val affirmation2 = AffirmationEvent.StreakMilestone(
            message = "Streak!",
            streakCount = 7,
            timestamp = timestamp
        )

        affirmation1 shouldNotBe affirmation2
    }

    // ============ Data Class Tests ============

    test("TaskComplete affirmation can be copied with new message") {
        val original = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )
        val copied = original.copy(message = "Awesome work!")

        copied.message shouldBe "Awesome work!"
        copied.taskId shouldBe original.taskId
        copied.timestamp shouldBe original.timestamp
    }

    test("DayComplete affirmation can be copied with new message") {
        val original = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = System.currentTimeMillis()
        )
        val copied = original.copy(message = "Amazing day!")

        copied.message shouldBe "Amazing day!"
        copied.timestamp shouldBe original.timestamp
    }

    test("StreakMilestone affirmation can be copied with new streak count") {
        val original = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )
        val copied = original.copy(streakCount = 7)

        copied.streakCount shouldBe 7
        copied.message shouldBe original.message
        copied.timestamp shouldBe original.timestamp
    }

    // ============ String Representation Tests ============

    test("TaskComplete affirmation has string representation") {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        affirmation.toString().isNotEmpty() shouldBe true
    }

    test("DayComplete affirmation has string representation") {
        val affirmation = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = System.currentTimeMillis()
        )

        affirmation.toString().isNotEmpty() shouldBe true
    }

    test("StreakMilestone affirmation has string representation") {
        val affirmation = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )

        affirmation.toString().isNotEmpty() shouldBe true
    }
})
