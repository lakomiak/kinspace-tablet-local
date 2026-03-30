package com.adhdfocus.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animation customization for accessibility.
 * Provides configurable animation speeds and options to disable animations.
 */
object AnimationCustomization {

    /**
     * Animation speed preferences.
     */
    enum class AnimationSpeed(val durationMultiplier: Float) {
        DISABLED(0f),      // No animations
        SLOWEST(2.0f),     // 2x slower
        SLOWER(1.5f),      // 1.5x slower
        NORMAL(1.0f),      // Default speed
        FASTER(0.75f),     // 25% faster
        FASTEST(0.5f)      // 50% faster
    }

    /**
     * Animation intensity levels.
     */
    enum class AnimationIntensity(val value: Float) {
        NONE(0f),          // No animations
        MINIMAL(0.25f),    // Minimal animations
        REDUCED(0.5f),     // Reduced animations
        NORMAL(1.0f),      // Full animations
        ENHANCED(1.5f)     // Enhanced animations
    }

    /**
     * Creates an animation spec with customized duration.
     * @param speed The animation speed preference
     * @param baseDuration The base duration in milliseconds
     * @return AnimationSpec with customized duration
     */
    fun createAnimationSpec(
        speed: AnimationSpeed,
        baseDuration: Int = 300
    ): AnimationSpec<Float> {
        if (speed == AnimationSpeed.DISABLED) {
            return tween(durationMillis = 0)
        }
        
        val duration = (baseDuration * speed.durationMultiplier).toInt().coerceAtLeast(1)
        return tween(durationMillis = duration, easing = EaseInOutCubic)
    }

    /**
     * Gets the animation duration based on speed preference.
     * @param speed The animation speed preference
     * @param baseDuration The base duration in milliseconds
     * @return Adjusted duration in milliseconds
     */
    fun getAnimationDuration(
        speed: AnimationSpeed,
        baseDuration: Int = 300
    ): Int {
        if (speed == AnimationSpeed.DISABLED) return 0
        return (baseDuration * speed.durationMultiplier).toInt().coerceAtLeast(1)
    }

    /**
     * Gets the animation alpha (opacity) based on intensity.
     * @param intensity The animation intensity level
     * @return Alpha value between 0 and 1
     */
    fun getAnimationAlpha(intensity: AnimationIntensity): Float {
        return intensity.value.coerceIn(0f, 1f)
    }

    /**
     * Gets the animation scale based on intensity.
     * @param intensity The animation intensity level
     * @return Scale value
     */
    fun getAnimationScale(intensity: AnimationIntensity): Float {
        return when (intensity) {
            AnimationIntensity.NONE -> 1f
            AnimationIntensity.MINIMAL -> 0.95f
            AnimationIntensity.REDUCED -> 0.98f
            AnimationIntensity.NORMAL -> 1f
            AnimationIntensity.ENHANCED -> 1.1f
        }
    }

    /**
     * Checks if animations should be disabled.
     * @param speed The animation speed preference
     * @return True if animations are disabled
     */
    fun areAnimationsDisabled(speed: AnimationSpeed): Boolean {
        return speed == AnimationSpeed.DISABLED
    }

    /**
     * Checks if animations should be reduced.
     * @param speed The animation speed preference
     * @return True if animations are reduced
     */
    fun areAnimationsReduced(speed: AnimationSpeed): Boolean {
        return speed in listOf(AnimationSpeed.SLOWER, AnimationSpeed.SLOWEST)
    }

    /**
     * Gets the description of an animation speed.
     * @param speed The animation speed preference
     * @return Human-readable description
     */
    fun getSpeedDescription(speed: AnimationSpeed): String {
        return when (speed) {
            AnimationSpeed.DISABLED -> "Disabled"
            AnimationSpeed.SLOWEST -> "Slowest (2x slower)"
            AnimationSpeed.SLOWER -> "Slower (1.5x slower)"
            AnimationSpeed.NORMAL -> "Normal"
            AnimationSpeed.FASTER -> "Faster (25% faster)"
            AnimationSpeed.FASTEST -> "Fastest (50% faster)"
        }
    }

    /**
     * Gets the description of an animation intensity.
     * @param intensity The animation intensity level
     * @return Human-readable description
     */
    fun getIntensityDescription(intensity: AnimationIntensity): String {
        return when (intensity) {
            AnimationIntensity.NONE -> "None"
            AnimationIntensity.MINIMAL -> "Minimal"
            AnimationIntensity.REDUCED -> "Reduced"
            AnimationIntensity.NORMAL -> "Normal"
            AnimationIntensity.ENHANCED -> "Enhanced"
        }
    }

    /**
     * Standard animation durations for common UI elements.
     */
    object StandardDurations {
        const val QUICK = 150      // Quick feedback (button press)
        const val SHORT = 300      // Short animations (fade in/out)
        const val MEDIUM = 500     // Medium animations (slide in/out)
        const val LONG = 800       // Long animations (complex transitions)
        const val VERY_LONG = 1200 // Very long animations (page transitions)
    }

    /**
     * Standard animation delays for staggered effects.
     */
    object StandardDelays {
        const val NONE = 0
        const val MINIMAL = 50
        const val SHORT = 100
        const val MEDIUM = 200
        const val LONG = 300
    }

    /**
     * Calculates adjusted duration based on speed preference.
     * @param baseDuration The base duration in milliseconds
     * @param speed The animation speed preference
     * @return Adjusted duration in milliseconds
     */
    fun adjustDuration(baseDuration: Int, speed: AnimationSpeed): Int {
        return getAnimationDuration(speed, baseDuration)
    }

    /**
     * Calculates adjusted delay based on speed preference.
     * @param baseDelay The base delay in milliseconds
     * @param speed The animation speed preference
     * @return Adjusted delay in milliseconds
     */
    fun adjustDelay(baseDelay: Int, speed: AnimationSpeed): Int {
        if (speed == AnimationSpeed.DISABLED) return 0
        return (baseDelay * speed.durationMultiplier).toInt().coerceAtLeast(0)
    }

    /**
     * Gets the recommended animation duration for a given element type.
     * @param elementType The type of UI element
     * @param speed The animation speed preference
     * @return Recommended duration in milliseconds
     */
    fun getRecommendedDuration(elementType: String, speed: AnimationSpeed): Int {
        val baseDuration = when (elementType) {
            "button" -> StandardDurations.QUICK
            "fade" -> StandardDurations.SHORT
            "slide" -> StandardDurations.MEDIUM
            "transition" -> StandardDurations.LONG
            else -> StandardDurations.SHORT
        }
        return adjustDuration(baseDuration, speed)
    }
}
