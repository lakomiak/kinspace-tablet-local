package com.adhdfocus.app.ui.theme

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for animation customization support.
 */
class AnimationCustomizationTest {

    @Test
    fun testAnimationSpeedMultipliers() {
        assertEquals(0f, AnimationCustomization.AnimationSpeed.DISABLED.durationMultiplier)
        assertEquals(2.0f, AnimationCustomization.AnimationSpeed.SLOWEST.durationMultiplier)
        assertEquals(1.5f, AnimationCustomization.AnimationSpeed.SLOWER.durationMultiplier)
        assertEquals(1.0f, AnimationCustomization.AnimationSpeed.NORMAL.durationMultiplier)
        assertEquals(0.75f, AnimationCustomization.AnimationSpeed.FASTER.durationMultiplier)
        assertEquals(0.5f, AnimationCustomization.AnimationSpeed.FASTEST.durationMultiplier)
    }

    @Test
    fun testAnimationIntensityValues() {
        assertEquals(0f, AnimationCustomization.AnimationIntensity.NONE.value)
        assertEquals(0.25f, AnimationCustomization.AnimationIntensity.MINIMAL.value)
        assertEquals(0.5f, AnimationCustomization.AnimationIntensity.REDUCED.value)
        assertEquals(1.0f, AnimationCustomization.AnimationIntensity.NORMAL.value)
        assertEquals(1.5f, AnimationCustomization.AnimationIntensity.ENHANCED.value)
    }

    @Test
    fun testGetAnimationDurationDisabled() {
        val duration = AnimationCustomization.getAnimationDuration(
            AnimationCustomization.AnimationSpeed.DISABLED,
            300
        )
        assertEquals(0, duration)
    }

    @Test
    fun testGetAnimationDurationNormal() {
        val duration = AnimationCustomization.getAnimationDuration(
            AnimationCustomization.AnimationSpeed.NORMAL,
            300
        )
        assertEquals(300, duration)
    }

    @Test
    fun testGetAnimationDurationSlower() {
        val duration = AnimationCustomization.getAnimationDuration(
            AnimationCustomization.AnimationSpeed.SLOWER,
            300
        )
        assertEquals(450, duration)  // 300 * 1.5
    }

    @Test
    fun testGetAnimationDurationFaster() {
        val duration = AnimationCustomization.getAnimationDuration(
            AnimationCustomization.AnimationSpeed.FASTER,
            300
        )
        assertEquals(225, duration)  // 300 * 0.75
    }

    @Test
    fun testGetAnimationAlpha() {
        assertEquals(0f, AnimationCustomization.getAnimationAlpha(AnimationCustomization.AnimationIntensity.NONE))
        assertEquals(0.25f, AnimationCustomization.getAnimationAlpha(AnimationCustomization.AnimationIntensity.MINIMAL))
        assertEquals(0.5f, AnimationCustomization.getAnimationAlpha(AnimationCustomization.AnimationIntensity.REDUCED))
        assertEquals(1.0f, AnimationCustomization.getAnimationAlpha(AnimationCustomization.AnimationIntensity.NORMAL))
        assertEquals(1.0f, AnimationCustomization.getAnimationAlpha(AnimationCustomization.AnimationIntensity.ENHANCED))  // Clamped to 1.0
    }

    @Test
    fun testGetAnimationScale() {
        assertEquals(1f, AnimationCustomization.getAnimationScale(AnimationCustomization.AnimationIntensity.NONE))
        assertEquals(0.95f, AnimationCustomization.getAnimationScale(AnimationCustomization.AnimationIntensity.MINIMAL))
        assertEquals(0.98f, AnimationCustomization.getAnimationScale(AnimationCustomization.AnimationIntensity.REDUCED))
        assertEquals(1f, AnimationCustomization.getAnimationScale(AnimationCustomization.AnimationIntensity.NORMAL))
        assertEquals(1.1f, AnimationCustomization.getAnimationScale(AnimationCustomization.AnimationIntensity.ENHANCED))
    }

    @Test
    fun testAreAnimationsDisabled() {
        assertTrue(AnimationCustomization.areAnimationsDisabled(AnimationCustomization.AnimationSpeed.DISABLED))
        assertFalse(AnimationCustomization.areAnimationsDisabled(AnimationCustomization.AnimationSpeed.NORMAL))
        assertFalse(AnimationCustomization.areAnimationsDisabled(AnimationCustomization.AnimationSpeed.SLOWER))
    }

    @Test
    fun testAreAnimationsReduced() {
        assertTrue(AnimationCustomization.areAnimationsReduced(AnimationCustomization.AnimationSpeed.SLOWER))
        assertTrue(AnimationCustomization.areAnimationsReduced(AnimationCustomization.AnimationSpeed.SLOWEST))
        assertFalse(AnimationCustomization.areAnimationsReduced(AnimationCustomization.AnimationSpeed.NORMAL))
        assertFalse(AnimationCustomization.areAnimationsReduced(AnimationCustomization.AnimationSpeed.FASTER))
    }

