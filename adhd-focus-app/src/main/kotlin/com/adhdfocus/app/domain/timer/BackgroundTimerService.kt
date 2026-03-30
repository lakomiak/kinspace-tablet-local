package com.adhdfocus.app.domain.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BackgroundTimerService manages timer operations in the background.
 *
 * Provides:
 * - Background timer that continues even when app is backgrounded
 * - System notifications for timer status
 * - Foreground service for reliability
 * - Timer completion callbacks
 */
@AndroidEntryPoint
class BackgroundTimerService : Service() {

    @Inject
    lateinit var audioNotificationManager: AudioNotificationManager

    private val binder = LocalBinder()
    private var timerJob: Job? = null
    private var timeRemaining = 0
    private var timerDuration = 0
    private var isRunning = false
    private var isPaused = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var timerCompletionCallback: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundTimerService = this@BackgroundTimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * Starts the background timer.
     *
     * @param durationMinutes Duration in minutes
     * @param onCompletion Callback when timer completes
     */
    fun startTimer(durationMinutes: Int, onCompletion: (() -> Unit)? = null) {
        if (durationMinutes <= 0) return

        timerDuration = durationMinutes * 60
        timeRemaining = timerDuration
        isRunning = true
        isPaused = false
        timerCompletionCallback = onCompletion

        startForegroundService()
        startCountdown()
    }

    /**
     * Pauses the timer.
     */
    fun pauseTimer() {
        if (isRunning && !isPaused) {
            isPaused = true
            updateNotification()
        }
    }

    /**
     * Resumes the timer.
     */
    fun resumeTimer() {
        if (isRunning && isPaused) {
            isPaused = false
            updateNotification()
        }
    }

    /**
     * Cancels the timer.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        isRunning = false
        isPaused = false
        timerDuration = 0
        timeRemaining = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Gets the time remaining in seconds.
     */
    fun getTimeRemaining(): Int = timeRemaining

    /**
     * Gets the timer duration in seconds.
     */
    fun getTimerDuration(): Int = timerDuration

    /**
     * Gets the progress (0.0 to 1.0).
     */
    fun getProgress(): Float {
        return if (timerDuration > 0) {
            (timerDuration - timeRemaining).toFloat() / timerDuration
        } else {
            0f
        }
    }

    /**
     * Checks if timer is running.
     */
    fun isTimerRunning(): Boolean = isRunning

    /**
     * Checks if timer is paused.
     */
    fun isTimerPaused(): Boolean = isPaused

    /**
     * Starts the countdown.
     */
    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (timeRemaining > 0 && isRunning) {
                if (!isPaused) {
                    delay(1000)
                    timeRemaining = (timeRemaining - 1).coerceAtLeast(0)
                    updateNotification()

                    if (timeRemaining == 0) {
                        completeTimer()
                    }
                } else {
                    delay(100)
                }
            }
        }
    }

    /**
     * Completes the timer.
     */
    private fun completeTimer() {
        isRunning = false
        isPaused = false
        audioNotificationManager.playTimerCompletionSound()
        timerCompletionCallback?.invoke()
        showCompletionNotification()
    }

    /**
     * Starts the foreground service.
     */
    private fun startForegroundService() {
        createNotificationChannel()
        val notification = buildTimerNotification()
        startForeground(TIMER_NOTIFICATION_ID, notification)
    }

    /**
     * Creates the notification channel.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TIMER_CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the timer notification.
     */
    private fun buildTimerNotification(): Notification {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setProgress(timerDuration, timerDuration - timeRemaining, false)
            .setOngoing(true)
            .build()
    }

    /**
     * Updates the notification.
     */
    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(TIMER_NOTIFICATION_ID, buildTimerNotification())
    }

    /**
     * Shows the completion notification.
     */
    private fun showCompletionNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle("Timer Complete")
            .setContentText("Your timer has finished!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        manager.notify(TIMER_COMPLETION_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        audioNotificationManager.release()
    }

    companion object {
        private const val TIMER_NOTIFICATION_ID = 1001
        private const val TIMER_COMPLETION_NOTIFICATION_ID = 1002
        private const val TIMER_CHANNEL_ID = "timer_channel"
    }
}
