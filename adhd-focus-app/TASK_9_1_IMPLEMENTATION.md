# Task 9.1: Implement REST API Client for calendar-cloud

## Summary

Implemented a complete REST API client for calendar-cloud integration with support for task synchronization, conflict resolution, and exponential backoff retry logic.

## Deliverables

### 1. RestApiClient.kt (Interface)
**Location**: `src/main/kotlin/com/adhdfocus/app/domain/sync/RestApiClient.kt`

Defines the contract for REST API operations:
- `createTask(householdId, task): Task` - Create new task
- `updateTask(householdId, taskId, updates): Task` - Update existing task
- `deleteTask(householdId, taskId): Unit` - Delete task
- `fetchTasks(householdId): List<Task>` - Fetch all tasks
- `batchSync(householdId, changes): SyncResult` - Batch sync operations

Supporting data classes:
- `SyncChange` - Represents a single change to sync
- `SyncResult` - Result of batch sync with conflict tracking
- `SyncConflict` - Represents a conflict during sync
- `NetworkException` - Network error handling
- `ApiException` - API error handling

### 2. RestApiClientImpl.kt (Implementation)
**Location**: `src/main/kotlin/com/adhdfocus/app/domain/sync/RestApiClientImpl.kt`

Retrofit-based implementation with:
- HTTP request/response handling via Retrofit
- Gson serialization for request/response bodies
- Bearer token authentication via TokenProvider interface
- Exponential backoff retry logic (100ms → 32s, 5 retries max)
- Automatic conversion between API models and domain models
- Comprehensive error handling with ApiException and NetworkException

Key features:
- Retries failed requests with exponential backoff
- Converts TaskResponse to Task domain model
- Handles null values in update requests
- Preserves task status and sync status during conversion
- Supports batch sync with conflict detection

### 3. SyncChangeSerializer.kt
**Location**: `src/main/kotlin/com/adhdfocus/app/domain/sync/SyncChangeSerializer.kt`

Handles serialization/deserialization of sync changes:
- `createSyncChange(task, operation): SyncChange` - Create sync change with JSON payload
- `deserializeTask(syncChange): Task` - Extract task from sync change
- `serializeTask(task): String` - Convert task to JSON
- `deserializeTaskFromJson(json): Task` - Parse JSON to task

Features:
- Uses Gson for JSON serialization
- Includes timestamp management
- Error handling with descriptive messages
- Supports all sync operations (CREATE, UPDATE, DELETE)

### 4. RestApiClientUnitTest.kt
**Location**: `src/test/kotlin/com/adhdfocus/app/domain/sync/RestApiClientUnitTest.kt`

Comprehensive unit tests with mocking:
- `createTask` - Verifies task creation and response handling
- `updateTask` - Tests update with null values in updates map
- `deleteTask` - Verifies delete request
- `fetchTasks` - Tests list retrieval and empty responses
- `batchSync` - Tests batch sync with conflict handling
- Error handling - Tests ApiException on error responses
- Status conversion - Verifies TaskStatus and SyncStatus conversion
- Conflict handling - Tests conflict detection and reporting

Total: 12 unit tests covering all API operations and error cases

### 5. RestApiClientPropertyTest.kt
**Location**: `src/test/kotlin/com/adhdfocus/app/domain/sync/RestApiClientPropertyTest.kt`

Property-based tests validating universal properties:

**Validates: Requirements 2, 10**

1. **Request serialization round-trip** - Task serializes and deserializes correctly with all fields preserved
2. **Response deserialization correctness** - TaskResponse converts to Task with all fields preserved
3. **Conflict resolution by timestamp** - Most recent timestamp wins in conflict resolution
4. **Exponential backoff calculation** - Backoff increases exponentially (100ms → 200ms → 400ms → 800ms → 1600ms)
5. **SyncChange serialization** - Payload contains all task data
6. **SyncResult aggregation** - Synced and failed counts are non-negative
7. **Task status transitions** - Valid status conversions
8. **Sync operation types** - All operations are valid
9. **Timestamp ordering** - Updated timestamp is after or equal to created timestamp

