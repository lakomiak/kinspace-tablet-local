package com.adhdfocus.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.BuildConfig
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.TimerAlarmSound
import com.adhdfocus.app.data.model.Theme

/**
 * SettingsScreen displays comprehensive app settings organized into logical sections.
 *
 * Sections:
 * - Display settings (theme, text size, animations)
 * - Notification settings (enable/disable, frequency, sound)
 * - Todo_Group visibility toggles
 * - Affirmation settings (frequency, enable/disable)
 * - About section
 *
 * Features:
 * - Per-member settings support
 * - Immediate persistence of changes
 * - Visual feedback for setting changes
 * - Reset to defaults option
 * - Loading and error states
 */
@Composable
fun SettingsScreen(
    userId: String,
    onViewReportsClick: () -> Unit,
    onManageFamilyClick: () -> Unit,
    onChangeMemberClick: () -> Unit,
    onRestartAppClick: () -> Unit,
    onOpenAccessibilitySettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val notificationPreferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()
    val affirmationFrequency by viewModel.affirmationFrequency.collectAsStateWithLifecycle()
    val settingsUnlocked by viewModel.settingsUnlocked.collectAsStateWithLifecycle()
    val hasSettingsPasscode by viewModel.hasSettingsPasscode.collectAsStateWithLifecycle()
    val allowTodoEditing by viewModel.allowTodoEditing.collectAsStateWithLifecycle()
    val customTodoGroups by viewModel.customTodoGroups.collectAsStateWithLifecycle()
    val showPasscodeSetupDialog by viewModel.showPasscodeSetupDialog.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val backupDirectory by viewModel.backupDirectory.collectAsStateWithLifecycle()
    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val restoreReady by viewModel.restoreReady.collectAsStateWithLifecycle()
    val restoreTargetName by viewModel.restoreTargetName.collectAsStateWithLifecycle()
    val tokenBank by viewModel.tokenBank.collectAsStateWithLifecycle()
    val tokenBankMessage by viewModel.tokenBankMessage.collectAsStateWithLifecycle()
    val kioskModeEnabled = BuildConfig.ENABLE_KIOSK_MODE

    var unlockPasscode by remember { mutableStateOf("") }
    var backupPendingRestore by remember { mutableStateOf<BackupListItem?>(null) }
    var backupPendingExport by remember { mutableStateOf<BackupListItem?>(null) }
    var tokenAmount by remember { mutableStateOf("1") }
    var tokenNote by remember { mutableStateOf("") }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(uri)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val backup = backupPendingExport
        if (uri != null && backup != null) {
            viewModel.exportBackupToUri(backup.path, uri)
        }
        backupPendingExport = null
    }

    DisposableEffect(hasSettingsPasscode) {
        onDispose {
            if (hasSettingsPasscode) {
                viewModel.lockSettings()
                unlockPasscode = ""
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val horizontalPadding = when {
                maxWidth < 600.dp -> 16.dp
                maxWidth < 900.dp -> 24.dp
                else -> 32.dp
            }
            val contentMaxWidth = when {
                maxWidth < 600.dp -> maxWidth
                maxWidth < 900.dp -> 760.dp
                else -> 940.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }

                if (hasSettingsPasscode && !settingsUnlocked) {
                    SettingSection(title = "Settings Locked") {
                        Text(
                            text = "Enter the 5-digit passcode to make changes to settings.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = unlockPasscode,
                            onValueChange = { newValue ->
                                unlockPasscode = newValue.filter { it.isDigit() }.take(5)
                            },
                            label = { Text("Passcode") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.unlockSettings(unlockPasscode) },
                                modifier = Modifier.weight(1f),
                                enabled = unlockPasscode.length == 5 && !isSaving
                            ) {
                                Text("Unlock")
                            }
                        }
                    }
                } else {
                    SettingSection(title = "Settings Passcode") {
                        Text(
                            text = if (hasSettingsPasscode) {
                                "Settings are protected by a 5-digit passcode."
                            } else {
                                "Protect settings with a 5-digit passcode."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.beginPasscodeSetup() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (hasSettingsPasscode) "Change Passcode" else "Set Passcode")
                            }
                            if (hasSettingsPasscode) {
                                OutlinedButton(
                                    onClick = { viewModel.lockSettings() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Lock Now")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Family") {
                        Text(
                            text = "Add or remove family members and choose which person this tablet should open for by default.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = onManageFamilyClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Family Members")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Token Bank") {
                        Text(
                            text = "Parents can add or remove tokens from a child account. To Do completions only earn tokens automatically for today's tasks.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (tokenBank.userId.isBlank()) {
                            Text(
                                text = "Choose a child before assigning tokens.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tokenBank.displayName,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${tokenBank.balance} token${if (tokenBank.balance == 1) "" else "s"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = tokenAmount,
                                onValueChange = {
                                    tokenAmount = it.filter { char -> char.isDigit() }.take(3)
                                    viewModel.clearTokenBankMessage()
                                },
                                label = { Text("Amount") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = tokenNote,
                                onValueChange = {
                                    tokenNote = it
                                    viewModel.clearTokenBankMessage()
                                },
                                label = { Text("Reason") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.adjustTokensForCurrentChild(tokenAmount, tokenNote, remove = false)
                                        tokenAmount = "1"
                                        tokenNote = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = tokenBank.userId.isNotBlank()
                                ) {
                                    Text("Add Tokens")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.adjustTokensForCurrentChild(tokenAmount, tokenNote, remove = true)
                                        tokenAmount = "1"
                                        tokenNote = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = tokenBank.userId.isNotBlank()
                                ) {
                                    Text("Remove")
                                }
                            }

                            tokenBankMessage?.let { message ->
                                Text(
                                    text = message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Storage") {
                        Text(
                            text = "Track how much local space Kinpilot is using for live family data and backups.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Live tablet data: ${formatBytes(storageUsage.databaseSizeBytes)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Backups on this tablet: ${formatBytes(storageUsage.backupSizeBytes)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Free device storage: ${formatBytes(storageUsage.availableStorageBytes)} of ${formatBytes(storageUsage.totalStorageBytes)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Local Backup") {
                        Text(
                            text = "Create exportable local backups of this tablet's family members, todos, reports, streaks, and settings.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Backup folder: $backupDirectory",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (backupStatusMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = backupStatusMessage!!,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.createBackup() },
                                modifier = Modifier.weight(1f),
                                enabled = !backupBusy
                            ) {
                                Text(if (backupBusy) "Working..." else "Create Backup")
                            }
                            OutlinedButton(
                                onClick = { viewModel.refreshBackupList() },
                                modifier = Modifier.weight(1f),
                                enabled = !backupBusy
                            ) {
                                Text("Refresh")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                modifier = Modifier.weight(1f),
                                enabled = !backupBusy
                            ) {
                                Text("Import Backup File")
                            }
                        }

                        if (backups.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.deleteAllBackups() },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete All Backups")
                            }
                        }

                        if (backups.isEmpty()) {
                            Text(
                                text = "No backups created yet.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            backups.forEach { backup ->
                                BackupItemRow(
                                    backup = backup,
                                    enabled = !backupBusy,
                                    onRestore = { backupPendingRestore = backup },
                                    onExport = {
                                        backupPendingExport = backup
                                        exportBackupLauncher.launch(backup.displayName)
                                    },
                                    onDelete = { viewModel.deleteBackup(backup.path) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Kiosk Help") {
                        Text(
                            text = "Use Kinpilot Tablet Local as the dedicated focus home screen.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Recommended steps:\n1. Install Kinpilot on the device\n2. Set Kinpilot as the Home app when Android asks\n3. Keep Settings protected with a passcode\n4. Create local backups regularly",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!kioskModeEnabled) {
                            Text(
                                text = "If you need TalkBack or other accessibility services, open Accessibility Settings from here before returning to kiosk mode.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_MAIN).apply {
                                            addCategory(Intent.CATEGORY_HOME)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Home Chooser")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("App Settings")
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenAccessibilitySettingsClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Accessibility Settings")
                            }
                        }
                        Text(
                            text = "For stricter managed-device setup, see KIOSK_DEPLOYMENT.md in the repo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Todo Management") {
                        Text(
                            text = "Allow editing or deleting todos from the Home screen.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SettingToggle(
                            label = "Allow edit/delete To Dos",
                            checked = allowTodoEditing,
                            onCheckedChange = { viewModel.updateTodoEditingEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Custom Time Periods") {
                        CustomTodoGroupsPanel(
                            categories = customTodoGroups,
                            onAddCategory = { viewModel.addCustomTodoGroup(it) },
                            onRemoveCategory = { viewModel.removeCustomTodoGroup(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Display") {
                        ThemeSelector(
                            selectedTheme = theme,
                            onThemeSelected = { viewModel.updateTheme(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Notifications") {
                        NotificationPreferencesPanel(
                            preferences = notificationPreferences,
                            onPreferencesChanged = { viewModel.updateNotificationPreferences(it) },
                            onPreviewTimerAlarm = { viewModel.previewTimerAlarm() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Time Period Reminders") {
                        CategoryReminderPreferencesPanel(
                            preferences = notificationPreferences,
                            customGroups = customTodoGroups,
                            onPreferencesChanged = { viewModel.updateNotificationPreferences(it) },
                            onPreviewReminder = { viewModel.previewCategoryReminder() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSection(title = "Affirmations") {
                        Text(
                            text = "Adjust how often Kinpilot shows encouraging affirmations after progress milestones.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FrequencySlider(
                            label = "Affirmation Frequency",
                            value = affirmationFrequency,
                            onValueChanged = { viewModel.updateAffirmationFrequency(it.toInt()) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                SettingSection(title = "About") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Kinpilot",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Version 1.0.0",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Dedicated focus support for tasks, timers, and progress",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!hasSettingsPasscode || settingsUnlocked) {
                    SettingSection(title = "Reporting") {
                        Text(
                            text = "View local completion, streak, timer, pause, restart, and cancel patterns for each family member using this tablet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onViewReportsClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Reports")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Done")
                        }
                    }

                    if (!hasSettingsPasscode || settingsUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onChangeMemberClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isSaving
                            ) {
                                Text("Switch Member")
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetToDefaults() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isSaving
                            ) {
                                Text("Reset Settings")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasscodeSetupDialog) {
        SettingsPasscodeSetupDialog(
            title = if (hasSettingsPasscode) "Change Settings Passcode" else "Set Settings Passcode",
            onDismiss = { viewModel.dismissPasscodeSetup() },
            onConfirm = { passcode -> viewModel.saveSettingsPasscode(passcode) },
            isLoading = isSaving,
            errorMessage = errorMessage
        )
    }

    if (backupPendingRestore != null) {
        val backup = backupPendingRestore!!
        AlertDialog(
            onDismissRequest = { if (!backupBusy) backupPendingRestore = null },
            title = { Text("Restore Backup") },
            text = {
                Text("Restore ${backup.displayName}? This will replace the current local tablet data and require an app restart.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(backup.path)
                        backupPendingRestore = null
                    },
                    enabled = !backupBusy
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { backupPendingRestore = null },
                    enabled = !backupBusy
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (restoreReady) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restart Required") },
            text = {
                Text("The backup ${restoreTargetName ?: "data"} was restored. Restart Kinpilot Tablet Local now to load the restored household and reports.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acknowledgeRestoreRestart()
                        onRestartAppClick()
                    }
                ) {
                    Text("Restart Now")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    val decimals = if (value >= 100 || unitIndex == 0) 0 else 1
    return "%.${decimals}f %s".format(value, units[unitIndex])
}

/**
 * SettingSection composable for grouping related settings.
 */
@Composable
fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
fun BackupItemRow(
    backup: BackupListItem,
    enabled: Boolean,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = backup.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${backup.subtitle} • ${backup.sizeLabel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore")
                }
                OutlinedButton(
                    onClick = onExport,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * SettingToggle composable for boolean settings.
 */
@Composable
fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * SettingSlider composable for numeric settings.
 */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toInt().toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * SettingDropdown composable for choice settings.
 */
@Composable
fun SettingDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                Button(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = selectedValue != option
                ) {
                    Text(option)
                }
            }
        }
    }
}

/**
 * ThemeSelector composable for selecting light/dark theme.
 */
@Composable
fun ThemeSelector(
    selectedTheme: Theme,
    onThemeSelected: (Theme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Theme.values().forEach { theme ->
            Button(
                onClick = { onThemeSelected(theme) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                enabled = selectedTheme != theme
            ) {
                Text(theme.name)
            }
        }
    }
}

/**
 * NotificationPreferencesPanel composable for managing notification settings.
 */
@Composable
fun NotificationPreferencesPanel(
    preferences: NotificationPreferences,
    onPreferencesChanged: (NotificationPreferences) -> Unit,
    onPreviewTimerAlarm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingToggle(
            label = "Sound",
            checked = preferences.soundEnabled,
            onCheckedChange = {
                onPreferencesChanged(preferences.copy(soundEnabled = it))
            }
        )

        SettingToggle(
            label = "Vibration",
            checked = preferences.vibrationEnabled,
            onCheckedChange = {
                onPreferencesChanged(preferences.copy(vibrationEnabled = it))
            }
        )

        SettingToggle(
            label = "Visual Alerts",
            checked = preferences.visualAlertsEnabled,
            onCheckedChange = {
                onPreferencesChanged(preferences.copy(visualAlertsEnabled = it))
            }
        )

        SettingDropdown(
            label = "Timer Alarm Sound",
            selectedValue = preferences.timerAlarmSound.name,
            options = TimerAlarmSound.values().map { it.name },
            onOptionSelected = { selected -> 
                val alarmSound = runCatching { TimerAlarmSound.valueOf(selected) }
                    .getOrDefault(TimerAlarmSound.ALARM)
                onPreferencesChanged(preferences.copy(timerAlarmSound = alarmSound))
            }
        )
        Text(
            text = "Current sound: ${preferences.timerAlarmSound.displayLabel()}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onPreviewTimerAlarm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Preview ${preferences.timerAlarmSound.displayLabel()}")
        }
    }
}

private fun TimerAlarmSound.displayLabel(): String = when (this) {
    TimerAlarmSound.ALARM -> "Alarm"
    TimerAlarmSound.NOTIFICATION -> "Notify"
    TimerAlarmSound.BEEP -> "Beep"
    TimerAlarmSound.MULTI_BEEP -> "Triple"
    TimerAlarmSound.SILENT -> "Silent"
}

/**
 * TimePickerField composable for selecting time in HH:mm format with 15-minute increments.
 * Supports times from 00:00 to 23:45 in 15-minute increments.
 */
@Composable
fun TimePickerField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Generate all valid times (15-minute increments from 00:00 to 23:45)
        val validTimes = generateValidTimes()
        
        // Display current time
        Text(
            text = "Current: $value",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Time picker with scroll
        DailyResetTimePicker(
            selectedTime = value,
            onTimeSelected = onValueChanged,
            validTimes = validTimes
        )
    }
}

/**
 * DailyResetTimePicker composable for selecting daily reset time with 15-minute increments.
 */
@Composable
fun DailyResetTimePicker(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    validTimes: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hour and Minute selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour selector
            TimeComponentSelector(
                label = "Hour",
                value = selectedTime.substringBefore(":").toIntOrNull() ?: 0,
                onValueChanged = { hour ->
                    val minute = selectedTime.substringAfter(":").toIntOrNull() ?: 0
                    val newTime = String.format("%02d:%02d", hour, minute)
                    if (validTimes.contains(newTime)) {
                        onTimeSelected(newTime)
                    }
                },
                range = 0..23,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Minute selector (15-minute increments)
            TimeComponentSelector(
                label = "Minute",
                value = selectedTime.substringAfter(":").toIntOrNull() ?: 0,
                onValueChanged = { minute ->
                    val hour = selectedTime.substringBefore(":").toIntOrNull() ?: 0
                    val newTime = String.format("%02d:%02d", hour, minute)
                    if (validTimes.contains(newTime)) {
                        onTimeSelected(newTime)
                    }
                },
                range = 0..45,
                step = 15,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick preset buttons
        Text(
            text = "Quick Presets",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("00:00", "06:00", "12:00", "18:00").forEach { time ->
                Button(
                    onClick = { onTimeSelected(time) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    enabled = selectedTime != time
                ) {
                    Text(time, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * TimeComponentSelector composable for selecting hour or minute.
 */
@Composable
fun TimeComponentSelector(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
    range: IntRange,
    step: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val newValue = (value - step).coerceIn(range)
                    onValueChanged(newValue)
                },
                modifier = Modifier
                    .weight(0.3f)
                    .height(36.dp)
            ) {
                Text("-", fontSize = 14.sp)
            }

            Text(
                text = String.format("%02d", value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(0.4f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = {
                    val newValue = (value + step).coerceIn(range)
                    onValueChanged(newValue)
                },
                modifier = Modifier
                    .weight(0.3f)
                    .height(36.dp)
            ) {
                Text("+", fontSize = 14.sp)
            }
        }
    }
}

/**
 * Generates all valid times in 15-minute increments from 00:00 to 23:45.
 */
fun generateValidTimes(): List<String> {
    val times = mutableListOf<String>()
    for (hour in 0..23) {
        for (minute in listOf(0, 15, 30, 45)) {
            times.add(String.format("%02d:%02d", hour, minute))
        }
    }
    return times
}

/**
 * FrequencySlider composable for selecting frequency (1-5).
 */
@Composable
fun FrequencySlider(
    label: String,
    value: Int,
    onValueChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = onValueChanged,
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * DurationInput composable for entering duration in minutes.
 */
@Composable
fun DurationInput(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextField(
            value = value.toString(),
            onValueChange = { newValue ->
                val intValue = newValue.toIntOrNull() ?: 0
                onValueChanged(intValue)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter minutes") },
            singleLine = true
        )
    }
}
