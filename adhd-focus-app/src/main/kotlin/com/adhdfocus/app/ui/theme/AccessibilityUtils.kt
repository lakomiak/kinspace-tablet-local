package com.adhdfocus.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Utility functions for WCAG 2.1 AA accessibility compliance.
 * Provides contrast ratio calculation and validation.
 */
object AccessibilityUtils {

    /**
     * Calculates the relative luminance of a color according to WCAG 2.1 formula.
     * @param color The color to calculate luminance for
     * @return Relative luminance value between 0 and 1
     */
    fun getRelativeLuminance(color: Color): Double {
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()

        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    /**
     * Calculates the contrast ratio between two colors according to WCAG 2.1 formula.
     * @param foreground The foreground color
     * @param background The background color
     * @return Contrast ratio between 1 and 21
     */
    fun getContrastRatio(foreground: Color, background: Color): Double {
        val l1 = getRelativeLuminance(foreground)
        val l2 = getRelativeLuminance(background)

        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Checks if a color pair meets WCAG 2.1 AA standard for normal text (4.5:1).
     * @param foreground The foreground color
     * @param background The background color
     * @return True if contrast ratio >= 4.5
     */
    fun meetsNormalTextContrast(foreground: Color, background: Color): Boolean {
        return getContrastRatio(foreground, background) >= 4.5
    }

    /**
     * Checks if a color pair meets WCAG 2.1 AA standard for large text (3:1).
     * @param foreground The foreground color
     * @param background The background color
     * @return True if contrast ratio >= 3.0
     */
    fun meetsLargeTextContrast(foreground: Color, background: Color): Boolean {
        return getContrastRatio(foreground, background) >= 3.0
    }

    /**
     * Checks if a color pair meets WCAG 2.1 AAA standard (7:1).
     * @param foreground The foreground color
     * @param background The background color
     * @return True if contrast ratio >= 7.0
     */
    fun meetsAAAContrast(foreground: Color, background: Color): Boolean {
        return getContrastRatio(foreground, background) >= 7.0
    }

    /**
     * Validates all critical color combinations for WCAG 2.1 AA compliance.
     * @return Map of color pair names to their contrast ratios
     */
    fun validateAllContrasts(): Map<String, Double> {
        return mapOf(
            // Light theme - text on background
            "Light: OnBackground on Background" to getContrastRatio(OnBackgroundLight, BackgroundLight),
            "Light: OnSurface on Surface" to getContrastRatio(OnSurfaceLight, SurfaceLight),
            "Light: Primary on Background" to getContrastRatio(PrimaryLight, BackgroundLight),
            "Light: Secondary on Background" to getContrastRatio(SecondaryLight, BackgroundLight),
            "Light: Error on Background" to getContrastRatio(ErrorLight, BackgroundLight),

            // Dark theme - text on background
            "Dark: OnBackground on Background" to getContrastRatio(OnBackgroundDark, BackgroundDark),
            "Dark: OnSurface on Surface" to getContrastRatio(OnSurfaceDark, SurfaceDark),
            "Dark: Primary on Background" to getContrastRatio(PrimaryDark, BackgroundDark),
            "Dark: Secondary on Background" to getContrastRatio(SecondaryDark, BackgroundDark),
            "Dark: Error on Background" to getContrastRatio(ErrorDark, BackgroundDark),

            // Task status colors
            "Light: Incomplete on Background" to getContrastRatio(IncompleteRed, BackgroundLight),
            "Light: InProgress on Background" to getContrastRatio(InProgressOrange, BackgroundLight),
            "Light: Completed on Background" to getContrastRatio(CompletedGreen, BackgroundLight),
            "Dark: Incomplete on Background" to getContrastRatio(IncompleteRed, BackgroundDark),
            "Dark: InProgress on Background" to getContrastRatio(InProgressOrange, BackgroundDark),
            "Dark: Completed on Background" to getContrastRatio(CompletedGreen, BackgroundDark),

            // Focus indicators
            "Light: Focus Outline on Background" to getContrastRatio(FocusOutlineLight, BackgroundLight),
            "Dark: Focus Outline on Background" to getContrastRatio(FocusOutlineDark, BackgroundDark)
        )
    }
}
