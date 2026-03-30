package com.adhdfocus.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text scaling support for accessibility.
 * Provides typography with configurable scaling up to 200%.
 */
object TextScaling {

    /**
     * Scaling factors for different accessibility levels.
     */
    enum class ScaleFactor(val multiplier: Float) {
        SMALL(0.85f),      // 85% - for users who prefer smaller text
        NORMAL(1.0f),      // 100% - default
        LARGE(1.15f),      // 115% - large text
        EXTRA_LARGE(1.3f), // 130% - extra large text
        HUGE(1.5f),        // 150% - huge text
        MAXIMUM(2.0f)      // 200% - maximum supported
    }

    /**
     * Creates typography with scaled text sizes.
     * @param scaleFactor The scaling factor to apply
     * @return Typography with scaled text sizes
     */
    fun createScaledTypography(scaleFactor: ScaleFactor): Typography {
        return createScaledTypography(scaleFactor.multiplier)
    }

    /**
     * Creates typography with custom scaling factor.
     * @param scaleFactor The scaling multiplier (0.5 to 2.0)
     * @return Typography with scaled text sizes
     */
    fun createScaledTypography(scaleFactor: Float): Typography {
        val clampedFactor = scaleFactor.coerceIn(0.5f, 2.0f)

        return Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (16 * clampedFactor).sp,
                lineHeight = (24 * clampedFactor).sp,
                letterSpacing = (0.5 * clampedFactor).sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (14 * clampedFactor).sp,
                lineHeight = (20 * clampedFactor).sp,
                letterSpacing = (0.25 * clampedFactor).sp
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (12 * clampedFactor).sp,
                lineHeight = (16 * clampedFactor).sp,
                letterSpacing = (0.4 * clampedFactor).sp
            ),
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (28 * clampedFactor).sp,
                lineHeight = (36 * clampedFactor).sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (24 * clampedFactor).sp,
                lineHeight = (32 * clampedFactor).sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (20 * clampedFactor).sp,
                lineHeight = (28 * clampedFactor).sp,
                letterSpacing = 0.sp
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (14 * clampedFactor).sp,
                lineHeight = (20 * clampedFactor).sp,
                letterSpacing = (0.1 * clampedFactor).sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (12 * clampedFactor).sp,
                lineHeight = (16 * clampedFactor).sp,
                letterSpacing = (0.5 * clampedFactor).sp
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (11 * clampedFactor).sp,
                lineHeight = (16 * clampedFactor).sp,
                letterSpacing = (0.5 * clampedFactor).sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (22 * clampedFactor).sp,
                lineHeight = (28 * clampedFactor).sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (16 * clampedFactor).sp,
                lineHeight = (24 * clampedFactor).sp,
                letterSpacing = (0.15 * clampedFactor).sp
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (14 * clampedFactor).sp,
                lineHeight = (20 * clampedFactor).sp,
                letterSpacing = (0.1 * clampedFactor).sp
            )
        )
    }

    /**
     * Gets the recommended line height for a given font size.
     * Ensures readability at all scaling levels.
     * @param fontSize The font size in sp
     * @return Recommended line height in sp
     */
    fun getRecommendedLineHeight(fontSize: Float): Float {
        return fontSize * 1.5f  // 1.5x line height for readability
    }

    /**
     * Gets the recommended letter spacing for a given font size.
     * Improves readability at larger scales.
     * @param fontSize The font size in sp
     * @return Recommended letter spacing in sp
     */
    fun getRecommendedLetterSpacing(fontSize: Float): Float {
        return when {
            fontSize >= 24 -> 0.5f
            fontSize >= 18 -> 0.25f
            fontSize >= 14 -> 0.15f
            else -> 0.1f
        }
    }

    /**
     * Validates that text scaling is within acceptable range.
     * @param scaleFactor The scaling factor to validate
     * @return True if scaling factor is valid (0.5 to 2.0)
     */
    fun isValidScaleFactor(scaleFactor: Float): Boolean {
        return scaleFactor in 0.5f..2.0f
    }

    /**
     * Gets the description of a scaling factor.
     * @param scaleFactor The scaling factor
     * @return Human-readable description
     */
    fun getScaleDescription(scaleFactor: ScaleFactor): String {
        return when (scaleFactor) {
            ScaleFactor.SMALL -> "Small (85%)"
            ScaleFactor.NORMAL -> "Normal (100%)"
            ScaleFactor.LARGE -> "Large (115%)"
            ScaleFactor.EXTRA_LARGE -> "Extra Large (130%)"
            ScaleFactor.HUGE -> "Huge (150%)"
            ScaleFactor.MAXIMUM -> "Maximum (200%)"
        }
    }
}
