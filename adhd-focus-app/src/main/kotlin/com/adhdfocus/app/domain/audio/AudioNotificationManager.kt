package com.adhdfocus.app.domain.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Plays the timer completion notification sound.
     */
    fun playTimerCompletionSound() {
        try {
            // Get the default notification sound
            val notificationUri: Uri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, notificationUri)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Handle error silently
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
}

/**
 * Audio notification types.
 */
enum class AudioNotificationType {
    TIMER_COMPLETION,
    WARNING,
    CUSTOM
}
