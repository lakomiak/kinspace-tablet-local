package com.adhdfocus.app.ui.family

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.TimerAlarmSound
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.ui.settings.CategoryReminderPreferencesPanel
import com.adhdfocus.app.ui.settings.CustomTodoGroupsPanel

/**
 * UserPreferencesScreen displays and manages per-member preferences.
 *
 * Features:
 * - Theme selection (light/dark)
 * - Todo group visibility toggles
 * - Notification preferences
 * - Daily reset time picker
 * - Affirmation frequency slider
 * - Gamification toggle
 * - Timer default duration input
 * - Auto-logout timeout input
 * - Reset to defaults button
 * - Save/cancel buttons
 * - Loading and error states
 */
@Composable
fun UserPreferencesScreen(
    userId: String,
    onBackClick: () -> Unit,
    viewModel: UserPreferencesViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val customTodoGroups by viewModel.customTodoGroups.collectAsStateWithLifecycle()
    val notificationPreferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()
    val dailyResetTime by viewModel.dailyResetTime.collectAsStateWithLifecycle()
    val affirmationFrequency by viewModel.affirmationFrequency.collectAsStateWithLifecycle()
    val gamificationEnabled by viewModel.gamificationEnabled.collectAsStateWithLifecycle()
    val timerDefaultDuration by viewModel.timerDefaultDuration.collectAsStateWithLifecycle()
    val autoLogoutTimeout by viewModel.autoLogoutTimeout.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

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
                    text = "Preferences",
                    fontSize = 28.sp,
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

                ThemeSelector(
                    selectedTheme = theme,
                    onThemeSelected = { viewModel.updateTheme(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTodoGroupsPanel(
                    categories = customTodoGroups,
                    onAddCategory = { viewModel.addCustomTodoGroup(it) },
                    onRemoveCategory = { viewModel.removeCustomTodoGroup(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NotificationPreferencesPanel(
                    preferences = notificationPreferences,
                    onPreferencesChanged = { viewModel.updateNotificationPreferences(it) },
                    onPreviewTimerAlarm = { viewModel.previewTimerAlarm() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CategoryReminderPreferencesPanel(
                    preferences = notificationPreferences,
                    onPreferencesChanged = { viewModel.updateNotificationPreferences(it) },
                    onPreviewReminder = { viewModel.previewCategoryReminder() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimePickerField(
                    label = "Daily Reset Time",
                    value = dailyResetTime,
                    onValueChanged = { viewModel.updateDailyResetTime(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FrequencySlider(
                    label = "Affirmation Frequency",
                    value = affirmationFrequency,
                    onValueChanged = { viewModel.updateAffirmationFrequency(it.toInt()) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Gamification",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = gamificationEnabled,
                        onCheckedChange = { viewModel.updateGamificationEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                DurationInput(
                    label = "Timer Default Duration (minutes)",
                    value = timerDefaultDuration,
                    onValueChanged = { viewModel.updateTimerDefaultDuration(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DurationInput(
                    label = "Auto-Logout Timeout (minutes, 0 = disabled)",
                    value = autoLogoutTimeout,
                    onValueChanged = { viewModel.updateAutoLogoutTimeout(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !isSaving
                    ) {
                        Text("Reset to Defaults")
                    }

                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Theme",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Notifications",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Sound
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sound",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = preferences.soundEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(soundEnabled = it))
                }
            )
        }

        // Vibration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vibration",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = preferences.vibrationEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(vibrationEnabled = it))
                }
            )
        }

        // Visual Alerts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visual Alerts",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = preferences.visualAlertsEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(visualAlertsEnabled = it))
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Timer Alarm Sound",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimerAlarmSound.values().forEach { sound ->
                    OutlinedButton(
                        onClick = { onPreferencesChanged(preferences.copy(timerAlarmSound = sound)) },
                        modifier = Modifier.weight(1f),
                        enabled = preferences.timerAlarmSound != sound
                    ) {
                        Text(
                            text = when (sound) {
                                TimerAlarmSound.ALARM -> "Alarm"
                                TimerAlarmSound.NOTIFICATION -> "Notify"
                                TimerAlarmSound.BEEP -> "Beep"
                                TimerAlarmSound.MULTI_BEEP -> "Triple"
                                TimerAlarmSound.SILENT -> "Silent"
                            }
                        )
                    }
                }
            }

            Text(
                text = "Current sound: ${preferences.timerAlarmSound.displayLabel()}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
 * TimePickerField composable for selecting time in HH:mm format.
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
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("HH:mm") },
            singleLine = true
        )
    }
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
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                fontSize = 16.sp,
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
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
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