Uses Kotest property generators for comprehensive input space coverage.

### 6. SyncChangeSerializerUnitTest.kt
**Location**: `src/test/kotlin/com/adhdfocus/app/domain/sync/SyncChangeSerializerUnitTest.kt`

Unit tests for serialization:
- `createSyncChange` - Creates SyncChange with serialized payload
- Payload validation - Verifies payload contains task data
- `deserializeTask` - Converts payload back to Task
- `serializeTask` - Produces valid JSON
- `deserializeTaskFromJson` - Parses JSON string
- Error handling - Tests invalid JSON handling
- Operation types - Tests all operation types (CREATE, UPDATE, DELETE)
- Status preservation - Verifies task status is preserved
- Idempotency - Serialization/deserialization is idempotent
- Timestamp handling - Verifies current timestamp is included
- Field preservation - All task fields are preserved during serialization

Total: 11 unit tests covering serialization and error cases

## Integration Points

### Existing Components Used
- `TaskService` - Retrofit service for task endpoints
- `SyncService` - Retrofit service for sync endpoints
- `Task` - Domain model with validation
- `SyncQueueManager` - Manages pending sync operations
- `Gson` - JSON serialization

### Dependencies
- Retrofit 2.9.0 - HTTP client
- Gson 2.10.1 - JSON serialization
- Kotest 5.7.2 - Property-based testing
- MockK - Mocking framework

## Key Design Decisions

1. **Exponential Backoff**: Implements exponential backoff (100ms → 32s) to handle transient failures gracefully without overwhelming the server.

2. **Conflict Resolution**: Supports timestamp-based conflict resolution where the most recent timestamp wins, enabling offline-first sync.

3. **Serialization**: Uses Gson for JSON serialization to match existing infrastructure and ensure consistency.

4. **Error Handling**: Distinguishes between NetworkException and ApiException for different error scenarios.

5. **TokenProvider Interface**: Abstracts token management to allow flexible authentication implementations.

6. **Minimal Implementation**: Focuses on essential functionality without over-engineering, following the spec requirements.

## Testing Coverage

- **Unit Tests**: 23 tests covering all API operations, error cases, and serialization
- **Property-Based Tests**: 9 properties validating universal invariants
- **Mocking**: Uses MockK for Retrofit service mocking
- **Error Scenarios**: Tests network errors, API errors, invalid JSON, and conflict handling

## Compliance with Requirements

✅ Requirement 2: Task Management with Cloud Sync
- REST API client supports task synchronization endpoints
- Handles pending task creation and updates
- Supports conflict resolution by timestamp
- Persists task data locally via SyncQueueManager integration

✅ Requirement 10: Cloud Synchronization with calendar-cloud
- REST API client sends pending changes to calendar-cloud
- Supports task creation, update, and deletion operations
- Handles authentication via TokenProvider
- Implements exponential backoff for failed attempts

## Files Created

1. `src/main/kotlin/com/adhdfocus/app/domain/sync/RestApiClient.kt` - Interface (95 lines)
2. `src/main/kotlin/com/adhdfocus/app/domain/sync/RestApiClientImpl.kt` - Implementation (180 lines)
3. `src/main/kotlin/com/adhdfocus/app/domain/sync/SyncChangeSerializer.kt` - Serialization (60 lines)
4. `src/test/kotlin/com/adhdfocus/app/domain/sync/RestApiClientUnitTest.kt` - Unit tests (280 lines)
5. `src/test/kotlin/com/adhdfocus/app/domain/sync/RestApiClientPropertyTest.kt` - Property tests (320 lines)
6. `src/test/kotlin/com/adhdfocus/app/domain/sync/SyncChangeSerializerUnitTest.kt` - Serializer tests (200 lines)

**Total**: ~1,135 lines of production code and tests

## Next Steps

Task 9.2 will implement WebSocket connection management for real-time updates from calendar-cloud.
