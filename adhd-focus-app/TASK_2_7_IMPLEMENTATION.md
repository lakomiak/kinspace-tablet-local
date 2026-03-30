# Task 2.7: Create Task JSON Serializer/Parser with Round-Trip Property Test

## Overview

This task implements JSON serialization and deserialization for the Task entity with comprehensive property-based testing to ensure data integrity through serialization cycles.

## Implementation Details

### 1. TaskSerializer (Updated)

**File**: `src/main/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializer.kt`

**Changes**:
- Updated to properly serialize `Instant` timestamps to milliseconds since epoch
- Refactored to use a shared `buildTaskJson()` method to reduce code duplication
- Maintains support for:
  - Single task serialization: `serialize(task: Task): String`
  - Pretty-printed JSON: `serializePretty(task: Task): String`
  - List serialization: `serializeList(tasks: List<Task>): String`

**Key Features**:
- Converts all Task fields to JSON representation
- Handles optional fields (description, estimatedDurationMinutes, actualDurationMinutes, completedAt)
- Serializes enums (TaskStatus, SyncStatus) as their string names
- Converts Instant timestamps to epoch milliseconds for JSON compatibility

### 2. TaskParser (New)

**File**: `src/main/kotlin/com/adhdfocus/app/domain/serialization/TaskParser.kt`

**Functionality**:
- Parses JSON strings into Task objects
- Validates required fields (id, householdId, assignedUserId, title, todoGroup)
- Handles optional fields gracefully with null values
- Converts epoch milliseconds back to Instant objects
- Provides descriptive error messages for invalid input

**Methods**:
- `parse(jsonString: String): Task` - Parse JSON string to Task
- `parseFromJson(json: JSONObject): Task` - Parse JSONObject to Task
- `parseList(jsonArrayString: String): List<Task>` - Parse JSON array to Task list

**Error Handling**:
- Throws `IllegalArgumentException` with descriptive messages for:
  - Missing required fields
  - Invalid enum values
  - Malformed JSON

### 3. Property-Based Tests

**File**: `src/test/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializationRoundTripTest.kt`

**Framework**: Kotest (added to build.gradle.kts)

**Properties Tested**:

#### Property 30: Task Serialization Round-Trip
- **Validates**: Requirements 2.1, 2.8, 19.1-19.8
- **Description**: FOR ALL valid Task objects, serializing then parsing SHALL produce an equivalent object
- **Tests**:
  - Round-trip with all fields populated
  - Complete equality verification
  - Pretty-printed JSON round-trip
  - Enum value preservation (TaskStatus, SyncStatus)
  - List serialization round-trip

#### Property 31: Task Parser Error Handling
- **Description**: FOR ALL invalid JSON inputs, the parser SHALL return a descriptive error message
- **Tests**:
  - Missing required fields
  - Empty required fields
  - Invalid enum values

#### Property 32: Task Parser Optional Fields
- **Description**: FOR ALL valid Task objects with optional fields, the parser SHALL handle them gracefully
- **Tests**:
  - Tasks with null optional fields
  - Tasks with populated optional fields
  - Proper default value handling

#### Property 33: Task Serializer Completeness
- **Description**: FOR ALL valid Task objects, the serializer SHALL include all task metadata
- **Tests**:
  - Verification that all required fields are present in JSON output
  - Metadata completeness validation

### 4. Unit Tests

**File**: `src/test/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializationBasicTest.kt`

**Test Coverage**:
- Complete task serialization/deserialization
- Tasks with optional fields
- Different task statuses (INCOMPLETE, IN_PROGRESS, COMPLETED)
- Different sync statuses (PENDING, SYNCED, CONFLICT)
- Pretty-printed JSON round-trip
- List serialization
- Invalid JSON rejection
- Invalid enum value handling
- Timestamp precision preservation
- Deleted task flag preservation
- All enum values serialization

## Data Model

### Task Fields Serialized

