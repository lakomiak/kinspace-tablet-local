package com.adhdfocus.app.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTodoScreen(
    onBackClick: () -> Unit,
    onCreateSuccess: () -> Unit,
    viewModel: CreateTodoViewModel = hiltViewModel()
) {
    var title by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf("Other") }
    var groupExpanded by remember { mutableStateOf(false) }
    var repeatValue by rememberSaveable { mutableStateOf("once") }
    var repeatExpanded by remember { mutableStateOf(false) }
    var customRepeatInterval by rememberSaveable { mutableStateOf("2") }
    var customRepeatUnit by rememberSaveable { mutableStateOf("day") }
    var customUnitExpanded by remember { mutableStateOf(false) }
    var timerMinutes by rememberSaveable { mutableStateOf("0") }
    var timerSeconds by rememberSaveable { mutableStateOf("0") }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val todoGroups = viewModel.todoGroups
    val repeatOptions = listOf("once", "daily", "weekly", "monthly", "yearly", "custom")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create To Do") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Add a To Do with the same fields used on the local device.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (error != null) viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("To Do title") },
                singleLine = true
            )

            OutlinedTextField(
                value = dueDate,
                onValueChange = {
                    dueDate = it
                    if (error != null) viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Due date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedGroup,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Todo group") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { groupExpanded = !groupExpanded }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose todo group"
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }
                ) {
                    todoGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group) },
                            onClick = {
                                selectedGroup = group
                                groupExpanded = false
                                viewModel.clearError()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("No group") },
                        onClick = {
                            selectedGroup = "Other"
                            groupExpanded = false
                            viewModel.clearError()
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = repeatValue,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repeat") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { repeatExpanded = !repeatExpanded }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose repeat"
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = repeatExpanded,
                    onDismissRequest = { repeatExpanded = false }
                ) {
                    repeatOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    }
                                )
                            },
                            onClick = {
                                repeatValue = option
                                repeatExpanded = false
                                viewModel.clearError()
                            }
                        )
                    }
                }
            }

            if (repeatValue == "custom") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customRepeatInterval,
                        onValueChange = {
                            customRepeatInterval = it.filter { char -> char.isDigit() }
                            if (error != null) viewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom interval") },
                        placeholder = { Text("2") },
                        singleLine = true
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val unitLabel = when (customRepeatUnit) {
                            "week" -> "Weeks"
                            "month" -> "Months"
                            else -> "Days"
                        }
                        OutlinedTextField(
                            value = unitLabel,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Custom unit") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { customUnitExpanded = !customUnitExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Choose repeat unit"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = customUnitExpanded,
                            onDismissRequest = { customUnitExpanded = false }
                        ) {
                            listOf(
                                "day" to "Days",
                                "week" to "Weeks",
                                "month" to "Months"
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        customRepeatUnit = value
                                        customUnitExpanded = false
                                        viewModel.clearError()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = timerMinutes,
                onValueChange = {
                    timerMinutes = it.filter { char -> char.isDigit() }
                    if (error != null) viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Timer min") },
                singleLine = true
            )

            OutlinedTextField(
                value = timerSeconds,
                onValueChange = {
                    timerSeconds = it.filter { char -> char.isDigit() }
                    if (error != null) viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Timer sec") },
                singleLine = true
            )

            Text(
                text = "Leave the due date blank to add it to today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val repeatRule = if (repeatValue == "custom") {
                        val interval = customRepeatInterval.toIntOrNull()?.takeIf { it > 0 } ?: 1
                        "custom:$interval:$customRepeatUnit"
                    } else {
                        repeatValue
                    }
                    viewModel.createTodo(
                        title = title,
                        dueDateText = dueDate,
                        todoGroup = selectedGroup,
                        repeatRule = repeatRule,
                        timerMinutesText = timerMinutes,
                        timerSecondsText = timerSeconds,
                        onSuccess = onCreateSuccess
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save To Do")
                }
            }
        }
    }
}
