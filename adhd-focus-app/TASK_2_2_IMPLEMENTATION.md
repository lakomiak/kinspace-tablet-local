# Task 2.2: Implement User Data Model and Room DAO

## Overview
This task implements the User data model and Room DAO with comprehensive CRUD operations, query methods, type converters, and unit tests for household member management and user preferences.

## Implementation Summary

### 1. Enhanced User Entity (`User.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/model/User.kt`

**Changes**:
- Added database indices for optimized queries:
  - Single column indices: `householdId`, `email`, `role`, `createdAt`, `updatedAt`
  - Composite indices: `(householdId, role)` for efficient household member filtering
- Added validation in `init` block:
  - `householdId` cannot be blank
  - `email` cannot be blank and must contain "@"
  - `displayName` cannot be blank
- Removed duplicate `UserPreferences` and `Theme` definitions (moved to separate file)

**Fields**:
- `id`: UUID primary key
- `householdId`: Foreign key to household
- `email`: User email address
- `displayName`: User display name
- `avatarUrl`: Optional avatar URL
- `role`: UserRole enum (ADHD_USER, CAREGIVER, ADMIN)
- `isPinProtected`: Boolean flag for PIN protection
- `pinHash`: Optional hashed PIN
- `createdAt`: Timestamp of creation
- `updatedAt`: Timestamp of last update

### 2. UserPreferences Entity (`UserPreferences.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/model/UserPreferences.kt`

**New File**: Separated from User model for better organization

**Features**:
- Foreign key relationship to User (CASCADE delete)
- Validation in `init` block:
  - `userId` cannot be blank
  - `affirmationFrequency` must be between 1-5
  - `timerDefaultDuration` must be positive
  - `autoLogoutTimeout` must be non-negative
- JSON serialization support for complex types

**Fields**:
- `userId`: Primary key, foreign key to User
- `theme`: Theme enum (LIGHT, DARK)
- `visibleTodoGroups`: JSON serialized list of visible groups
- `notificationPreferences`: JSON serialized NotificationPreferences
- `dailyResetTime`: Time in HH:mm format
- `affirmationFrequency`: 1-5 scale
- `enableGamification`: Boolean flag
- `timerDefaultDuration`: Default timer duration in minutes
- `autoLogoutTimeout`: Auto-logout timeout in seconds (0 = disabled)

### 3. Enhanced UserDao (`UserDao.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/dao/UserDao.kt`

**CRUD Operations**:
- `insert(user: User)`: Insert new user
- `update(user: User)`: Update existing user
- `delete(user: User)`: Delete user
- `getUserById(userId: String)`: Get user by ID
- `getUserByEmail(email: String)`: Get user by email

**Household Queries**:
- `getUsersByHousehold(householdId: String)`: Flow of users in household
- `getUsersByHouseholdOnce(householdId: String)`: Single query for household users
- `getUsersByHouseholdSorted(householdId: String)`: Users sorted by display name

**Role-Based Queries**:
- `getUsersByRole(householdId: String, role: UserRole)`: Flow of users by role
- `getUsersByRoleOnce(householdId: String, role: UserRole)`: Single query by role
- `getFirstUserByRole(householdId: String, role: UserRole)`: Get first user with role

**PIN Protection Queries**:
- `getPinProtectedUsers(householdId: String)`: Flow of PIN-protected users
- `getPinProtectedUsersOnce(householdId: String)`: Single query for PIN-protected users
- `updatePinProtection(userId: String, isPinProtected: Boolean, pinHash: String?)`: Update PIN protection

**Count Operations**:
- `getUserCountByHousehold(householdId: String)`: Count users in household
- `getUserCountByRole(householdId: String, role: UserRole)`: Count users by role

**Utility Queries**:
- `getRecentUsers(householdId: String, limit: Int)`: Get recent users
- `deleteUserById(userId: String)`: Delete user by ID
- `deleteUsersByHousehold(householdId: String)`: Delete all users in household

### 4. UserPreferencesDao (`UserPreferencesDao.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/dao/UserPreferencesDao.kt`

**New File**: Dedicated DAO for user preferences management

**CRUD Operations**:
- `insert(preferences: UserPreferences)`: Insert preferences
- `update(preferences: UserPreferences)`: Update preferences
- `delete(preferences: UserPreferences)`: Delete preferences
- `getPreferencesByUserId(userId: String)`: Get preferences by user ID
- `getPreferencesByUserIdFlow(userId: String)`: Flow of preferences