```kotlin
data class Task(
    val id: String,                          // UUID
    val householdId: String,                 // Required
    val assignedUserId: String,              // Required
    val title: String,                       // Required
    val description: String? = null,         // Optional
    val todoGroup: String,                   // Required
    val estimatedDurationMinutes: Int? = null,  // Optional
    val actualDurationMinutes: Int? = null,     // Optional
    val status: TaskStatus = TaskStatus.INCOMPLETE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,       // Optional
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
```

### Enum Serialization

**TaskStatus**:
- INCOMPLETE
- IN_PROGRESS
- COMPLETED

**SyncStatus**:
- PENDING
- SYNCED
- CONFLICT

## JSON Format Example

```json
{
  "id": "task-123",
  "householdId": "hh-1",
  "assignedUserId": "user-1",
  "title": "Complete Task",
  "description": "A complete task with all fields",
  "todoGroup": "Morning",
  "estimatedDurationMinutes": 30,
  "actualDurationMinutes": 25,
  "status": "COMPLETED",
  "createdAt": 1609459200000,
  "updatedAt": 1609459200000,
  "completedAt": 1609459200000,
  "syncStatus": "SYNCED",
  "isDeleted": false
}
```

## Build Configuration

### Dependencies Added

```kotlin
val kotestVersion = "5.7.2"

testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
testImplementation("io.kotest:kotest-property:$kotestVersion")
```

## Testing Strategy

### Property-Based Testing (Kotest)
- 100 iterations per property test
- Random task generation with valid data
- Validates universal properties across all inputs
- Tests enum serialization for all possible values
- Tests optional field handling

### Unit Testing (JUnit)
- Specific examples demonstrating correct behavior
- Edge cases (empty strings, null values, boundary values)
- Error conditions (invalid JSON, missing fields)
- Enum value coverage
- Timestamp precision

## Requirements Addressed

### Requirement 19: Parser and Serializer for Task Data

1. ✅ WHEN a task object is serialized, THE Task_Serializer SHALL convert it to a valid JSON representation with all required fields
2. ✅ WHEN a JSON task payload is received from calendar-cloud, THE Task_Parser SHALL parse it into a Task object
3. ✅ IF an invalid JSON payload is provided, THEN THE Task_Parser SHALL return a descriptive error message
4. ✅ THE Task_Pretty_Printer SHALL format Task objects back into valid JSON with proper indentation and formatting
5. ✅ FOR ALL valid Task objects, parsing then printing then parsing SHALL produce an equivalent object (round-trip property)
6. ✅ THE Task_Parser SHALL handle optional fields gracefully, using default values when fields are missing
7. ✅ THE Task_Parser SHALL validate that required fields (id, title, householdId) are present and non-empty
8. ✅ THE Task_Serializer SHALL include all task metadata (estimated duration, Todo_Group, assigned user, completion status, timestamps)

## Files Created/Modified

### Created
- `src/main/kotlin/com/adhdfocus/app/domain/serialization/TaskParser.kt`
- `src/test/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializationRoundTripTest.kt`
- `src/test/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializationBasicTest.kt`

### Modified
- `src/main/kotlin/com/adhdfocus/app/domain/serialization/TaskSerializer.kt` - Updated for proper Instant serialization
- `build.gradle.kts` - Added Kotest dependencies

## Running the Tests

### Property-Based Tests
```bash
./gradlew adhd-focus-app:test --tests "TaskSerializationRoundTripTest"
```

### Unit Tests
```bash
./gradlew adhd-focus-app:test --tests "TaskSerializationBasicTest"
```

### All Serialization Tests
```bash
./gradlew adhd-focus-app:test --tests "*Serialization*"
```

## Next Steps

The implementation is complete and ready for:
1. Integration with cloud sync operations
2. Use in REST API request/response handling
3. Local database persistence
4. Real-time update handling from WebSocket events

Task 2.8 will implement similar serialization/deserialization for Affirmation entities.
