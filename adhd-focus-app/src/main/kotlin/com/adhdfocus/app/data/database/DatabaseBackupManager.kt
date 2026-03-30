package com.adhdfocus.app.data.database

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages database backup and recovery operations.
 * Provides utilities to backup the database to external storage and restore from backups.
 */
class DatabaseBackupManager(private val context: Context) {

    private val backupDir = File(context.filesDir, "database_backups")
    private val databaseFile = context.getDatabasePath("adhdfocus_database")

    init {
        // Ensure backup directory exists
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    /**
     * Create a backup of the current database.
     * Returns the path to the backup file if successful, null otherwise.
     */
    fun createBackup(): String? {
        return try {
            if (!databaseFile.exists()) {
                return null
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val backupFile = File(backupDir, "adhdfocus_backup_$timestamp.db")

            databaseFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            backupFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restore the database from a backup file.
     * Returns true if restoration was successful, false otherwise.
     */
    fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return false
            }

            // Close the database connection before restoring
            // This should be called from the application layer
            backupFile.inputStream().use { input ->
                databaseFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get a list of all available backups.
     * Returns a list of backup file paths sorted by creation time (newest first).
     */
    fun getAvailableBackups(): List<String> {
        return try {
            backupDir.listFiles()?.filter { it.name.startsWith("adhdfocus_backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { it.absolutePath }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Delete a specific backup file.
     * Returns true if deletion was successful, false otherwise.
     */
    fun deleteBackup(backupPath: String): Boolean {
        return try {
            File(backupPath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete all backup files.
     * Returns the number of backups deleted.
     */
    fun deleteAllBackups(): Int {
        return try {
            var count = 0
            backupDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("adhdfocus_backup_") && file.delete()) {
                    count++
                }
            }
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Get the size of a backup file in bytes.
     */
    fun getBackupSize(backupPath: String): Long {
        return try {
            File(backupPath).length()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * Get the total size of all backups in bytes.
     */
    fun getTotalBackupSize(): Long {
        return try {
            backupDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