**Individual Updates** (for efficient partial updates):
- `updateTheme(userId: String, theme: Theme)`
- `updateVisibleTodoGroups(userId: String, visibleTodoGroups: String)`
- `updateNotificationPreferences(userId: String, notificationPreferences: String)`
- `updateDailyResetTime(userId: String, dailyResetTime: String)`
- `updateAffirmationFrequency(userId: String, affirmationFrequency: Int)`
- `updateGamificationEnabled(userId: String, enableGamification: Boolean)`
- `updateTimerDefaultDuration(userId: String, timerDefaultDuration: Int)`
- `updateAutoLogoutTimeout(userId: String, autoLogoutTimeout: Int)`

**Filtering Queries**:
- `getPreferencesByTheme(theme: Theme)`: Get preferences by theme
- `getGamificationEnabledPreferences()`: Get preferences with gamification enabled
- `getAutoLogoutEnabledPreferences()`: Get preferences with auto-logout enabled

**Utility Queries**:
- `preferencesExist(userId: String)`: Check if preferences exist
- `deletePreferencesByUserId(userId: String)`: Delete preferences

### 5. Type Converters (`Converters.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/database/Converters.kt`

**New Converters Added**:
- `Theme` ↔ `String`: Enum conversion
- `UserRole` ↔ `String`: Enum conversion
- `TaskStatus` ↔ `String`: Enum conversion
- `SyncStatus` ↔ `String`: Enum conversion
- `NotificationPreferences` ↔ `String`: JSON serialization/deserialization
- `List<String>` ↔ `String`: JSON serialization/deserialization

**Existing Converters**:
- `Instant` ↔ `Long`: Timestamp conversion
- `LocalDate` ↔ `String`: Date conversion

### 6. Database Configuration (`AdhdfocusDatabase.kt`)
**Location**: `src/main/kotlin/com/adhdfocus/app/data/database/AdhdfocusDatabase.kt`

**Changes**:
- Added `UserPreferences` entity to database
- Added `UserPreferencesDao` abstract method
- Updated entity list to include UserPreferences

### 7. Build Configuration (`build.gradle.kts`)
**Changes**:
- Added `kotlin("plugin.serialization")` plugin
- Added `kotlinx-serialization-json` dependency (version 1.6.0)

### 8. Unit Tests

#### UserDaoTest (`UserDaoTest.kt`)
**Location**: `src/androidTest/kotlin/com/adhdfocus/app/data/dao/UserDaoTest.kt`

**Test Coverage** (40+ tests):
- **CRUD Operations**: Insert, update, delete, retrieve
- **Household Queries**: Get users by household, sorted by name
- **Email Queries**: Get user by email
- **Role-Based Queries**: Filter by role, get first user by role
- **PIN Protection**: Get PIN-protected users, update PIN protection
- **Count Operations**: Count users by household and role
- **Delete Operations**: Delete by ID, delete by household
- **Recent Users**: Get recent users with limit
- **Validation**: Test all validation rules
- **Multiple Households**: Verify data isolation
- **Role Distribution**: Test multiple roles in household

#### UserPreferencesDaoTest (`UserPreferencesDaoTest.kt`)
**Location**: `src/androidTest/kotlin/com/adhdfocus/app/data/dao/UserPreferencesDaoTest.kt`

**Test Coverage** (35+ tests):
- **CRUD Operations**: Insert, update, delete, retrieve
- **Individual Updates**: Test each preference field update
- **Theme Filtering**: Get preferences by theme
- **Gamification Filtering**: Get gamification-enabled preferences
- **Auto-Logout Filtering**: Get auto-logout-enabled preferences
- **Existence Checks**: Verify preference existence
- **Default Values**: Test default preference values
- **Multiple Users**: Test preferences for multiple users
- **Validation**: Test all validation rules
- **Preference Ranges**: Test valid ranges for frequency and duration

## Database Schema

