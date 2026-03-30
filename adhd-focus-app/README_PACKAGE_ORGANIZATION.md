# ADHD Focus App - Package Organization Guide

## Quick Start

This document provides a guide to the package organization of the ADHD Focus App. The project follows Android best practices with a combination of layer-based and feature-based architecture.

## Project Structure Overview

```
com.adhdfocus.app/
├── MainActivity.kt                          # Application entry point
├── di/                                      # Dependency Injection
├── data/                                    # Data Layer
├── domain/                                  # Domain Layer (Business Logic)
├── ui/                                      # UI Layer (Jetpack Compose)
└── util/                                    # Utilities
```

## Layer Architecture

### 1. Data Layer (`data/`)

**Responsibility**: Data access and persistence

**Packages**:
- `model/` - Data models (Room entities)
- `dao/` - Data Access Objects (Room DAOs)
- `database/` - Database configuration and initialization
- `network/` - API clients and network configuration
- `security/` - Secure storage and encryption
- `repository/` - Repository pattern for data access abstraction

**Key Classes**:
- `Task`, `User`, `Affirmation`, `Badge`, `Streak`, `EfficiencyMetric` - Data models
- `TaskDao`, `UserDao`, etc. - Database access
- `AdhdfocusDatabase` - Room database instance
- `TaskRepository`, `UserRepository`, etc. - Data access abstraction

**Dependencies**: Android framework, Room, Retrofit, security libraries

### 2. Domain Layer (`domain/`)

**Responsibility**: Business logic and use cases

**Packages**:
- `auth/` - Authentication logic
- `task/` - Task management business logic
- `progress/` - Progress tracking and streak calculation
- `affirmation/` - Affirmation engine
- `gamification/` - Badge system and efficiency calculation
- `sync/` - Cloud synchronization logic
- `serialization/` - Data serialization and parsing

**Key Classes**:
- `TaskManager` - Task creation, updates, completion
- `ProgressTracker` - Progress calculation and tracking
- `StreakCalculator` - Streak calculation logic
- `AffirmationEngine` - Affirmation selection and delivery
- `BadgeSystem` - Badge earning and tracking
- `EfficiencyCalculator` - Efficiency metrics
- `SyncManager` - Cloud synchronization
- `TaskSerializer`, `TaskParser` - JSON serialization

**Dependencies**: Data layer repositories

### 3. UI Layer (`ui/`)

**Responsibility**: User interface and state management

**Packages**:
- `theme/` - Theme and styling (colors, typography, shapes)
- `common/` - Shared UI components and utilities
  - `component/` - Reusable Compose components
  - `util/` - UI utilities and constants
- `auth/` - Authentication screens and ViewModels
- `focus/` - Daily Focus View feature
- `task/` - Task management screens
- `timer/` - Timer feature
- `achievements/` - Achievements and badges display
- `family/` - Family member switching
- `settings/` - Settings screens
- `navigation/` - Navigation structure

**Key Classes**:
- `FocusViewModel` - Daily Focus View state
- `TaskViewModel` - Task management state
- `TimerViewModel` - Timer state
- `AchievementViewModel` - Achievements state
- `FamilyMemberSwitcherViewModel` - Family member switching state
- `SettingsViewModel` - Settings state
- Compose screens and components

**Dependencies**: Domain layer use cases, Jetpack Compose

### 4. DI Layer (`di/`)

**Responsibility**: Dependency injection configuration

**Packages**:
- `AppModule` - Application-level dependencies
- `DataModule` - Data layer dependencies
- `DomainModule` - Domain layer dependencies
- `UiModule` - UI layer dependencies

**Key Classes**:
- Hilt modules for each layer

**Dependencies**: All layers

### 5. Utilities (`util/`)

**Responsibility**: Common utility functions

**Key Classes**:
- `Constants` - Application-wide constants
- `DateTimeUtils` - Date and time utilities
- `StringUtils` - String manipulation utilities
- `LoggingUtils` - Logging utilities
- `Extensions` - Kotlin extension functions

## Feature Organization

### Major Features

1. **Auth** (`ui/auth/`)
   - Sign-in screen
   - Authentication ViewModel
   - Navigation

2. **Focus** (`ui/focus/`)
   - Daily Focus View (primary interface)
   - Task list organization by Todo_Group
   - Progress display
   - Streak display
   - FocusViewModel

3. **Task** (`ui/task/`)
   - Task creation screen
   - Task detail screen
   - Task editing screen
   - TaskViewModel

4. **Timer** (`ui/timer/`)
   - Timer display with countdown
   - Visual feedback (progress ring, color changes)
   - Audio notification
   - TimerViewModel

5. **Achievements** (`ui/achievements/`)
   - Badges display
   - Streak display
   - Efficiency metrics
   - AchievementViewModel

6. **Family** (`ui/family/`)
   - Family member switcher
   - Member selection modal
   - PIN protection
   - FamilyMemberSwitcherViewModel

7. **Settings** (`ui/settings/`)
   - User preferences
   - Theme selection
   - Notification settings
   - SettingsViewModel

## Dependency Flow

```
UI Layer
   ↓ (depends on)
Domain Layer
   ↓ (depends on)
Data Layer
   ↓ (depends on)
Android Framework & Libraries
```

**Key Rule**: Dependencies flow downward only. Never have lower layers depend on upper layers.

## Circular Dependency Prevention

### Rules

