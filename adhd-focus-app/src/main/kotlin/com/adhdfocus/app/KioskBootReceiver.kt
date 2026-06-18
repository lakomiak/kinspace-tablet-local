package com.adhdfocus.app

import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver

class KioskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        if (!BuildConfig.ENABLE_KIOSK_MODE) {
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
            devicePolicyManager.addPersistentPreferredActivity(
                admin,
                IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
                ComponentName(context, MainActivity::class.java)
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        val options = ActivityOptions.makeBasic().apply {
            setLockTaskEnabled(true)
        }
        context.startActivity(launchIntent, options.toBundle())
    }
}
