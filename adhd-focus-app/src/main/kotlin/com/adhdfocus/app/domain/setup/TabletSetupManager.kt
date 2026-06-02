package com.adhdfocus.app.domain.setup

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
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

    fun getHouseholdName(): String? = prefs.getString(KEY_HOUSEHOLD_NAME, null)

    fun getCurrentFocusDate(): LocalDate? = prefs.getString(KEY_CURRENT_FOCUS_DATE, null)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun getSettingsPasscodeHash(): String? = prefs.getString(KEY_SETTINGS_PASSCODE_HASH, null)

    fun setSettingsPasscodeHash(hash: String?) {
        prefs.edit().apply {
            if (hash.isNullOrBlank()) {
                remove(KEY_SETTINGS_PASSCODE_HASH)
            } else {
                putString(KEY_SETTINGS_PASSCODE_HASH, hash)
            }
        }.apply()
    }

    fun setCurrentFocusDate(date: LocalDate) {
        prefs.edit()
            .putString(KEY_CURRENT_FOCUS_DATE, date.toString())
            .apply()
    }

    fun completeSetup(memberId: String, memberName: String, householdId: String, householdName: String? = null) {
        prefs.edit()
            .putString(KEY_MEMBER_ID, memberId)
            .putString(KEY_MEMBER_NAME, memberName)
            .putString(KEY_HOUSEHOLD_ID, householdId)
            .apply {
                if (!householdName.isNullOrBlank()) {
                    putString(KEY_HOUSEHOLD_NAME, householdName)
                }
            }
            .apply()
    }

    fun resetSetup() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_MEMBER_ID = "assigned_member_id"
        private const val KEY_MEMBER_NAME = "assigned_member_name"
        private const val KEY_HOUSEHOLD_ID = "household_id"
        private const val KEY_HOUSEHOLD_NAME = "household_name"
        private const val KEY_CURRENT_FOCUS_DATE = "current_focus_date"
        private const val KEY_SETTINGS_PASSCODE_HASH = "settings_passcode_hash"
    }
}
