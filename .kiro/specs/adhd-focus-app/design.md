# ADHD Focus App - Design Document

## Overview

The ADHD Focus App is a specialized Android tablet interface designed to support family members with ADHD in managing daily tasks through a distraction-free, visually engaging experience. The app combines task management, real-time progress tracking, and gamification elements specifically tailored to ADHD users, while maintaining seamless synchronization with the existing calendar-cloud infrastructure and desktop calendar application.

### Design Goals

1. **Cognitive Clarity**: Present information in a way that minimizes cognitive load and decision fatigue
2. **Visual Hierarchy**: Use high-contrast indicators and clear visual cues to communicate task status at a glance
3. **Positive Reinforcement**: Provide immediate, varied affirmations and achievement recognition
4. **Real-Time Synchronization**: Ensure tasks stay in sync across devices without requiring manual intervention
5. **Offline-First**: Enable full functionality even without network connectivity
6. **Tablet Optimization**: Leverage larger screen real estate with appropriately sized touch targets and layouts
7. **Family Integration**: Support household coordination while maintaining individual focus

### Key Differentiators

- **ADHD-Specific UX**: High-contrast visuals, affirmations, and gamification designed specifically for ADHD users
- **Family-Focused Sync**: Real-time bidirectional synchronization with family members' devices
- **Desktop Integration**: Seamless integration with existing Electron-based calendar application
- **Offline Capability**: Full offline functionality with automatic sync when connectivity returns
- **Measurable Progress**: Streaks, efficiency metrics, and completion rates provide concrete motivation

---

## Architecture

### System Overview

The ADHD Focus App follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)               │
│  Daily Focus View | Task Details | Timer | Achievements    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              State Management & ViewModel Layer             │
│  FocusViewModel | TaskViewModel | TimerViewModel           │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Business Logic & Domain Layer                  │
│  TaskManager | AffirmationEngine | BadgeSystem             │
│  StreakTracker | EfficiencyCalculator | SyncManager        │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│           Data Access & Persistence Layer                   │
│  Local Database (Room) | Sync Queue | Cache Manager        │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Network & Cloud Integration                    │
│  REST API Client | WebSocket Manager | Auth Manager        │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **User Action** → UI Layer captures interaction
2. **ViewModel** → Updates state and triggers business logic
3. **Domain Layer** → Executes business rules (task creation, completion, sync)
4. **Data Layer** → Persists changes locally and queues for sync
5. **Network Layer** → Synchronizes with calendar-cloud when connectivity available
6. **Real-Time Updates** → WebSocket receives updates from other devices
7. **UI Refresh** → State changes trigger UI recomposition

---

## Components and Interfaces

### 1. Daily Focus View Component

**Purpose**: Primary interface displaying today's tasks with real-time progress tracking

**Key Features**:
- Task list organized by Todo_Group with visual separation
- Real-time completion percentage and task count
- Current streak display with visual emphasis
- High-contrast task status indicators (incomplete/in-progress/completed)
- Smooth scrolling performance (60 FPS)
- Offline capability with cached data

**Data Inputs**:
- Today's tasks from local database
- Completion status for each task
- Current streak count
- Sync status indicator

**User Interactions**:
- Tap task to view details
- Swipe to mark complete (optional quick action)
- Tap "+" button to create new task
- Access settings/achievements via bottom navigation

### 2. Task Management System

**Purpose**: Handle task creation, updates, completion, and cloud synchronization

**Key Responsibilities**:
- Accept task input (title, description, estimated duration, Todo_Group)
- Display pending-sync indicator for offline changes
- Queue changes for synchronization
- Resolve sync conflicts using timestamp-based resolution
- Trigger affirmation and badge evaluation on completion
- Persist all changes locally

**Interfaces**:
```
TaskManager:
  - createTask(title, description?, duration?, todoGroup) → Task
  - updateTask(taskId, updates) → Task
  - completeTask(taskId) → Task
  - deleteTask(taskId) → void
  - getTasksForToday() → List<Task>
  - syncPendingChanges() → SyncResult
```

### 3. Timer Interface Component

**Purpose**: Provide visual and audio feedback for task duration tracking

**Key Features**:
- Large, easy-to-read countdown display
- Animated progress ring showing elapsed time
- Visual warnings at 50% and 90% duration
- Audio notification on completion
- Pause/resume/cancel controls
- Background timer with system notification
- Option to extend or mark complete on timer end

**Visual Feedback**:
- 0-50%: Green progress ring
- 50-90%: Yellow/orange progress ring with color transition
- 90-100%: Red progress ring with increased animation intensity
- Completion: Celebratory animation with audio chime

### 4. Progress Tracking System

**Purpose**: Calculate and display real-time progress metrics

**Key Metrics**:
- Completion percentage (tasks completed / total tasks)
- Task count display (e.g., "5 of 8 complete")
- Current streak (consecutive days at 100%)
- Best streak (historical maximum)
- Day completion status

**Behavior**:
- Updates in real-time as tasks are completed
- Resets daily task list while preserving streak
- Triggers day completion affirmation when all tasks done
- Persists data for offline access

### 5. Affirmation Engine

**Purpose**: Deliver positive reinforcement at key moments

**Affirmation Types**:
- **Task Completion**: Varied messages on individual task completion
- **Day Completion**: Enhanced affirmation when all tasks done
- **Streak Milestone**: Special messages at 3+, 7+, 14+, 30+ day streaks

**Behavior**:
- Displays for 2-3 seconds before auto-dismissing
- Allows manual dismissal
- Varies messages to avoid repetition
- Uses age-appropriate, encouraging language
- Avoids patronizing tone for adult users

**Message Examples**:
- "Great job!"
- "You're on a roll!"
- "Awesome work!"
- "3-day streak! Keep it up!"
- "Perfect day! You crushed it!"

### 6. Badge and Achievement System

**Purpose**: Recognize milestones and maintain long-term motivation

**Badge Categories**:
- **Daily Milestones**: First Task Complete, 5-Task Day, Perfect Day
- **Weekly Achievements**: Week Warrior, Perfect Week
- **Streak Milestones**: 3-Day Streak, 7-Day Streak, 30-Day Streak
- **Efficiency Badges**: Speed Demon (20% faster), Consistent Performer

