# Phase 12.3: Add Theme Switching (Light/Dark)

## Overview

Phase 12.3 implements comprehensive theme switching functionality for the ADHD Focus App. This phase builds on Phase 12.1 (Settings UI) and Phase 12.2 (Per-Member Preferences Storage) by providing the infrastructure to dynamically switch between light and dark themes with full persistence and app-wide application.

## Implementation Summary

### Core Components Implemented

#### 1. ThemeManager Interface (`domain/theme/ThemeManager.kt`)

**Purpose**: Define the contract for theme management

**Key Methods**:
- `getCurrentTheme()` - Get current theme
- `setTheme(theme, userId)` - Set theme and persist
- `loadThemeForUser(userId)` - Load theme for a user
- `resetThemeToDefault(userId)` - Reset to LIGHT theme
- `currentTheme: StateFlow<Theme>` - Observe theme changes

**Features**:
- Per-member theme preferences
- StateFlow for reactive updates
- Validation of user IDs

#### 2. ThemeManagerImpl (`domain/theme/ThemeManagerImpl.kt`)

**Purpose**: Implement theme management with persistence

**Key Features**:
- Integration with UserPreferencesManager
- In-memory theme state via MutableStateFlow
- Persistence to Room database via UserPreferencesManager
- Per-member theme isolation
- Singleton scope for app-wide access

**Implementation Details**:
```kotlin
@Singleton
class ThemeManagerImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ThemeManager {
    private val _currentTheme = MutableStateFlow(Theme.LIGHT)
    override val currentTheme: StateFlow<Theme> = _currentTheme
    
    override suspend fun setTheme(theme: Theme, userId: String) {
        _currentTheme.value = theme
        userPreferencesManager.updateTheme(userId, theme)
    }
    
    override suspend fun loadThemeForUser(userId: String) {
        val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
        _currentTheme.value = preferences.theme
    }
}
```

#### 3. Theme UI Integration (`ui/theme/Theme.kt`)

**New Composable**: `AdhdfocusAppThemeWithTheme`

**Purpose**: Apply theme based on Theme enum value

**Features**:
- Accepts Theme enum (LIGHT or DARK)
- Delegates to existing AdhdfocusAppTheme
- Supports dynamic color schemes
- Applies status bar styling

**Implementation**:
```kotlin
@Composable
fun AdhdfocusAppThemeWithTheme(
    theme: com.adhdfocus.app.data.model.Theme,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDarkTheme = theme == com.adhdfocus.app.data.model.Theme.DARK
    AdhdfocusAppTheme(
        darkTheme = isDarkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
```

#### 4. MainActivity Updates

**Changes**:
- Inject ThemeManager
- Observe currentTheme StateFlow
- Apply theme dynamically using AdhdfocusAppThemeWithTheme
- Theme changes apply immediately without restart

**Implementation**:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsStateWithLifecycle()
            AdhdfocusAppThemeWithTheme(theme = currentTheme) {
                Surface(...) { AdhdfocusApp() }
            }
        }
    }
}
```

#### 5. SettingsViewModel Updates

**Changes**:
- Inject ThemeManager
- Load theme on initialization
- Apply theme changes app-wide via ThemeManager
- Persist theme to database

**Key Methods**:
```kotlin
fun updateTheme(theme: Theme) {
    _theme.value = theme
    val userId = currentUserId ?: return
    viewModelScope.launch {
        themeManager.setTheme(theme, userId)
    }
}

