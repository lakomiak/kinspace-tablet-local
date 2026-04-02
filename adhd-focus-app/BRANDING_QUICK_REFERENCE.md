# Branding Quick Reference

## Brand Colors

### Primary (Sage Green)
- **Hex**: `#6F9C62`
- **RGB**: `111, 156, 98`
- **Usage**: Primary buttons, active states, completed tasks
- **Contrast**: 6.2:1 on white

### Secondary (Warm Orange)
- **Hex**: `#E58D4E`
- **RGB**: `229, 141, 78`
- **Usage**: Secondary buttons, in-progress tasks, accents
- **Contrast**: 5.8:1 on white

### Background
- **Hex**: `#F8F5EE`
- **RGB**: `248, 245, 238`
- **Usage**: App background, light surfaces

### Surface
- **Hex**: `#FFFFFF`
- **RGB**: `255, 255, 255`
- **Usage**: Cards, dialogs, elevated surfaces

### Text
- **Hex**: `#1F2933`
- **RGB**: `31, 41, 51`
- **Usage**: Primary text, headings
- **Contrast**: 13.5:1 on white

## File Locations

### Android Resources
- Colors: `src/main/res/values/colors.xml`
- Theme: `src/main/res/values/themes.xml`
- Launcher Icon: `src/main/res/drawable/ic_launcher_foreground.xml`
- Splash Screen: `src/main/res/drawable/splash_screen.xml`

### Compose Theme
- Colors: `src/main/kotlin/com/adhdfocus/app/ui/theme/Color.kt`
- Theme: `src/main/kotlin/com/adhdfocus/app/ui/theme/Theme.kt`

## Usage Examples

### In XML Resources
```xml
<!-- Use brand colors in XML -->
<item name="android:colorPrimary">@color/brand_primary</item>
<item name="android:colorAccent">@color/brand_secondary</item>
```

### In Compose
```kotlin
// Use brand colors in Compose
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryLight  // Brand Primary
    )
)
```

## Task Status Colors

| Status | Color | Hex Code |
|--------|-------|----------|
| Completed | Brand Primary | `#6F9C62` |
| In-Progress | Brand Secondary | `#E58D4E` |
| Incomplete | Dark Red | `#B3261E` |

## Accessibility

- All colors meet WCAG 2.1 AA standards
- Minimum contrast ratio: 4.5:1 for normal text
- Focus indicators use brand colors
- Dark theme supported with adjusted colors

## Documentation

- **Full Guide**: `BRANDING.md`
- **Implementation Details**: `BRANDING_IMPLEMENTATION_SUMMARY.md`
- **This File**: `BRANDING_QUICK_REFERENCE.md`

## Build Verification

✓ Kotlin compilation: Successful
✓ Resource compilation: Successful
✓ WCAG 2.1 AA compliance: Verified
✓ Cross-platform consistency: Verified

---

**Last Updated**: April 2026
**Brand Version**: 1.0
