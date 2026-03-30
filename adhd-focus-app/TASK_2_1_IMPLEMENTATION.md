# Task 2.1: Implement Task Data Model and Room DAO

## Overview
This task implements the complete Task data model and Room DAO with comprehensive CRUD operations, filtering, sorting, and query methods as specified in the design document.

## Implementation Details

### 1. Task Entity Model (`Task.kt`)

**Enhanced Features:**
- Added database constraints and validation in the init block
- Implemented comprehensive database indexes for query optimization
- All required fields from design.md specification

**Constraints & Validation:**
- `householdId`: Non-blank, required
- `assignedUserId`: Non-blank, required
- `title`: Non-blank, required
- `todoGroup`: Non-blank, required
- `estimatedDurationMinutes`: Must be positive if provided
- `actualDurationMinutes`: Must be non-negative if provided

**Database Indexes:**
- Single-column indexes on frequently queried fields:
  - `householdId`, `assignedUserId`, `status`, `syncStatus`, `todoGroup`, `createdAt`, `updatedAt`, `isDeleted`
- Composite indexes for common query patterns:
  - `(householdId, status)` - Filter tasks by household and status
  - `(assignedUserId, status)` - Filter user tasks by status
  - `(householdId, todoGroup)` - Filter tasks by group
  - `(assignedUserId, todoGroup)` - Filter user tasks by group
  - `(householdId, syncStatus)` - Find pending sync tasks
  - `(assignedUserId, syncStatus)` - Find user's pending sync tasks

### 2. Task DAO (`TaskDao.kt`)

**CRUD Operations:**
- `insert(task)` - Create new task
- `update(task)` - Update existing task
- `delete(task)` - Hard delete task
- `getTaskById(taskId)` - Retrieve single task
- `deleteTaskById(taskId)` - Delete by ID

**Household-Level Queries:**
- `getTasksByHousehold(householdId)` - Flow of all household tasks
- `getTasksByHouseholdOnce(householdId)` - Single query result
- `getTasksByStatus(householdId, status)` - Filter by status
- `getTasksByTodoGroup(householdId, todoGroup)` - Filter by group
- `getTasksByStatusAndGroup(householdId, status, todoGroup)` - Combined filter
- `getTasksBySyncStatus(householdId, syncStatus)` - Find pending sync tasks
- `getRecentTasks(householdId, limit)` - Get N most recent tasks
- `getTasksInDateRange(householdId, startTime, endTime)` - Date range query
- `getTasksByStatusOnce(householdId, status)` - Single query by status
- `getTasksByTodoGroupOnce(householdId, todoGroup)` - Single query by group
- `getTasksBySyncStatusOnce(householdId, syncStatus)` - Single query by sync status

**User-Level Queries:**
- `getTasksByUser(userId)` - Flow of all user tasks
- `getUserTasksByStatus(userId, status)` - Filter user tasks by status
- `getUserTasksByTodoGroup(userId, todoGroup)` - Filter user tasks by group
- `getUserTasksByStatusAndGroup(userId, status, todoGroup)` - Combined filter
- `getUserTasksBySyncStatus(userId, syncStatus)` - Find user's pending sync tasks
- `getUserRecentTasks(userId, limit)` - Get N most recent user tasks
- `getUserTasksInDateRange(userId, startTime, endTime)` - User date range query

**Count Operations:**
- `getTaskCount(householdId)` - Total task count
- `getUserTaskCount(userId)` - User task count
- `getTaskCountByStatus(householdId, status)` - Count by status
- `getUserTaskCountByStatus(userId, status)` - User count by status
- `getPendingSyncTaskCount(householdId, syncStatus)` - Count pending sync tasks

**Soft Delete Operations:**
- `softDeleteTask(taskId)` - Mark task as deleted without removing
- `softDeleteAllHouseholdTasks(householdId)` - Soft delete all household tasks
- `deleteOldSoftDeletedTasks(cutoffTime)` - Permanently delete old soft-deleted tasks

**Query Characteristics:**
- All queries exclude soft-deleted tasks (isDeleted = 0)
- Results ordered by creation date (descending) for consistency
- Sync status queries ordered by update time (ascending) for FIFO processing
- Flow-based queries for real-time updates
- Suspend functions for one-time queries