**Badge Display**:
- Notification with animation and sound on earn
- Dedicated Achievements section showing all earned badges
- Progress indicators for locked badges
- Sync with calendar-cloud for cross-device visibility

### 7. Cloud Sync Manager

**Purpose**: Manage bidirectional synchronization with calendar-cloud

**Key Responsibilities**:
- Establish and maintain WebSocket connection
- Queue local changes for synchronization
- Send pending changes via REST API
- Receive and apply remote updates
- Resolve conflicts using timestamp-based resolution
- Maintain sync status indicator
- Implement exponential backoff for failed attempts

**Sync Flow**:
1. Local change → Queue with timestamp
2. Network available → Send via REST API
3. Remote update received → Apply locally with conflict resolution
4. WebSocket signal → Fetch updates and refresh UI
5. Offline → Queue changes, sync on reconnection

### 8. Data Persistence Layer

**Purpose**: Store tasks and sync state locally for offline capability

**Storage Components**:
- **Local Database (Room)**: Tasks, completion history, settings
- **Sync Queue**: Pending changes with timestamps
- **Cache**: Recently accessed data for quick retrieval
- **Secure Storage**: Authentication tokens, sensitive data

**Data Retention**:
- 30+ days of task history
- Automatic cleanup after 90 days
- Encryption at rest using device secure storage

### 9. Authentication System

**Purpose**: Manage user authentication and household access

**Key Responsibilities**:
- Sign-in with household account
- Authenticate using calendar-cloud system
- Retrieve household ID and associated tasks
- Manage authentication tokens securely
- Auto-refresh expired tokens
- Handle sign-out and data clearing

**Token Management**:
- Store tokens in device secure storage
- Implement automatic refresh before expiration
- Prompt for re-authentication if refresh fails

### 10. Family Member Switcher

**Purpose**: Enable quick switching between household members on a shared device

**Key Features**:
- Quick-access switcher on Daily Focus View (top-left corner)
- Visual profile indicators with member name and avatar
- One-tap switching between household members
- Automatic profile loading with member-specific tasks and settings
- Session persistence (remember last selected member)
- Household member list with visual indicators
- Optional PIN/biometric protection for sensitive profiles

**Behavior**:
- Switcher displays current member name and avatar
- Tap to open member selection modal
- Modal shows all household members with avatars
- Tap member to switch (loads their tasks, settings, streaks, badges)
- Smooth transition animation between profiles
- Sync status maintained per member
- Offline changes queued per member

**Data Isolation**:
- Each member has isolated task view
- Streaks and badges are per-member
- Settings are per-member
- Affirmations are per-member
- Efficiency metrics are per-member
- Sync queue is per-member

**Performance**:
- Member switching completes within 500ms
- Profile data cached locally for instant switching
- No network required for switching (uses cached data)
- Background sync for each member's pending changes

### 11. Settings and Customization

**Purpose**: Allow users to personalize the app experience

**Customizable Settings**:
- Visible Todo_Groups in Daily_Focus_View
- Light/dark theme preference
- Notification preferences (sound, vibration, visual)
- Daily reset time
- Affirmation frequency and tone
- Gamification element toggles
- Timer default duration
- Family member switching preferences (PIN protection, auto-logout timeout)

**Persistence**:
- Store locally and sync with calendar-cloud
- Apply immediately without app restart
- Per-member settings stored separately

---

## Data Models

### Task Model

```
Task {
  id: String (UUID)
  householdId: String
  assignedUserId: String
  title: String
  description: String?
  todoGroup: String
  estimatedDurationMinutes: Int?
  actualDurationMinutes: Int?
  status: TaskStatus (INCOMPLETE | IN_PROGRESS | COMPLETED)
  createdAt: Timestamp
  updatedAt: Timestamp
  completedAt: Timestamp?
  syncStatus: SyncStatus (PENDING | SYNCED | CONFLICT)
  isDeleted: Boolean (soft delete)
}

enum TaskStatus {
  INCOMPLETE,
  IN_PROGRESS,
  COMPLETED
}

enum SyncStatus {
  PENDING,
  SYNCED,
  CONFLICT
}
```

### Affirmation Model

```
Affirmation {
  id: String (UUID)
  type: AffirmationType
  message: String
  tone: AffirmationTone
  ageAppropriatenessLevel: Int (1-5, where 5 is most mature)
  createdAt: Timestamp
}

enum AffirmationType {
  TASK_COMPLETION,
  DAY_COMPLETION,
  STREAK_MILESTONE
}

enum AffirmationTone {
  ENCOURAGING,
  CELEBRATORY,
  MOTIVATIONAL,
  SUPPORTIVE
}
```

### Badge Model

```
Badge {
  id: String (UUID)
  householdId: String
  userId: String
  badgeType: String
  name: String
  description: String
  iconUrl: String
  earnedAt: Timestamp
  progress: Int? (for locked badges)
  isLocked: Boolean
}
```

### User Model

```
User {
  id: String (UUID)
  householdId: String
  email: String
  displayName: String
  avatarUrl: String?
  role: UserRole (ADHD_USER | CAREGIVER | ADMIN)
  preferences: UserPreferences
  isPinProtected: Boolean
  pinHash: String? (hashed PIN for protection)
  createdAt: Timestamp
  updatedAt: Timestamp
}

enum UserRole {
  ADHD_USER,
  CAREGIVER,
  ADMIN
}

UserPreferences {
  theme: Theme (LIGHT | DARK)
  visibleTodoGroups: List<String>
  notificationPreferences: NotificationPreferences
  dailyResetTime: String (HH:mm format)
  affirmationFrequency: Int (1-5)
  enableGamification: Boolean
  timerDefaultDuration: Int (minutes)
  autoLogoutTimeout: Int (minutes, 0 = disabled)
}
```

### Household Model

```
Household {
  id: String (UUID)
  name: String
  members: List<User>
  tasks: List<Task>
  createdAt: Timestamp
  updatedAt: Timestamp
}
```

### Streak Model

```
Streak {
  id: String (UUID)
  userId: String
  householdId: String
  currentCount: Int
  bestCount: Int
  lastCompletionDate: Date
  startDate: Date
  updatedAt: Timestamp
}
```

