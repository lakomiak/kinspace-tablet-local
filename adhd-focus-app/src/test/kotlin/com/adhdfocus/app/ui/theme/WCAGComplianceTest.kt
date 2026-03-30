package com.adhdfocus.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Tests for WCAG 2.1 AA color contrast compliance.
 * Validates that all critical color combinations meet minimum contrast ratios.
 */
class WCAGComplianceTest {

    @Test
    fun testLightThemeTextContrast() {
        // Normal text (4.5:1 minimum)
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(OnBackgroundLight, BackgroundLight),
            "OnBackground text on Background should meet 4.5:1 contrast"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(OnSurfaceLight, SurfaceLight),
            "OnSurface text on Surface should meet 4.5:1 contrast"
        )
    }

    @Test
    fun testDarkThemeTextContrast() {
        // Normal text (4.5:1 minimum)
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(OnBackgroundDark, BackgroundDark),
            "OnBackground text on Background should meet 4.5:1 contrast"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(OnSurfaceDark, SurfaceDark),
            "OnSurface text on Surface should meet 4.5:1 contrast"
        )
    }

    @Test
    fun testTaskStatusColorsLightTheme() {
        // Task status indicators should meet 3:1 for large text
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(IncompleteRed, BackgroundLight),
            "Incomplete red indicator should meet 3:1 contrast on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(InProgressOrange, BackgroundLight),
            "In-progress orange indicator should meet 3:1 contrast on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(CompletedGreen, BackgroundLight),
            "Completed green indicator should meet 3:1 contrast on light background"
        )
    }

    @Test
    fun testTaskStatusColorsDarkTheme() {
        // Task status indicators should meet 3:1 for large text
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(IncompleteRed, BackgroundDark),
            "Incomplete red indicator should meet 3:1 contrast on dark background"
        )
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(InProgressOrange, BackgroundDark),
            "In-progress orange indicator should meet 3:1 contrast on dark background"
        )
        assertTrue(
            AccessibilityUtils.meetsLargeTextContrast(CompletedGreen, BackgroundDark),
            "Completed green indicator should meet 3:1 contrast on dark background"
        )
    }

    @Test
    fun testFocusIndicatorContrast() {
        // Focus indicators should be clearly visible
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(FocusOutlineLight, BackgroundLight),
            "Focus outline should be visible on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(FocusOutlineDark, BackgroundDark),
            "Focus outline should be visible on dark background"
        )
    }

    @Test
    fun testErrorStateContrast() {
        // Error colors should be clearly visible
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(ErrorLight, BackgroundLight),
            "Error color should meet 4.5:1 contrast on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(ErrorDark, BackgroundDark),
            "Error color should meet 4.5:1 contrast on dark background"
        )
    }

    @Test
    fun testPrimaryColorContrast() {
        // Primary colors should be readable
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(PrimaryLight, BackgroundLight),
            "Primary light should meet 4.5:1 contrast on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(PrimaryDark, BackgroundDark),
            "Primary dark should meet 4.5:1 contrast on dark background"
        )
    }

    @Test
    fun testSecondaryColorContrast() {
        // Secondary colors should be readable
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(SecondaryLight, BackgroundLight),
            "Secondary light should meet 4.5:1 contrast on light background"
        )
        assertTrue(
            AccessibilityUtils.meetsNormalTextContrast(SecondaryDark, BackgroundDark),
            "Secondary dark should meet 4.5:1 contrast on dark background"
        )
    }

    @Test
    fun testContrastRatioCalculation() {
        // Test contrast ratio calculation with known values
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)
        val ratio = AccessibilityUtils.getContrastRatio(black, white)
        
        // Black on white should be 21:1
        assertTrue(
            ratio >= 20.0,
            "Black on white should have contrast ratio of 21:1, got $ratio"
        )
    }

    @Test
    fun testRelativeLuminanceCalculation() {
        // Test luminance calculation
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)
        
        val whiteLuminance = AccessibilityUtils.getRelativeLuminance(white)
        val blackLuminance = AccessibilityUtils.getRelativeLuminance(black)
        
        assertTrue(whiteLuminance > blackLuminance, "White should have higher luminance than black")
        assertTrue(whiteLuminance > 0.9, "White luminance should be close to 1.0")
        assertTrue(blackLuminance < 0.1, "Black luminance should be close to 0.0")
    }

    @Test
    fun testAllContrastsValidation() {
        // Validate all critical color combinations
        val contrasts = AccessibilityUtils.validateAllContrasts()
        
        // All contrasts should be >= 3.0 (minimum for large text)
        contrasts.forEach { (name, ratio) ->
            assertTrue(
                ratio >= 3.0,
                "$name has contrast ratio of $ratio, which is below 3.0"
            )
        }
    }
}