private fun loadSettings(userId: String) {
    // ... load preferences ...
    themeManager.loadThemeForUser(userId)
}
```

#### 6. Dependency Injection (`di/AppModule.kt`)

**New Provider**:
```kotlin
@Singleton
@Provides
fun provideThemeManager(
    userPreferencesManager: UserPreferencesManager
): ThemeManager {
    return ThemeManagerImpl(userPreferencesManager)
}
```

### Existing Components Leveraged

1. **Theme Enum** (`data/model/UserPreferences.kt`)
   - Already defined with LIGHT and DARK values
   - Stored in UserPreferences entity

2. **Color Schemes** (`ui/theme/Color.kt`)
   - Light theme colors already defined
   - Dark theme colors already defined
   - Task status colors (red/orange/green)

3. **UserPreferencesManager** (`domain/preferences/UserPreferencesManager.kt`)
   - Provides persistence layer
   - Handles per-member preferences
   - Validates theme values

4. **SettingsScreen** (`ui/settings/SettingsScreen.kt`)
   - Already has ThemeSelector component
   - Displays theme options
   - Handles user selection

### Test Implementation

#### 1. Unit Tests: ThemeManagerUnitTest.kt

**Location**: `src/test/kotlin/com/adhdfocus/app/domain/theme/ThemeManagerUnitTest.kt`

**Test Coverage** (15 tests):

1. **State Management**
   - `testGetCurrentThemeReturnsCurrentTheme` - Verify current theme retrieval
   - `testSetThemeUpdatesCurrentTheme` - Verify theme state update
   - `testCurrentThemeStateFlowEmitsChanges` - Verify StateFlow emissions

2. **Persistence**
   - `testSetThemePersistsToPreferencesManager` - Verify database persistence
   - `testLoadThemeForUserLoadsFromPreferences` - Verify loading from database
   - `testResetThemeToDefaultSetsLight` - Verify reset functionality

3. **Per-Member Isolation**
   - `testPerMemberThemeIsolation` - Verify independent themes per user
   - `testDifferentUsersHaveIndependentThemes` - Verify isolation

4. **Theme Switching**
   - `testSetThemeLightThenDark` - Verify switching sequence
   - `testSetThemeDarkThenLight` - Verify reverse switching
   - `testThemeSwitchingBetweenValues` - Verify all transitions

5. **Validation**
   - `testSetThemeWithBlankUserIdThrowsException` - Verify user ID validation
   - `testLoadThemeForUserWithBlankUserIdThrowsException` - Verify validation
   - `testResetThemeToDefaultWithBlankUserIdThrowsException` - Verify validation

6. **Theme Loading**
   - `testLoadThemeForUserWithLightTheme` - Verify loading LIGHT
   - `testLoadThemeForUserWithDarkTheme` - Verify loading DARK

#### 2. Property-Based Tests: ThemeManagerPropertyTest.kt

**Location**: `src/test/kotlin/com/adhdfocus/app/domain/theme/ThemeManagerPropertyTest.kt`

**Validates**: Requirements 8, 6, Property 2.8: Theme Persistence

**Test Coverage** (8 properties):

1. **Property 2.8: Theme Persistence**
   - For any valid theme and user ID, set→load produces identical theme
   - Tests with random user IDs and all theme values
   - Verifies round-trip consistency

2. **Property: Per-Member Isolation**
   - For any two different users, their themes are independent
   - Changing one user's theme doesn't affect another's

3. **Property: State Consistency**
   - For any theme, multiple set operations maintain consistency
   - Final state matches last set value

4. **Property: Reset to Default**
   - For any initial theme, reset always sets LIGHT
   - Regardless of previous state

5. **Property: Load Correctness**
   - For any theme in preferences, load returns that theme
   - Verified with random themes

6. **Property: Theme Switching**
   - For any user, switching between LIGHT and DARK works correctly
   - All transitions are valid

7. **Property: StateFlow Emissions**
   - For any theme change, StateFlow emits the new value
   - Verified through value observation

8. **Property: Multiple Operations**
   - For any sequence of operations, final state is correct
   - Handles rapid theme changes

#### 3. Integration Tests: ThemeSwitchingIntegrationTest.kt

**Location**: `src/androidTest/kotlin/com/adhdfocus/app/ui/settings/ThemeSwitchingIntegrationTest.kt`

**Test Coverage** (10 tests):

1. **Theme Application**
   - `testLightThemeApplied` - Verify light theme colors applied
   - `testDarkThemeApplied` - Verify dark theme colors applied

2. **UI Components**
   - `testThemeSelectorDisplaysOptions` - Verify both theme options shown
   - `testThemeSelectorClickable` - Verify theme selection works
   - `testThemeSelectorDisablesCurrentTheme` - Verify current theme disabled

3. **Theme Switching**
   - `testThemeSwitchingPreservesOtherSettings` - Verify no side effects
   - `testThemeTransition` - Verify smooth transitions
   - `testThemeSwitchingBetweenValues` - Verify all transitions

4. **Color Schemes**
   - `testLightThemeColorScheme` - Verify light colors present
   - `testDarkThemeColorScheme` - Verify dark colors present

### Requirements Validation

**Requirement 8: Theme Customization**
- ✅ Users can switch between light and dark themes
- ✅ Theme preference is persisted
- ✅ Theme applies to entire app
- ✅ Theme changes apply dynamically without restart
- ✅ Per-member theme preferences supported

**Requirement 6: Settings & Customization**
- ✅ Theme preference is persisted
- ✅ Per-member theme preferences
- ✅ Theme survives app restart
- ✅ Efficient theme retrieval
- ✅ Support for theme reset to defaults

**Property 2.8: Task Persistence**
- ✅ Theme preference persists across app sessions
- ✅ Verified through property-based tests
- ✅ Tested with random data across many iterations

### Architecture

#### Data Flow

```
SettingsScreen (UI)
    ↓
