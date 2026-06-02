package com.adhdfocus.app.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.ui.focus.components.ProgressHeader
import com.adhdfocus.app.ui.focus.components.TaskListByGroup
import android.widget.DatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    onTaskEditRequested: (Task) -> Unit = {},
    householdId: String = "",
    memberId: String = "",
    memberName: String? = null,
    onChangeMemberClick: () -> Unit = {},
    refreshToken: Int = 0,
    viewModel: FocusViewModel = hiltViewModel()
) {
    LaunchedEffect(householdId, memberId, refreshToken) {
        if (householdId.isNotBlank() && memberId.isNotBlank()) {
            viewModel.refreshFromCloud(householdId, memberId, LocalDate.now())
        } else {
            viewModel.refreshCurrentTasks(fromCloud = true)
        }
    }

    val todaysTasks by viewModel.todaysTasks.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val completionPercentage by viewModel.completionPercentage.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val allowTodoEditing by viewModel.allowTodoEditing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val activeMemberLabel = memberName ?: "Member"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (activeMemberLabel.isBlank()) "Hello" else "Hello, $activeMemberLabel")
                },
                actions = {
                    IconButton(onClick = onChangeMemberClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Change family member"
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Choose date"
                        )
                    }
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
            val isViewingToday = selectedDate == LocalDate.now()
            val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
            val dateLabel = if (isViewingToday) {
                "Today • ${selectedDate.format(dateFormatter)}"
            } else {
                selectedDate.format(dateFormatter)
            }
            val emptyStateTitle = if (isViewingToday) "No To Do's for today" else "No To Do's for this day"
            val emptyStateBody = if (isViewingToday) {
                "Tap + to create a new To Do"
            } else {
                "Browse to a different day or tap Today"
            }

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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { viewModel.showPreviousDay() }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Previous day"
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (!isViewingToday) {
                                            Text(
                                                text = "Viewing a previous day",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(onClick = { viewModel.showToday() }) {
                                        Text("Today")
                                    }
                                }
                            }
                        }

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
                                            onTaskEdit = onTaskEditRequested,
                                            onTaskDelete = { taskPendingDelete = it },
                                            showManagementActions = allowTodoEditing,
                                            showStartAction = isViewingToday,
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
                                            text = emptyStateTitle,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = emptyStateBody,
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

    if (taskPendingDelete != null) {
        val task = taskPendingDelete!!
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Delete To Do?") },
            text = { Text("Delete \"${task.title}\" from the tablet and cloud?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskPendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        Dialog(onDismissRequest = { showDatePicker = false }) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.extraLarge) {
                AndroidView(
                    modifier = Modifier.padding(16.dp),
                    factory = { context ->
                        DatePicker(context).apply {
                            calendarViewShown = true
                            spinnersShown = false
                            val currentDate = selectedDate
                            init(
                                currentDate.year,
                                currentDate.monthValue - 1,
                                currentDate.dayOfMonth
                            ) { _, year, monthOfYear, dayOfMonth ->
                                viewModel.selectDate(LocalDate.of(year, monthOfYear + 1, dayOfMonth))
                                showDatePicker = false
                            }
                        }
                    }
                )
            }
        }
    }

}