### 3. Type Converters

**Existing Converters (in `Converters.kt`):**
- `Instant` ↔ `Long` (milliseconds since epoch)
- `LocalDate` ↔ `String` (ISO format)

These converters handle the complex types used in the Task model.

### 4. Database Integration

**Database Configuration (in `AdhdfocusDatabase.kt`):**
- Task entity registered in database
- All DAOs accessible via database instance
- Type converters applied globally
- Migration framework ready for future schema changes

### 5. Unit Tests (`TaskDaoTest.kt`)

**Test Coverage:**

**Basic CRUD Operations (5 tests):**
- Insert task
- Update task
- Delete task
- Get task by ID
- Get non-existent task

**Filtering by Status (2 tests):**
- Get household tasks by status
- Get user tasks by status

**Filtering by Todo Group (2 tests):**
- Get household tasks by group
- Get user tasks by group

**Combined Filtering (2 tests):**
- Get tasks by status and group (household)
- Get tasks by status and group (user)

**Filtering by Sync Status (2 tests):**
- Get household tasks by sync status
- Get user tasks by sync status

**Recent Tasks (2 tests):**
- Get recent household tasks
- Get recent user tasks

**Date Range Queries (2 tests):**
- Get household tasks in date range
- Get user tasks in date range

**Count Operations (6 tests):**
- Get total task count
- Get user task count
- Get task count by status
- Get user task count by status
- Get pending sync task count
- Verify count accuracy

**Soft Delete Operations (5 tests):**
- Soft delete single task
- Soft deleted tasks excluded from queries
- Soft delete all household tasks
- Delete old soft-deleted tasks
- Soft delete exclusion in all query types

**Ordering (1 test):**
- Tasks ordered by creation date descending

**Validation (6 tests):**
- Reject blank householdId
- Reject blank assignedUserId
- Reject blank title
- Reject blank todoGroup
- Reject negative estimated duration
- Reject negative actual duration

**Total: 37 comprehensive unit tests**

## Design Compliance

✅ **Requirement 2.1 - Task Manager Acceptance Criteria:**
- Task creation with required fields (title, optional description, optional duration, todoGroup)
- Task status transitions (INCOMPLETE → IN_PROGRESS → COMPLETED)
- Task persistence with sync status tracking
- Support for filtering and sorting operations

✅ **Design Document Specifications:**
- All fields from Task model specification included
- Database constraints and indexes for performance
- Soft delete support for data retention
- Type converters for complex types (Instant, LocalDate)
- Query methods for all filtering scenarios

## Files Modified/Created

1. **Modified:** `src/main/kotlin/com/adhdfocus/app/data/model/Task.kt`
   - Added database indexes
   - Added validation constraints in init block

2. **Modified:** `src/main/kotlin/com/adhdfocus/app/data/dao/TaskDao.kt`
   - Added 30+ query methods for filtering, sorting, and counting
   - Added soft delete operations
   - Added date range queries

3. **Created:** `src/androidTest/kotlin/com/adhdfocus/app/data/dao/TaskDaoTest.kt`
   - 37 comprehensive unit tests
   - Tests for all CRUD operations
   - Tests for all query methods
   - Tests for validation and constraints

## Testing

All tests are designed to run on Android instrumented test environment:
- Uses `@RunWith(AndroidJUnit4::class)` for Android test runner
- Uses in-memory Room database for fast, isolated testing
- Tests verify both positive and negative scenarios
- Tests validate data integrity and query correctness

To run tests:
```bash
./gradlew connectedAndroidTest
```

## Performance Considerations

**Database Indexes:**
- Composite indexes optimize common query patterns
- Reduces query execution time for filtered results
- Supports efficient sorting and pagination

**Query Optimization:**
- Flow-based queries for real-time updates
- Suspend functions for one-time queries
- Efficient count operations without loading full data

**Soft Delete Strategy:**
- Preserves data for audit trails
- Allows recovery of accidentally deleted tasks
- Automatic cleanup of old soft-deleted tasks

## Future Enhancements

1. Add pagination support for large result sets
2. Implement full-text search on task titles and descriptions
3. Add query result caching for frequently accessed data
4. Implement batch operations for bulk updates
5. Add query performance monitoring and logging
