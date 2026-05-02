# ADHD Focus App - Package Structure Documentation

## Overview

This document describes the package organization of the ADHD Focus App, following Android best practices with a combination of layer-based and feature-based architecture.

## Architecture Principles

1. **Layer-Based Organization**: Core infrastructure organized by layer (data, domain, ui, di)
2. **Feature-Based Organization**: Major features organized as self-contained modules
3. **Clear Separation of Concerns**: Each package has a single, well-defined responsibility
4. **Scalability**: Structure supports adding new features without modifying existing code
5. **Testability**: Test structure mirrors source structure for easy test discovery

## Package Structure

```
com.adhdfocus.app/
├── MainActivity.kt                          # Application entry point
│
├── di/                                      # Dependency Injection (Hilt modules)
│   ├── AppModule.kt                         # Application-level dependencies
│   ├── DataModule.kt                        # Data layer dependencies
│   ├── DomainModule.kt                      # Domain layer dependencies
│   └── UiModule.kt                          # UI layer dependencies
│
├── data/                                    # Data Layer
│   ├── model/                               # Data models (entities)
│   │   ├── Task.kt
│   │   ├── User.kt
│   │   ├── Affirmation.kt
│   │   ├── Badge.kt
│   │   ├── Streak.kt
│   │   ├── EfficiencyMetric.kt
│   │   └── Household.kt
│   │
│   ├── dao/                                 # Room Data Access Objects
│   │   ├── TaskDao.kt
│   │   ├── UserDao.kt
│   │   ├── AffirmationDao.kt
│   │   ├── BadgeDao.kt
│   │   ├── StreakDao.kt
│   │   ├── EfficiencyMetricDao.kt
│   │   └── SyncQueueDao.kt
│   │
│   ├── database/                            # Database configuration
│   │   ├── AdhdfocusDatabase.kt             # Room database instance
│   │   ├── Converters.kt                    # Type converters
│   │   ├── DatabaseInitializer.kt           # Database initialization
│   │   ├── DatabaseBackupManager.kt         # Backup/restore functionality
│   │   ├── README.md                        # Database documentation
│   │   └── MIGRATION_GUIDE.md               # Migration instructions
│   │
│   ├── network/                             # Network/API layer
│   │   ├── ApiConfig.kt                     # API configuration
│   │   ├── ApiModels.kt                     # API request/response models
│   │   ├── AuthService.kt                   # Authentication API
│   │   ├── TaskService.kt                   # Task API
│   │   ├── SyncService.kt                   # Sync API
│   │   ├── AuthInterceptor.kt               # Auth token interceptor
│   │   ├── TokenRefreshInterceptor.kt       # Token refresh logic
│   │   └── ErrorHandler.kt                  # API error handling
│   │
│   ├── security/                            # Security utilities
│   │   ├── TokenStorage.kt                  # Secure token storage
│   │   └── EncryptionManager.kt             # Data encryption
│   │
│   └── repository/                          # Repository pattern (data access abstraction)
│       ├── TaskRepository.kt
│       ├── UserRepository.kt
│       ├── AffirmationRepository.kt
│       ├── BadgeRepository.kt
│       ├── StreakRepository.kt
│       └── SyncRepository.kt
│
├── domain/                                  # Domain Layer (Business Logic)
│   ├── auth/                                # Authentication domain
│   │   ├── AuthManager.kt
│   │   └── AuthUseCase.kt
│   │
│   ├── task/                                # Task management domain
│   │   ├── TaskManager.kt
│   │   ├── TaskValidator.kt
│   │   └── TaskUseCase.kt
│   │
│   ├── progress/                            # Progress tracking domain
│   │   ├── ProgressTracker.kt
│   │   ├── StreakCalculator.kt
│   │   └── ProgressUseCase.kt
│   │
│   ├── affirmation/                         # Affirmation engine domain
│   │   ├── AffirmationEngine.kt
│   │   ├── AffirmationSelector.kt
│   │   └── AffirmationUseCase.kt
│   │
│   ├── gamification/                        # Gamification domain
│   │   ├── BadgeSystem.kt
│   │   ├── EfficiencyCalculator.kt
│   │   └── GamificationUseCase.kt
│   │
│   ├── sync/                                # Cloud sync domain
│   │   ├── SyncManager.kt
│   │   ├── ConflictResolver.kt
│   │   └── SyncUseCase.kt
│   │
│   └── serialization/                       # Data serialization/parsing
│       ├── TaskSerializer.kt
│       ├── TaskParser.kt
│       ├── AffirmationSerializer.kt
│       └── AffirmationParser.kt
│
├── ui/                                      # UI Layer (Jetpack Compose)
│   ├── theme/                               # Theme and styling
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   └── Shapes.kt
│   │
│   ├── common/                              # Shared UI components
│   │   ├── component/
│   │   │   ├── TaskStatusIndicator.kt
│   │   │   ├── ProgressBar.kt
│   │   │   ├── StreakDisplay.kt
│   │   │   └── SyncStatusIndicator.kt
│   │   │
│   │   └── util/
│   │       ├── UiConstants.kt
│   │       └── UiExtensions.kt
│   │
│   ├── auth/                                # Authentication feature UI
│   │   ├── AuthViewModel.kt
│   │   ├── SignInScreen.kt
│   │   └── AuthNavigation.kt
│   │
│   ├── focus/                               # Home screen feature
│   │   ├── FocusViewModel.kt
│   │   ├── DailyFocusScreen.kt
│   │   ├── TaskListSection.kt
│   │   ├── TaskItem.kt
│   │   └── FocusNavigation.kt
│   │
│   ├── task/                                # Task management feature
│   │   ├── TaskViewModel.kt
│   │   ├── TaskDetailScreen.kt
│   │   ├── CreateTaskScreen.kt
│   │   ├── EditTaskScreen.kt
│   │   └── TaskNavigation.kt
│   │
│   ├── timer/                               # Timer feature
│   │   ├── TimerViewModel.kt
│   │   ├── TimerScreen.kt
│   │   ├── TimerDisplay.kt
│   │   ├── TimerControls.kt
│   │   └── TimerNavigation.kt
│   │
│   ├── achievements/                        # Achievements/Badges feature
│   │   ├── AchievementViewModel.kt
│   │   ├── AchievementsScreen.kt
│   │   ├── BadgeGrid.kt
│   │   ├── BadgeDetail.kt
│   │   └── AchievementNavigation.kt
│   │
│   ├── family/                              # Family member switching feature
│   │   ├── FamilyMemberSwitcherViewModel.kt
│   │   ├── FamilyMemberSwitcher.kt
│   │   ├── MemberSelectionModal.kt
│   │   ├── MemberItem.kt
│   │   └── FamilyNavigation.kt
│   │
│   ├── settings/                            # Settings feature
│   │   ├── SettingsViewModel.kt
│   │   ├── SettingsScreen.kt
│   │   ├── PreferenceItem.kt
│   │   ├── ThemeSettings.kt
│   │   ├── NotificationSettings.kt
│   │   └── SettingsNavigation.kt
│   │
│   └── navigation/                          # Navigation structure
│       ├── NavGraph.kt
│       ├── NavDestinations.kt
│       └── NavController.kt
│
└── util/                                    # Utility functions
    ├── DateTimeUtils.kt
    ├── StringUtils.kt
    ├── LoggingUtils.kt
    ├── Constants.kt
    └── Extensions.kt
```

