package com.adhdfocus.app.ui.settings

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.adhdfocus.app.data.model.CategoryReminderPreferences
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.reminder.TodoCategoryReminder

@Composable
fun CategoryReminderPreferencesPanel(
    preferences: NotificationPreferences,
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
            text = "Category Reminders",
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
                    text = "Audible reminders before category end times",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Set how many minutes before each category ends the reminder should sound.",
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
            CategoryReminderLeadPickerGrid(
                preferences = reminderPreferences,
                onPreferencesChanged = { updated ->
                    onPreferencesChanged(preferences.copy(categoryReminderPreferences = updated))
                }
            )

            androidx.compose.material3.OutlinedButton(
                onClick = onPreviewReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview Category Reminder")
            }
        } else {
            Text(
                text = "Enable category reminders to choose lead times for Morning, Afternoon, Evening, and Bedtime.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        leadMinutes = leadMinutesFor(category, preferences),
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
    leadMinutes: Int,
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
        Text(
            text = "Ends at ${category.endTimeLabel}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$leadMinutes minute${if (leadMinutes == 1) "" else "s"} before end",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
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
                val clamped = leadMinutes.coerceIn(0, 720)
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