### Efficiency Metric Model

```
EfficiencyMetric {
  id: String (UUID)
  taskId: String
  userId: String
  householdId: String
  estimatedDurationMinutes: Int
  actualDurationMinutes: Int
  efficiencyPercentage: Float (actual / estimated * 100)
  completedAt: Timestamp
}
```

---

## API Integration Points

### REST API Endpoints (calendar-cloud)

**Task Management**:
- `POST /api/households/{householdId}/tasks` - Create task
- `GET /api/households/{householdId}/tasks` - Fetch tasks
- `PUT /api/households/{householdId}/tasks/{taskId}` - Update task
- `DELETE /api/households/{householdId}/tasks/{taskId}` - Delete task

**Sync Operations**:
- `POST /api/households/{householdId}/sync` - Batch sync pending changes
- `GET /api/households/{householdId}/sync/status` - Check sync status

**Authentication**:
- `POST /api/auth/login` - Sign in
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/logout` - Sign out

### WebSocket Events (calendar-cloud)

**Real-Time Updates**:
- `task.created` - New task added by family member
- `task.updated` - Task modified by family member
- `task.deleted` - Task removed by family member
- `sync.signal` - Signal to fetch updates
- `connection.established` - WebSocket connected
- `connection.lost` - WebSocket disconnected

---

## Local Storage Schema

### Room Database Tables

**tasks**
```
CREATE TABLE tasks (
  id TEXT PRIMARY KEY,
  householdId TEXT NOT NULL,
  assignedUserId TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  todoGroup TEXT NOT NULL,
  estimatedDurationMinutes INTEGER,
  actualDurationMinutes INTEGER,
  status TEXT NOT NULL,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL,
  completedAt INTEGER,
  syncStatus TEXT NOT NULL,
  isDeleted BOOLEAN NOT NULL DEFAULT 0
)
```

**users**
```
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
```

**user_preferences**
```
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
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