### users table
```sql
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  householdId TEXT NOT NULL,
  email TEXT NOT NULL,
  displayName TEXT NOT NULL,
  avatarUrl TEXT,
  role TEXT NOT NULL,
  isPinProtected BOOLEAN NOT NULL DEFAULT 0,
  pinHash TEXT,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL
)

CREATE INDEX idx_users_householdId ON users(householdId)
CREATE INDEX idx_users_email ON users(email)
CREATE INDEX idx_users_role ON users(role)
CREATE INDEX idx_users_householdId_role ON users(householdId, role)
```

### user_preferences table
```sql
CREATE TABLE user_preferences (
  userId TEXT PRIMARY KEY,
  theme TEXT NOT NULL,
  visibleTodoGroups TEXT NOT NULL,
  notificationPreferences TEXT NOT NULL,
  dailyResetTime TEXT NOT NULL,
  affirmationFrequency INTEGER NOT NULL,
  enableGamification BOOLEAN NOT NULL,
  timerDefaultDuration INTEGER NOT NULL,
  autoLogoutTimeout INTEGER NOT NULL,
  FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
)
```

## Key Features

### 1. Household Member Management
- Support for multiple household members with different roles
- Role-based access control (ADHD_USER, CAREGIVER, ADMIN)
- PIN protection for sensitive profiles
- Efficient queries for household member listing

### 2. User Preferences
- Per-user customization of app experience
- Theme selection (light/dark)
- Notification preferences
- Gamification toggles
- Timer defaults
- Auto-logout configuration

### 3. Data Validation
- All required fields validated in entity constructors
- Email format validation
- Preference range validation (1-5 for frequency)
- Positive duration validation

### 4. Query Optimization
- Composite indices for common query patterns
- Separate Flow and suspend queries for flexibility
- Efficient filtering by household, role, and email
- Count operations for statistics

### 5. Type Safety
- Enum-based role and theme selection
- JSON serialization for complex types
- Type converters for database storage

## Testing Strategy

### Unit Tests
- 40+ tests for UserDao covering all CRUD and query operations
- 35+ tests for UserPreferencesDao covering all preference operations
- Comprehensive validation testing
- Edge case testing (empty results, non-existent records)
- Multiple household isolation testing

### Test Execution
Tests can be run using:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.adhdfocus.app.data.dao.UserDaoTest,com.adhdfocus.app.data.dao.UserPreferencesDaoTest
```

## Compliance with Design Document

This implementation fully complies with the design document specifications:

1. **User Model** (Design Section: Data Models → User Model)
   - All required fields implemented
   - Proper validation
   - Database constraints and indices

2. **Room DAO** (Design Section: Local Storage Schema)
   - CRUD operations implemented
   - Query methods for filtering and sorting
   - Proper type converters for complex types
   - Database constraints and indices

3. **Household Member Management** (Design Section: Family Member Switcher)
   - Support for multiple household members
   - Role-based access control
   - PIN protection functionality
   - Per-member preferences storage

4. **User Preferences** (Design Section: Settings and Customization)
   - Theme selection
   - Notification preferences
   - Gamification toggles
   - Timer defaults
   - Auto-logout configuration

## Files Created/Modified

### Created Files
1. `src/main/kotlin/com/adhdfocus/app/data/model/UserPreferences.kt` - UserPreferences entity
2. `src/main/kotlin/com/adhdfocus/app/data/dao/UserPreferencesDao.kt` - UserPreferencesDao interface
3. `src/androidTest/kotlin/com/adhdfocus/app/data/dao/UserDaoTest.kt` - UserDao tests
4. `src/androidTest/kotlin/com/adhdfocus/app/data/dao/UserPreferencesDaoTest.kt` - UserPreferencesDao tests

### Modified Files
1. `src/main/kotlin/com/adhdfocus/app/data/model/User.kt` - Enhanced with validation and indices
2. `src/main/kotlin/com/adhdfocus/app/data/dao/UserDao.kt` - Added query methods
3. `src/main/kotlin/com/adhdfocus/app/data/database/Converters.kt` - Added type converters
4. `src/main/kotlin/com/adhdfocus/app/data/database/AdhdfocusDatabase.kt` - Added UserPreferences entity
5. `build.gradle.kts` - Added kotlinx-serialization dependency

## Next Steps

The implementation is complete and ready for:
1. Integration with UserRepository for business logic
2. Implementation of family member switching UI (Task 3.x)
3. Implementation of settings UI (Task 12.x)
4. Integration with authentication system (Task 13.x)

All code compiles without errors and is ready for testing.
