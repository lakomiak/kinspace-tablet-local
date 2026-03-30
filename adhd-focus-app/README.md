# ADHD Focus App - Android Implementation

A specialized Android tablet application designed to support family members with ADHD in managing daily tasks through a distraction-free, visually engaging experience.

## Project Structure

```
adhd-focus-app/
├── src/
│   ├── main/
│   │   ├── kotlin/com/adhdfocus/app/
│   │   │   ├── MainActivity.kt                 # Main entry point
│   │   │   ├── di/                             # Dependency injection
│   │   │   │   └── AppModule.kt
│   │   │   ├── data/
│   │   │   │   ├── database/                   # Room database setup
│   │   │   │   │   ├── AdhdfocusDatabase.kt
│   │   │   │   │   ├── Converters.kt
│   │   │   │   ├── model/                      # Data models
│   │   │   │   │   ├── Task.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Affirmation.kt
│   │   │   │   │   ├── Badge.kt
│   │   │   │   │   ├── Streak.kt
│   │   │   │   │   └── EfficiencyMetric.kt
│   │   │   │   └── dao/                        # Data access objects
│   │   │   │       ├── TaskDao.kt
│   │   │   │       ├── UserDao.kt
│   │   │   │       ├── AffirmationDao.kt
│   │   │   │       ├── BadgeDao.kt
│   │   │   │       ├── StreakDao.kt
│   │   │   │       └── EfficiencyMetricDao.kt
│   │   │   └── ui/
│   │   │       └── theme/                      # Jetpack Compose theming
│   │   │           ├── Theme.kt
│   │   │           ├── Color.kt
│   │   │           └── Type.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   └── test/                                   # Unit tests
├── build.gradle.kts                            # Module build configuration
├── proguard-rules.pro                          # ProGuard rules
└── .gitignore
```

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **WebSocket**: OkHttp WebSocket
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose
- **Preferences**: DataStore
- **Security**: Android Security Crypto
- **Coroutines**: Kotlin Coroutines
- **Testing**: JUnit, Espresso, Compose UI Testing

## Build Configuration

### Minimum SDK
- **minSdk**: 28 (Android 9.0 - Pie)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34

### Kotlin Configuration
- **JVM Target**: 17
- **Compose Compiler Version**: 1.5.3

## Key Features

### Phase 1: Project Setup & Infrastructure ✓
- [x] Android project with Kotlin and Jetpack Compose
- [x] Gradle build system configured
- [x] Room database infrastructure
- [x] Dependency injection setup
- [x] Project structure and package organization

### Phase 2: Core Data Models & Database
- [ ] Task data model and Room DAO
- [ ] User data model and Room DAO
- [ ] Affirmation data model and Room DAO
- [ ] Badge data model and Room DAO
- [ ] Streak data model and Room DAO
- [ ] EfficiencyMetric data model and Room DAO
- [ ] Task JSON serializer/parser
- [ ] Affirmation JSON serializer/parser
- [ ] Sync queue table and DAO

### Phase 3+: Additional Features
- Family member switching
- Task management core
- Daily focus view
- Timer functionality
- Progress tracking
- Affirmations & gamification
- Cloud synchronization
- Real-time updates
- Offline capability
- Settings & customization
- Authentication
- Accessibility
- Error handling
- Testing & QA

## Building the Project

### Prerequisites
- Android Studio Flamingo or later
- JDK 17 or later
- Android SDK 34

### Build Commands

```bash
# Build debug APK
./gradlew :adhd-focus-app:assembleDebug

# Build release APK
./gradlew :adhd-focus-app:assembleRelease

# Run tests
./gradlew :adhd-focus-app:test

# Run instrumented tests
./gradlew :adhd-focus-app:connectedAndroidTest

# Clean build
./gradlew clean
```

## Dependencies

### Core Android
- androidx.core:core-ktx:1.12.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
- androidx.activity:activity-compose:1.8.0

### Jetpack Compose
- androidx.compose.ui:ui
- androidx.compose.ui:ui-graphics
- androidx.compose.ui:ui-tooling-preview
- androidx.compose.material3:material3
- androidx.compose.material:material-icons-extended

### Database
- androidx.room:room-runtime:2.6.0
- androidx.room:room-ktx:2.6.0

### Networking
- com.squareup.retrofit2:retrofit:2.9.0
- com.squareup.retrofit2:converter-gson:2.9.0
- com.squareup.okhttp3:okhttp:4.11.0
- com.google.code.gson:gson:2.10.1

### Dependency Injection
- com.google.dagger:hilt-android:2.48

### Navigation
- androidx.navigation:navigation-compose:2.7.4

### Preferences
- androidx.datastore:datastore-preferences:1.0.0

### Security
- androidx.security:security-crypto:1.1.0-alpha06

## Architecture

The app follows a layered architecture:

1. **UI Layer**: Jetpack Compose components
2. **ViewModel Layer**: State management with Hilt
3. **Domain Layer**: Business logic and use cases
4. **Data Layer**: Room database and DAOs
5. **Network Layer**: Retrofit and WebSocket

## Accessibility

The app is designed with ADHD users in mind:
- High-contrast visual indicators
- Clear typography with sufficient sizing
- Minimal visual clutter
- Haptic feedback support
- WCAG 2.1 AA compliance target

## Next Steps

1. Implement data models and DAOs (Phase 2)
2. Create family member switching UI (Phase 3)
3. Build task management core (Phase 4)
4. Develop Daily Focus View (Phase 5)
5. Implement timer functionality (Phase 6)
6. Add progress tracking (Phase 7)
7. Implement affirmations and gamification (Phase 8)
8. Set up cloud synchronization (Phase 9)
9. Add real-time updates (Phase 10)
10. Implement offline capability (Phase 11)
11. Create settings UI (Phase 12)
12. Add authentication (Phase 13)
13. Implement accessibility features (Phase 14)
14. Add error handling (Phase 15)
15. Comprehensive testing (Phase 16)
16. Documentation and deployment (Phase 17)

## License

This project is part of the ADHD Focus App initiative.