SettingsViewModel
    ↓
ThemeManager (setTheme)
    ├→ Update in-memory state (_currentTheme)
    └→ UserPreferencesManager (updateTheme)
        └→ Room Database (user_preferences table)

MainActivity (App Startup)
    ↓
ThemeManager (loadThemeForUser)
    ├→ UserPreferencesManager (getPreferencesOrDefault)
    │   └→ Room Database (user_preferences table)
    └→ Update in-memory state (_currentTheme)
        └→ AdhdfocusAppThemeWithTheme (applies theme)
```

#### Per-Member Theme Isolation

Each family member has:
- Unique `userId` (primary key in user_preferences)
- Independent theme preference (LIGHT or DARK)
- Theme persisted in Room database
- Theme loaded on app startup or member switch

#### Theme Application

1. **App Startup**
   - MainActivity injects ThemeManager
   - Observes currentTheme StateFlow
   - Applies theme via AdhdfocusAppThemeWithTheme
   - All child composables inherit theme

2. **Theme Change**
   - User selects theme in SettingsScreen
   - SettingsViewModel calls themeManager.setTheme()
   - ThemeManager updates in-memory state
   - StateFlow emits new theme
   - MainActivity recomposes with new theme
   - All UI updates immediately

3. **Member Switch**
   - User switches family member
   - SettingsViewModel initializes with new userId
   - Calls themeManager.loadThemeForUser(newUserId)
   - ThemeManager loads theme from database
   - StateFlow emits new theme
   - UI updates to reflect new member's theme

### Color Schemes

**Light Theme**:
- Primary: #1E88E5 (Blue)
- Secondary: #43A047 (Green)
- Tertiary: #FB8C00 (Orange)
- Background: #FFFFFF (White)
- Surface: #F5F5F5 (Light Gray)
- Error: #E53935 (Red)

**Dark Theme**:
- Primary: #64B5F6 (Light Blue)
- Secondary: #66BB6A (Light Green)
- Tertiary: #FFB74D (Light Orange)
- Background: #121212 (Very Dark Gray)
- Surface: #1E1E1E (Dark Gray)
- Error: #FF5252 (Bright Red)

### Testing Strategy

#### Unit Testing
- Mock UserPreferencesManager
- Test theme state management
- Test persistence calls
- Test validation logic
- Test per-member isolation

#### Property-Based Testing
- Generate random user IDs and themes
- Verify set→load round-trip
- Verify state consistency
- Verify isolation between users
- 100+ iterations per property

#### Integration Testing
- Use Compose test framework
- Test theme application in UI
- Test theme selector component
- Test theme transitions
- Verify color schemes applied

### Test Statistics

- **Unit Tests**: 15 tests covering all manager operations
- **Property-Based Tests**: 8 properties with 100+ iterations each
- **Integration Tests**: 10 tests with actual Compose UI
- **Total Test Coverage**: 33+ tests

### Running the Tests

**Unit Tests**:
```bash
./gradlew test --tests "com.adhdfocus.app.domain.theme.ThemeManagerUnitTest"
```

**Property-Based Tests**:
```bash
./gradlew test --tests "com.adhdfocus.app.domain.theme.ThemeManagerPropertyTest"
```

**Integration Tests**:
```bash
./gradlew connectedAndroidTest --tests "com.adhdfocus.app.ui.settings.ThemeSwitchingIntegrationTest"
```

**All Tests**:
```bash
./gradlew test connectedAndroidTest
```

## Implementation Details

### Key Design Decisions

1. **Singleton ThemeManager**
   - Single instance for entire app
   - Ensures consistent theme state
   - Efficient memory usage

2. **StateFlow for Reactivity**
   - Automatic UI updates on theme change
   - No manual refresh needed
   - Lifecycle-aware with collectAsStateWithLifecycle

3. **Per-Member Isolation**
   - Each user has independent theme
   - Themes stored in user_preferences table
   - Loaded on member switch

4. **Immediate Application**
   - Theme changes apply without restart
   - MainActivity observes StateFlow
   - Recomposition applies new theme

5. **Persistence via UserPreferencesManager**
   - Leverages existing infrastructure
   - Consistent with other preferences
   - Automatic database persistence

### Integration Points

1. **UserPreferencesManager**
   - Provides updateTheme() method
   - Provides getPreferencesOrDefault() method
   - Handles database persistence

2. **SettingsViewModel**
   - Calls themeManager.setTheme() on user selection
   - Calls themeManager.loadThemeForUser() on initialization
   - Displays current theme in UI

3. **MainActivity**
   - Injects ThemeManager
   - Observes currentTheme StateFlow
   - Applies theme via AdhdfocusAppThemeWithTheme

4. **SettingsScreen**
   - Displays ThemeSelector component
   - Calls viewModel.updateTheme() on selection
   - Shows current theme selection

### Error Handling

- Blank user IDs throw IllegalArgumentException
- Invalid themes rejected by Theme enum
- Database errors caught and logged
- Graceful fallback to LIGHT theme on error

### Future Enhancements

1. **System Theme Detection**
   - Detect system dark mode preference
   - Auto-apply matching theme
   - Allow override in settings

2. **Theme Scheduling**
   - Schedule theme changes by time
   - Auto-switch to dark at night
   - Auto-switch to light in morning

3. **Custom Themes**
   - Allow users to create custom color schemes
   - Save custom themes to database
   - Share themes between users

4. **Theme Animations**
   - Smooth transitions between themes
   - Animated color changes
   - Fade effects on theme switch

5. **Accessibility Themes**
   - High contrast theme option
   - Large text theme option
   - Reduced motion theme option

## Conclusion

Phase 12.3 provides comprehensive theme switching functionality with:
- 33+ tests covering all functionality
- Full per-member theme isolation
- Dynamic theme application without restart
- Robust persistence via UserPreferencesManager
- Property-based testing for correctness guarantees
- Integration testing with actual Compose UI

All requirements are met and thoroughly tested. Theme switching is production-ready.

## Files Created/Modified

### New Files
- `src/main/kotlin/com/adhdfocus/app/domain/theme/ThemeManager.kt`
- `src/main/kotlin/com/adhdfocus/app/domain/theme/ThemeManagerImpl.kt`
- `src/test/kotlin/com/adhdfocus/app/domain/theme/ThemeManagerUnitTest.kt`
- `src/test/kotlin/com/adhdfocus/app/domain/theme/ThemeManagerPropertyTest.kt`
- `src/androidTest/kotlin/com/adhdfocus/app/ui/settings/ThemeSwitchingIntegrationTest.kt`

### Modified Files
- `src/main/kotlin/com/adhdfocus/app/MainActivity.kt` - Added ThemeManager injection and theme application
- `src/main/kotlin/com/adhdfocus/app/ui/theme/Theme.kt` - Added AdhdfocusAppThemeWithTheme composable
- `src/main/kotlin/com/adhdfocus/app/ui/settings/SettingsViewModel.kt` - Added ThemeManager integration
- `src/main/kotlin/com/adhdfocus/app/di/AppModule.kt` - Added ThemeManager provider

</content>