## Test Structure

The test structure mirrors the source structure:

```
com.adhdfocus.app/
├── data/
│   ├── dao/
│   │   ├── TaskDaoTest.kt
│   │   ├── UserDaoTest.kt
│   │   └── ...
│   │
│   ├── database/
│   │   ├── DatabaseSetupTest.kt
│   │   └── ConvertersTest.kt
│   │
│   ├── network/
│   │   ├── AuthInterceptorTest.kt
│   │   ├── ErrorHandlerTest.kt
│   │   └── ...
│   │
│   ├── security/
│   │   ├── TokenStorageTest.kt
│   │   └── EncryptionManagerTest.kt
│   │
│   └── repository/
│       ├── TaskRepositoryTest.kt
│       └── ...
│
├── domain/
│   ├── auth/
│   │   ├── AuthManagerTest.kt
│   │   └── AuthUseCaseTest.kt
│   │
│   ├── task/
│   │   ├── TaskManagerTest.kt
│   │   ├── TaskValidatorTest.kt
│   │   └── TaskUseCaseTest.kt
│   │
│   ├── progress/
│   │   ├── ProgressTrackerTest.kt
│   │   ├── StreakCalculatorTest.kt
│   │   └── ProgressUseCaseTest.kt
│   │
│   ├── affirmation/
│   │   ├── AffirmationEngineTest.kt
│   │   ├── AffirmationSelectorTest.kt
│   │   └── AffirmationUseCaseTest.kt
│   │
│   ├── gamification/
│   │   ├── BadgeSystemTest.kt
│   │   ├── EfficiencyCalculatorTest.kt
│   │   └── GamificationUseCaseTest.kt
│   │
│   ├── sync/
│   │   ├── SyncManagerTest.kt
│   │   ├── ConflictResolverTest.kt
│   │   └── SyncUseCaseTest.kt
│   │
│   └── serialization/
│       ├── TaskSerializerTest.kt
│       ├── TaskParserTest.kt
│       ├── AffirmationSerializerTest.kt
│       └── AffirmationParserTest.kt
│
└── ui/
    ├── auth/
    │   ├── AuthViewModelTest.kt
    │   └── SignInScreenTest.kt
    │
    ├── focus/
    │   ├── FocusViewModelTest.kt
    │   └── DailyFocusScreenTest.kt
    │
    └── ... (other feature tests)
```