**current_user**
```
CREATE TABLE current_user (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  householdId TEXT NOT NULL,
  lastSwitchTime INTEGER NOT NULL,
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

**sync_queue**
```
CREATE TABLE sync_queue (
  id TEXT PRIMARY KEY,
  taskId TEXT NOT NULL,
  userId TEXT NOT NULL,
  operation TEXT NOT NULL,
  payload TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  retryCount INTEGER DEFAULT 0,
  FOREIGN KEY(taskId) REFERENCES tasks(id),
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

**streaks**
```
CREATE TABLE streaks (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  householdId TEXT NOT NULL,
  currentCount INTEGER NOT NULL,
  bestCount INTEGER NOT NULL,
  lastCompletionDate INTEGER NOT NULL,
  startDate INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL,
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

**badges**
```
CREATE TABLE badges (
  id TEXT PRIMARY KEY,
  householdId TEXT NOT NULL,
  userId TEXT NOT NULL,
  badgeType TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  earnedAt INTEGER NOT NULL,
  progress INTEGER,
  isLocked BOOLEAN NOT NULL,
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

**efficiency_metrics**
```
CREATE TABLE efficiency_metrics (
  id TEXT PRIMARY KEY,
  taskId TEXT NOT NULL,
  userId TEXT NOT NULL,
  householdId TEXT NOT NULL,
  estimatedDurationMinutes INTEGER,
  actualDurationMinutes INTEGER,
  efficiencyPercentage REAL,
  completedAt INTEGER NOT NULL,
  FOREIGN KEY(taskId) REFERENCES tasks(id),
  FOREIGN KEY(userId) REFERENCES users(id)
)
```

---

## State Management

### ViewModel Architecture

**FocusViewModel** (Daily Focus View state)
- `todaysTasks: StateFlow<List<Task>>`
- `completionPercentage: StateFlow<Int>`
- `currentStreak: StateFlow<Int>`
- `syncStatus: StateFlow<SyncStatus>`
- `currentUser: StateFlow<User>`
- `loadTodaysTasks()`
- `refreshFromCloud()`
- `switchUser(userId)`

**FamilyMemberSwitcherViewModel** (Family member switching state)
- `householdMembers: StateFlow<List<User>>`
- `currentUser: StateFlow<User>`
- `isModalOpen: StateFlow<Boolean>`
- `loadHouseholdMembers()`
- `switchToMember(userId, pin?)`
- `openMemberSelector()`
- `closeMemberSelector()`
- `validatePin(pin): Boolean`

**TaskViewModel** (Task detail state)
- `selectedTask: StateFlow<Task?>`
- `isEditing: StateFlow<Boolean>`
- `currentUser: StateFlow<User>`
- `createTask(title, description, duration, group)`
- `updateTask(updates)`
- `completeTask()`
- `deleteTask()`

**TimerViewModel** (Timer state)
- `timerDuration: StateFlow<Int>`
- `timeRemaining: StateFlow<Int>`
- `isRunning: StateFlow<Boolean>`
- `progress: StateFlow<Float>`
- `startTimer(duration)`
- `pauseTimer()`
- `resumeTimer()`
- `cancelTimer()`

**AffirmationViewModel** (Affirmation display state)
- `currentAffirmation: StateFlow<Affirmation?>`
- `isVisible: StateFlow<Boolean>`
- `displayAffirmation(type)`
- `dismissAffirmation()`

**AchievementViewModel** (Badges and streaks state)
- `earnedBadges: StateFlow<List<Badge>>`
- `lockedBadges: StateFlow<List<Badge>>`
- `currentStreak: StateFlow<Int>`
- `bestStreak: StateFlow<Int>`
- `currentUser: StateFlow<User>`
- `loadAchievements()`

---

## UI/UX Patterns for ADHD Accessibility

### Visual Design Principles

1. **High Contrast**: Task status indicators use distinct colors (red/yellow/green)
2. **Clear Hierarchy**: Most important information (completion %, streak) at top
3. **Minimal Clutter**: Only essential UI elements visible in Daily Focus View
4. **Consistent Iconography**: Same icons used consistently across app
5. **Purposeful Animation**: Animations draw attention to important updates without overwhelming

### Color Scheme

**Light Theme**:
- Background: Clean white (#FFFFFF)
- Incomplete tasks: Bold red (#E53935)
- In-progress tasks: Warm orange (#FB8C00)
- Completed tasks: Fresh green (#43A047)
- Text: Dark gray (#212121)
- Accents: Vibrant blue (#1E88E5)

**Dark Theme**:
- Background: Deep gray (#121212)
- Incomplete tasks: Bright red (#FF5252)
- In-progress tasks: Warm orange (#FFB74D)
- Completed tasks: Light green (#66BB6A)
- Text: Light gray (#FFFFFF)
- Accents: Light blue (#64B5F6)

### Typography

- **Headlines**: 24-28sp, bold, high contrast
- **Body Text**: 16-18sp, regular, sufficient line spacing
- **Small Text**: 14sp minimum, never below 12sp
- **Line Height**: 1.5x for improved readability

### Touch Targets

- Minimum 48dp x 48dp for all interactive elements
- Tablet optimization: 56-64dp for primary actions
- Adequate spacing between targets to prevent accidental taps

### Accessibility Features

- Screen reader support with descriptive labels
- Keyboard navigation for all interactive elements
- Text scaling support up to 200%
- Haptic feedback for important actions
- Customizable animation speed
- Captions for audio content

---

## Navigation Structure

### Top Bar (Primary)
- Family Member Switcher (left) - Quick access to switch between household members
- Sync Status Indicator (right) - Shows current sync state
- Settings/Menu (right) - Access to app settings

### Bottom Navigation (Primary)
- Daily Focus (home icon)
- Achievements (trophy icon)
- Settings (gear icon)

### Daily Focus View Hierarchy
- Family Member Switcher (top-left)
- Today's tasks (organized by Todo_Group)
- Task detail view (on tap)
- Timer interface (when timer active)
- Create task modal (on "+" tap)

### Family Member Selection Modal
- List of all household members with avatars
- Current member highlighted
- One-tap switching
- Optional PIN entry for protected profiles
- Back button to return to Daily Focus

### Achievements View
- Earned badges with dates
- Locked badges with progress
- Streak display with history
- Efficiency metrics chart

### Settings View
- Visible Todo_Groups toggle
- Theme selection
- Notification preferences
- Daily reset time
- Affirmation settings
- Gamification toggles
- Timer defaults
- Family member switching preferences (PIN protection, auto-logout timeout)

---

## Error Handling Strategy

### Network Errors

**Transient Errors** (timeout, temporary unavailability):
- Display "Syncing..." indicator
- Implement exponential backoff (1s, 2s, 4s, 8s, 16s max)
- Automatically retry without user intervention
- Queue changes for later sync

**Persistent Errors** (auth failure, server error):
- Display user-friendly error message
- Offer manual retry option
- Log error for debugging
- Preserve task data for recovery

### Sync Conflicts

**Resolution Strategy**:
- Use timestamp-based resolution (most recent wins)
- If timestamps equal, prefer local version
- Log conflict for debugging
- Notify user if manual intervention needed

### Data Errors

**Invalid Task Data**:
- Validate required fields (id, title, householdId)
- Use default values for optional fields
- Log validation errors
- Prevent corrupted data from persisting

**Storage Errors**:
- Display warning if device storage low
- Offer cleanup of old task data (>90 days)
- Implement graceful degradation

### App Crashes

**Recovery**:
- Preserve unsaved data before crash
- Restore state on restart
- Notify user of recovery
- Log crash details for debugging

---

## Testing Strategy

### Unit Testing Approach

**Focus Areas**:
- Task creation and validation
- Completion percentage calculation
- Streak counting logic
- Efficiency metric calculation
- Affirmation message selection
- Badge earning logic
- Sync conflict resolution
- Data serialization/deserialization

**Test Examples**:
- Creating a task with valid data succeeds
- Creating a task with missing title fails
- Completing all tasks increments streak
- Failing to complete one task resets streak
- Efficiency calculated correctly (actual/estimated)
- Affirmation messages vary and don't repeat
- Sync conflict resolved by timestamp

### Property-Based Testing Approach

**Configuration**:
- Minimum 100 iterations per property test
- Random task generation with valid data
- Random user and household generation
- Random timing and network conditions

**Property Test Tags**:
- Format: `Feature: adhd-focus-app, Property {number}: {property_text}`
- Each property maps to one design correctness property
- Tests verify universal properties across all inputs

### Integration Testing

**Focus Areas**:
- Task creation → sync → desktop calendar display
- Real-time update reception → UI refresh
- Offline changes → sync on reconnection
- Authentication → household task loading
- Timer completion → affirmation display → badge evaluation

### Performance Testing

**Benchmarks**:
- Daily Focus View loads within 1 second
- Task list scrolls at 60 FPS
- Timer updates at least once per second
- Button tap feedback within 100ms
- Memory usage stays below 150MB

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptance Criteria Testing Prework

**Requirement 1: Daily Focus View**

1.1. WHEN the ADHD_User opens the app, THE Daily_Focus_View SHALL display only today's tasks from all Todo_Groups
  - Thoughts: This is a rule about what should be displayed. We can generate random tasks with various dates, then verify that only today's tasks appear in the view.
  - Testable: yes - property

1.2. THE Daily_Focus_View SHALL show task count, completion percentage, and current Streak prominently at the top
  - Thoughts: This is about UI layout and information display. We can verify that these elements are present and visible.
  - Testable: yes - example

1.3-1.5. WHEN a task is incomplete/in-progress/completed, THE Daily_Focus_View SHALL display distinct Visual_Cues
  - Thoughts: This is a rule about visual indicators for all tasks. We can generate tasks with different statuses and verify the correct visual cue is applied.
  - Testable: yes - property

1.6. THE Daily_Focus_View SHALL organize tasks by Todo_Group with clear section headers
  - Thoughts: This is a rule about organization. We can generate tasks with different groups and verify they're organized correctly.
  - Testable: yes - property

1.7. WHEN the ADHD_User scrolls through tasks, THE Daily_Focus_View SHALL maintain smooth performance
  - Thoughts: This is a performance requirement that's hard to test as a property. We'd need performance benchmarks.
  - Testable: no

1.8. THE Daily_Focus_View SHALL be accessible offline
  - Thoughts: This is a rule about offline capability. We can verify that cached tasks display when offline.
  - Testable: yes - property

**Requirement 2: Task Management with Cloud Sync**

2.1. WHEN the ADHD_User creates a new task, THE Task_Manager SHALL accept required fields
  - Thoughts: This is about input validation. We can test that valid inputs are accepted and invalid ones rejected.
  - Testable: yes - property

2.2. WHEN a task is created locally, THE Task_Manager SHALL display it with a pending-sync indicator
  - Thoughts: This is a rule about local task display. We can verify pending tasks show the indicator.
  - Testable: yes - property

2.3. WHEN network connectivity is available, THE Task_Manager SHALL synchronize pending tasks
  - Thoughts: This is a rule about sync behavior. We can verify that pending tasks are sent when network available.
  - Testable: yes - property

2.4. WHEN a task is updated remotely, THE Task_Manager SHALL receive and refresh within 2 seconds
  - Thoughts: This is a timing requirement. We can verify updates are applied, but the 2-second timing is a performance benchmark.
  - Testable: yes - property (for update application, not timing)

2.5. WHEN the ADHD_User marks a task complete, THE Task_Manager SHALL update status and queue for sync
  - Thoughts: This is a rule about task completion. We can verify status changes and sync queuing.
  - Testable: yes - property

2.6. WHEN a task is completed, THE Task_Manager SHALL trigger Affirmation and Badge evaluation
  - Thoughts: This is a rule about side effects. We can verify these are triggered.
  - Testable: yes - property

2.7. WHEN network is restored, THE Task_Manager SHALL sync and resolve conflicts by timestamp
  - Thoughts: This is a rule about conflict resolution. We can verify timestamp-based resolution works.
  - Testable: yes - property

2.8. THE Task_Manager SHALL persist all task data locally
  - Thoughts: This is a rule about persistence. We can verify data survives app restart.
  - Testable: yes - property

**Requirement 3: Timer Functionality**

3.1-3.10. Timer display, visual feedback, audio notification, pause/resume
  - Thoughts: These are mostly UI/UX requirements about visual and audio feedback. Some are testable (timer countdown accuracy, state transitions), others are not (visual appearance, audio quality).
  - Testable: yes - property (for timer accuracy and state transitions), no (for visual/audio quality)

**Requirement 4: Progress Tracking**

4.1. THE Progress_Tracker SHALL calculate and display completion percentage in real-time
  - Thoughts: This is a rule about calculation. We can verify the percentage is correct for any task set.
  - Testable: yes - property

4.2. WHEN the ADHD_User completes a task, THE Progress_Tracker SHALL immediately update the percentage
  - Thoughts: This is a rule about real-time updates. We can verify updates happen on completion.
  - Testable: yes - property

4.3. THE Progress_Tracker SHALL display task count (e.g., "5 of 8 complete")
  - Thoughts: This is a rule about display. We can verify the count is correct.
  - Testable: yes - property

4.4. WHEN all tasks are completed, THE Progress_Tracker SHALL display "Day Complete" indicator
  - Thoughts: This is a rule about state. We can verify this indicator appears when all tasks done.
  - Testable: yes - property

4.5. THE Progress_Tracker SHALL track current Streak and display it prominently
  - Thoughts: This is a rule about streak tracking. We can verify streak is calculated correctly.
  - Testable: yes - property

4.6. WHEN all tasks completed, THE Progress_Tracker SHALL trigger Day_Completion_Affirmation
  - Thoughts: This is a rule about side effects. We can verify the affirmation is triggered.
  - Testable: yes - property

4.7. THE Progress_Tracker SHALL persist completion data
  - Thoughts: This is a rule about persistence. We can verify data survives app restart.
  - Testable: yes - property

4.8. WHEN viewing on a new day, THE Progress_Tracker SHALL reset daily list while preserving Streak
  - Thoughts: This is a rule about daily reset. We can verify tasks reset but streak persists.
  - Testable: yes - property

**Requirement 5: Affirmations**

5.1. WHEN task completed, THE Affirmation_Engine SHALL display a positive message
  - Thoughts: This is a rule about affirmation display. We can verify an affirmation is shown.
  - Testable: yes - property

5.2. THE Affirmation_Engine SHALL vary messages to avoid repetition
  - Thoughts: This is a rule about message variety. We can verify different messages are used.
  - Testable: yes - property

5.3. WHEN all tasks completed, THE Affirmation_Engine SHALL display enhanced Day_Completion_Affirmation
  - Thoughts: This is a rule about special affirmations. We can verify enhanced affirmation on day completion.
  - Testable: yes - property

5.4. THE Affirmation_Engine SHALL display affirmations for 2-3 seconds
  - Thoughts: This is a timing requirement. We can verify affirmations are shown and dismissed.
  - Testable: yes - property

5.5. WHEN Streak is 3+, THE Affirmation_Engine SHALL acknowledge it
  - Thoughts: This is a rule about streak-aware affirmations. We can verify streak is mentioned.
  - Testable: yes - property

5.6-5.7. Age-appropriate, non-patronizing language
  - Thoughts: These are subjective quality requirements about tone and appropriateness.
  - Testable: no

**Requirement 6: Badges**

6.1. THE Badge_System SHALL award badges for specific milestones
  - Thoughts: This is a rule about badge earning. We can verify badges are awarded at milestones.
  - Testable: yes - property

6.2. WHEN badge earned, THE Badge_System SHALL display notification
  - Thoughts: This is a rule about badge notification. We can verify notification is shown.
  - Testable: yes - property

6.3. THE Badge_System SHALL display earned badges in Achievements section
  - Thoughts: This is a rule about badge display. We can verify earned badges appear.
  - Testable: yes - property

6.4. THE Badge_System SHALL show progress toward next achievement
  - Thoughts: This is a rule about progress display. We can verify progress is calculated correctly.
  - Testable: yes - property

6.5. WHEN viewing Achievements, THE Badge_System SHALL display all earned badges with dates
  - Thoughts: This is a rule about badge history. We can verify all earned badges are shown.
  - Testable: yes - property

6.6. THE Badge_System SHALL display locked badges with hints
  - Thoughts: This is a rule about locked badge display. We can verify locked badges are shown.
  - Testable: yes - property

6.7. THE Badge_System SHALL sync badge data with calendar-cloud
  - Thoughts: This is a rule about sync. We can verify badges sync to cloud.
  - Testable: yes - property

6.8. THE Badge_System SHALL award badges based on: daily rate, weekly rate, Streak, efficiency
  - Thoughts: This is a rule about badge earning criteria. We can verify badges are awarded correctly.
  - Testable: yes - property

**Requirement 7: Streaks and Efficiency**

7.1. THE Streak_Tracker SHALL count consecutive days with 100% completion
  - Thoughts: This is a rule about streak calculation. We can verify streak counts correctly.
  - Testable: yes - property

7.2. WHEN all tasks completed, THE Streak_Tracker SHALL increment counter
  - Thoughts: This is a rule about streak increment. We can verify counter increments.
  - Testable: yes - property

7.3. WHEN not all tasks completed, THE Streak_Tracker SHALL reset to 0
  - Thoughts: This is a rule about streak reset. We can verify counter resets.
  - Testable: yes - property

7.4. THE Streak_Tracker SHALL display current Streak prominently
  - Thoughts: This is a display requirement. We can verify streak is shown.
  - Testable: yes - example

7.5. THE Streak_Tracker SHALL display historical Streak data
  - Thoughts: This is a display requirement. We can verify best streak is shown.
  - Testable: yes - example

7.6. THE Efficiency_Metric_Calculator SHALL calculate ratio of actual to estimated time
  - Thoughts: This is a rule about calculation. We can verify efficiency is calculated correctly.
  - Testable: yes - property

7.7. WHEN task completed, THE Efficiency_Metric_Calculator SHALL record actual duration
  - Thoughts: This is a rule about recording. We can verify duration is recorded.
  - Testable: yes - property

7.8. THE Efficiency_Metric_Calculator SHALL display efficiency as percentage
  - Thoughts: This is a display requirement. We can verify efficiency is shown.
  - Testable: yes - example

7.9. THE Efficiency_Metric_Calculator SHALL aggregate efficiency over time
  - Thoughts: This is a rule about aggregation. We can verify aggregation is correct.
  - Testable: yes - property

7.10. THE Efficiency_Metric_Calculator SHALL sync efficiency data
  - Thoughts: This is a rule about sync. We can verify efficiency syncs.
  - Testable: yes - property

**Requirement 19: Parser and Serializer for Task Data**

19.1. WHEN task serialized, THE Task_Serializer SHALL convert to valid JSON
  - Thoughts: This is a rule about serialization. We can verify JSON is valid.
  - Testable: yes - property

19.2. WHEN JSON received, THE Task_Parser SHALL parse into Task object
  - Thoughts: This is a rule about parsing. We can verify parsing works.
  - Testable: yes - property

19.3. IF invalid JSON provided, THEN THE Task_Parser SHALL return error
  - Thoughts: This is a rule about error handling. We can verify errors are returned.
  - Testable: yes - property

19.4. THE Task_Pretty_Printer SHALL format Task objects into valid JSON
  - Thoughts: This is a rule about formatting. We can verify JSON is valid.
  - Testable: yes - property

19.5. FOR ALL valid Task objects, parsing then printing then parsing SHALL produce equivalent object (round-trip)
  - Thoughts: This is a round-trip property. We can verify parse→print→parse produces same object.
  - Testable: yes - property

19.6. THE Task_Parser SHALL handle optional fields gracefully
  - Thoughts: This is a rule about optional field handling. We can verify defaults are used.
  - Testable: yes - property

19.7. THE Task_Parser SHALL validate required fields are present and non-empty
  - Thoughts: This is a rule about validation. We can verify validation works.
  - Testable: yes - property

19.8. THE Task_Serializer SHALL include all task metadata
  - Thoughts: This is a rule about serialization completeness. We can verify all fields are included.
  - Testable: yes - property

**Requirement 20: Parser and Serializer for Affirmation Data**

20.1-20.7. Similar to Requirement 19 for Affirmation objects
  - Thoughts: Same patterns as Task serialization/parsing.
  - Testable: yes - property (for round-trip and validation)

### Property Reflection

After analyzing all acceptance criteria, I've identified the following testable properties:

**Redundancy Analysis**:
- Task creation validation (2.1) and Task serialization validation (19.7) both test field validation - can be combined
- Streak increment (7.2) and Streak reset (7.3) are complementary, not redundant
- Efficiency calculation (7.6) and efficiency aggregation (7.9) are separate concerns
- Badge earning (6.1) and badge display (6.3) are separate concerns
- Sync operations (2.3, 2.7, 6.7, 7.10) can be generalized into one sync property

**Consolidated Properties**:
1. Task validation (combines 2.1, 19.7)
2. Task serialization round-trip (19.5)
3. Affirmation serialization round-trip (20.5)
4. Daily task filtering (1.1)
5. Task status visual cues (1.3-1.5)
6. Task organization by group (1.6)
7. Offline task caching (1.8)
8. Pending sync indicator (2.2)
9. Cloud sync on connectivity (2.3, 6.7, 7.10)
10. Remote update application (2.4)
11. Task completion triggers side effects (2.6)
12. Conflict resolution by timestamp (2.7)
13. Task persistence (2.8)
14. Completion percentage calculation (4.1)
15. Real-time progress update (4.2)
16. Task count display (4.3)
17. Day complete indicator (4.4)
18. Streak calculation (4.5, 7.1-7.3)
19. Day completion affirmation (4.6)
20. Completion data persistence (4.7)
21. Daily reset with streak preservation (4.8)
22. Affirmation on task completion (5.1)
23. Affirmation message variety (5.2)
24. Day completion affirmation (5.3)
25. Affirmation display duration (5.4)
26. Streak-aware affirmations (5.5)
27. Badge earning at milestones (6.1)
28. Badge notification (6.2)
29. Badge display in achievements (6.3)
30. Badge progress calculation (6.4)
31. Badge history display (6.5)
32. Locked badge display (6.6)
33. Efficiency calculation (7.6)
34. Efficiency recording (7.7)
35. Efficiency aggregation (7.9)

---

### Correctness Properties

**Property 1: Daily Task Filtering**

*For any* set of tasks with various dates, the Daily_Focus_View should display only tasks with today's date.

**Validates: Requirements 1.1**

**Property 2: Task Status Visual Cues**

*For any* task with a given status (incomplete, in-progress, or completed), the Daily_Focus_View should display the corresponding distinct visual cue (red/bold, yellow/orange, or green/dimmed).

**Validates: Requirements 1.3, 1.4, 1.5**

**Property 3: Task Organization by Todo_Group**

*For any* set of tasks with different Todo_Groups, the Daily_Focus_View should organize them by group with clear section headers and visual separation.

**Validates: Requirements 1.6**

**Property 4: Offline Task Caching**

*For any* set of tasks synced before going offline, the Daily_Focus_View should display the cached tasks when network connectivity is unavailable.

**Validates: Requirements 1.8, 12.2**

**Property 5: Task Validation**

*For any* task creation request, if all required fields (title, householdId, assignedUserId) are present and non-empty, the task should be created; otherwise, creation should fail with a descriptive error.

**Validates: Requirements 2.1, 19.7**

**Property 6: Pending Sync Indicator**

*For any* task created locally while offline, the task should display a pending-sync indicator until successfully synced to calendar-cloud.

**Validates: Requirements 2.2**

**Property 7: Cloud Sync on Connectivity**

*For any* pending changes queued locally, when network connectivity becomes available, all pending changes should be synchronized to calendar-cloud using the REST API.

**Validates: Requirements 2.3, 2.8, 6.7, 7.10**

**Property 8: Remote Update Application**

*For any* update received via WebSocket from calendar-cloud, the local task data should be updated and the Daily_Focus_View should refresh to reflect the change.

**Validates: Requirements 2.4, 11.1, 11.2**

**Property 9: Task Completion Side Effects**

*For any* task marked as completed, the system should trigger affirmation evaluation and badge evaluation.

**Validates: Requirements 2.6**

**Property 10: Sync Conflict Resolution**

*For any* conflict where both local and remote changes exist for the same task, the system should resolve it by preferring the version with the most recent timestamp.

**Validates: Requirements 2.7**

**Property 11: Completion Percentage Calculation**

*For any* set of tasks for today, the completion percentage should equal (completed tasks / total tasks) * 100, and should update in real-time as tasks are completed.

**Validates: Requirements 4.1, 4.2**

**Property 12: Task Count Display**

*For any* set of tasks for today, the display should show the correct count of completed tasks versus total tasks (e.g., "5 of 8 complete").

**Validates: Requirements 4.3**

**Property 13: Day Complete Indicator**

*For any* day where all tasks are completed, the Daily_Focus_View should display a "Day Complete" indicator with prominent visual emphasis.

**Validates: Requirements 4.4**

**Property 14: Streak Calculation**

*For any* user, the streak counter should increment by 1 when all tasks for a day are completed, and should reset to 0 if any day has incomplete tasks. The streak should represent consecutive days with 100% completion.

**Validates: Requirements 4.5, 7.1, 7.2, 7.3**

**Property 15: Day Completion Affirmation**

*For any* day where all tasks are completed, the system should trigger a Day_Completion_Affirmation with enhanced visual emphasis.

**Validates: Requirements 4.6, 5.3**

**Property 16: Completion Data Persistence**

*For any* completed task, the completion data should persist locally and survive app restart.

**Validates: Requirements 4.7**

**Property 17: Daily Reset with Streak Preservation**

*For any* user viewing the app on a new day, the daily task list should reset to show only new day's tasks, but the streak count should be preserved from the previous day.

**Validates: Requirements 4.8**

**Property 18: Affirmation on Task Completion**

*For any* task marked as completed, the system should display a positive affirmation message.

**Validates: Requirements 5.1**

**Property 19: Affirmation Message Variety**

*For any* sequence of task completions, the affirmation messages displayed should vary and avoid repetition across multiple completions.

**Validates: Requirements 5.2**

**Property 20: Affirmation Display Duration**

*For any* affirmation displayed, it should remain visible for 2-3 seconds before auto-dismissing, or allow manual dismissal.

**Validates: Requirements 5.4**

**Property 21: Streak-Aware Affirmations**

*For any* user with a streak of 3 or more consecutive days, affirmation messages should acknowledge and reference the streak.

**Validates: Requirements 5.5**

**Property 22: Badge Earning at Milestones**

*For any* user reaching a defined milestone (e.g., first task complete, 5-task day, 7-day streak), the system should award the corresponding badge.

**Validates: Requirements 6.1, 6.8**

**Property 23: Badge Notification**

*For any* badge earned, the system should display a badge notification with visual emphasis (animation, sound, celebratory message).

**Validates: Requirements 6.2**

**Property 24: Badge Display in Achievements**

*For any* earned badge, it should appear in the dedicated Achievements section with date and description.

**Validates: Requirements 6.3, 6.5**

**Property 25: Badge Progress Calculation**

*For any* locked badge, the system should display accurate progress toward unlocking it (e.g., "3 of 5 tasks complete today").

**Validates: Requirements 6.4**

**Property 26: Locked Badge Display**

*For any* locked badge, the system should display it in the Achievements section with hints about how to unlock it.

**Validates: Requirements 6.6**

**Property 27: Efficiency Calculation**

*For any* completed task with estimated and actual duration, the efficiency metric should be calculated as (actual duration / estimated duration) * 100.

**Validates: Requirements 7.6**

**Property 28: Efficiency Recording**

*For any* completed task, the actual duration should be recorded and compared to the estimated duration.

**Validates: Requirements 7.7**

**Property 29: Efficiency Aggregation**

*For any* set of completed tasks over a time period, the system should aggregate efficiency metrics to show trends (e.g., weekly average efficiency).

**Validates: Requirements 7.9**

**Property 30: Task Serialization Round-Trip**

*For any* valid Task object, serializing it to JSON and then parsing it back should produce an equivalent Task object with all fields preserved.

**Validates: Requirements 19.5**

**Property 31: Task Parser Error Handling**

*For any* invalid JSON payload, the Task_Parser should return a descriptive error message without crashing.

**Validates: Requirements 19.3**

**Property 32: Task Parser Optional Fields**

*For any* JSON payload with missing optional fields, the Task_Parser should use default values and successfully create a Task object.

**Validates: Requirements 19.6**

**Property 33: Task Serializer Completeness**

*For any* Task object, serialization should include all task metadata (id, title, description, estimated duration, Todo_Group, assigned user, completion status, timestamps).

**Validates: Requirements 19.8**

**Property 34: Affirmation Serialization Round-Trip**

*For any* valid Affirmation object, serializing it to JSON and then parsing it back should produce an equivalent Affirmation object with all fields preserved.

**Validates: Requirements 20.5**

**Property 35: Affirmation Parser Error Handling**

*For any* invalid JSON payload, the Affirmation_Parser should return a descriptive error message without crashing.

**Validates: Requirements 20.3**

---

## Testing Strategy

### Dual Testing Approach

The ADHD Focus App requires both unit testing and property-based testing to ensure comprehensive correctness:

**Unit Testing** focuses on:
- Specific examples and edge cases (e.g., creating a task with empty title fails)
- Integration points between components (e.g., task creation triggers sync queue)
- Error conditions and recovery (e.g., network timeout handling)
- UI interactions and state transitions (e.g., timer pause/resume)

**Property-Based Testing** focuses on:
- Universal properties that hold for all inputs (e.g., completion percentage is always correct)
- Comprehensive input coverage through randomization
- Invariants that must be maintained (e.g., streak only increments on 100% completion)
- Round-trip properties (e.g., parse→print→parse produces equivalent object)

Together, unit tests and property tests provide comprehensive coverage: unit tests catch concrete bugs in specific scenarios, while property tests verify general correctness across all possible inputs.

### Property-Based Testing Configuration

**Testing Library**: Use Kotest (Kotlin) or JUnit with QuickCheck-style generators for Android

**Test Structure**:
```kotlin
// Example property test structure
@Test
fun `Property 1: Daily task filtering`() {
  forAll(
    Arb.list(taskGenerator()),
    Arb.localDate()
  ) { tasks, today ->
    val todaysTasks = tasks.filter { it.date == today }
    val displayedTasks = dailyFocusView.getTodaysTasks()
    
    displayedTasks shouldContainExactly todaysTasks
  }
}
```

**Configuration Requirements**:
- Minimum 100 iterations per property test
- Each test must reference its design document property
- Tag format: `Feature: adhd-focus-app, Property {number}: {property_text}`
- Each correctness property must be implemented by a SINGLE property-based test
- Use custom generators for domain objects (Task, Affirmation, Badge, etc.)

**Generator Examples**:
```kotlin
fun taskGenerator() = Arb.bind(
  Arb.string(1..50),  // title
  Arb.string(0..200), // description
  Arb.int(5..120),    // estimatedDuration
  Arb.enum<TaskStatus>(),
  Arb.localDateTime()
) { title, desc, duration, status, createdAt ->
  Task(
    id = UUID.randomUUID().toString(),
    title = title,
    description = desc,
    estimatedDurationMinutes = duration,
    status = status,
    createdAt = createdAt
  )
}
```

### Unit Testing Strategy

**Focus Areas**:
- Task creation with valid/invalid data
- Completion percentage calculation with edge cases (0 tasks, 1 task, all complete, none complete)
- Streak increment/reset logic
- Efficiency metric calculation
- Affirmation message selection and variety
- Badge earning conditions
- Sync conflict resolution
- Data serialization/deserialization
- Timer state transitions (start, pause, resume, cancel)
- Offline data caching and retrieval

**Example Unit Tests**:
```kotlin
@Test
fun `Creating task with empty title fails`() {
  val result = taskManager.createTask("", "description", 30, "Morning")
  result shouldBe TaskCreationFailure("Title cannot be empty")
}

@Test
fun `Completing all tasks triggers day completion affirmation`() {
  val tasks = listOf(task1, task2, task3)
  tasks.forEach { taskManager.completeTask(it.id) }
  
  verify(affirmationEngine).displayDayCompletionAffirmation()
}

@Test
fun `Streak resets when not all tasks completed`() {
  streakTracker.currentStreak shouldBe 5
  taskManager.completeTask(task1.id)
  taskManager.completeTask(task2.id)
  // task3 not completed
  
  streakTracker.currentStreak shouldBe 0
}
```

### Integration Testing

**Focus Areas**:
- Task creation → local persistence → sync queue
- Remote update reception → local update → UI refresh
- Offline changes → reconnection → sync → conflict resolution
- Authentication → household loading → task display
- Timer completion → affirmation display → badge evaluation
- Daily reset → streak preservation

**Example Integration Tests**:
```kotlin
@Test
fun `Task creation syncs to cloud when connectivity available`() {
  // Create task while offline
  val task = taskManager.createTask("Buy groceries", "", 30, "Errands")
  task.syncStatus shouldBe SyncStatus.PENDING
  
  // Simulate network connectivity
  networkManager.setConnected(true)
  
  // Verify sync attempt
  verify(cloudSyncManager).syncPendingChanges()
  
  // Verify task synced
  task.syncStatus shouldBe SyncStatus.SYNCED
}
```

### Performance Testing

**Benchmarks**:
- Daily Focus View loads within 1 second on typical Android device
- Task list scrolls at 60 FPS with 100+ tasks
- Timer updates at least once per second without lag
- Button tap feedback within 100ms
- Memory usage stays below 150MB
- Sync operations complete within 5 seconds on typical network

**Performance Test Example**:
```kotlin
@Test
fun `Daily Focus View loads within 1 second`() {
  val startTime = System.currentTimeMillis()
  dailyFocusView.loadTodaysTasks()
  val loadTime = System.currentTimeMillis() - startTime
  
  loadTime shouldBeLessThan 1000
}
```

### Test Coverage Goals

- **Unit Tests**: 80%+ code coverage for business logic
- **Property Tests**: All 35 correctness properties implemented
- **Integration Tests**: All major user workflows covered
- **Performance Tests**: All critical paths benchmarked

### Continuous Integration

- Run all tests on every commit
- Property tests run with 100+ iterations
- Performance tests run on representative device
- Coverage reports generated and tracked
- Failed tests block merge to main branch

