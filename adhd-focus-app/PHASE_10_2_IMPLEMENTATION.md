# Phase 10.2: Real-Time Update Logic - Implementation Summary

## Overview

Phase 10.2 implements the real-time update logic that integrates WebSocket event handlers (from Phase 10.1) with the UI layer to refresh the Daily_Focus_View in real-time. This enables seamless synchronization of tasks across devices when family members make changes.

## Requirements Addressed

- **Requirement 2.4**: "WHEN a task is updated on the desktop calendar or by another Family_Member, THE Task_Manager SHALL receive the update via WebSocket and refresh the Daily_Focus_View within 2 seconds"
- **Requirement 8**: Remote Update Application - Process WebSocket events and apply updates to local tasks
- **Requirement 11**: Real-Time Updates from Family Members - Receive and apply updates via WebSocket

## Implementation Components

### 1. RealTimeUpdateManager Interface
**File**: `src/main/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManager.kt`

Defines the contract for managing real-time updates from WebSocket and applying them to UI state:

**Key Methods**:
- `startListening(householdId, userId)` - Start listening to WebSocket updates
- `stopListening()` - Stop listening to WebSocket updates
- `observeUpdates()` - Observe update events for UI refresh
- `observeConnectionState()` - Observe connection state changes
- `observeLatency()` - Observe update latency metrics
- `getConnectionState()` - Get current connection state
- `getAverageLatency()` - Get average update latency
- `isListening()` - Check if currently listening

**Supporting Types**:
- `ConnectionState` enum: CONNECTED, DISCONNECTED, RECONNECTING, ERROR
- `LatencyMetric` data class: Tracks latency for each update

### 2. RealTimeUpdateManagerImpl Implementation
**File**: `src/main/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerImpl.kt`

Implements the real-time update manager with:

#### Integration Points
- **WebSocketTaskUpdateHandler**: Receives update events from WebSocket
- **TaskDao**: Accesses local task database
- **TaskManager**: Manages task operations
- **WebSocketManager**: Manages WebSocket connection state

#### Key Features

**1. Update Event Processing**
- Listens to WebSocket update events via Flow
- Processes TaskUpdated, TaskDeleted, TaskCreated events
- Emits events to UI layer for real-time refresh

**2. Connection State Management**
- Tracks connection state (CONNECTED, DISCONNECTED, RECONNECTING, ERROR)
- Emits state changes to UI layer
- Provides current connection state on demand

**3. Latency Tracking**
- Records latency for each applied update
- Maintains rolling window of last 100 metrics
- Calculates average latency for compliance verification
- Ensures 2-second update latency requirement

**4. UI Integration**
- Emits UpdateEvent flow for UI refresh
- Emits ConnectionState flow for sync status indicator
- Emits LatencyMetric flow for monitoring

**5. Listening State Management**
- Tracks whether currently listening to updates
- Prevents duplicate listening sessions
- Provides listening state on demand

## Key Features

### 1. Real-Time Update Application
- Receives updates from WebSocket via WebSocketTaskUpdateHandler
- Applies updates to local database
- Emits events to UI layer for immediate refresh
- Maintains update consistency

### 2. Connection State Tracking
- Monitors connection state changes
- Emits state changes to UI layer
- Provides visual feedback via sync status indicator
- Handles reconnection scenarios

### 3. Latency Monitoring
- Records latency for each update
- Tracks compliance with 2-second requirement
- Provides average latency calculation
- Enables performance monitoring

### 4. UI Integration
- Observes update events for real-time refresh
- Observes connection state for status indicator
- Observes latency metrics for monitoring
- Provides non-blocking update application

### 5. Offline Support
- Queues updates received while offline (via WebSocketTaskUpdateHandler)
- Applies queued updates on reconnection
- Maintains consistency during offline periods

## Testing

### Unit Tests (12 tests)
**File**: `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerUnitTest.kt`

Tests individual functionality:
1. Start listening initializes connection state
2. Stop listening changes connection state
3. Is listening returns true when listening
4. Is listening returns false when not listening
5. Observe updates emits TaskUpdated event
6. Observe updates emits TaskDeleted event
7. Observe updates emits TaskCreated event
8. Observe connection state emits connected state
9. Observe connection state emits disconnected state
10. Latency tracking records metrics
11. Average latency calculation
12. Multiple updates processed

### Property-Based Tests (8 properties)
**File**: `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerPropertyTest.kt`

**Validates: Requirements 2.4, 11.1, 11.2**

