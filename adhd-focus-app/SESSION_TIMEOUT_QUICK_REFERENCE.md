# Session Timeout Quick Reference Guide

## Overview

The Session Timeout system automatically logs out users after a configurable period of inactivity. This is essential for shared tablet devices to ensure security.

## Key Components

### 1. SessionTimeoutManager
**Location**: `domain/userswitching/SessionTimeoutManager.kt`

Core business logic for session timeout management.

**Key Methods**:
```kotlin
// Start a session
startSession(userId: String, timeoutMinutes: Int, scope: CoroutineScope)

// Record user activity (resets timeout)
recordActivity()

// Extend the session
extendSession()

// End the session
endSession()

// Get time remaining in seconds
getTimeRemaining(): Long

// Check if session is active
isSessionActive(): Boolean

// Dismiss warning
dismissWarning()
```

**State Properties**:
```kotlin
isSessionActive: StateFlow<Boolean>
timeRemaining: StateFlow<Long>
showWarning: StateFlow<Boolean>
warningTimeRemaining: StateFlow<Long>
```

### 2. SessionTimeoutViewModel
**Location**: `ui/family/SessionTimeoutViewModel.kt`

Manages UI state and operations for session timeout.

**Key Methods**:
```kotlin
// Initialize session for a user
initialize(userId: String)

// Record user activity
recordActivity()

// Extend session
extendSession()

// Dismiss warning
dismissWarning()

// End session
endSession()
```

**State Properties**:
```kotlin
isSessionActive: StateFlow<Boolean>
timeRemaining: StateFlow<Long>
showWarning: StateFlow<Boolean>
warningTimeRemaining: StateFlow<Long>
```

### 3. SessionTimeoutWarningDialog
**Location**: `ui/family/SessionTimeoutWarningDialog.kt`

Composable for displaying timeout warning.

**Variants**:
- `SessionTimeoutWarningDialog()` - Vertical layout
- `SessionTimeoutWarningDialogWithButtons()` - Horizontal layout (tablet-friendly)

## Usage Examples

### Basic Setup

```kotlin
@Composable
fun MyScreen() {
    val sessionTimeoutViewModel: SessionTimeoutViewModel = hiltViewModel()
    val userId = "user-123"
    
    // Initialize session when screen loads
    LaunchedEffect(userId) {
        sessionTimeoutViewModel.initialize(userId)
    }
    
    // Display warning dialog
    SessionTimeoutWarningDialog(
        isVisible = sessionTimeoutViewModel.showWarning.value,
        timeRemaining = sessionTimeoutViewModel.warningTimeRemaining.value,
        onExtendSession = { sessionTimeoutViewModel.extendSession() },
        onLogout = { 
            sessionTimeoutViewModel.endSession()
            // Navigate to login
        }
    )
}
```

### Track User Activity

```kotlin
// On button click
Button(onClick = {
    sessionTimeoutViewModel.recordActivity()
    // Handle click
}) {
    Text("Click Me")
}

// On scroll
LazyColumn(
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures {
            sessionTimeoutViewModel.recordActivity()
        }
    }
) {
    // Items
}

// On text input
TextField(
    value = text,
    onValueChange = { newText ->
        text = newText
        sessionTimeoutViewModel.recordActivity()
    }
)
```

### Monitor Session State

```kotlin
// Check if session is active
if (sessionTimeoutViewModel.isSessionActive.value) {
    // Session is active
}

// Get time remaining
val timeRemaining = sessionTimeoutViewModel.timeRemaining.value
val minutes = timeRemaining / 60
val seconds = timeRemaining % 60

// Check if warning is showing
if (sessionTimeoutViewModel.showWarning.value) {
    // Show warning dialog
}
```

## Configuration

### Set Timeout Duration

Timeout is configured per user in `UserPreferences`:

```kotlin
// In database or preferences
UserPreferences(
    userId = "user-123",
    autoLogoutTimeout = 900  // 15 minutes in seconds
)
```

**Common Durations**:
- 5 minutes: 300 seconds
- 10 minutes: 600 seconds
- 15 minutes: 900 seconds (default)
- 30 minutes: 1800 seconds
- 1 hour: 3600 seconds

### Disable Timeout

Set `autoLogoutTimeout` to 0:

```kotlin
UserPreferences(
    userId = "user-123",
    autoLogoutTimeout = 0  // Disabled
)
```

## Integration Points

### With FamilyMemberSwitcherViewModel

