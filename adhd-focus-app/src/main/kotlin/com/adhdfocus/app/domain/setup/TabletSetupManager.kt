package com.adhdfocus.app.domain.setup

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the one-time tablet setup — which family member this tablet is assigned to.
 * Stored in plain SharedPreferences (not sensitive data).
 */
@Singleton
class TabletSetupManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tablet_setup", Context.MODE_PRIVATE)

    fun isSetupComplete(): Boolean = prefs.getString(KEY_MEMBER_ID, null) != null

    fun getAssignedMemberId(): String? = prefs.getString(KEY_MEMBER_ID, null)

    fun getAssignedMemberName(): String? = prefs.getString(KEY_MEMBER_NAME, null)

    fun getHouseholdId(): String? = prefs.getString(KEY_HOUSEHOLD_ID, null)

    fun completeSetup(memberId: String, memberName: String, householdId: String) {
        prefs.edit()
            .putString(KEY_MEMBER_ID, memberId)
            .putString(KEY_MEMBER_NAME, memberName)
            .putString(KEY_HOUSEHOLD_ID, householdId)
            .apply()
    }

    fun resetSetup() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_MEMBER_ID = "assigned_member_id"
        private const val KEY_MEMBER_NAME = "assigned_member_name"
        private const val KEY_HOUSEHOLD_ID = "household_id"
    }
}
