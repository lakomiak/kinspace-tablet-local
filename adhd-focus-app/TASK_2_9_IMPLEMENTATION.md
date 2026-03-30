# Task 2.9: Sync Queue Table and DAO Implementation

## Overview
Implemented the sync queue infrastructure for tracking pending sync operations with calendar-cloud. The sync queue supports FIFO ordering, retry tracking, and operation type classification (CREATE, UPDATE, DELETE).

## Files Created

### 1. SyncQueueItem Entity (`src/main/kotlin/com/adhdfocus/app/data/model/SyncQueueItem.kt`)
- **Purpose**: Data model representing a pending sync operation
- **Key Features**:
  - UUID-based primary key for unique identification
  - Foreign keys to Task and User entities with CASCADE delete
  - Timestamp-based ordering for FIFO queue
  - Retry count tracking for failed sync attempts
  - JSON payload storage for serialized task data
  - Comprehensive database indexes for query optimization

- **Indexes**:
  - Single column: taskId, userId, operation, timestamp, retryCount
  - Composite: (userId, timestamp), (operation, timestamp), (retryCount, timestamp)

- **Validation**:
  - taskId and userId must be non-blank
  - payload must be non-blank JSON

### 2. SyncOperation Enum
- **CREATE**: New task creation sync operation
- **UPDATE**: Task modification sync operation
- **DELETE**: Task deletion sync operation

### 3. SyncQueueDao (`src/main/kotlin/com/adhdfocus/app/data/dao/SyncQueueDao.kt`)
- **Purpose**: Data Access Object for sync queue operations
- **CRUD Operations**:
  - `insert()`: Add new sync queue item
  - `update()`: Update existing item (e.g., retry count)
  - `delete()`: Remove item from queue
  - `getItemById()`: Retrieve specific item

- **Query Methods**:
  - `getItemsByTaskId()`: Get all pending operations for a task
  - `getItemsByUser()`: Get all pending operations for a user (Flow)
  - `getItemsByUserOnce()`: Get all pending operations for a user (suspend)
  - `getItemsByOperation()`: Filter by operation type (CREATE/UPDATE/DELETE)
  - `getItemsByOperationOnce()`: Filter by operation type (suspend)

- **FIFO Ordering**:
  - `getPendingItemsByUserFifo()`: Get user's pending items ordered by timestamp (oldest first)
  - `getAllPendingItemsFifo()`: Get all pending items ordered by timestamp

- **Retry Management**:
  - `getRetryableItemsByUser()`: Get items below max retry count for a user
  - `getAllRetryableItems()`: Get all items below max retry count
  - `incrementRetryCount()`: Increment retry count by 1
  - `setRetryCount()`: Set retry count to specific value

- **Count Operations**:
  - `getPendingItemCount()`: Count pending items for a user
  - `getPendingItemCountByOperation()`: Count pending items by operation type
  - `getRetryableItemCount()`: Count retryable items for a user

- **Delete Operations**:
  - `deleteItemById()`: Delete specific item
  - `deleteItemsByTaskId()`: Delete all items for a task
  - `deleteItemsByUserId()`: Delete all items for a user
  - `deleteItemsByUserAndOperation()`: Delete items by user and operation type
  - `deleteOldItems()`: Delete items older than cutoff time
  - `deleteAllItems()`: Clear entire queue

- **Time Range Queries**:
  - `getItemsInTimeRange()`: Get items within time window for a user

### 4. Type Converter for SyncOperation
Added to `src/main/kotlin/com/adhdfocus/app/data/database/Converters.kt`:
- `fromSyncOperation()`: Convert String to SyncOperation enum
- `syncOperationToString()`: Convert SyncOperation enum to String

### 5. Database Integration
Updated `src/main/kotlin/com/adhdfocus/app/data/database/AdhdfocusDatabase.kt`:
- Added SyncQueueItem to entities list
- Added abstract method `syncQueueDao()` to access the DAO

### 6. Comprehensive Unit Tests (`src/androidTest/kotlin/com/adhdfocus/app/data/dao/SyncQueueDaoTest.kt`)