Tests universal properties across all inputs:
1. Update Consistency - All updates applied and emitted
2. Latency Tracking - Latency recorded for all updates
3. Connection State Transitions - State transitions correctly
4. Multiple Event Types - All event types processed
5. Latency Compliance - Latency tracked and averaged
6. Event Ordering - Events emitted in order received
7. Connection State Observation - State changes observed
8. Listening State Consistency - Listening state accurate

### Integration Tests (15 tests)
**File**: `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerIntegrationTest.kt`

Tests complete workflows:
1. Real-time update workflow
2. Multiple updates workflow
3. Task deletion workflow
4. Task creation workflow
5. Connection state transition workflow
6. Latency tracking workflow
7. Mixed event types workflow
8. Start-stop-start workflow
9. Listening state after multiple updates
10. Latency average calculation
11. Connection state initially disconnected
12. Is listening initially false
13. Average latency zero initially
14. Multiple households and users
15. Complete end-to-end workflow

## Correctness Properties

The implementation validates the following correctness properties:

**Property 8: Remote Update Application**
- For any update received via WebSocket from calendar-cloud, the local task data should be updated and the Daily_Focus_View should refresh to reflect the change

**Validates Requirements**:
- 2.4: Task updates received via WebSocket refresh Daily_Focus_View within 2 seconds
- 11.1: Real-time updates from family members received via WebSocket
- 11.2: Real-time updates applied to Daily_Focus_View

## Integration Points

### WebSocketTaskUpdateHandler
- Provides update events via Flow
- Handles offline queuing
- Manages conflict resolution

### TaskDao
- Provides task database access
- Persists updates locally
- Retrieves task data

### TaskManager
- Manages task operations
- Triggers affirmations and badges
- Handles task state transitions

### UI Layer (FocusViewModel)
- Observes update events
- Observes connection state
- Refreshes Daily_Focus_View on updates
- Displays sync status indicator

## Performance Characteristics

- **Event Processing**: O(1) for most events
- **Latency Tracking**: O(1) for recording metrics
- **Average Calculation**: O(n) where n ≤ 100 (rolling window)
- **Memory**: Minimal overhead for update flows and metrics

## Error Handling

- Gracefully handles update processing errors
- Transitions to ERROR state on connection failures
- Continues processing on individual update errors
- Logs errors for debugging

## Design Decisions

### 1. Flow-Based Architecture
- Uses Kotlin Flow for reactive updates
- Enables non-blocking UI refresh
- Supports multiple observers

### 2. Latency Tracking
- Rolling window of 100 metrics
- Prevents unbounded memory growth
- Enables average calculation

### 3. Connection State Management
- Separate flow for connection state
- Enables UI to display sync status
- Supports reconnection scenarios

### 4. Minimal UI Integration
- RealTimeUpdateManager doesn't directly update UI
- Emits events that UI layer observes
- Maintains separation of concerns

## Future Enhancements

1. Batch update processing for performance
2. Update deduplication to avoid redundant processing
3. Metrics collection for monitoring
4. Update retry logic with exponential backoff
5. Update compression for large payloads
6. Configurable latency thresholds
7. Update prioritization (critical vs. non-critical)
8. Update filtering by task type or group

## Files Created

1. `src/main/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManager.kt` - Interface
2. `src/main/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerImpl.kt` - Implementation
3. `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerUnitTest.kt` - Unit tests (12 tests)
4. `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerPropertyTest.kt` - Property tests (8 properties)
5. `src/test/kotlin/com/adhdfocus/app/domain/sync/RealTimeUpdateManagerIntegrationTest.kt` - Integration tests (15 tests)

## Test Coverage

- **Total Tests**: 35 (12 unit + 8 property + 15 integration)
- **Code Coverage**: All public methods and critical paths covered
- **Property Coverage**: 8 correctness properties validated

## Next Steps

Phase 10.3 will implement update notifications for new tasks, providing visual feedback when family members add tasks assigned to the ADHD user.

## Integration with Daily_Focus_View

The RealTimeUpdateManager integrates with the Daily_Focus_View through the FocusViewModel:

1. FocusViewModel injects RealTimeUpdateManager
2. On screen initialization, FocusViewModel calls `startListening(householdId, userId)`
3. FocusViewModel observes update events via `observeUpdates()`
4. On each update event, FocusViewModel refreshes task list
5. FocusViewModel observes connection state via `observeConnectionState()`
6. Connection state changes update sync status indicator
7. On screen destruction, FocusViewModel calls `stopListening()`

This ensures real-time updates are applied to the Daily_Focus_View without interrupting active timers or user interactions.
