package com.adhdfocus.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme

/**
 * SettingsScreen displays comprehensive app settings organized into logical sections.
 *
 * Sections:
 * - Display settings (theme, text size, animations)
 * - Notification settings (enable/disable, frequency, sound)
 * - Behavior settings (daily reset time, auto-logout timeout)
 * - Todo_Group visibility toggles
 * - Affirmation settings (frequency, enable/disable)
 * - Gamification settings (badges, streaks, efficiency)
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
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val notificationPreferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()
    val dailyResetTime by viewModel.dailyResetTime.collectAsStateWithLifecycle()
    val affirmationFrequency by viewModel.affirmationFrequency.collectAsStateWithLifecycle()
    val gamificationEnabled by viewModel.gamificationEnabled.collectAsStateWithLifecycle()
    val badgesEnabled by viewModel.badgesEnabled.collectAsStateWithLifecycle()
    val streaksEnabled by viewModel.streaksEnabled.collectAsStateWithLifecycle()
    val efficiencyMetricsEnabled by viewModel.efficiencyMetricsEnabled.collectAsStateWithLifecycle()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Error message
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

            // Display Settings Section
            SettingSection(title = "Display") {
                ThemeSelector(
                    selectedTheme = theme,
                    onThemeSelected = { viewModel.updateTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notification Settings Section
            SettingSection(title = "Notifications") {
                NotificationPreferencesPanel(
                    preferences = notificationPreferences,
                    onPreferencesChanged = { viewModel.updateNotificationPreferences(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Behavior Settings Section
            SettingSection(title = "Behavior") {
                TimePickerField(
                    label = "Daily Reset Time",
                    value = dailyResetTime,
                    onValueChanged = { viewModel.updateDailyResetTime(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DurationInput(
                    label = "Auto-Logout Timeout (minutes, 0 = disabled)",
                    value = autoLogoutTimeout,
                    onValueChanged = { viewModel.updateAutoLogoutTimeout(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Affirmation Settings Section
            SettingSection(title = "Affirmations") {
                FrequencySlider(
                    label = "Affirmation Frequency",
                    value = affirmationFrequency,
                    onValueChanged = { viewModel.updateAffirmationFrequency(it.toInt()) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gamification Settings Section
            SettingSection(title = "Gamification") {
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Gamification Elements",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )

                SettingToggle(
                    label = "Badges",
                    checked = badgesEnabled,
                    onCheckedChange = { viewModel.updateBadgesEnabled(it) }
                )

                SettingToggle(
                    label = "Streaks",
                    checked = streaksEnabled,
                    onCheckedChange = { viewModel.updateStreaksEnabled(it) }
                )

                SettingToggle(
                    label = "Efficiency Metrics",
                    checked = efficiencyMetricsEnabled,
                    onCheckedChange = { viewModel.updateEfficiencyMetricsEnabled(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DurationInput(
                    label = "Timer Default Duration (minutes)",
                    value = timerDefaultDuration,
                    onValueChanged = { viewModel.updateTimerDefaultDuration(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // About Section
            SettingSection(title = "About") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ADHD Focus App",
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
                        text = "A distraction-free task management app designed for ADHD users",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
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

/**
 * SettingSection composable for grouping related settings.
 */
@Composable
fun SettingSection(
    title: String,
    content: @Composable () -> Unit
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
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
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
    onPreferencesChanged: (NotificationPreferences) -> Unit
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
    }
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
