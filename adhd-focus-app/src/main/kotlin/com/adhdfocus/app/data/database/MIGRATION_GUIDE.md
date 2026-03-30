# Database Migration Guide

This document provides guidance for implementing database schema migrations as the ADHD Focus App evolves.

## Overview

The ADHD Focus App uses Room Database with a migration framework to handle schema changes across app versions. The current database version is **1**.

## Current Schema (Version 1)

### Tables

- **tasks**: Stores task information with status, sync state, and timestamps
- **users**: Stores user profiles and authentication data
- **affirmations**: Stores affirmation messages for positive reinforcement
- **badges**: Stores earned badges and achievement data
- **streaks**: Stores streak tracking data per user
- **efficiency_metrics**: Stores task completion efficiency data

### Type Converters

- **Instant**: Converts between `java.time.Instant` and millisecond timestamps
- **LocalDate**: Converts between `java.time.LocalDate` and ISO-8601 string format

## Creating a New Migration

### Step 1: Update Database Version

In `AdhdfocusDatabase.kt`, increment the version number:

```kotlin
@Database(
    entities = [...],
    version = 2,  // Increment from 1 to 2
    exportSchema = true
)
```

### Step 2: Create Migration Class

Create a new migration in `AdhdfocusDatabase.kt`:

```kotlin
companion object {
    val MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2
    )

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new column to tasks table
            database.execSQL("ALTER TABLE tasks ADD COLUMN newColumn TEXT DEFAULT NULL")
        }
    }
}
```

### Step 3: Update Entity Models

Modify the affected entity classes to match the new schema:

```kotlin
@Entity(tableName = "tasks")
data class Task(
    // ... existing fields ...
    val newColumn: String? = null  // Add new field
)
```

### Step 4: Export Schema

After updating the database version, Room will automatically export the new schema to `schemas/` directory when you build the project. Commit these schema files to version control for reference.

### Step 5: Test Migration

- Test on a device with the old database version
- Verify data integrity after migration
- Check that all queries still work correctly

## Migration Examples

### Example 1: Adding a New Column

```kotlin
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN priority INTEGER DEFAULT 0"
        )
    }
}
```

### Example 2: Creating a New Table

```kotlin
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_preferences (
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
            """.trimIndent()
        )
    }
}
```

### Example 3: Modifying Column Constraints

```kotlin
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite doesn't support dropping columns directly
        // Create new table with updated schema
        database.execSQL(
            """
            CREATE TABLE tasks_new (
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
                isDeleted BOOLEAN NOT NULL DEFAULT 0,
                priority INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        
        // Copy data from old table
        database.execSQL(
            """
            INSERT INTO tasks_new 
            SELECT id, householdId, assignedUserId, title, description, todoGroup,
                   estimatedDurationMinutes, actualDurationMinutes, status, createdAt,
                   updatedAt, completedAt, syncStatus, isDeleted, 0
            FROM tasks
            """.trimIndent()
        )
        
        // Drop old table and rename new one
        database.execSQL("DROP TABLE tasks")
        database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
    }
}
```

## Best Practices

1. **Always Test**: Test migrations on devices with old database versions
2. **Preserve Data**: Ensure user data is preserved during migrations
3. **Backward Compatibility**: Consider backward compatibility when designing schema changes
4. **Document Changes**: Document the purpose and impact of each migration
5. **Version Control**: Commit exported schema files to track schema evolution
6. **Incremental Versions**: Avoid skipping version numbers; use sequential versions
7. **Handle Null Values**: Provide sensible defaults for new columns

## Fallback to Destructive Migration

The app is currently configured with `fallbackToDestructiveMigration()` for development. This means:

- If a migration is missing, the database will be cleared and recreated
- **This should only be used during development**
- For production releases, proper migrations must be implemented

To disable destructive migration for production:

```kotlin
// Remove or comment out this line in AppModule.kt
.fallbackToDestructiveMigration()
```

## Schema Export Location

Room automatically exports database schemas to:

```
app/schemas/com.adhdfocus.app.data.database.AdhdfocusDatabase/
```

These files should be committed to version control to track schema evolution over time.

## Troubleshooting

### Migration Not Applied

- Ensure the migration is added to the `MIGRATIONS` array
- Verify the version numbers are correct (from → to)
- Check that the entity model matches the new schema

### Data Loss During Migration

- Review the migration SQL for data preservation
- Test on a backup database first
- Consider using a temporary table approach for complex migrations

### Compilation Errors

- Ensure entity models match the database schema
- Check for type mismatches in type converters
- Verify all required fields have default values or are nullable

## Future Considerations

As the app evolves, consider these potential schema changes:

1. **User Preferences Table**: Separate user preferences from the users table
2. **Sync Queue Table**: Add a dedicated table for pending sync operations
3. **Current User Table**: Track the currently selected user for family member switching
4. **Indexes**: Add database indexes for frequently queried columns
5. **Constraints**: Add foreign key constraints for data integrity
