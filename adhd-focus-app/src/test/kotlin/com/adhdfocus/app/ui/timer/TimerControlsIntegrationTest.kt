package com.adhdfocus.app.ui.timer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Integration Tests for Timer Controls
 *
 * Tests the complete workflow of:
 * 1. Starting a timer
 * 2. Pausing the timer
 * 3. Resuming the timer
 * 4. Canceling the timer
 * 5. Extending the timer
 * 6. Handling state transitions
 */
class TimerControlsIntegrationTest : FunSpec({

    test("Integration 1: Start and pause timer") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe false

        viewModel.pauseTimer()
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe true
    }

    test("Integration 2: Start, pause, and resume timer") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()
        viewModel.isPaused.value shouldBe true

        viewModel.resumeTimer()
        viewModel.isPaused.value shouldBe false
        viewModel.isRunning.value shouldBe true
    }

    test("Integration 3: Start and cancel timer") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.isRunning.value shouldBe true

        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
    }

    test("Integration 4: Multiple pause/resume cycles") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(10)

        for (i in 1..5) {
            viewModel.pauseTimer()
            viewModel.isPaused.value shouldBe true

            viewModel.resumeTimer()
            viewModel.isPaused.value shouldBe false
        }

        viewModel.isRunning.value shouldBe true
    }

    test("Integration 5: Extend timer while running") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        val initialDuration = viewModel.timerDuration.value

        viewModel.extendTimer(2)
        viewModel.timerDuration.value shouldBe initialDuration + 120
    }

    test("Integration 6: Extend timer while paused") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()

        val initialDuration = viewModel.timerDuration.value
        viewModel.extendTimer(3)

        viewModel.timerDuration.value shouldBe initialDuration + 180
        viewModel.isPaused.value shouldBe true
    }

    test("Integration 7: Cancel after pause") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()
        viewModel.cancelTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
    }

    test("Integration 8: Reset timer") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()
        viewModel.resetTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
    }

    test("Integration 9: Format time display") {
        val viewModel = TimerViewModel()

        viewModel.getFormattedTime(0) shouldBe "00:00"
        viewModel.getFormattedTime(30) shouldBe "00:30"
        viewModel.getFormattedTime(60) shouldBe "01:00"
        viewModel.getFormattedTime(300) shouldBe "05:00"
        viewModel.getFormattedTime(3661) shouldBe "61:01"
    }

    test("Integration 10: Progress calculation") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(10)
        viewModel._progress.value = 0f
        viewModel.getProgressPercentage() shouldBe 0

        viewModel._progress.value = 0.5f
        viewModel.getProgressPercentage() shouldBe 50

        viewModel._progress.value = 1.0f
        viewModel.getProgressPercentage() shouldBe 100
    }

    test("Integration 11: Color feedback transitions") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(10)

        // Green at start
        viewModel._progress.value = 0.25f
        viewModel.getProgressColor() shouldBe TimerProgressColor.GREEN

        // Orange at 50%
        viewModel._progress.value = 0.5f
        viewModel.getProgressColor() shouldBe TimerProgressColor.ORANGE

        // Red at 90%
        viewModel._progress.value = 0.9f
        viewModel.getProgressColor() shouldBe TimerProgressColor.RED
    }

    test("Integration 12: Warning threshold detection") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(10)

        viewModel._progress.value = 0.5f
        viewModel.isAtWarningThreshold() shouldBe true

        viewModel._progress.value = 0.9f
        viewModel.isAtWarningThreshold() shouldBe true

        viewModel._progress.value = 0.75f
        viewModel.isAtWarningThreshold() shouldBe false
    }

    test("Integration 13: Complete timer workflow") {
        val viewModel = TimerViewModel()

        // Start timer
        viewModel.startTimer(5)
        viewModel.isRunning.value shouldBe true
        viewModel.timerDuration.value shouldBe 300

        // Pause timer
        viewModel.pauseTimer()
        viewModel.isPaused.value shouldBe true

        // Resume timer
        viewModel.resumeTimer()
        viewModel.isPaused.value shouldBe false

        // Extend timer
        viewModel.extendTimer(2)
        viewModel.timerDuration.value shouldBe 420

        // Cancel timer
        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false
    }

    test("Integration 14: State consistency throughout lifecycle") {
        val viewModel = TimerViewModel()

        // Initial state
        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false

        // After start
        viewModel.startTimer(5)
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe false

        // After pause
        viewModel.pauseTimer()
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe true

        // After resume
        viewModel.resumeTimer()
        viewModel.isRunning.value shouldBe true
        viewModel.isPaused.value shouldBe false

        // After cancel
        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
    }

    test("Integration 15: Multiple timer sessions") {
        val viewModel = TimerViewModel()

        // First session
        viewModel.startTimer(5)
        viewModel.isRunning.value shouldBe true
        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false

        // Second session
        viewModel.startTimer(10)
        viewModel.isRunning.value shouldBe true
        viewModel.pauseTimer()
        viewModel.isPaused.value shouldBe true
        viewModel.resumeTimer()
        viewModel.isPaused.value shouldBe false
        viewModel.cancelTimer()
        viewModel.isRunning.value shouldBe false
    }

    test("Integration 16: Extend timer multiple times") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        val initialDuration = viewModel.timerDuration.value

        viewModel.extendTimer(2)
        viewModel.timerDuration.value shouldBe initialDuration + 120

        viewModel.extendTimer(3)
        viewModel.timerDuration.value shouldBe initialDuration + 120 + 180

        viewModel.extendTimer(1)
        viewModel.timerDuration.value shouldBe initialDuration + 120 + 180 + 60
    }

    test("Integration 17: Pause/resume preserves duration") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        val duration = viewModel.timerDuration.value

        viewModel.pauseTimer()
        viewModel.timerDuration.value shouldBe duration

        viewModel.resumeTimer()
        viewModel.timerDuration.value shouldBe duration
    }

    test("Integration 18: Cancel clears all state") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()
        viewModel.extendTimer(2)

        viewModel.cancelTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
        viewModel.timeRemaining.value shouldBe 0
        viewModel.progress.value shouldBe 0f
    }

    test("Integration 19: Reset timer clears state") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)
        viewModel.pauseTimer()

        viewModel.resetTimer()

        viewModel.isRunning.value shouldBe false
        viewModel.isPaused.value shouldBe false
        viewModel.timerDuration.value shouldBe 0
    }

    test("Integration 20: Control operations are idempotent") {
        val viewModel = TimerViewModel()

        viewModel.startTimer(5)

        // Multiple pauses should be safe
        viewModel.pauseTimer()
        viewModel.pauseTimer()
        viewModel.isPaused.value shouldBe true

        // Multiple resumes should be safe
        viewModel.resumeTimer()
        viewModel.resumeTimer()
        viewModel.isPaused.value shouldBe false

        // Multiple cancels should be safe
        viewModel.cancelTimer()
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

// Import for MutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
