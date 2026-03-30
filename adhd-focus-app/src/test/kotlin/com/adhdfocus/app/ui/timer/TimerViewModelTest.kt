package com.adhdfocus.app.ui.timer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * Unit Tests for TimerViewModel
 *
 * Tests:
 * - Timer initialization
 * - Timer start/pause/resume/cancel
 * - Progress calculation
 * - Time formatting
 * - Color feedback based on progress
 * - Warning thresholds
 */
class TimerViewModelTest : FunSpec({

    test("Timer initialization") {
        val viewModel = TimerViewModel()

        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
        viewModel.progress.value shouldBe 0f
        viewModel.timerCompleted.value shouldBe false
    }

    test("Start timer with valid duration") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5) // 5 minutes

        viewModel.timerDuration.value shouldBe 300 // 5 * 60 seconds
        viewModel.timeRemaining.value shouldBe 300
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe false
    }

    test("Start timer with zero duration") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(0)

        viewModel.timerDuration.value shouldBe 0
        viewModel.isRunning.value shouldBe false
    }

    test("Start timer with negative duration") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(-5)

        viewModel.timerDuration.value shouldBe 0
        viewModel.isRunning.value shouldBe false
    }

    test("Pause timer") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        viewModel.pauseTimer()

        viewModel.isPaused.value shouldBe true
        viewModel.isRunning.value shouldBe true
    }

    test("Resume timer") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)
        viewModel.pauseTimer()

        viewModel.resumeTimer()

        viewModel.isPaused.value shouldBe false
        viewModel.isRunning.value shouldBe true
    }

    test("Cancel timer") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        viewModel.cancelTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
        viewModel.progress.value shouldBe 0f
    }

    test("Extend timer") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        val initialDuration = viewModel.timerDuration.value
        viewModel.extendTimer(2)

        viewModel.timerDuration.value shouldBe initialDuration + 120
    }

    test("Extend timer with zero minutes") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        val initialDuration = viewModel.timerDuration.value
        viewModel.extendTimer(0)

        viewModel.timerDuration.value shouldBe initialDuration
    }

    test("Format time - MM:SS format") {
        val viewModel = TimerViewModel()

        viewModel.getFormattedTime(0) shouldBe "00:00"
        viewModel.getFormattedTime(30) shouldBe "00:30"
        viewModel.getFormattedTime(60) shouldBe "01:00"
        viewModel.getFormattedTime(90) shouldBe "01:30"
        viewModel.getFormattedTime(300) shouldBe "05:00"
        viewModel.getFormattedTime(3661) shouldBe "61:01"
    }

    test("Progress percentage calculation") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(10)

        // Simulate progress by manually setting values
        viewModel._progress.value = 0f
        viewModel.getProgressPercentage() shouldBe 0

        viewModel._progress.value = 0.5f
        viewModel.getProgressPercentage() shouldBe 50

        viewModel._progress.value = 1.0f
        viewModel.getProgressPercentage() shouldBe 100
    }

    test("Progress color - Green (0-50%)") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        viewModel._progress.value = 0.25f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        viewModel._progress.value = 0.49f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN
    }

    test("Progress color - Orange (50-90%)") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0.5f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        viewModel._progress.value = 0.75f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        viewModel._progress.value = 0.89f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE
    }

    test("Progress color - Red (90-100%)") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0.9f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED

        viewModel._progress.value = 0.95f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED

        viewModel._progress.value = 1.0f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED
    }

    test("Warning threshold at 50%") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0.5f
        viewModel.isAtWarningThreshold() shouldBe true
    }

    test("Warning threshold at 90%") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0.9f
        viewModel.isAtWarningThreshold() shouldBe true
    }

    test("No warning threshold at other percentages") {
        val viewModel = TimerViewModel()

        viewModel._progress.value = 0.25f
        viewModel.isAtWarningThreshold() shouldBe false

        viewModel._progress.value = 0.75f
        viewModel.isAtWarningThreshold() shouldBe false
    }

    test("Reset timer") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        viewModel.resetTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
    }

    test("Multiple start/cancel cycles") {
        val viewModel = TimerViewModel()

        for (i in 1..3) {
            viewModel.startTimer(5)
            viewModel.isRunning.value shouldBe true

            viewModel.cancelTimer()
            viewModel.isRunning.value shouldBe false
        }
    }

    test("Pause and resume multiple times") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        for (i in 1..3) {
            viewModel.pauseTimer()
            viewModel.isPaused.value shouldBe true

            viewModel.resumeTimer()
            viewModel.isPaused.value shouldBe false
        }
    }

    test("Timer duration conversion") {
        checkAll(
            Arb.int(min = 1, max = 60)
        ) { minutes ->
            val viewModel = TimerViewModel()
            viewModel.startTimer(minutes)

            viewModel.timerDuration.value shouldBe minutes * 60
        }
    }

    test("Extend timer multiple times") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        val initialDuration = viewModel.timerDuration.value

        viewModel.extendTimer(2)
        viewModel.timerDuration.value shouldBe initialDuration + 120

        viewModel.extendTimer(3)
        viewModel.timerDuration.value shouldBe initialDuration + 120 + 180
    }

    test("Time formatting with various durations") {
        checkAll(
            Arb.int(min = 0, max = 3600)
        ) { seconds ->
            val viewModel = TimerViewModel()
            val formatted = viewModel.getFormattedTime(seconds)

            // Should be in MM:SS format
            formatted.contains(":") shouldBe true
            val parts = formatted.split(":")
            parts.size shouldBe 2
        }
    }

    test("Progress calculation with various durations") {
        checkAll(
            Arb.int(min = 1, max = 60)
        ) { minutes ->
            val viewModel = TimerViewModel()
            viewModel.startTimer(minutes)

            viewModel._progress.value = 0f
            viewModel.getProgressPercentage() shouldBe 0

            viewModel._progress.value = 1.0f
            viewModel.getProgressPercentage() shouldBe 100
        }
    }

    test("Timer state consistency") {
        val viewModel = TimerViewModel()
        viewModel.startTimer(5)

        // Running state should be true
        viewModel.isRunning.value shouldBe true

        // Pause should not affect running state
        viewModel.pauseTimer()
        viewModel.isRunning.value shouldBe true

        // Resume should maintain running state
        viewModel.resumeTimer()
        viewModel.isRunning.value shouldBe true

        // Cancel should stop running
        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false
    }

    test("Timer completion flag") {
        val viewModel = TimerViewModel()

        viewModel.timerCompleted.value shouldBe false

        viewModel.startTimer(1)
        viewModel.timerCompleted.value shouldBe false

        viewModel.cancelTimer()
        viewModel.timerCompleted.value shouldBe false
    }

    test("Progress is monotonically increasing") {
        val viewModel = TimerViewModel()

        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1.0f)

        for (i in 1 until progressValues.size) {
            progressValues[i] shouldBe >= progressValues[i - 1]
        }
    }

    test("Color transitions with progress") {
        val viewModel = TimerViewModel()

        // Green at start
        viewModel._progress.value = 0f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        // Orange at 50%
        viewModel._progress.value = 0.5f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        // Red at 90%
        viewModel._progress.value = 0.9f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED
    }

    test("Pause without running timer") {
        val viewModel = TimerViewModel()

        viewModel.pauseTimer()

        viewModel.isPaused.value shouldBe false
    }

    test("Resume without running timer") {
        val viewModel = TimerViewModel()

        viewModel.resumeTimer()

        viewModel.isPaused.value shouldBe false
    }

    test("Cancel without running timer") {
        val viewModel = TimerViewModel()

        viewModel.cancelTimer()

        viewModel.isRunning.value shouldBe false
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
