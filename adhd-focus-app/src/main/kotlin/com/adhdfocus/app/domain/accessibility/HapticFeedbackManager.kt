package com.adhdfocus.app.domain.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Manages haptic feedback for accessibility and user feedback.
 * Provides haptic patterns for different user interactions.
 */
interface HapticFeedbackManager {
    /**
     * Provides light haptic feedback (e.g., button press).
     */
    fun provideLightFeedback()

    /**
     * Provides medium haptic feedback (e.g., task completion).
     */
    fun provideMediumFeedback()

    /**
     * Provides strong haptic feedback (e.g., day completion).
     */
    fun provideStrongFeedback()

    /**
     * Provides success pattern (e.g., task marked complete).
     */
    fun provideSuccessFeedback()

    /**
     * Provides warning pattern (e.g., timer warning).
     */
    fun provideWarningFeedback()

    /**
     * Provides error pattern (e.g., sync failure).
     */
    fun provideErrorFeedback()

    /**
     * Provides custom haptic pattern.
     * @param pattern Array of durations in milliseconds (off, on, off, on, ...)
     */
    fun provideCustomFeedback(pattern: LongArray)

    /**
     * Checks if device supports haptic feedback.
     * @return True if device has vibrator
     */
    fun isHapticSupported(): Boolean
}

/**
 * Implementation of HapticFeedbackManager using Android Vibrator API.
 */
class HapticFeedbackManagerImpl(private val context: Context) : HapticFeedbackManager {

    private val vibrator: Vibrator? = getVibrator()

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun provideLightFeedback() {
        vibrate(10)  // 10ms light vibration
    }

    override fun provideMediumFeedback() {
        vibrate(20)  // 20ms medium vibration
    }

    override fun provideStrongFeedback() {
        vibrate(40)  // 40ms strong vibration
    }

    override fun provideSuccessFeedback() {
        // Success pattern: short, short, long
        val pattern = longArrayOf(0, 20, 50, 20, 50, 100)
        provideCustomFeedback(pattern)
    }

    override fun provideWarningFeedback() {
        // Warning pattern: medium, pause, medium
        val pattern = longArrayOf(0, 30, 100, 30)
        provideCustomFeedback(pattern)
    }

    override fun provideErrorFeedback() {
        // Error pattern: long, pause, long, pause, long
        val pattern = longArrayOf(0, 50, 100, 50, 100, 50)
        provideCustomFeedback(pattern)
    }

    override fun provideCustomFeedback(pattern: LongArray) {
        if (!isHapticSupported()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Silently fail if vibration is not available
        }
    }

    override fun isHapticSupported(): Boolean {
        return vibrator?.hasVibrator() == true
    }

    private fun vibrate(duration: Long) {
        if (!isHapticSupported()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Silently fail if vibration is not available
        }
    }
}

/**
 * Haptic feedback patterns for different scenarios.
 */
object HapticPatterns {
    // Light feedback patterns
    val LIGHT_TAP = longArrayOf(0, 10)
    val LIGHT_DOUBLE_TAP = longArrayOf(0, 10, 50, 10)

    // Medium feedback patterns
    val MEDIUM_PRESS = longArrayOf(0, 20)
    val MEDIUM_DOUBLE_PRESS = longArrayOf(0, 20, 50, 20)

    // Strong feedback patterns
    val STRONG_PRESS = longArrayOf(0, 40)
    val STRONG_DOUBLE_PRESS = longArrayOf(0, 40, 100, 40)

    // Success patterns
    val SUCCESS_SHORT = longArrayOf(0, 20, 50, 20, 50, 100)
    val SUCCESS_LONG = longArrayOf(0, 30, 100, 30, 100, 30, 100, 200)

    // Warning patterns
    val WARNING_SINGLE = longArrayOf(0, 30, 100, 30)
    val WARNING_DOUBLE = longArrayOf(0, 30, 100, 30, 100, 30)

    // Error patterns
    val ERROR_SINGLE = longArrayOf(0, 50, 100, 50, 100, 50)
    val ERROR_DOUBLE = longArrayOf(0, 50, 100, 50, 100, 50, 100, 50, 100, 50)

    // Notification patterns
    val NOTIFICATION_ALERT = longArrayOf(0, 20, 50, 20, 50, 20)
    val NOTIFICATION_REMINDER = longArrayOf(0, 30, 100, 30)
}
