package com.adhdfocus.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver

class KioskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        if (!BuildConfig.ENABLE_KIOSK_MODE) {
            return
        }

        val shouldDeduplicateForBoot =
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == Intent.ACTION_BOOT_COMPLETED
        val currentBootCount = if (shouldDeduplicateForBoot) {
            runCatching {
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
            }.getOrDefault(-1)
        } else {
            -1
        }
        val bootStatePrefs = context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (
            shouldDeduplicateForBoot &&
            currentBootCount >= 0 &&
            bootStatePrefs.getInt(KEY_LAST_HANDLED_BOOT_COUNT, -1) == currentBootCount
        ) {
            return
        }

        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
            ?: return
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            return
        }

        val admin = ComponentName(context, KinspaceDeviceAdminReceiver::class.java)
        runCatching {
            devicePolicyManager.setLockTaskPackages(admin, arrayOf(context.packageName))
            devicePolicyManager.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                devicePolicyManager.setStatusBarDisabled(admin, true)
            }
            devicePolicyManager.addPersistentPreferredActivity(
                admin,
                IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
                ComponentName(context, MainActivity::class.java)
            )
        }

        val launched = runCatching {
            launchKinspace(context)
        }.isSuccess
        if (launched && shouldDeduplicateForBoot && currentBootCount >= 0) {
            bootStatePrefs.edit().putInt(KEY_LAST_HANDLED_BOOT_COUNT, currentBootCount).apply()
        }
    }

    private fun launchKinspace(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        context.startActivity(launchIntent)
    }

    companion object {
        private const val PREF_NAME = "kiosk_boot_state"
        private const val KEY_LAST_HANDLED_BOOT_COUNT = "last_handled_boot_count"
    }
}
