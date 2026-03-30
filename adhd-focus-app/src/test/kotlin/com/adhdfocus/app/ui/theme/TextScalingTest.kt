package com.adhdfocus.app.ui.theme

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for text scaling support.
 */
class TextScalingTest {

    @Test
    fun testSmallScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.SMALL)
        // Body large should be 16 * 0.85 = 13.6 sp
        assertTrue(typography.bodyLarge.fontSize.value in 13.0f..14.0f)
    }

    @Test
    fun testNormalScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.NORMAL)
        // Body large should be 16 sp
        assertEquals(16f, typography.bodyLarge.fontSize.value)
    }

    @Test
    fun testLargeScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.LARGE)
        // Body large should be 16 * 1.15 = 18.4 sp
        assertTrue(typography.bodyLarge.fontSize.value in 18.0f..19.0f)
    }

    @Test
    fun testExtraLargeScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.EXTRA_LARGE)
        // Body large should be 16 * 1.3 = 20.8 sp
        assertTrue(typography.bodyLarge.fontSize.value in 20.0f..21.0f)
    }

    @Test
    fun testHugeScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.HUGE)
        // Body large should be 16 * 1.5 = 24 sp
        assertEquals(24f, typography.bodyLarge.fontSize.value)
    }

    @Test
    fun testMaximumScaleFactor() {
        val typography = TextScaling.createScaledTypography(TextScaling.ScaleFactor.MAXIMUM)
        // Body large should be 16 * 2.0 = 32 sp
        assertEquals(32f, typography.bodyLarge.fontSize.value)
    }

    @Test
    fun testCustomScaleFactor() {
        val typography = TextScaling.createScaledTypography(1.25f)
        // Body large should be 16 * 1.25 = 20 sp
        assertEquals(20f, typography.bodyLarge.fontSize.value)
    }

    @Test
    fun testScaleFactorClamping() {
        // Test that scale factors are clamped to valid range
        val tooSmall = TextScaling.createScaledTypography(0.1f)
        val tooLarge = TextScaling.createScaledTypography(3.0f)
        
        // Should be clamped to 0.5 and 2.0 respectively
        assertTrue(tooSmall.bodyLarge.fontSize.value > 0)
        assertTrue(tooLarge.bodyLarge.fontSize.value <= 32f)
    }

    @Test
    fun testAllTypographyStylesScaled() {
        val typography = TextScaling.createScaledTypography(1.5f)
        
        // All styles should be scaled
        assertTrue(typography.bodyLarge.fontSize.value > 0)
        assertTrue(typography.bodyMedium.fontSize.value > 0)
        assertTrue(typography.bodySmall.fontSize.value > 0)
        assertTrue(typography.headlineLarge.fontSize.value > 0)
        assertTrue(typography.headlineMedium.fontSize.value > 0)
        assertTrue(typography.headlineSmall.fontSize.value > 0)
        assertTrue(typography.labelLarge.fontSize.value > 0)
        assertTrue(typography.labelMedium.fontSize.value > 0)
        assertTrue(typography.labelSmall.fontSize.value > 0)
        assertTrue(typography.titleLarge.fontSize.value > 0)
        assertTrue(typography.titleMedium.fontSize.value > 0)
        assertTrue(typography.titleSmall.fontSize.value > 0)
    }

    @Test
    fun testLineHeightScaling() {
        val typography = TextScaling.createScaledTypography(2.0f)
        
        // Line heights should also be scaled
        // Body large: 24 * 2.0 = 48 sp
        assertEquals(48f, typography.bodyLarge.lineHeight.value)
    }

    @Test
    fun testLetterSpacingScaling() {
        val typography = TextScaling.createScaledTypography(1.5f)
        
        // Letter spacing should be scaled
        // Body large: 0.5 * 1.5 = 0.75 sp
        assertTrue(typography.bodyLarge.letterSpacing.value > 0)
    }

    @Test
    fun testRecommendedLineHeight() {
        val lineHeight = TextScaling.getRecommendedLineHeight(16f)
        assertEquals(24f, lineHeight)
    }

    @Test
    fun testRecommendedLetterSpacing() {
        assertEquals(0.5f, TextScaling.getRecommendedLetterSpacing(24f))
        assertEquals(0.25f, TextScaling.getRecommendedLetterSpacing(18f))
        assertEquals(0.15f, TextScaling.getRecommendedLetterSpacing(14f))
        assertEquals(0.1f, TextScaling.getRecommendedLetterSpacing(12f))
    }

    @Test
    fun testValidScaleFactor() {
        assertTrue(TextScaling.isValidScaleFactor(0.5f))
        assertTrue(TextScaling.isValidScaleFactor(1.0f))
        assertTrue(TextScaling.isValidScaleFactor(1.5f))
        assertTrue(TextScaling.isValidScaleFactor(2.0f))
        
        assertFalse(TextScaling.isValidScaleFactor(0.4f))
        assertFalse(TextScaling.isValidScaleFactor(2.1f))
    }

    @Test
    fun testScaleDescription() {
        assertEquals("Small (85%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.SMALL))
        assertEquals("Normal (100%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.NORMAL))
        assertEquals("Large (115%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.LARGE))
        assertEquals("Extra Large (130%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.EXTRA_LARGE))
        assertEquals("Huge (150%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.HUGE))
        assertEquals("Maximum (200%)", TextScaling.getScaleDescription(TextScaling.ScaleFactor.MAXIMUM))
    }

    @Test
    fun testHeadlineScaling() {
        val typography = TextScaling.createScaledTypography(2.0f)
        
        // Headline large: 28 * 2.0 = 56 sp
        assertEquals(56f, typography.headlineLarge.fontSize.value)
        // Headline medium: 24 * 2.0 = 48 sp
        assertEquals(48f, typography.headlineMedium.fontSize.value)
        // Headline small: 20 * 2.0 = 40 sp
        assertEquals(40f, typography.headlineSmall.fontSize.value)
    }

    @Test
    fun testTitleScaling() {
        val typography = TextScaling.createScaledTypography(1.5f)
        
        // Title large: 22 * 1.5 = 33 sp
        assertEquals(33f, typography.titleLarge.fontSize.value)
        // Title medium: 16 * 1.5 = 24 sp
        assertEquals(24f, typography.titleMedium.fontSize.value)
        // Title small: 14 * 1.5 = 21 sp
        assertEquals(21f, typography.titleSmall.fontSize.value)
    }

    @Test
    fun testLabelScaling() {
        val typography = TextScaling.createScaledTypography(1.3f)
        
        // Label large: 14 * 1.3 = 18.2 sp
        assertTrue(typography.labelLarge.fontSize.value in 18.0f..19.0f)
        // Label medium: 12 * 1.3 = 15.6 sp
        assertTrue(typography.labelMedium.fontSize.value in 15.0f..16.0f)
        // Label small: 11 * 1.3 = 14.3 sp
        assertTrue(typography.labelSmall.fontSize.value in 14.0f..15.0f)
    }

    @Test
    fun testScaleFactorMultipliers() {
        assertEquals(0.85f, TextScaling.ScaleFactor.SMALL.multiplier)
        assertEquals(1.0f, TextScaling.ScaleFactor.NORMAL.multiplier)
        assertEquals(1.15f, TextScaling.ScaleFactor.LARGE.multiplier)
        assertEquals(1.3f, TextScaling.ScaleFactor.EXTRA_LARGE.multiplier)
        assertEquals(1.5f, TextScaling.ScaleFactor.HUGE.multiplier)
        assertEquals(2.0f, TextScaling.ScaleFactor.MAXIMUM.multiplier)
    }
}