    @Test
    fun testGetSpeedDescription() {
        assertEquals("Disabled", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.DISABLED))
        assertEquals("Slowest (2x slower)", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.SLOWEST))
        assertEquals("Slower (1.5x slower)", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.SLOWER))
        assertEquals("Normal", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.NORMAL))
        assertEquals("Faster (25% faster)", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.FASTER))
        assertEquals("Fastest (50% faster)", AnimationCustomization.getSpeedDescription(AnimationCustomization.AnimationSpeed.FASTEST))
    }

    @Test
    fun testGetIntensityDescription() {
        assertEquals("None", AnimationCustomization.getIntensityDescription(AnimationCustomization.AnimationIntensity.NONE))
        assertEquals("Minimal", AnimationCustomization.getIntensityDescription(AnimationCustomization.AnimationIntensity.MINIMAL))
        assertEquals("Reduced", AnimationCustomization.getIntensityDescription(AnimationCustomization.AnimationIntensity.REDUCED))
        assertEquals("Normal", AnimationCustomization.getIntensityDescription(AnimationCustomization.AnimationIntensity.NORMAL))
        assertEquals("Enhanced", AnimationCustomization.getIntensityDescription(AnimationCustomization.AnimationIntensity.ENHANCED))
    }

    @Test
    fun testStandardDurations() {
        assertEquals(150, AnimationCustomization.StandardDurations.QUICK)
        assertEquals(300, AnimationCustomization.StandardDurations.SHORT)
        assertEquals(500, AnimationCustomization.StandardDurations.MEDIUM)
        assertEquals(800, AnimationCustomization.StandardDurations.LONG)
        assertEquals(1200, AnimationCustomization.StandardDurations.VERY_LONG)
    }

    @Test
    fun testStandardDelays() {
        assertEquals(0, AnimationCustomization.StandardDelays.NONE)
        assertEquals(50, AnimationCustomization.StandardDelays.MINIMAL)
        assertEquals(100, AnimationCustomization.StandardDelays.SHORT)
        assertEquals(200, AnimationCustomization.StandardDelays.MEDIUM)
        assertEquals(300, AnimationCustomization.StandardDelays.LONG)
    }

    @Test
    fun testAdjustDuration() {
        val baseDuration = 300
        assertEquals(0, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.DISABLED))
        assertEquals(600, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.SLOWEST))
        assertEquals(450, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.SLOWER))
        assertEquals(300, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.NORMAL))
        assertEquals(225, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.FASTER))
        assertEquals(150, AnimationCustomization.adjustDuration(baseDuration, AnimationCustomization.AnimationSpeed.FASTEST))
    }

    @Test
    fun testAdjustDelay() {
        val baseDelay = 100
        assertEquals(0, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.DISABLED))
        assertEquals(200, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.SLOWEST))
        assertEquals(150, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.SLOWER))
        assertEquals(100, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.NORMAL))
        assertEquals(75, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.FASTER))
        assertEquals(50, AnimationCustomization.adjustDelay(baseDelay, AnimationCustomization.AnimationSpeed.FASTEST))
    }

    @Test
    fun testGetRecommendedDuration() {
        val speed = AnimationCustomization.AnimationSpeed.NORMAL
        
        assertEquals(150, AnimationCustomization.getRecommendedDuration("button", speed))
        assertEquals(300, AnimationCustomization.getRecommendedDuration("fade", speed))
        assertEquals(500, AnimationCustomization.getRecommendedDuration("slide", speed))
        assertEquals(800, AnimationCustomization.getRecommendedDuration("transition", speed))
        assertEquals(300, AnimationCustomization.getRecommendedDuration("unknown", speed))
    }

    @Test
    fun testGetRecommendedDurationWithSlowerSpeed() {
        val speed = AnimationCustomization.AnimationSpeed.SLOWER
        
        // Button: 150 * 1.5 = 225
        assertEquals(225, AnimationCustomization.getRecommendedDuration("button", speed))
        // Fade: 300 * 1.5 = 450
        assertEquals(450, AnimationCustomization.getRecommendedDuration("fade", speed))
    }

    @Test
    fun testGetRecommendedDurationWithDisabledAnimations() {
        val speed = AnimationCustomization.AnimationSpeed.DISABLED
        
        assertEquals(0, AnimationCustomization.getRecommendedDuration("button", speed))
        assertEquals(0, AnimationCustomization.getRecommendedDuration("fade", speed))
        assertEquals(0, AnimationCustomization.getRecommendedDuration("slide", speed))
    }

    @Test
    fun testCreateAnimationSpec() {
        val spec = AnimationCustomization.createAnimationSpec(
            AnimationCustomization.AnimationSpeed.NORMAL,
            300
        )
        assertFalse(spec == null)
    }

    @Test
    fun testCreateAnimationSpecDisabled() {
        val spec = AnimationCustomization.createAnimationSpec(
            AnimationCustomization.AnimationSpeed.DISABLED,
            300
        )
        assertFalse(spec == null)
    }
}
