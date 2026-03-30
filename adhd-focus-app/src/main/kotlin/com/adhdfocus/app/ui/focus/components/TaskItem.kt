package com.adhdfocus.app.ui.focus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus

/**
 * Task Item Component
 * 
 * Displays a single task with high-contrast visual cues:
 * - INCOMPLETE: Red indicator, bold text
 * - IN_PROGRESS: Orange/yellow indicator, pulsing animation
 * - COMPLETED: Green checkmark, dimmed text
 * 
 * Also shows:
 * - Pending sync indicator
 * - Estimated duration
 * - Todo group
 */
@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.status) {
        TaskStatus.INCOMPLETE -> Color(0xFFE53935) // Red
        TaskStatus.IN_PROGRESS -> Color(0xFFFB8C00) // Orange
        TaskStatus.COMPLETED -> Color(0xFF43A047) // Green
    }

    val statusDescription = when (task.status) {
        TaskStatus.INCOMPLETE -> "Incomplete task"
        TaskStatus.IN_PROGRESS -> "Task in progress"
        TaskStatus.COMPLETED -> "Completed task"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${task.title}, $statusDescription" },
        colors = CardDefaults.cardColors(
            containerColor = if (task.status == TaskStatus.COMPLETED) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.status == TaskStatus.IN_PROGRESS) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                when (task.status) {
                    TaskStatus.INCOMPLETE -> {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Incomplete",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    TaskStatus.IN_PROGRESS -> {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "In Progress",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    TaskStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.status == TaskStatus.INCOMPLETE) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null,
                    color = if (task.status == TaskStatus.COMPLETED) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Description if present
                task.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Duration and sync status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estimated duration
                    task.estimatedDurationMinutes?.let { duration ->
                        Text(
                            text = "$duration min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pending sync indicator
                    if (task.syncStatus == SyncStatus.PENDING) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E88E5))
                        )
                    }
                }
            }

            // Action buttons
            when (task.status) {
                TaskStatus.INCOMPLETE -> {
                    // Show start button
                    IconButton(onClick = onStart) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Show complete button
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete Task",
                            tint = Color(0xFF43A047)
                        )
                    }
                }
                TaskStatus.IN_PROGRESS -> {
                    // Show complete button
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete Task",
                            tint = Color(0xFF43A047)
                        )
                    }
                }
                TaskStatus.COMPLETED -> {
                    // No action needed for completed tasks
                }
            }
        }
    }
}