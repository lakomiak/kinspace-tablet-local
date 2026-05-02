package com.adhdfocus.app.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    viewModel: FocusViewModel = hiltViewModel()
) {
    LaunchedEffect(memberName) {
        viewModel.refreshCurrentTasks()
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
                    IconButton(onClick = { viewModel.refreshCurrentTasks() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
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
