package com.adhdfocus.app.ui.focus.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.data.model.Task

/**
 * Todo List By Group Component
 *
 * Organizes todos by Todo_Group with:
 * - Clear section headers
 * - Visual separation between groups
 * - Group-specific todo counts
 */
@Composable
fun TaskListByGroup(
    todoGroup: String,
    tasks: List<Task>,
    onTaskToggle: (String, Boolean) -> Unit,
    onTaskStart: (String) -> Unit,
    onTaskEdit: (Task) -> Unit = {},
    onTaskDelete: (Task) -> Unit = {},
    showManagementActions: Boolean = false,
    onTimerStartRequested: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Group Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = todoGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Group todo count
            val completedCount = tasks.count { it.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED }
            Text(
                text = "$completedCount/${tasks.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tasks in this group
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskToggle(task.id, task.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED) },
                    onStart = {
                        onTaskStart(task.id)
                        if ((task.timerDurationMs ?: 0L) > 0L) {
                            onTimerStartRequested(task)
                        }
                    },
                    onEdit = { onTaskEdit(task) },
                    onDelete = { onTaskDelete(task) },
                    showManagementActions = showManagementActions
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
