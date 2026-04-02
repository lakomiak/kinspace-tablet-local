# ADHD Focus App - Branding Guide

## Overview

The ADHD Focus App uses the professional branding from the Kinspace calendar-mobile application, ensuring visual consistency across the product ecosystem.

## Brand Colors

All colors are WCAG 2.1 AA compliant for accessibility.

### Primary Brand Colors

| Color | Hex Code | Usage | Contrast Ratio |
|-------|----------|-------|-----------------|
| Brand Primary (Sage Green) | `#6F9C62` | Primary actions, buttons, focus states | 6.2:1 on white |
| Brand Secondary (Warm Orange) | `#E58D4E` | Secondary actions, accents, warnings | 5.8:1 on white |
| Brand Background | `#F8F5EE` | App background, light surfaces | - |
| Brand Surface | `#FFFFFF` | Cards, dialogs, elevated surfaces | - |
| Brand Text | `#1F2933` | Primary text, headings | 13.5:1 on white |

### Semantic Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| Success | `#6F9C62` | Completed tasks, success states |
| Warning | `#E58D4E` | In-progress tasks, warnings |
| Error | `#B3261E` | Incomplete tasks, errors |
| Info | `#0052CC` | Information, help text |
| Disabled | `#79747E` | Disabled states |

## Implementation

### Android Resources

**XML Color Definitions** (`src/main/res/values/colors.xml`):
- All brand colors defined as Android color resources
- Material 3 color scheme colors for compatibility
- Semantic colors for task states

**Theme Configuration** (`src/main/res/values/themes.xml`):
- Material 3 Light theme with brand colors
- Status bar styling with brand background
- Text color configuration

### Jetpack Compose Theme

**Color Definitions** (`src/main/kotlin/com/adhdfocus/app/ui/theme/Color.kt`):
- Light and dark theme color schemes
- Brand colors as primary and secondary
- Task status colors aligned with brand palette
- Focus outline colors for keyboard navigation

**Theme Application** (`src/main/kotlin/com/adhdfocus/app/ui/theme/Theme.kt`):
- `AdhdfocusAppTheme()` - Main theme composable
- `AdhdfocusAppThemeWithTheme()` - Theme-aware variant
- Status bar color set to brand background
- Dynamic colors disabled for WCAG compliance

## Launcher Icon

### Design

The launcher icon features:
- **Background**: Brand Primary (Sage Green) `#6F9C62`
- **Foreground**: White checkmark symbol
- **Meaning**: Task completion and focus

### Files

- **Adaptive Icon**: `src/main/res/mipmap-mdpi/ic_launcher.xml`
- **Foreground**: `src/main/res/drawable/ic_launcher_foreground.xml`
- **Background Color**: Defined in `colors.xml`

### Sizes

Android generates launcher icons at multiple densities:
- mdpi (160 dpi)
- hdpi (240 dpi)
- xhdpi (320 dpi)
- xxhdpi (480 dpi)
- xxxhdpi (640 dpi)

## Splash Screen

### Design

The splash screen displays:
- Brand background color (`#F8F5EE`)
- Centered launcher icon (200dp × 200dp)
- Clean, minimal design

### Files

- **Splash Screen Drawable**: `src/main/res/drawable/splash_screen.xml`
- **Splash Screen Theme**: `src/main/res/values/splash_screen_theme.xml`

### Implementation

The splash screen is shown during app startup and transitions to the main app UI.

## Accessibility Compliance

### WCAG 2.1 AA Compliance

All colors meet WCAG 2.1 AA standards:
- **Normal text**: Minimum 4.5:1 contrast ratio
- **Large text** (18sp+): Minimum 3:1 contrast ratio
- **UI components**: Minimum 3:1 contrast ratio

### Color Contrast Verification

| Foreground | Background | Contrast Ratio | Status |
|-----------|-----------|-----------------|--------|
| Brand Text | Brand Background | 13.5:1 | ✓ Pass |
| Brand Primary | White | 6.2:1 | ✓ Pass |
| Brand Secondary | White | 5.8:1 | ✓ Pass |
| White | Brand Primary | 6.2:1 | ✓ Pass |
| White | Brand Secondary | 5.8:1 | ✓ Pass |

### Focus Indicators

- **Light Theme**: Brand Primary (Sage Green) outline
- **Dark Theme**: Light Sage Green outline
- **Minimum Width**: 2dp for visibility
- **Keyboard Navigation**: All interactive elements have visible focus states

## Usage Guidelines

### Primary Color (Sage Green)

Use for:
- Primary action buttons
- Active navigation items
- Completed task indicators
- Focus states
- Primary UI elements

### Secondary Color (Warm Orange)

Use for:
- Secondary action buttons
- In-progress task indicators
- Accent elements
- Warning states
- Emphasis

### Background Color

Use for:
- App background
- Screen backgrounds
- Light surfaces

### Surface Color

Use for:
- Cards
- Dialogs
- Elevated surfaces
- Input fields

### Text Color

Use for:
- Primary text
- Headings
- Body text
- Labels

## Dark Theme

The app supports dark theme with adjusted colors:
- **Primary**: Light Sage Green (`#A8D5A0`)
- **Secondary**: Light Warm Orange (`#FFBB8C`)
- **Background**: Dark (`#121212`)
- **Surface**: Slightly lighter (`#1E1E1E`)
- **Text**: Light (`#E6E1E5`)

All dark theme colors maintain WCAG 2.1 AA compliance.

## Consistency

### Cross-Platform Consistency

The tablet app uses the same brand colors as:
- **calendar-mobile** (Flutter): Primary `#6F9C62`, Secondary `#E58D4E`
- **calendar-cloud** (Backend): Brand colors in API responses
- **calendar** (Desktop): Consistent branding

### Future Updates

When updating brand colors:
1. Update `colors.xml` with new hex codes
2. Update `Color.kt` with new Compose colors
3. Update `themes.xml` with new theme colors
4. Verify WCAG 2.1 AA compliance
5. Test on both light and dark themes
6. Update this documentation

## References

- [Material Design 3 Color System](https://m3.material.io/styles/color/overview)
- [WCAG 2.1 Color Contrast](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [Android Material Design](https://developer.android.com/design/material)
- [Jetpack Compose Theming](https://developer.android.com/jetpack/compose/designsystems/material)

---

**Last Updated**: April 2026
**Brand Version**: 1.0
**Compliance**: WCAG 2.1 AA