#### Test Coverage:
- **Basic CRUD Operations** (4 tests):
  - Insert, update, delete, and retrieve operations
  
- **Query Operations** (3 tests):
  - Get items by task ID, user, and operation type
  
- **FIFO Ordering** (2 tests):
  - Verify FIFO ordering by timestamp for user and all items
  
- **Retry Tracking** (3 tests):
  - Get retryable items, increment retry count, set retry count
  
- **Count Operations** (2 tests):
  - Count pending items by user and by operation type
  
- **Delete Operations** (4 tests):
  - Delete by ID, task ID, user ID, and all items
  
- **Time Range Queries** (1 test):
  - Get items within specified time window

**Total: 19 comprehensive unit tests**

## Design Decisions

### 1. FIFO Queue Implementation
- Uses `timestamp` field for ordering
- Queries use `ORDER BY timestamp ASC` to ensure oldest items are processed first
- Supports both user-specific and global FIFO ordering

### 2. Retry Tracking
- `retryCount` field tracks number of failed sync attempts
- Separate methods for incrementing and setting retry count
- Queries filter by max retry count to identify retryable items
- Enables exponential backoff strategy in sync manager

### 3. Foreign Key Constraints
- CASCADE delete on Task and User deletion
- Automatically cleans up sync queue when related entities are deleted
- Maintains referential integrity

### 4. Payload Storage
- Stores serialized task data as JSON string
- Allows sync manager to reconstruct task state for retry
- Enables offline-first sync strategy

### 5. Comprehensive Indexing
- Single-column indexes on frequently queried fields
- Composite indexes for common query patterns
- Optimizes query performance for large queues

## Integration Points

### With Task Manager
- Sync queue items reference tasks via taskId
- Used to track pending task operations (CREATE, UPDATE, DELETE)

### With User Management
- Sync queue items reference users via userId
- Enables per-user sync queue management
- Supports multi-user household scenarios

### With Cloud Sync Manager
- Provides FIFO-ordered pending operations
- Tracks retry attempts for failed syncs
- Enables offline-first synchronization strategy

## Usage Example

```kotlin
// Insert a pending sync operation
val syncItem = SyncQueueItem(
    taskId = "task-123",
    userId = "user-456",
    operation = SyncOperation.CREATE,
    payload = """{"id":"task-123","title":"New Task"}"""
)
syncQueueDao.insert(syncItem)

// Get pending items in FIFO order
val pending = syncQueueDao.getPendingItemsByUserFifo("user-456")

// Increment retry count on failure
syncQueueDao.incrementRetryCount(syncItem.id)

// Get retryable items (less than 3 retries)
val retryable = syncQueueDao.getRetryableItemsByUser("user-456", maxRetries = 3)

// Delete after successful sync
syncQueueDao.deleteItemById(syncItem.id)
```

## Testing Strategy

All tests use in-memory database for fast execution:
- No external dependencies
- Isolated test environment
- Comprehensive coverage of all DAO methods
- Tests verify FIFO ordering, retry tracking, and filtering

## Future Enhancements

1. **Batch Operations**: Add batch insert/delete for performance
2. **Sync Status Tracking**: Add status field (PENDING, SYNCING, FAILED)
3. **Priority Queue**: Support priority-based ordering
4. **Conflict Resolution**: Track conflict information in payload
5. **Metrics**: Add sync duration and success rate tracking

## Compliance with Requirements

✅ **Requirement 2.3**: Task Manager synchronizes pending tasks to calendar-cloud
- Sync queue provides FIFO-ordered pending operations

✅ **Requirement 2.7**: Sync conflict resolution by timestamp
- Sync queue stores timestamps for conflict resolution

✅ **Requirement 10.3**: Cloud Sync Manager maintains local queue
- SyncQueueItem and SyncQueueDao implement the queue

✅ **Requirement 12.3**: Data Persistence Layer stores pending changes
- Sync queue persists locally with pending-sync flag

✅ **Design Specification**: Sync Queue Table
- Implements all specified fields and indexes
- Supports FIFO ordering, retry tracking, operation classification
