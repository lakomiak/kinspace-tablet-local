package com.adhdfocus.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class KinpilotDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Kinpilot device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin can prevent Kinpilot from staying in dedicated-device mode."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Kinpilot device admin disabled", Toast.LENGTH_SHORT).show()
    }
}
