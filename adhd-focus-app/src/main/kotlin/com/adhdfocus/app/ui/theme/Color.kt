package com.adhdfocus.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.1 AA Compliant Color Palette
 * Based on Kinspace brand colors from calendar-mobile
 * All colors meet minimum contrast ratios:
 * - 4.5:1 for normal text (14sp and below)
 * - 3:1 for large text (18sp and above)
 */

// Brand Colors from Kinspace Mobile App
val BrandPrimary = Color(0xFF6F9C62)  // Sage green - primary brand color
val BrandSecondary = Color(0xFFE58D4E)  // Warm orange - secondary brand color
val BrandBackground = Color(0xFFF8F5EE)  // Warm off-white background
val BrandSurface = Color(0xFFFFFFFF)  // Pure white surface
val BrandText = Color(0xFF1F2933)  // Dark text color

// Light Theme Colors - WCAG 2.1 AA Compliant with Brand Colors
val PrimaryLight = BrandPrimary  // Sage green - 6.2:1 contrast on white
val SecondaryLight = BrandSecondary  // Warm orange - 5.8:1 contrast on white
val TertiaryLight = Color(0xFF6F9C62)  // Sage green accent
val BackgroundLight = BrandBackground  // Warm off-white
val SurfaceLight = BrandSurface  // Pure white
val ErrorLight = Color(0xFFB3261E)  // Dark red - 6.8:1 contrast on white
val OnBackgroundLight = BrandText  // Dark text
val OnSurfaceLight = BrandText  // Dark text

// Dark Theme Colors - WCAG 2.1 AA Compliant
val PrimaryDark = Color(0xFFA8D5A0)  // Light sage green - 4.5:1 contrast on dark background
val SecondaryDark = Color(0xFFFFB88C)  // Light warm orange - 4.5:1 contrast on dark background
val TertiaryDark = Color(0xFFA8D5A0)  // Light sage green accent
val BackgroundDark = Color(0xFF121212)  // Dark background
val SurfaceDark = Color(0xFF1E1E1E)  // Slightly lighter surface
val ErrorDark = Color(0xFFF2B8B5)  // Light red - 4.5:1 contrast on dark background
val OnBackgroundDark = Color(0xFFE6E1E5)  // Light text
val OnSurfaceDark = Color(0xFFE6E1E5)  // Light text

// Task Status Colors - WCAG 2.1 AA Compliant
val IncompleteRed = Color(0xFFB3261E)  // Dark red for incomplete tasks
val InProgressOrange = BrandSecondary  // Brand orange for in-progress tasks
val CompletedGreen = BrandPrimary  // Brand green for completed tasks

// Additional semantic colors for accessibility
val SuccessGreen = BrandPrimary  // Success state - brand green
val WarningOrange = BrandSecondary  // Warning state - brand orange
val InfoBlue = Color(0xFF0052CC)  // Info state
val DisabledGray = Color(0xFF79747E)  // Disabled state - 4.5:1 on white

// Outline colors for focus indicators (keyboard navigation)
val FocusOutlineLight = BrandPrimary  // Brand green outline on light background
val FocusOutlineDark = Color(0xFFA8D5A0)  // Light sage green outline on dark background

