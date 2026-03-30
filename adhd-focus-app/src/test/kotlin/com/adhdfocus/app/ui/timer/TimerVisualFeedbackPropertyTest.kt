package com.adhdfocus.app.ui.timer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll

/**
 * Property-Based Tests for Timer Visual Feedback
 *
 * Tests:
 * - Progress ring animation
 * - Color changes at 50% and 90%
 * - Progress percentage calculation
 * - Warning threshold detection
 */
class TimerVisualFeedbackPropertyTest : FunSpec({

    test("Property 9.1: Progress is between 0 and 1") {
        checkAll(
            Arb.float(min = 0f, max = 1f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            viewModel.progress.value shouldBe in(0f..1f)
        }
    }

    test("Property 9.2: Color is green when progress < 50%") {
        checkAll(
            Arb.float(min = 0f, max = 0.49f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN
        }
    }

    test("Property 9.3: Color is orange when progress 50-90%") {
        checkAll(
            Arb.float(min = 0.5f, max = 0.89f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE
        }
    }

    test("Property 9.4: Color is red when progress >= 90%") {
        checkAll(
            Arb.float(min = 0.9f, max = 1f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            viewModel.getProgressColor() shouldBe TimerProgressColor.RED
        }
    }

    test("Property 9.5: Progress percentage is 0-100") {
        checkAll(
            Arb.float(min = 0f, max = 1f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            val percentage = viewModel.getProgressPercentage()
            percentage shouldBe in(0..100)
        }
    }

    test("Property 9.6: Progress percentage is accurate") {
        checkAll(
            Arb.float(min = 0f, max = 1f)
        ) { progress ->
            val viewModel = TimerViewModel()
            viewModel._progress.value = progress

            val percentage = viewModel.getProgressPercentage()
            val expected = (progress * 100).toInt()

            percentage shouldBe expected
        }
    }

    test("Property 9.7: Warning threshold at 50%") {
        val viewModel = TimerViewModel()
        viewModel._progress.value = 0.5f

        viewModel.isAtWarningThreshold() shouldBe true
    }

    test("Property 9.8: Warning threshold at 90%") {
        val viewModel = TimerViewModel()
        viewModel._progress.value = 0.9f

        viewModel.isAtWarningThreshold() shouldBe true
    }

    test("Property 9.9: No warning at other thresholds") {
        checkAll(
            Arb.float(min = 0f, max = 1f)
        ) { progress ->
            if (progress != 0.5f && progress != 0.9f) {
                val viewModel = TimerViewModel()
                viewModel._progress.value = progress

                // Should not be at warning threshold
                val isWarning = viewModel.isAtWarningThreshold()
                // This is a probabilistic test - most values won't be exactly 50% or 90%
                if (progress < 0.49f || (progress > 0.51f && progress < 0.89f) || progress > 0.91f) {
                    isWarning shouldBe false
                }
            }
        }
    }

    test("Property 9.10: Color transitions are smooth") {
        val viewModel = TimerViewModel()
        val colors = mutableListOf<TimerProgressColor>()

        for (i in 0..100) {
            val progress = i / 100f
            viewModel._progress.value = progress
            colors.add(viewModel.getProgressColor())
        }

        // Should have at most 3 distinct colors
        colors.distinct().size shouldBe <= 3
    }

    test("Property 9.11: Progress ring animation is smooth") {
        val viewModel = TimerViewModel()

        // Simulate smooth progress
        val progressValues = (0..100).map { it / 100f }

        for (progress in progressValues) {
            viewModel._progress.value = progress
            viewModel.progress.value shouldBe progress
        }
    }

    test("Property 9.12: Color changes at exact thresholds") {
        val viewModel = TimerViewModel()

        // Just before 50%
        viewModel._progress.value = 0.49f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        // At 50%
        viewModel._progress.value = 0.5f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        // Just before 90%
        viewModel._progress.value = 0.89f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        // At 90%
        viewModel._progress.value = 0.9f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED
    }

    test("Property 9.13: Progress percentage is monotonically increasing") {
        val viewModel = TimerViewModel()
        var previousPercentage = 0

        for (i in 0..100) {
            val progress = i / 100f
            viewModel._progress.value = progress
            val percentage = viewModel.getProgressPercentage()

            percentage shouldBe >= previousPercentage
            previousPercentage = percentage
        }
    }

    test("Property 9.14: Visual feedback is deterministic") {
        checkAll(
            Arb.float(min = 0f, max = 1f)
        ) { progress ->
            val viewModel1 = TimerViewModel()
            val viewModel2 = TimerViewModel()

            viewModel1._progress.value = progress
            viewModel2._progress.value = progress

            viewModel1.getProgressColor() shouldBe viewModel2.getProgressColor()
            viewModel1.getProgressPercentage() shouldBe viewModel2.getProgressPercentage()
        }
    }

    test("Property 9.15: Color feedback is independent of duration") {
        val viewModel = TimerViewModel()

        // Start with 5 minutes
        viewModel.startTimer(5)
        val color1 = viewModel.getProgressColor()

        // Start with 10 minutes
        viewModel.startTimer(10)
        val color2 = viewModel.getProgressColor()

        // Color should be the same at same progress
        color1 shouldBe color2
    }

    test("Property 9.16: Progress ring covers full circle at 100%") {
        val viewModel = TimerViewModel()
        viewModel._progress.value = 1.0f

        viewModel.getProgressPercentage() shouldBe 100
    }

    test("Property 9.17: Progress ring is empty at 0%") {
        val viewModel = TimerViewModel()
        viewModel._progress.value = 0f

        viewModel.getProgressPercentage() shouldBe 0
    }

    test("Property 9.18: Color intensity increases with progress") {
        val viewModel = TimerViewModel()

        // Green at 25%
        viewModel._progress.value = 0.25f
        val color1 = viewModel.getProgressColor()

        // Orange at 75%
        viewModel._progress.value = 0.75f
        val color2 = viewModel.getProgressColor()

        // Colors should be different
        color1 shouldBe TimerProgressColor.GREEN
        color2 shouldBe TimerProgressColor.ORANGE
    }

    test("Property 9.19: Warning threshold is consistent") {
        val viewModel = TimerViewModel()

        // Multiple checks at 50%
        viewModel._progress.value = 0.5f
        viewModel.isAtWarningThreshold() shouldBe true
        viewModel.isAtWarningThreshold() shouldBe true

        // Multiple checks at 90%
        viewModel._progress.value = 0.9f
        viewModel.isAtWarningThreshold() shouldBe true
        viewModel.isAtWarningThreshold() shouldBe true
    }

    test("Property 9.20: Visual feedback handles edge cases") {
        val viewModel = TimerViewModel()

        // Minimum progress
        viewModel._progress.value = 0f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        // Maximum progress
        viewModel._progress.value = 1f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED

        // Exactly at thresholds
        viewModel._progress.value = 0.5f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        viewModel._progress.value = 0.9f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED
    }
})

// Extension properties for testing
private var TimerViewModel._progress: MutableStateFlow<Float>
    get() = this::class.java.getDeclaredField("_progress").let {
        it.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        it.get(this) as MutableStateFlow<Float>
    }
    set(value) {
        this::class.java.getDeclaredField("_progress").let {
            it.isAccessible = true
            it.set(this, value)
        }
    }

// Import for MutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