1. **No cross-feature dependencies**: Features should not import from each other
2. **Unidirectional dependencies**: UI → Domain → Data (never reverse)
3. **Common components**: Shared UI components in `ui/common/`
4. **Shared utilities**: Common functions in `util/`
5. **Dependency injection**: All dependencies injected via Hilt

### Verification

- Use Gradle dependency analysis: `./gradlew dependencies`
- Code review process checks for cross-feature imports
- Lint rules configured to prevent violations

## File Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Screens | `*Screen.kt` | `DailyFocusScreen.kt` |
| ViewModels | `*ViewModel.kt` | `FocusViewModel.kt` |
| Components | `*Component.kt` or descriptive | `TaskStatusIndicator.kt` |
| Managers | `*Manager.kt` | `TaskManager.kt` |
| Calculators | `*Calculator.kt` | `StreakCalculator.kt` |
| Validators | `*Validator.kt` | `TaskValidator.kt` |
| Serializers | `*Serializer.kt` | `TaskSerializer.kt` |
| Parsers | `*Parser.kt` | `TaskParser.kt` |
| DAOs | `*Dao.kt` | `TaskDao.kt` |
| Services | `*Service.kt` | `AuthService.kt` |
| Tests | `*Test.kt` | `TaskManagerTest.kt` |

## Test Structure

The test structure mirrors the source structure:

```
src/test/kotlin/com/adhdfocus/app/
├── data/
│   ├── dao/
│   ├── database/
│   ├── network/
│   ├── security/
│   └── repository/
├── domain/
│   ├── auth/
│   ├── task/
│   ├── progress/
│   ├── affirmation/
│   ├── gamification/
│   ├── sync/
│   └── serialization/
└── ui/
    ├── auth/
    ├── focus/
    ├── task/
    ├── timer/
    ├── achievements/
    ├── family/
    └── settings/
```

## Adding New Features

To add a new feature:

1. **Create UI package**: `ui/newfeature/`
   - `NewFeatureViewModel.kt`
   - `NewFeatureScreen.kt`
   - `NewFeatureNavigation.kt`

2. **Create domain logic** (if needed): `domain/newfeature/`
   - `NewFeatureManager.kt`
   - `NewFeatureUseCase.kt`

3. **Create data layer** (if needed): `data/repository/NewFeatureRepository.kt`

4. **Add navigation**: Update `ui/navigation/NavGraph.kt`

5. **Create tests**: Mirror the structure in `src/test/`

6. **Update documentation**: Add to this guide

## Import Organization

Within each file, organize imports as follows:

```kotlin
// 1. Android framework imports
import android.content.*
import androidx.compose.*

// 2. Third-party library imports
import com.google.hilt.*
import retrofit2.*

// 3. Internal app imports (organized by layer)
import com.adhdfocus.app.data.*
import com.adhdfocus.app.domain.*
import com.adhdfocus.app.ui.*
import com.adhdfocus.app.util.*
```

## Best Practices

### 1. Separation of Concerns
- Each class has a single responsibility
- Features are self-contained
- Layers have clear boundaries

### 2. Dependency Injection
- Use Hilt for dependency injection
- Inject dependencies via constructor
- Avoid service locator pattern

### 3. State Management
- Use StateFlow for UI state
- Use ViewModel for screen state
- Avoid mutable state in composables

### 4. Error Handling
- Handle errors at appropriate layers
- Provide user-friendly error messages
- Log errors for debugging

### 5. Testing
- Write unit tests for business logic
- Write integration tests for features
- Use property-based tests for core logic
- Mock external dependencies

### 6. Documentation
- Document complex logic
- Add comments for non-obvious code
- Keep README files updated
- Document API contracts

## Performance Considerations

- **Lazy loading**: Screens and features loaded on-demand
- **Caching**: Frequently accessed data cached in repositories
- **Pagination**: Large lists paginated to reduce memory usage
- **Coroutines**: Async operations using Kotlin coroutines
- **Flow**: Reactive data streams using StateFlow and Flow

## Security Considerations

- **Token storage**: Secure storage in `data/security/`
- **Data encryption**: Sensitive data encrypted at rest
- **API security**: HTTPS only, certificate pinning
- **Input validation**: All user inputs validated in domain layer
- **Error handling**: Sensitive errors not exposed to UI

## Common Tasks

### Adding a new screen
1. Create `NewFeatureScreen.kt` in `ui/newfeature/`
2. Create `NewFeatureViewModel.kt` in `ui/newfeature/`
3. Add navigation to `ui/navigation/NavGraph.kt`
4. Create tests in `src/test/kotlin/com/adhdfocus/app/ui/newfeature/`

### Adding business logic
1. Create manager/calculator in `domain/feature/`
2. Create repository in `data/repository/` if needed
3. Inject into ViewModel
4. Create tests in `src/test/kotlin/com/adhdfocus/app/domain/feature/`

### Adding a data model
1. Create entity in `data/model/`
2. Create DAO in `data/dao/`
3. Create repository in `data/repository/`
4. Add to database schema in `data/database/`
5. Create tests in `src/test/kotlin/com/adhdfocus/app/data/`

## Troubleshooting

### Circular dependency detected
- Check that dependencies flow downward (UI → Domain → Data)
- Ensure features don't import from each other
- Use dependency injection to break cycles

### ViewModel not injected
- Ensure Hilt module is configured
- Check that @HiltViewModel annotation is present
- Verify dependency is provided in DI module

### Test fails with missing dependency
- Check that test dependencies are configured
- Ensure mocks are provided for external dependencies
- Verify test uses correct package structure

## References

- [Android Architecture Guide](https://developer.android.com/guide/architecture)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