## Layer Responsibilities

### Data Layer (`data/`)
- **Responsibility**: Data access and persistence
- **Contains**: Models, DAOs, database configuration, network clients, repositories
- **Dependencies**: Android framework, Room, Retrofit, security libraries
- **Exports**: Repository interfaces for domain layer

### Domain Layer (`domain/`)
- **Responsibility**: Business logic and use cases
- **Contains**: Managers, calculators, validators, serializers
- **Dependencies**: Data layer repositories
- **Exports**: Use cases and business logic interfaces for UI layer

### UI Layer (`ui/`)
- **Responsibility**: User interface and state management
- **Contains**: Screens, ViewModels, Compose components, navigation
- **Dependencies**: Domain layer use cases
- **Exports**: Navigation graph and screens

### DI Layer (`di/`)
- **Responsibility**: Dependency injection configuration
- **Contains**: Hilt modules for each layer
- **Dependencies**: All layers
- **Exports**: Configured dependencies

## Feature Organization

### Major Features
1. **Auth** - Authentication and sign-in
2. **Home** - Home screen (primary interface)
3. **Task** - Task creation, editing, deletion
4. **Timer** - Timer functionality with visual feedback
5. **Achievements** - Badges, streaks, efficiency metrics
6. **Family** - Family member switching
7. **Settings** - User preferences and customization

### Feature Characteristics
- Each feature has its own ViewModel
- Each feature has its own Screen/UI components
- Each feature has its own navigation
- Features can depend on domain layer and common components
- Features should not directly depend on other features

## Circular Dependency Prevention

### Rules
1. **No cross-feature dependencies**: Features should not import from each other
2. **Unidirectional dependencies**: UI → Domain → Data (never reverse)
3. **Common components**: Shared UI components in `ui/common/`
4. **Shared utilities**: Common functions in `util/`
5. **Dependency injection**: All dependencies injected via Hilt

### Verification
- Use Gradle dependency analysis to detect circular dependencies
- Code review process checks for cross-feature imports
- Lint rules configured to prevent violations

## Adding New Features

To add a new feature:

1. Create feature package under `ui/` (e.g., `ui/newfeature/`)
2. Create corresponding domain logic under `domain/` if needed
3. Create ViewModel for the feature
4. Create Screen composable for the feature
5. Add navigation to `ui/navigation/NavGraph.kt`
6. Create tests mirroring the structure
7. Update this documentation

## File Naming Conventions

- **Screens**: `*Screen.kt` (e.g., `DailyFocusScreen.kt`)
- **ViewModels**: `*ViewModel.kt` (e.g., `FocusViewModel.kt`)
- **Components**: `*Component.kt` or descriptive name (e.g., `TaskStatusIndicator.kt`)
- **Managers**: `*Manager.kt` (e.g., `TaskManager.kt`)
- **Calculators**: `*Calculator.kt` (e.g., `StreakCalculator.kt`)
- **Validators**: `*Validator.kt` (e.g., `TaskValidator.kt`)
- **Serializers**: `*Serializer.kt` (e.g., `TaskSerializer.kt`)
- **Parsers**: `*Parser.kt` (e.g., `TaskParser.kt`)
- **DAOs**: `*Dao.kt` (e.g., `TaskDao.kt`)
- **Services**: `*Service.kt` (e.g., `AuthService.kt`)
- **Tests**: `*Test.kt` (e.g., `TaskManagerTest.kt`)

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

## Documentation

Each package should include:
- Clear responsibility statement in code comments
- README.md for complex packages (e.g., database, network)
- Inline documentation for non-obvious logic
- Test documentation for complex test scenarios

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

## Future Enhancements

- **Modularization**: Convert features to separate modules
- **Feature flags**: Runtime feature toggling
- **Analytics**: Event tracking and analytics
- **Offline-first**: Enhanced offline capabilities
- **Internationalization**: Multi-language support
