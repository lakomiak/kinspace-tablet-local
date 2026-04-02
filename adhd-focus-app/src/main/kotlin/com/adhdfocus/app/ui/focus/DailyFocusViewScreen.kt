package com.adhdfocus.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.ui.focus.components.ProgressHeader
import com.adhdfocus.app.ui.focus.components.TaskItem
import com.adhdfocus.app.ui.focus.components.TaskListByGroup

/**
 * Daily Focus View Screen - Main interface showing today's tasks
 *
 * Features:
 * - Task list organized by Todo_Group with visual separation
 * - Real-time completion percentage and task count
 * - Current streak display with visual emphasis
 * - High-contrast task status indicators
 * - Smooth scrolling performance (60 FPS)
 * - Offline capability with cached data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFocusViewScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onCreateTask: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val todaysTasks by viewModel.todaysTasks.collectAsState()
    val completionPercentage by viewModel.completionPercentage.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Focus") },
                actions = {
                    // Sync status indicator
                    IconButton(onClick = { /* Refresh */ }) {
                        when (syncStatus) {
                            SyncStatus.SYNCING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            SyncStatus.SYNCED -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Synced"
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Offline"
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Task"
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Focus") },
                    label = { Text("Focus") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Achievements") },
                    label = { Text("Achievements") },
                    selected = false,
                    onClick = onNavigateToAchievements
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onNavigateToSettings
                )
            }
        }
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Progress Header
                    item {
                        ProgressHeader(
                            completionPercentage = completionPercentage,
                            completedCount = todaysTasks.count { it.status == TaskStatus.COMPLETED },
                            totalCount = todaysTasks.size,
                            currentStreak = currentStreak
                        )
                    }

                    // Task List organized by Todo_Group
                    if (todaysTasks.isNotEmpty()) {
                        val tasksByGroup = todaysTasks.groupBy { it.todoGroup }
                        
                        tasksByGroup.forEach { (group, tasks) ->
                            item {
                                TaskListByGroup(
                                    todoGroup = group,
                                    tasks = tasks,
                                    onTaskClick = onTaskClick,
                                    onTaskComplete = { taskId ->
                                        viewModel.completeTask(taskId)
                                    },
                                    onTaskStart = { taskId ->
                                        viewModel.startTask(taskId)
                                    }
                                )
                            }
                        }
                    } else {
                        // Empty state
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
                                        text = "No tasks for today",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap + to create a new task",
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