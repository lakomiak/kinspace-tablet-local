package com.adhdfocus.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File

class LocalDataWipeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIPE_LOCAL_DATA) {
            return
        }

        if (!BuildConfig.DEBUG) {
            setResultCode(Activity.RESULT_CANCELED)
            setResultData("Local data wipe is only available in debug builds.")
            return
        }

        val appContext = context.applicationContext
        val deviceProtectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appContext.createDeviceProtectedStorageContext()
        } else {
            appContext
        }

        listOf(appContext, deviceProtectedContext).distinct().forEach { targetContext ->
            val dataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                targetContext.dataDir
            } else {
                File(targetContext.applicationInfo.dataDir)
            }
            targetContext.getSharedPreferences("tablet_setup", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            deleteDirectoryContents(File(dataDir, "shared_prefs"))
            deleteDirectoryContents(targetContext.filesDir)
            deleteDirectoryContents(targetContext.cacheDir)
            deleteDirectoryContents(targetContext.codeCacheDir)
            deleteDirectoryContents(targetContext.noBackupFilesDir)
        }

        listOf("adhdfocus_database", "adhdfocus.db").forEach { databaseName ->
            appContext.deleteDatabase(databaseName)
            deleteDatabaseSidecars(appContext.getDatabasePath(databaseName))
        }

        setResultCode(Activity.RESULT_OK)
        setResultData("KINSPACE_WIPE_COMPLETE")
    }

    private fun deleteDatabaseSidecars(databaseFile: File) {
        listOf(
            databaseFile,
            File(databaseFile.parentFile, "${databaseFile.name}-wal"),
            File(databaseFile.parentFile, "${databaseFile.name}-shm"),
            File(databaseFile.parentFile, "${databaseFile.name}-journal")
        ).forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun deleteDirectoryContents(directory: File?) {
        directory?.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }

    companion object {
        const val ACTION_WIPE_LOCAL_DATA = "com.adhdfocus.app.action.WIPE_LOCAL_DATA"
    }
}
