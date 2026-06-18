package com.adhdfocus.app.domain.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.adhdfocus.app.data.model.TimerAlarmSound
import javax.inject.Inject

/**
 * AudioNotificationManager handles audio notifications for timer completion.
 *
 * Provides:
 * - Timer completion chime
 * - Customizable notification sounds
 * - Volume control
 * - Sound resource management
 */
class AudioNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    /**
     * Plays the timer completion notification sound.
     */
    fun playTimerCompletionSound(
        alarmSound: TimerAlarmSound = TimerAlarmSound.ALARM,
        durationMs: Long? = null
    ) {
        clearStopTimer()
        when (alarmSound) {
            TimerAlarmSound.ALARM -> playRingtone(RingtoneManager.TYPE_ALARM, durationMs)
            TimerAlarmSound.NOTIFICATION -> playRingtone(RingtoneManager.TYPE_NOTIFICATION, durationMs)
            TimerAlarmSound.BEEP -> playBeep()
            TimerAlarmSound.MULTI_BEEP -> playMultipleBeeps(3)
            TimerAlarmSound.SILENT -> Unit
        }
    }

    fun playCategoryReminderSound(
        alarmSound: TimerAlarmSound = TimerAlarmSound.ALARM,
        durationMs: Long = 15_000L
    ) {
        clearStopTimer()
        when (alarmSound) {
            TimerAlarmSound.ALARM -> playLoopingRingtone(RingtoneManager.TYPE_ALARM, durationMs)
            TimerAlarmSound.NOTIFICATION -> playLoopingRingtone(RingtoneManager.TYPE_NOTIFICATION, durationMs)
            TimerAlarmSound.BEEP -> playLoopingRingtone(RingtoneManager.TYPE_NOTIFICATION, durationMs)
            TimerAlarmSound.MULTI_BEEP -> playLoopingRingtone(RingtoneManager.TYPE_NOTIFICATION, durationMs)
            TimerAlarmSound.SILENT -> Unit
        }
    }

    /**
     * Plays a custom sound file.
     *
     * @param soundResId Resource ID of the sound file
     */
    fun playCustomSound(soundResId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Stops the currently playing sound.
     */
    fun stopSound() {
        try {
            clearStopTimer()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Releases all audio resources.
     */
    fun release() {
        try {
            clearStopTimer()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Plays a short beep sound.
     */
    fun playBeep() {
        try {
            val ringtone = RingtoneManager.getRingtone(
                context,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone.play()
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun playRingtone(type: Int, durationMs: Long? = null) {
        try {
            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(type)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, ringtoneUri)
            mediaPlayer?.isLooping = false
            mediaPlayer?.start()
            durationMs?.let {
                val stopTask = Runnable { stopSound() }
                stopRunnable = stopTask
                mainHandler.postDelayed(stopTask, it.coerceAtLeast(750L))
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun playLoopingRingtone(type: Int, durationMs: Long) {
        try {
            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(type)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, ringtoneUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            val stopTask = Runnable { stopSound() }
            stopRunnable = stopTask
            mainHandler.postDelayed(stopTask, durationMs.coerceAtLeast(1_000L))
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Plays multiple beeps for emphasis.
     *
     * @param count Number of beeps
     * @param delayMs Delay between beeps in milliseconds
     */
    fun playMultipleBeeps(count: Int, delayMs: Long = 200) {
        Thread {
            repeat(count) {
                playBeep()
                Thread.sleep(delayMs)
            }
        }.start()
    }

    private fun clearStopTimer() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
    }
}

/**
 * Audio notification types.
 */
enum class AudioNotificationType {
    TIMER_COMPLETION,
    WARNING,
    CUSTOM
}
