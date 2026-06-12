package com.adhdfocus.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class KinspaceDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Kinspace device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin can prevent Kinspace from staying in dedicated-device mode."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Kinspace device admin disabled", Toast.LENGTH_SHORT).show()
    }
}
