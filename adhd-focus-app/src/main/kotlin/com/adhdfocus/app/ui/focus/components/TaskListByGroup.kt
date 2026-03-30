package com.adhdfocus.app.ui.focus.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.data.model.Task

/**
 * Task List By Group Component
 * 
 * Organizes tasks by Todo_Group with:
 * - Clear section headers
 * - Visual separation between groups
 * - Group-specific task counts
 */
@Composable
fun TaskListByGroup(
    todoGroup: String,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskStart: (String) -> Unit,
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
            
            // Group task count
            val completedCount = tasks.count { it.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED }
            Text(
                text = "$completedCount/${tasks.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tasks in this group
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onComplete = { onTaskComplete(task.id) },
                    onStart = { onTaskStart(task.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}