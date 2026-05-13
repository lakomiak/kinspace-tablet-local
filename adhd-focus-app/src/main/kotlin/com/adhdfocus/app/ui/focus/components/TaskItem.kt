package com.adhdfocus.app.ui.focus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus

/**
 * To Do Item Component
 *
 * Displays a single todo with high-contrast visual cues:
 * - INCOMPLETE: Red indicator, bold text
 * - IN_PROGRESS: Orange/yellow indicator
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
    onStart: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    showManagementActions: Boolean = false,
    showStartAction: Boolean = true,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 360.dp
        val isMedium = maxWidth < 600.dp
        val statusSize = when {
            isCompact -> 48.dp
            isMedium -> 56.dp
            else -> 64.dp
        }
        val statusIconSize = when {
            isCompact -> 20.dp
            isMedium -> 24.dp
            else -> 26.dp
        }
        val horizontalPadding = when {
            isCompact -> 10.dp
            isMedium -> 12.dp
            else -> 16.dp
        }
        val itemSpacing = when {
            isCompact -> 10.dp
            else -> 12.dp
        }

        val isCompleted = task.status == TaskStatus.COMPLETED
        val isInProgress = task.status == TaskStatus.IN_PROGRESS
        val statusColor = when (task.status) {
            TaskStatus.INCOMPLETE -> Color(0xFFE53935)
            TaskStatus.IN_PROGRESS -> Color(0xFFFB8C00)
            TaskStatus.COMPLETED -> Color(0xFF43A047)
        }
        val timerLabel = remember(task.timerDurationMs, task.estimatedDurationMinutes, task.estimatedDurationSeconds) {
            buildTimerLabel(task)
        }
        val statusDescription = when (task.status) {
            TaskStatus.INCOMPLETE -> "Incomplete To Do"
            TaskStatus.IN_PROGRESS -> "To Do in progress"
            TaskStatus.COMPLETED -> "Completed To Do"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${task.title}, $statusDescription" },
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = when {
                    isInProgress -> 4.dp
                    isCompleted -> 0.dp
                    else -> 1.dp
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontalPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onClick,
                    shape = CircleShape,
                    color = when (task.status) {
                        TaskStatus.INCOMPLETE -> statusColor.copy(alpha = 0.12f)
                        TaskStatus.IN_PROGRESS -> statusColor.copy(alpha = 0.10f)
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    },
                    contentColor = when (task.status) {
                        TaskStatus.INCOMPLETE -> statusColor
                        TaskStatus.IN_PROGRESS -> statusColor
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(statusSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (task.status) {
                            TaskStatus.INCOMPLETE -> Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Mark task complete",
                                tint = statusColor,
                                modifier = Modifier.size(statusIconSize)
                            )

                            TaskStatus.IN_PROGRESS -> Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "In Progress",
                                tint = statusColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(statusIconSize)
                            )

                            TaskStatus.COMPLETED -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier.size(statusIconSize)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(itemSpacing))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (task.status == TaskStatus.INCOMPLETE) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    task.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (timerLabel != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = if (isCompleted) 0.7f else 1f
                                ),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = if (isCompleted) 0.7f else 1f
                                ),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                                    )
                                    Text(
                                        text = timerLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = if (isCompleted) 0.7f else 1f
                                        )
                                    )
                                }
                            }
                        }

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

                if (showManagementActions || (showStartAction && task.status == TaskStatus.INCOMPLETE && timerLabel != null)) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (showStartAction && task.status == TaskStatus.INCOMPLETE && timerLabel != null) {
                            ActionButton(
                                onClick = onStart,
                                icon = Icons.Default.PlayArrow,
                                label = "Start",
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        if (showManagementActions) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActionPillButton(
                                    onClick = onEdit,
                                    icon = Icons.Default.Edit,
                                    label = "Edit",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                ActionPillButton(
                                    onClick = onDelete,
                                    icon = Icons.Default.Delete,
                                    label = "Delete",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun ActionPillButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun buildTimerLabel(task: Task): String? {
    val timerMs = task.timerDurationMs
    if (timerMs != null && timerMs > 0L) {
        return formatTimerLabel(timerMs)
    }

    return when {
        (task.estimatedDurationMinutes ?: 0) > 0 && (task.estimatedDurationSeconds ?: 0) > 0 ->
            "Time allowed: ${task.estimatedDurationMinutes}m ${task.estimatedDurationSeconds}s"
        (task.estimatedDurationMinutes ?: 0) > 0 ->
            "Time allowed: ${task.estimatedDurationMinutes} min"
        (task.estimatedDurationSeconds ?: 0) > 0 ->
            "Time allowed: ${task.estimatedDurationSeconds} sec"
        else -> null
    }
}

private fun formatTimerLabel(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "Time allowed: ${minutes}m ${seconds}s"
        minutes > 0 -> "Time allowed: ${minutes} min"
        else -> "Time allowed: ${seconds} sec"
    }
}
