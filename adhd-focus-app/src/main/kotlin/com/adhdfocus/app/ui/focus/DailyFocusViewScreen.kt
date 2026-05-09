package com.adhdfocus.app.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.ui.focus.components.ProgressHeader
import com.adhdfocus.app.ui.focus.components.TaskListByGroup

/**
 * Home screen - Main interface showing today's todos
 *
 * Features:
 * - Todo list organized by Todo_Group with visual separation
 * - Real-time completion percentage and todo count
 * - Current streak display with visual emphasis
 * - High-contrast todo status indicators
 * - Smooth scrolling performance (60 FPS)
 * - Offline capability with cached data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFocusViewScreen(
    onTimerStartRequested: (Task) -> Unit = {},
    memberName: String? = null,
    refreshToken: Int = 0,
    viewModel: FocusViewModel = hiltViewModel()
) {
    LaunchedEffect(memberName, refreshToken) {
        viewModel.refreshCurrentTasks(fromCloud = false)
    }

    val todaysTasks by viewModel.todaysTasks.collectAsStateWithLifecycle()
    val completionPercentage by viewModel.completionPercentage.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memberName.isNullOrBlank()) "Hello" else "Hello, $memberName") },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentTasks(fromCloud = true) }) {
                        when (syncStatus) {
                            SyncStatus.SYNCING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(4.dp),
                                    strokeWidth = 2.dp
                                )
                            }

                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Refresh tasks"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val horizontalPadding = when {
                maxWidth < 600.dp -> 12.dp
                maxWidth < 840.dp -> 16.dp
                else -> 24.dp
            }
            val contentMaxWidth = when {
                maxWidth < 600.dp -> maxWidth
                maxWidth < 840.dp -> 720.dp
                else -> 920.dp
            }
            val bottomPadding = if (maxWidth < 600.dp) 88.dp else 112.dp

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            top = 16.dp,
                            end = horizontalPadding,
                            bottom = bottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ProgressHeader(
                                completionPercentage = completionPercentage,
                                completedCount = todaysTasks.count { it.status == TaskStatus.COMPLETED },
                                totalCount = todaysTasks.size,
                                currentStreak = currentStreak
                            )
                        }

                        if (todaysTasks.isNotEmpty()) {
                            val tasksByGroup = todaysTasks.groupBy { it.todoGroup }

                            tasksByGroup.forEach { (group, tasks) ->
                                item {
                                    TaskListByGroup(
                                        todoGroup = group,
                                        tasks = tasks,
                                        onTaskToggle = { taskId, isCompleted ->
                                            viewModel.toggleTaskCompletion(taskId, isCompleted)
                                        },
                                        onTaskStart = { taskId -> viewModel.startTask(taskId) },
                                        onTimerStartRequested = onTimerStartRequested
                                    )
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No To Do's for today",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap + to create a new To Do",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
