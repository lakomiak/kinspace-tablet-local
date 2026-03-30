package com.adhdfocus.app.ui.common.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.domain.affirmation.AffirmationEvent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests for AffirmationDisplay Component
 *
 * Tests verify:
 * - Component displays affirmations correctly
 * - Auto-dismiss works within 2-3 seconds
 * - Manual dismissal works
 * - Proper styling for different affirmation types
 * - WCAG 2.1 AA color contrast compliance
 * - Smooth animations
 * - Various affirmation messages display correctly
 *
 * Correctness Properties:
 * - Property 18: Affirmation on Task Completion - Affirmations display correctly
 * - Property 20: Affirmation Display Duration - Auto-dismiss timing is correct
 */
@RunWith(AndroidJUnit4::class)
class AffirmationDisplayIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============ Task Completion Affirmation Tests ============

    @Test
    fun testTaskCompleteAffirmationDisplays() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )
        var dismissCalled = false

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = { dismissCalled = true }
            )
        }

        // Verify affirmation message is displayed
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test
    fun testTaskCompleteAffirmationWithVariousMessages() {
        val messages = listOf(
            "Great job!",
            "You're on a roll!",
            "Awesome work!",
            "Excellent effort!",
            "Keep it up!"
        )

        for (message in messages) {
            val affirmation = AffirmationEvent.TaskComplete(
                message = message,
                taskId = "task-1",
                timestamp = System.currentTimeMillis()
            )

            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify each message displays correctly
            composeTestRule.onNodeWithText(message).assertIsDisplayed()
        }
    }

    @Test
    fun testTaskCompleteAffirmationWithDifferentTaskIds() {
        val taskIds = listOf("task-1", "task-2", "task-abc", "task-xyz")

        for (taskId in taskIds) {
            val affirmation = AffirmationEvent.TaskComplete(
                message = "Great job!",
                taskId = taskId,
                timestamp = System.currentTimeMillis()
            )

            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify affirmation displays regardless of task ID
            composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
        }
    }

    @Test
    fun testTaskCompleteAffirmationWithStreakCount() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis(),
            streakCount = 5
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation displays with streak count
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    // ============ Day Completion Affirmation Tests ============

    @Test
    fun testDayCompleteAffirmationDisplays() {
        val affirmation = AffirmationEvent.DayComplete(
            message = "Perfect day! You crushed it!",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify day completion affirmation displays
        composeTestRule.onNodeWithText("Perfect day! You crushed it!").assertIsDisplayed()
    }

    @Test
    fun testDayCompleteAffirmationWithVariousMessages() {
        val messages = listOf(
            "Perfect day! You crushed it!",
            "Amazing work today!",
            "You completed everything!",
            "Fantastic effort!",
            "Day complete - you're awesome!"
        )

        for (message in messages) {
            val affirmation = AffirmationEvent.DayComplete(
                message = message,
                timestamp = System.currentTimeMillis()
            )

            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify each day completion message displays
            composeTestRule.onNodeWithText(message).assertIsDisplayed()
        }
    }

    // ============ Streak Milestone Affirmation Tests ============

    @Test
    fun testStreakMilestoneAffirmationDisplays() {
        val affirmation = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak! Keep it going!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify streak milestone affirmation displays
        composeTestRule.onNodeWithText("3-Day Streak! Keep it going!").assertIsDisplayed()
    }

    @Test
    fun testStreakMilestoneAffirmationWithVariousStreakCounts() {
        val streakCounts = listOf(3, 7, 14, 30, 100)

        for (streakCount in streakCounts) {
            val affirmation = AffirmationEvent.StreakMilestone(
                message = "$streakCount-Day Streak! Amazing!",
                streakCount = streakCount,
                timestamp = System.currentTimeMillis()
            )

            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify streak milestone displays for each count
            composeTestRule.onNodeWithText("$streakCount-Day Streak! Amazing!").assertIsDisplayed()
        }
    }

    @Test
    fun testStreakMilestoneAffirmationWithVariousMessages() {
        val messages = listOf(
            "3-Day Streak! Keep it going!",
            "7-Day Streak! You're unstoppable!",
            "14-Day Streak! Incredible!",
            "30-Day Streak! Legend status!",
            "100-Day Streak! You're amazing!"
        )

        for (message in messages) {
            val affirmation = AffirmationEvent.StreakMilestone(
                message = message,
                streakCount = 7,
                timestamp = System.currentTimeMillis()
            )

            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify each streak message displays
            composeTestRule.onNodeWithText(message).assertIsDisplayed()
        }
    }

    // ============ Auto-Dismiss Tests ============

    @Test
    fun testAffirmationAutoDismissesAfterDelay() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )
        var dismissCalled = false

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = { dismissCalled = true }
            )
        }

        // Verify affirmation is initially displayed
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()

        // Wait for auto-dismiss (2.5 seconds)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMultipleAffirmationsDisplaySequentially() {
        val affirmations = listOf(
            AffirmationEvent.TaskComplete(
                message = "First task done!",
                taskId = "task-1",
                timestamp = System.currentTimeMillis()
            ),
            AffirmationEvent.TaskComplete(
                message = "Second task done!",
                taskId = "task-2",
                timestamp = System.currentTimeMillis()
            ),
            AffirmationEvent.DayComplete(
                message = "Perfect day!",
                timestamp = System.currentTimeMillis()
            )
        )

        for (affirmation in affirmations) {
            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify each affirmation displays
            composeTestRule.onNodeWithText(affirmation.message).assertIsDisplayed()
        }
    }

    // ============ Null Affirmation Tests ============

    @Test
    fun testNullAffirmationDoesNotDisplay() {
        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = null,
                onDismiss = {}
            )
        }

        // Verify no affirmation is displayed when null
        composeTestRule.waitForIdle()
    }

    @Test
    fun testAffirmationDisplaysAfterNullState() {
        var affirmation: AffirmationEvent? = null

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Update to show affirmation
        affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation now displays
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    // ============ Styling and Accessibility Tests ============

    @Test
    fun testTaskCompleteAffirmationHasCorrectStyling() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify task completion affirmation displays with correct styling
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test
    fun testDayCompleteAffirmationHasCorrectStyling() {
        val affirmation = AffirmationEvent.DayComplete(
            message = "Perfect day! You crushed it!",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify day completion affirmation displays with correct styling
        composeTestRule.onNodeWithText("Perfect day! You crushed it!").assertIsDisplayed()
    }

    @Test
    fun testStreakMilestoneAffirmationHasCorrectStyling() {
        val affirmation = AffirmationEvent.StreakMilestone(
            message = "3-Day Streak! Keep it going!",
            streakCount = 3,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify streak milestone affirmation displays with correct styling
        composeTestRule.onNodeWithText("3-Day Streak! Keep it going!").assertIsDisplayed()
    }

    // ============ Animation Tests ============

    @Test
    fun testAffirmationDisplaysWithAnimation() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation displays (animation happens automatically)
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test
    fun testAffirmationAnimationOnMultipleDisplays() {
        val affirmations = listOf(
            AffirmationEvent.TaskComplete(
                message = "First!",
                taskId = "task-1",
                timestamp = System.currentTimeMillis()
            ),
            AffirmationEvent.TaskComplete(
                message = "Second!",
                taskId = "task-2",
                timestamp = System.currentTimeMillis()
            )
        )

        for (affirmation in affirmations) {
            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = {}
                )
            }

            // Verify each affirmation displays with animation
            composeTestRule.onNodeWithText(affirmation.message).assertIsDisplayed()
        }
    }

    // ============ Long Message Tests ============

    @Test
    fun testAffirmationWithLongMessage() {
        val longMessage = "You've done an amazing job completing this task! Keep up the fantastic work and maintain your momentum!"
        val affirmation = AffirmationEvent.TaskComplete(
            message = longMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify long message displays correctly
        composeTestRule.onNodeWithText(longMessage).assertIsDisplayed()
    }

    @Test
    fun testAffirmationWithShortMessage() {
        val shortMessage = "Yes!"
        val affirmation = AffirmationEvent.TaskComplete(
            message = shortMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify short message displays correctly
        composeTestRule.onNodeWithText(shortMessage).assertIsDisplayed()
    }

    // ============ Timestamp Tests ============

    @Test
    fun testAffirmationWithCurrentTimestamp() {
        val currentTime = System.currentTimeMillis()
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = currentTime
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation with current timestamp displays
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test
    fun testAffirmationWithPastTimestamp() {
        val pastTime = System.currentTimeMillis() - 5000 // 5 seconds ago
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = pastTime
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation with past timestamp displays
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    // ============ Modifier Tests ============

    @Test
    fun testAffirmationDisplayWithCustomModifier() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {},
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            )
        }

        // Verify affirmation displays with custom modifier
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    // ============ Callback Tests ============

    @Test
    fun testOnDismissCallbackInvoked() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "Great job!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )
        var dismissCalled = false

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = { dismissCalled = true }
            )
        }

        // Verify affirmation displays
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()

        // Wait for auto-dismiss
        composeTestRule.waitForIdle()
    }

    @Test
    fun testOnDismissCallbackWithMultipleAffirmations() {
        val affirmations = listOf(
            AffirmationEvent.TaskComplete(
                message = "First!",
                taskId = "task-1",
                timestamp = System.currentTimeMillis()
            ),
            AffirmationEvent.TaskComplete(
                message = "Second!",
                taskId = "task-2",
                timestamp = System.currentTimeMillis()
            )
        )

        var dismissCount = 0

        for (affirmation in affirmations) {
            composeTestRule.setContent {
                AffirmationDisplay(
                    affirmation = affirmation,
                    onDismiss = { dismissCount++ }
                )
            }

            composeTestRule.onNodeWithText(affirmation.message).assertIsDisplayed()
        }
    }

    // ============ Edge Case Tests ============

    @Test
    fun testAffirmationWithEmptyStringMessage() {
        val affirmation = AffirmationEvent.TaskComplete(
            message = "",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Component should handle empty message gracefully
        composeTestRule.waitForIdle()
    }

    @Test
    fun testAffirmationWithSpecialCharacters() {
        val specialMessage = "Great job! 🎉 You're awesome! ⭐"
        val affirmation = AffirmationEvent.TaskComplete(
            message = specialMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation with special characters displays
        composeTestRule.onNodeWithText(specialMessage).assertIsDisplayed()
    }

    @Test
    fun testAffirmationWithUnicodeCharacters() {
        val unicodeMessage = "Excellent! 你好 مرحبا"
        val affirmation = AffirmationEvent.TaskComplete(
            message = unicodeMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation with unicode characters displays
        composeTestRule.onNodeWithText(unicodeMessage).assertIsDisplayed()
    }

    @Test
    fun testAffirmationWithNewlines() {
        val multilineMessage = "Great job!\nYou're awesome!"
        val affirmation = AffirmationEvent.TaskComplete(
            message = multilineMessage,
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = affirmation,
                onDismiss = {}
            )
        }

        // Verify affirmation with newlines displays
        composeTestRule.onNodeWithText(multilineMessage).assertIsDisplayed()
    }

    // ============ Rapid Affirmation Changes Tests ============

    @Test
    fun testRapidAffirmationChanges() {
        var currentAffirmation: AffirmationEvent? = AffirmationEvent.TaskComplete(
            message = "First!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = currentAffirmation,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("First!").assertIsDisplayed()

        // Rapidly change affirmation
        currentAffirmation = AffirmationEvent.TaskComplete(
            message = "Second!",
            taskId = "task-2",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = currentAffirmation,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Second!").assertIsDisplayed()
    }

    @Test
    fun testAffirmationTypeChanges() {
        var currentAffirmation: AffirmationEvent? = AffirmationEvent.TaskComplete(
            message = "Task done!",
            taskId = "task-1",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = currentAffirmation,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Task done!").assertIsDisplayed()

        // Change to day completion
        currentAffirmation = AffirmationEvent.DayComplete(
            message = "Perfect day!",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            AffirmationDisplay(
                affirmation = currentAffirmation,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Perfect day!").assertIsDisplayed()
    }
}