```kotlin
// When user switches
fun switchToMember(userId: String) {
    // Switch user
    userSwitchingManager.switchUser(userId, householdId)
    
    // Initialize session timeout
    sessionTimeoutViewModel.initialize(userId)
}
```

### With Daily Focus View

```kotlin
@Composable
fun DailyFocusView(userId: String) {
    val sessionTimeoutViewModel: SessionTimeoutViewModel = hiltViewModel()
    
    LaunchedEffect(userId) {
        sessionTimeoutViewModel.initialize(userId)
    }
    
    // Track activity on task interactions
    TaskList(
        onTaskClick = {
            sessionTimeoutViewModel.recordActivity()
            // Handle task click
        }
    )
    
    // Show warning
    SessionTimeoutWarningDialog(
        isVisible = sessionTimeoutViewModel.showWarning.value,
        timeRemaining = sessionTimeoutViewModel.warningTimeRemaining.value,
        onExtendSession = { sessionTimeoutViewModel.extendSession() },
        onLogout = { /* Handle logout */ }
    )
}
```

## Testing

### Unit Tests

```bash
# Run all session timeout tests
./gradlew test --tests "*SessionTimeout*"

# Run specific test class
./gradlew test --tests "*SessionTimeoutManagerTest"
./gradlew test --tests "*SessionTimeoutViewModelTest"
./gradlew test --tests "*SessionTimeoutIntegrationTest"
```

### Instrumented Tests

```bash
# Run UI tests
./gradlew connectedAndroidTest --tests "*SessionTimeoutWarningDialogTest"
```

## Common Scenarios

### Scenario 1: User Inactive for 14 Minutes

1. Session starts with 15-minute timeout
2. User doesn't interact for 14 minutes
3. Warning shows (1 minute remaining)
4. User can extend or logout

### Scenario 2: User Extends Session

1. Warning shows
2. User clicks "Extend Session"
3. Timeout resets to 15 minutes
4. Warning dismisses

### Scenario 3: User Logs Out

1. Warning shows
2. User clicks "Logout"
3. Session ends
4. User is logged out

### Scenario 4: User Active During Timeout

1. Session starts with 15-minute timeout
2. User interacts with app (taps, scrolls, types)
3. Each interaction resets the timeout
4. Session continues as long as user is active

## Troubleshooting

### Session Not Starting

**Problem**: Session doesn't start when user switches

**Solution**: 
- Ensure `initialize()` is called with correct userId
- Check that UserPreferences has `autoLogoutTimeout > 0`
- Verify CoroutineScope is provided

### Warning Not Showing

**Problem**: Warning dialog doesn't appear

**Solution**:
- Check that `showWarning.value` is true
- Verify SessionTimeoutWarningDialog is in composition
- Ensure `isVisible` parameter is true

### Timeout Not Resetting

**Problem**: Timeout doesn't reset on activity

**Solution**:
- Call `recordActivity()` on user interactions
- Verify activity tracking is implemented
- Check that timeout is enabled (> 0)

### Session Ending Unexpectedly

**Problem**: Session ends before timeout

**Solution**:
- Check timeout duration configuration
- Verify no other code is calling `endSession()`
- Check for exceptions in logs

## Best Practices

1. **Always Initialize**: Call `initialize()` when user switches
2. **Track All Activity**: Record activity on all user interactions
3. **Handle Logout**: Implement proper logout handling in callback
4. **Test Thoroughly**: Test with different timeout durations
5. **Monitor Logs**: Check logs for timeout-related errors
6. **Provide Feedback**: Show clear warning messages to users
7. **Allow Extension**: Always provide "Extend Session" option
8. **Clean Up**: Ensure session is ended on logout

## Performance Considerations

- Session timeout uses minimal resources
- Time updates occur every second
- StateFlow updates are efficient
- Coroutines are properly managed
- No memory leaks with proper cleanup

## Security Considerations

- Timeout is enforced automatically
- User cannot bypass timeout
- Session data is cleared on timeout
- PIN protection works alongside timeout
- Timeout applies to all users equally

## Future Enhancements

- Customizable warning time
- Activity logging for audit trail
- Biometric authentication
- Session history tracking
- Configurable timeout per activity type
- Notification on timeout

## Support

For issues or questions:
1. Check the implementation summary: `TASK_3_5_IMPLEMENTATION.md`
2. Review test cases for usage examples
3. Check logs for error messages
4. Verify configuration in UserPreferences
