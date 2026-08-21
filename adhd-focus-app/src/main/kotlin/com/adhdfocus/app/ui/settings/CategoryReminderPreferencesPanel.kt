package com.adhdfocus.app.ui.settings

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.adhdfocus.app.data.model.CustomTimePeriodReminderPreference
import com.adhdfocus.app.data.model.CategoryReminderPreferences
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.reminder.TodoCategoryReminder

@Composable
fun CategoryReminderPreferencesPanel(
    preferences: NotificationPreferences,
    customGroups: List<String> = emptyList(),
    onPreferencesChanged: (NotificationPreferences) -> Unit,
    onPreviewReminder: () -> Unit
) {
    val reminderPreferences = preferences.categoryReminderPreferences

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Time Period Reminders",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audible reminders before time period alert times",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Choose which time period alarms are active, set each time period alert time, and decide how many minutes before each one sounds.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = reminderPreferences.enabled,
                onCheckedChange = {
                    onPreferencesChanged(
                        preferences.copy(
                            categoryReminderPreferences = reminderPreferences.copy(enabled = it)
                        )
                    )
                }
            )
        }

        if (reminderPreferences.enabled) {
            CategoryReminderToggleList(
                preferences = reminderPreferences,
                onPreferencesChanged = { updated ->
                    onPreferencesChanged(preferences.copy(categoryReminderPreferences = updated))
                }
            )

            CategoryReminderLeadPickerGrid(
                preferences = reminderPreferences,
                onPreferencesChanged = { updated ->
                    onPreferencesChanged(preferences.copy(categoryReminderPreferences = updated))
                }
            )

            if (customGroups.isNotEmpty()) {
                CustomTimePeriodReminderGrid(
                    customGroups = customGroups,
                    preferences = preferences,
                    onPreferencesChanged = onPreferencesChanged
                )
            }

            OutlinedButton(
                onClick = onPreviewReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview Reminder")
            }
        } else {
            Text(
                text = "Enable time period reminders to choose alert times and lead times for Morning, Afternoon, Evening, Bedtime, and any custom time periods you add.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryReminderToggleList(
    preferences: CategoryReminderPreferences,
    onPreferencesChanged: (CategoryReminderPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Time Period Alarm Toggles",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Turn each time period alarm on or off individually.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TodoCategoryReminder.values().forEach { category ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Alarm before ${endTimeFor(category, preferences)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enabledFor(category, preferences),
                    onCheckedChange = { newValue ->
                        onPreferencesChanged(
                            preferences.copy(
                                morningEnabled = if (category == TodoCategoryReminder.MORNING) newValue else preferences.morningEnabled,
                                afternoonEnabled = if (category == TodoCategoryReminder.AFTERNOON) newValue else preferences.afternoonEnabled,
                                eveningEnabled = if (category == TodoCategoryReminder.EVENING) newValue else preferences.eveningEnabled,
                                bedtimeEnabled = if (category == TodoCategoryReminder.BEDTIME) newValue else preferences.bedtimeEnabled
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryReminderLeadPickerGrid(
    preferences: CategoryReminderPreferences,
    onPreferencesChanged: (CategoryReminderPreferences) -> Unit
) {
    val categories = remember { TodoCategoryReminder.values().toList() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryReminderLeadPicker(
                        category = category,
                        categoryEnabled = enabledFor(category, preferences),
                        endTime = endTimeFor(category, preferences),
                        leadMinutes = leadMinutesFor(category, preferences),
                        onCategoryEnabledChanged = { newValue ->
                            onPreferencesChanged(
                                preferences.copy(
                                    morningEnabled = if (category == TodoCategoryReminder.MORNING) newValue else preferences.morningEnabled,
                                    afternoonEnabled = if (category == TodoCategoryReminder.AFTERNOON) newValue else preferences.afternoonEnabled,
                                    eveningEnabled = if (category == TodoCategoryReminder.EVENING) newValue else preferences.eveningEnabled,
                                    bedtimeEnabled = if (category == TodoCategoryReminder.BEDTIME) newValue else preferences.bedtimeEnabled
                                )
                            )
                        },
                        onEndTimeChanged = { newValue ->
                            onPreferencesChanged(
                                preferences.copy(
                                    morningEndTime = if (category == TodoCategoryReminder.MORNING) newValue else preferences.morningEndTime,
                                    afternoonEndTime = if (category == TodoCategoryReminder.AFTERNOON) newValue else preferences.afternoonEndTime,
                                    eveningEndTime = if (category == TodoCategoryReminder.EVENING) newValue else preferences.eveningEndTime,
                                    bedtimeEndTime = if (category == TodoCategoryReminder.BEDTIME) newValue else preferences.bedtimeEndTime
                                )
                            )
                        },
                        onLeadMinutesChanged = { newValue ->
                            onPreferencesChanged(
                                preferences.copy(
                                    morningLeadMinutes = if (category == TodoCategoryReminder.MORNING) newValue else preferences.morningLeadMinutes,
                                    afternoonLeadMinutes = if (category == TodoCategoryReminder.AFTERNOON) newValue else preferences.afternoonLeadMinutes,
                                    eveningLeadMinutes = if (category == TodoCategoryReminder.EVENING) newValue else preferences.eveningLeadMinutes,
                                    bedtimeLeadMinutes = if (category == TodoCategoryReminder.BEDTIME) newValue else preferences.bedtimeLeadMinutes
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryReminderLeadPicker(
    category: TodoCategoryReminder,
    categoryEnabled: Boolean,
    endTime: String,
    leadMinutes: Int,
    onCategoryEnabledChanged: (Boolean) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onLeadMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerInitialized by remember(category) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = category.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (categoryEnabled) "Alarm on" else "Alarm off",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = categoryEnabled,
                onCheckedChange = onCategoryEnabledChanged
            )
        }
        Text(
            text = "Alerts at $endTime",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$leadMinutes minute${if (leadMinutes == 1) "" else "s"} before alert",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )

        TimePickerField(
            label = "Time Period Alert Time",
            value = endTime,
            onValueChanged = onEndTimeChanged
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    minValue = 0
                    maxValue = 720
                    wrapSelectorWheel = false
                    isEnabled = categoryEnabled
                    setOnValueChangedListener { _, _, newVal ->
                        if (pickerInitialized) {
                            onLeadMinutesChanged(newVal)
                        }
                    }
                }
            },
            update = { picker ->
                picker.minValue = 0
                picker.maxValue = 720
                picker.isEnabled = categoryEnabled
                picker.alpha = if (categoryEnabled) 1f else 0.5f
                val clamped = leadMinutes.coerceIn(0, 720)
                if (picker.value != clamped) {
                    picker.value = clamped
                }
                pickerInitialized = true
            }
        )
    }
}

@Composable
private fun CustomTimePeriodReminderGrid(
    customGroups: List<String>,
    preferences: NotificationPreferences,
    onPreferencesChanged: (NotificationPreferences) -> Unit
) {
    val customRemindersByGroup = remember(customGroups, preferences.customTimePeriodReminderPreferences) {
        preferences.customTimePeriodReminderPreferences.associateBy { it.groupName.lowercase() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Custom Time Periods",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "New time periods are added here automatically and can use their own reminder time.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        customGroups.forEach { groupName ->
            val reminder = customRemindersByGroup[groupName.lowercase()]
                ?: CustomTimePeriodReminderPreference(groupName = groupName)
            CustomTimePeriodReminderCard(
                reminder = reminder,
                onReminderChanged = { updated ->
                    val updatedList = preferences.customTimePeriodReminderPreferences
                        .filterNot { it.groupName.equals(groupName, ignoreCase = true) } + updated
                    onPreferencesChanged(
                        preferences.copy(customTimePeriodReminderPreferences = updatedList)
                    )
                }
            )
        }
    }
}

@Composable
private fun CustomTimePeriodReminderCard(
    reminder: CustomTimePeriodReminderPreference,
    onReminderChanged: (CustomTimePeriodReminderPreference) -> Unit
) {
    var pickerInitialized by remember(reminder.groupName) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = reminder.groupName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (reminder.enabled) "Alarm on" else "Alarm off",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = reminder.enabled,
                onCheckedChange = { enabled ->
                    onReminderChanged(reminder.copy(enabled = enabled))
                }
            )
        }
        Text(
            text = "Alerts at ${reminder.endTime}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${reminder.leadMinutes} minute${if (reminder.leadMinutes == 1) "" else "s"} before alert",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )

        TimePickerField(
            label = "Time Period Alert Time",
            value = reminder.endTime,
            onValueChanged = { newValue ->
                onReminderChanged(reminder.copy(endTime = newValue))
            }
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    minValue = 0
                    maxValue = 720
                    wrapSelectorWheel = false
                    value = reminder.leadMinutes.coerceIn(minValue, maxValue)
                    pickerInitialized = true
                    setOnValueChangedListener { _, _, newValue ->
                        if (pickerInitialized) {
                            onReminderChanged(reminder.copy(leadMinutes = newValue))
                        }
                    }
                    isEnabled = reminder.enabled
                    alpha = if (reminder.enabled) 1f else 0.5f
                }
            },
            update = { picker ->
                picker.isEnabled = reminder.enabled
                picker.alpha = if (reminder.enabled) 1f else 0.5f
                val clamped = reminder.leadMinutes.coerceIn(0, 720)
                if (picker.value != clamped) {
                    picker.value = clamped
                }
                pickerInitialized = true
            }
        )
    }
}

private fun leadMinutesFor(
    category: TodoCategoryReminder,
    preferences: CategoryReminderPreferences
): Int {
    return when (category) {
        TodoCategoryReminder.MORNING -> preferences.morningLeadMinutes
        TodoCategoryReminder.AFTERNOON -> preferences.afternoonLeadMinutes
        TodoCategoryReminder.EVENING -> preferences.eveningLeadMinutes
        TodoCategoryReminder.BEDTIME -> preferences.bedtimeLeadMinutes
    }
}

private fun endTimeFor(
    category: TodoCategoryReminder,
    preferences: CategoryReminderPreferences
): String {
    return when (category) {
        TodoCategoryReminder.MORNING -> preferences.morningEndTime
        TodoCategoryReminder.AFTERNOON -> preferences.afternoonEndTime
        TodoCategoryReminder.EVENING -> preferences.eveningEndTime
        TodoCategoryReminder.BEDTIME -> preferences.bedtimeEndTime
    }
}

private fun enabledFor(
    category: TodoCategoryReminder,
    preferences: CategoryReminderPreferences
): Boolean {
    return when (category) {
        TodoCategoryReminder.MORNING -> preferences.morningEnabled
        TodoCategoryReminder.AFTERNOON -> preferences.afternoonEnabled
        TodoCategoryReminder.EVENING -> preferences.eveningEnabled
        TodoCategoryReminder.BEDTIME -> preferences.bedtimeEnabled
    }
}
