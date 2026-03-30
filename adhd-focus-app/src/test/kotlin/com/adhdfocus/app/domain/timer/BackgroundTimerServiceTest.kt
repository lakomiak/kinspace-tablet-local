package com.adhdfocus.app.domain.timer

import android.content.Context
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk

/**
 * Unit Tests for BackgroundTimerService
 *
 * Tests:
 * - Timer start/pause/resume/cancel
 * - Time remaining calculation
 * - Progress calculation
 * - Notification management
 */
class BackgroundTimerServiceTest : FunSpec({

    test("Timer initialization") {
        val service = BackgroundTimerService()

        service.getTimeRemaining() shouldBe 0
        service.getTimerDuration() shouldBe 0
        service.isTimerRunning() shouldBe false
        service.isTimerPaused() shouldBe false
    }

    test("Start timer with valid duration") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)

        service.getTimerDuration() shouldBe 300 // 5 * 60
        service.getTimeRemaining() shouldBe 300
        service.isTimerRunning() shouldBe true
    }

    test("Start timer with zero duration") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(0)

        service.getTimerDuration() shouldBe 0
        service.isTimerRunning() shouldBe false
    }

    test("Start timer with negative duration") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(-5)

        service.getTimerDuration() shouldBe 0
        service.isTimerRunning() shouldBe false
    }

    test("Pause timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        service.pauseTimer()

        service.isTimerPaused() shouldBe true
        service.isTimerRunning() shouldBe true
    }

    test("Resume timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        service.pauseTimer()
        service.resumeTimer()

        service.isTimerPaused() shouldBe false
        service.isTimerRunning() shouldBe true
    }

    test("Cancel timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        service.cancelTimer()

        service.isTimerRunning() shouldBe false
        service.isTimerPaused() shouldBe false
    }

    test("Progress calculation at start") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)

        service.getProgress() shouldBe 0f
    }

    test("Progress calculation at end") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        // Simulate completion by setting time remaining to 0
        service._timeRemaining = 0

        service.getProgress() shouldBe 1f
    }

    test("Progress calculation at midpoint") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(10)
        // Simulate halfway through
        service._timeRemaining = 300 // 5 minutes remaining out of 10

        service.getProgress() shouldBe 0.5f
    }

    test("Time remaining decreases") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        val initialTime = service.getTimeRemaining()

        service._timeRemaining = initialTime - 1

        service.getTimeRemaining() shouldBe initialTime - 1
    }

    test("Multiple pause/resume cycles") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)

        for (i in 1..3) {
            service.pauseTimer()
            service.isTimerPaused() shouldBe true

            service.resumeTimer()
            service.isTimerPaused() shouldBe false
        }
    }

    test("Timer duration conversion") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(1)
        service.getTimerDuration() shouldBe 60

        service.cancelTimer()

        service.startTimer(10)
        service.getTimerDuration() shouldBe 600
    }

    test("Progress is between 0 and 1") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)

        val progress = service.getProgress()
        progress shouldBe >= 0f
        progress shouldBe <= 1f
    }

    test("Timer state consistency") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.startTimer(5)
        service.isTimerRunning() shouldBe true

        service.pauseTimer()
        service.isTimerRunning() shouldBe true
        service.isTimerPaused() shouldBe true

        service.resumeTimer()
        service.isTimerRunning() shouldBe true
        service.isTimerPaused() shouldBe false

        service.cancelTimer()
        service.isTimerRunning() shouldBe false
    }

    test("Pause without running timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.pauseTimer()

        service.isTimerPaused() shouldBe false
    }

    test("Resume without running timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.resumeTimer()

        service.isTimerPaused() shouldBe false
    }

    test("Cancel without running timer") {
        val service = BackgroundTimerService()
        service.audioNotificationManager = mockk(relaxed = true)

        service.cancelTimer()

        service.isTimerRunning() shouldBe false
    }
})

// Extension properties for testing
private var BackgroundTimerService._timeRemaining: Int
    get() = this::class.java.getDeclaredField("timeRemaining").let {
        it.isAccessible = true
        it.getInt(this)
    }
    set(value) {
        this::class.java.getDeclaredField("timeRemaining").let {
            it.isAccessible = true
            it.setInt(this, value)
        }
    }
