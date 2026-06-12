package com.adhdfocus.app.data.database

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DatabaseBackupInfo(
    val path: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedAt: Long
)

/**
 * Manages database backup and recovery operations.
 * Provides utilities to backup the database to external storage and restore from backups.
 */
@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AdhdfocusDatabase
) {

    private val backupDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        "kinspace_backups"
    )
    private val databaseFile = context.getDatabasePath("adhdfocus_database")
    private val walFile = File(databaseFile.parentFile, "${databaseFile.name}-wal")
    private val shmFile = File(databaseFile.parentFile, "${databaseFile.name}-shm")

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
    fun createBackup(): DatabaseBackupInfo? {
        return try {
            if (!databaseFile.exists()) {
                return null
            }

            checkpointDatabase()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val backupFile = File(backupDir, "kinspace_backup_$timestamp.zip")

            ZipOutputStream(backupFile.outputStream().buffered()).use { zipOutputStream ->
                listOf(databaseFile, walFile, shmFile)
                    .filter { it.exists() }
                    .forEach { file ->
                        zipOutputStream.putNextEntry(ZipEntry(file.name))
                        file.inputStream().buffered().use { input ->
                            input.copyTo(zipOutputStream)
                        }
                        zipOutputStream.closeEntry()
                    }

                val manifest = buildManifestJson(timestamp)
                zipOutputStream.putNextEntry(ZipEntry("backup_manifest.json"))
                zipOutputStream.write(manifest.toByteArray(Charsets.UTF_8))
                zipOutputStream.closeEntry()
            }

            backupFile.toBackupInfo()
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

            database.close()
            if (databaseFile.exists()) {
                databaseFile.delete()
            }
            if (walFile.exists()) {
                walFile.delete()
            }
            if (shmFile.exists()) {
                shmFile.delete()
            }

            ZipInputStream(backupFile.inputStream().buffered()).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    val target = when (entry.name) {
                        databaseFile.name -> databaseFile
                        walFile.name -> walFile
                        shmFile.name -> shmFile
                        else -> null
                    }
                    if (target != null) {
                        target.outputStream().buffered().use { output ->
                            zipInputStream.copyTo(output)
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
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
    fun getAvailableBackups(): List<DatabaseBackupInfo> {
        return try {
            backupDir.listFiles()?.filter { it.name.startsWith("kinspace_backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { it.toBackupInfo() }
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
                if (file.name.startsWith("kinspace_backup_") && file.delete()) {
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

    fun getDatabaseSize(): Long {
        return try {
            listOf(databaseFile, walFile, shmFile)
                .filter { it.exists() }
                .sumOf { it.length() }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val statFs = StatFs((context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir).absolutePath)
            statFs.availableBytes
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun getTotalStorageBytes(): Long {
        return try {
            val statFs = StatFs((context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir).absolutePath)
            statFs.totalBytes
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun getBackupDirectoryPath(): String = backupDir.absolutePath

    fun importBackupFromUri(uri: Uri): DatabaseBackupInfo? {
        return try {
            val name = buildImportedBackupName(uri)
            val importedFile = File(backupDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                importedFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            importedFile.toBackupInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportBackupToUri(backupPath: String, targetUri: Uri): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return false
            }
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                backupFile.inputStream().buffered().use { input ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun checkpointDatabase() {
        val sqliteDatabase = database.openHelper.writableDatabase
        sqliteDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
    }

    private fun buildManifestJson(timestamp: String): String {
        val householdId = context.getSharedPreferences("tablet_setup", Context.MODE_PRIVATE)
            .getString("household_id", "")
            .orEmpty()
        val installType = context.getSharedPreferences("tablet_setup", Context.MODE_PRIVATE)
            .getString("install_type", "")
            .orEmpty()
        return """
            {
              "createdAt": "$timestamp",
              "database": "${databaseFile.name}",
              "householdId": "$householdId",
              "installType": "$installType",
              "format": "kinspace-local-backup-v1"
            }
        """.trimIndent()
    }

    private fun File.toBackupInfo(): DatabaseBackupInfo {
        return DatabaseBackupInfo(
            path = absolutePath,
            displayName = name,
            sizeBytes = length(),
            lastModifiedAt = lastModified()
        )
    }

    private fun buildImportedBackupName(uri: Uri): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val sourceName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
            ?.lowercase(Locale.US)
            ?.replace(Regex("[^a-z0-9._-]+"), "-")
            ?.trim('-')
        val suffix = when {
            sourceName?.endsWith(".zip") == true -> ".zip"
            else -> ".zip"
        }
        return "kinspace_backup_imported_${timestamp}$suffix"
    }
}
