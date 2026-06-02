package com.adhdfocus.app.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Timer Screen Component
 *
 * Displays:
 * - Large countdown display (MM:SS)
 * - Animated progress ring
 * - Visual feedback (color changes at 50% and 90%)
 * - Pause/resume/cancel controls
 * - Option to extend timer
 */
@Composable
fun TimerScreen(
    taskId: String = "",
    initialDurationSeconds: Int = 0,
    onTaskCompleted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val timerDuration by viewModel.timerDuration.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val timerCompleted by viewModel.timerCompleted.collectAsState()

    LaunchedEffect(taskId) {
        if (taskId.isNotBlank()) {
            viewModel.setTaskId(taskId)
        }
    }

    LaunchedEffect(initialDurationSeconds) {
        if (initialDurationSeconds > 0 && timerDuration == 0 && !isRunning) {
            viewModel.startTimer(initialDurationSeconds)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    val progressColor = when {
        animatedProgress < 0.5f -> Color(0xFF43A047)
        animatedProgress < 0.9f -> Color(0xFFFB8C00)
        else -> Color(0xFFE53935)
    }
    val timerStateSummary = remember(timeRemaining, isPaused, timerCompleted, progress, taskId) {
        buildString {
            append("Timer")
            if (taskId.isNotBlank()) {
                append(". Task timer active")
            }
            append(". ")
            append("Time remaining ")
            append(viewModel.getFormattedTime(timeRemaining))
            append(". ")
            append("${viewModel.getProgressPercentage()} percent complete")
            append(". ")
            append(
                when {
                    timerCompleted -> "Timer complete"
                    isPaused -> "Paused"
                    isRunning -> "Running"
                    else -> "Stopped"
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxWidth < 520.dp || maxHeight < 700.dp
        val pagePadding = if (compact) 20.dp else 32.dp
        val ringSize = when {
            maxWidth < 360.dp -> 200.dp
            maxWidth < 600.dp -> 240.dp
            else -> 280.dp
        }
        val ringStroke = if (compact) 6.dp else 8.dp
        val sectionSpacing = if (compact) 20.dp else 32.dp
        val actionButtonSpacing = if (compact) 10.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (maxWidth < 700.dp) maxWidth else 720.dp)
                .padding(pagePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .semantics {
                        contentDescription = "Timer countdown"
                        stateDescription = timerStateSummary
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.size(ringSize)) {
                    drawCircle(
                        color = surfaceVariantColor,
                        radius = size.minDimension / 2,
                        style = Stroke(width = ringStroke.toPx())
                    )
                }

                Canvas(modifier = Modifier.size(ringSize)) {
                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = ringStroke.toPx())
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = viewModel.getFormattedTime(timeRemaining),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "${viewModel.getProgressPercentage()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isPaused) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFB8C00).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB8C00)
                        )
                    }
                }
            }

            if (timerCompleted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE53935).copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timer complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(actionButtonSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(actionButtonSpacing)
                    ) {
                        TimerActionButton(
                            text = if (isPaused) "Resume" else "Pause",
                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFFB8C00),
                            enabled = isRunning
                        ) {
                            if (isPaused) {
                                viewModel.resumeTimer()
                            } else {
                                viewModel.pauseTimer()
                            }
                        }

                        TimerActionButton(
                            text = "Complete To Do",
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF43A047)
                        ) {
                            viewModel.completeCurrentTask(onTaskCompleted)
                        }
                    }

                    if (taskId.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(actionButtonSpacing)
                        ) {
                            TimerActionButton(
                                text = "Reset Timer",
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                viewModel.resetTimer()
                            }

                            TimerActionButton(
                                text = "Cancel",
                                icon = Icons.Default.Close,
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFE53935)
                            ) {
                                viewModel.cancelTimer()
                                onCancel()
                            }
                        }
                    } else {
                        TimerActionButton(
                            text = "Cancel",
                            icon = Icons.Default.Close,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFFE53935)
                        ) {
                            viewModel.cancelTimer()
                            onCancel()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Timer Countdown Component (Compact version for display in task list)
 *
 * Shows:
 * - Countdown timer
 * - Progress indicator
 * - Quick controls
 */
@Composable
fun CompactTimerDisplay(
    timeRemaining: Int,
    progress: Float,
    isRunning: Boolean,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val progressColor = when {
        progress < 0.5f -> Color(0xFF43A047)
        progress < 0.9f -> Color(0xFFFB8C00)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = viewModel.getFormattedTime(timeRemaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = "Timer progress"
                        stateDescription = "${viewModel.getProgressPercentage()} percent complete"
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFB8C00)
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}
