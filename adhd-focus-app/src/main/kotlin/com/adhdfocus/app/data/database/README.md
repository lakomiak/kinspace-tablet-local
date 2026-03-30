# Database Module

This module contains all database-related code for the ADHD Focus App, including Room database configuration, entities, DAOs, type converters, and backup/recovery utilities.

## Structure

```
database/
├── AdhdfocusDatabase.kt          # Main Room database class with migration framework
├── Converters.kt                 # Type converters for Instant and LocalDate
├── DatabaseInitializer.kt        # Database initialization with pre-populated data
├── DatabaseBackupManager.kt      # Backup and recovery utilities
├── MIGRATION_GUIDE.md            # Guide for implementing future migrations
└── README.md                      # This file
```

## Key Components

### AdhdfocusDatabase

The main Room database class that:
- Defines all entities (Task, User, Affirmation, Badge, Streak, EfficiencyMetric)
- Manages database version (currently version 1)
- Provides DAOs for data access
- Includes migration framework for future schema changes
- Exports schema for version control

**Features:**
- Version management: Currently at version 1
- Migration framework: Ready for future schema changes
- Fallback to destructive migration: For development/testing
- Schema export: Enabled for version control

### Converters

Type converters for Room database:
- **Instant**: Converts `java.time.Instant` ↔ millisecond timestamps
- **LocalDate**: Converts `java.time.LocalDate` ↔ ISO-8601 strings

### DatabaseInitializer

Handles database initialization with pre-populated data:
- Populates default affirmations on database creation
- Includes task completion, day completion, and streak milestone affirmations
- Executes during database creation via Room callback

### DatabaseBackupManager

Provides backup and recovery utilities:
- `createBackup()`: Create a timestamped backup of the database
- `restoreFromBackup()`: Restore database from a backup file
- `getAvailableBackups()`: List all available backups
- `deleteBackup()`: Delete a specific backup
- `deleteAllBackups()`: Delete all backups
- `getBackupSize()`: Get size of a backup file
- `getTotalBackupSize()`: Get total size of all backups

## Database Schema

### Version 1 Tables

#### tasks
```sql
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

#### users
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
```

#### affirmations
```sql
CREATE TABLE affirmations (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  message TEXT NOT NULL,
  tone TEXT NOT NULL,
  ageAppropriatenessLevel INTEGER NOT NULL,
  createdAt INTEGER NOT NULL
)
```

#### badges
```sql
CREATE TABLE badges (
  id TEXT PRIMARY KEY,
  householdId TEXT NOT NULL,
  userId TEXT NOT NULL,
  badgeType TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  iconUrl TEXT,
  earnedAt INTEGER NOT NULL,
  progress INTEGER,
  isLocked BOOLEAN NOT NULL
)
```

#### streaks
```sql
CREATE TABLE streaks (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  householdId TEXT NOT NULL,
  currentCount INTEGER NOT NULL,
  bestCount INTEGER NOT NULL,
  lastCompletionDate TEXT,
  startDate TEXT,
  updatedAt INTEGER NOT NULL
)
```

#### efficiency_metrics
```sql
CREATE TABLE efficiency_metrics (
  id TEXT PRIMARY KEY,
  taskId TEXT NOT NULL,
  userId TEXT NOT NULL,
  householdId TEXT NOT NULL,
  estimatedDurationMinutes INTEGER,
  actualDurationMinutes INTEGER,
  efficiencyPercentage REAL,
  completedAt INTEGER NOT NULL
)
```

## Usage

### Accessing the Database

The database is provided via Hilt dependency injection:

```kotlin
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val database: AdhdfocusDatabase
) : ViewModel() {
    private val taskDao = database.taskDao()
    
    fun getTasks(householdId: String) = taskDao.getTasksByHousehold(householdId)
}
```

### Creating Backups

```kotlin
val backupManager = DatabaseBackupManager(context)
val backupPath = backupManager.createBackup()
if (backupPath != null) {
    // Backup created successfully
}
```

### Restoring from Backup

```kotlin
val backupManager = DatabaseBackupManager(context)
val success = backupManager.restoreFromBackup(backupPath)
if (success) {
    // Restore successful, restart app to reload database
}
```

## Configuration

### Database Builder (AppModule.kt)

```kotlin
Room.databaseBuilder(context, AdhdfocusDatabase::class.java, "adhdfocus_database")
    .addMigrations(*AdhdfocusDatabase.MIGRATIONS)
    .fallbackToDestructiveMigration()
    .addCallback(DatabaseInitializer.getCallback())
    .build()
```

**Options:**
- `addMigrations()`: Adds migration framework for schema changes
- `fallbackToDestructiveMigration()`: Clears database if migration missing (dev only)
- `addCallback()`: Initializes database with pre-populated data

## Migration Framework

For implementing future schema changes, see [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md).

### Adding a Migration

1. Increment database version in `AdhdfocusDatabase.kt`
2. Create migration class in `AdhdfocusDatabase.kt`
3. Add migration to `MIGRATIONS` array
4. Update entity models to match new schema
5. Test migration on old database version

## Backup and Recovery

### Backup Location

Backups are stored in the app's internal files directory:
```
/data/data/com.adhdfocus.app/files/database_backups/
```

### Backup Naming

Backups are named with timestamps:
```
adhdfocus_backup_2024-01-15_14-30-45.db
```

### Backup Management

- Backups are stored locally on the device
- Multiple backups can be maintained
- Backups can be deleted individually or all at once
- Total backup size can be monitored

## Best Practices

1. **Type Safety**: Use type converters for complex types (Instant, LocalDate)
2. **Null Safety**: Use nullable types for optional fields
3. **Defaults**: Provide sensible defaults for new columns
4. **Migrations**: Always implement proper migrations for production
5. **Testing**: Test database operations with unit tests
6. **Backup**: Regularly backup user data
7. **Performance**: Use indexes for frequently queried columns

## Future Enhancements

- [ ] Add database indexes for performance optimization
- [ ] Implement foreign key constraints for data integrity
- [ ] Add user preferences table (separate from users)
- [ ] Add sync queue table for offline changes
- [ ] Add current user table for family member switching
- [ ] Implement automatic backup scheduling
- [ ] Add cloud backup integration
- [ ] Implement database encryption at rest

## Troubleshooting

### Database Not Initializing

- Check that all entities are properly annotated with `@Entity`
- Verify DAOs are properly annotated with `@Dao`
- Ensure type converters are registered with `@TypeConverters`

### Migration Errors

- Verify migration version numbers are sequential
- Check that entity models match the new schema
- Test migration on a backup database first

### Backup/Restore Issues

- Ensure database is closed before restoring
- Check file permissions for backup directory
- Verify backup file exists before restoring

## References

- [Room Database Documentation](https://developer.android.com/training/data-storage/room)
- [Type Converters](https://developer.android.com/training/data-storage/room/referencing-data)
- [Database Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)
