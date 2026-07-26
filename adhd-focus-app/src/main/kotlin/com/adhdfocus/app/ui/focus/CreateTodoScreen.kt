package com.adhdfocus.app.ui.focus

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.window.Dialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Spinner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTodoScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    taskId: String? = null,
    viewModel: CreateTodoViewModel = hiltViewModel()
) {
    val isEditing = !taskId.isNullOrBlank()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val todoGroups by viewModel.todoGroups.collectAsStateWithLifecycle()
    val pastTodoTitles by viewModel.pastTodoTitles.collectAsStateWithLifecycle()

    var title by rememberSaveable(taskId) { mutableStateOf("") }
    var selectedEmoji by rememberSaveable(taskId) { mutableStateOf("") }
    var showEmojiPicker by rememberSaveable(taskId) { mutableStateOf(false) }
    var emojiSearch by rememberSaveable(taskId) { mutableStateOf("") }
    var dueDate by rememberSaveable(taskId) { mutableStateOf("") }
    var showDueDatePicker by rememberSaveable(taskId) { mutableStateOf(false) }
    var selectedGroup by rememberSaveable(taskId) { mutableStateOf("Other") }
    var groupExpanded by remember { mutableStateOf(false) }
    var repeatValue by rememberSaveable(taskId) { mutableStateOf("once") }
    var repeatExpanded by remember { mutableStateOf(false) }
    var customRepeatInterval by rememberSaveable(taskId) { mutableStateOf("2") }
    var customRepeatUnit by rememberSaveable(taskId) { mutableStateOf("day") }
    var customUnitExpanded by remember { mutableStateOf(false) }
    var timerMinutes by rememberSaveable(taskId) { mutableStateOf(0) }
    var timerSeconds by rememberSaveable(taskId) { mutableStateOf(0) }
    var formInitialized by rememberSaveable(taskId) { mutableStateOf(false) }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val repeatOptions = listOf(
        "Once" to "once",
        "Daily" to "daily",
        "Weekdays" to "weekdays",
        "Weekends" to "weekends",
        "Weekly" to "weekly",
        "Monthly" to "monthly",
        "Yearly" to "yearly",
        "Custom" to "custom"
    )

    LaunchedEffect(taskId) {
        if (isEditing) {
            viewModel.loadTaskForEdit(taskId.orEmpty())
        } else {
            viewModel.clearTaskForEdit()
            formInitialized = false
        }
        viewModel.refreshTodoGroups()
        viewModel.refreshPastTodoTitles()
    }

    val filteredTitleSuggestions = remember(title, pastTodoTitles) {
        val query = title.trim()
        if (query.isBlank()) {
            emptyList()
        } else {
            pastTodoTitles
                .filter { suggestion ->
                    suggestion.contains(query, ignoreCase = true) &&
                        !suggestion.equals(query, ignoreCase = true)
                }
                .take(6)
        }
    }

    LaunchedEffect(editingTask?.id) {
        val task = editingTask ?: return@LaunchedEffect
        if (formInitialized) return@LaunchedEffect

        title = task.title
        selectedEmoji = task.emoji.orEmpty()
        dueDate = task.dueDate?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty()
        selectedGroup = task.todoGroup
        repeatValue = repeatValueFrom(task.repeatRule)
        if (repeatValue == "custom") {
            val customParts = task.repeatRule.split(":")
            customRepeatInterval = customParts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "2"
            customRepeatUnit = when (customParts.getOrNull(2)) {
                "week", "weeks" -> "week"
                "month", "months" -> "month"
                else -> "day"
            }
        }
        val timerMinutesValue = task.timerDurationMs?.let {
            val totalSeconds = (it / 1000L).toInt()
            totalSeconds / 60
        } ?: task.estimatedDurationMinutes ?: 0
        val timerSecondsValue = task.timerDurationMs?.let {
            val totalSeconds = (it / 1000L).toInt()
            totalSeconds % 60
        } ?: (task.estimatedDurationSeconds ?: 0)
        timerMinutes = timerMinutesValue
        timerSeconds = timerSecondsValue
        formInitialized = true
    }

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val horizontalPadding = when {
                maxWidth < 600.dp -> 16.dp
                maxWidth < 900.dp -> 24.dp
                else -> 32.dp
            }
            val contentMaxWidth = when {
                maxWidth < 600.dp -> maxWidth
                maxWidth < 900.dp -> 720.dp
                else -> 840.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
                    .padding(horizontal = horizontalPadding, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isEditing) {
                        "Edit this To Do using the same fields used on the local device."
                    } else {
                        "Add a To Do with the same fields used on the local device."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (error != null) viewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("To Do title") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Password
                        )
                    )

                    TitleSuggestionStrip(
                        suggestions = filteredTitleSuggestions,
                        onSuggestionSelected = {
                            title = it
                            if (error != null) viewModel.clearError()
                        }
                    )
                }

                TodoEmojiField(
                    selectedEmoji = selectedEmoji,
                    onChooseClick = {
                        emojiSearch = title.trim()
                        showEmojiPicker = true
                    },
                    onClearClick = {
                        selectedEmoji = ""
                        if (error != null) viewModel.clearError()
                    }
                )

                NativeDateField(
                    label = "Due date",
                    value = dueDate.ifBlank { "No due date selected" },
                    placeholder = "Tap to pick a date",
                    onClick = { showDueDatePicker = true }
                )

                if (showDueDatePicker) {
                    val initialDateMillis = remember(dueDate) {
                        parseDateToMillis(dueDate)
                    }
                    Dialog(onDismissRequest = { showDueDatePicker = false }) {
                        Surface(shape = MaterialTheme.shapes.extraLarge) {
                            AndroidView(
                                modifier = Modifier.padding(16.dp),
                                factory = { context ->
                                    android.widget.DatePicker(context).apply {
                                        calendarViewShown = true
                                        spinnersShown = false
                                        val baseDate = initialDateMillis?.let {
                                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                        } ?: LocalDate.now()
                                        init(
                                            baseDate.year,
                                            baseDate.monthValue - 1,
                                            baseDate.dayOfMonth
                                        ) { _, year, monthOfYear, dayOfMonth ->
                                            dueDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth).toString()
                                            showDueDatePicker = false
                                            if (error != null) viewModel.clearError()
                                        }
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showDueDatePicker = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }

                NativeSpinnerField(
                    label = "Time period",
                    value = selectedGroup,
                    options = (todoGroups + "Other").distinct(),
                    onValueSelected = {
                        selectedGroup = if (it == "Other") "Other" else it
                        viewModel.clearError()
                    }
                )

                NativeSpinnerField(
                    label = "Repeat",
                    value = repeatOptions.firstOrNull { it.second == repeatValue }?.first ?: "Once",
                    options = repeatOptions.map { it.first },
                    onValueSelected = {
                        repeatValue = repeatOptions.firstOrNull { option -> option.first == it }?.second ?: "once"
                        viewModel.clearError()
                    }
                )

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

                        NativeSpinnerField(
                            label = "Custom unit",
                            value = when (customRepeatUnit) {
                                "week" -> "Weeks"
                                "month" -> "Months"
                                else -> "Days"
                            },
                            options = listOf("Days", "Weeks", "Months"),
                            onValueSelected = {
                                customRepeatUnit = when (it) {
                                    "Weeks" -> "week"
                                    "Months" -> "month"
                                    else -> "day"
                                }
                                viewModel.clearError()
                            }
                        )
                    }
                }

                Text(
                    text = "Timer duration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        NativeNumberPickerField(
                            label = "Minutes",
                            value = timerMinutes,
                            onValueChange = {
                                timerMinutes = it
                                if (error != null) viewModel.clearError()
                            },
                            maxValue = 99999
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        NativeNumberPickerField(
                            label = "Seconds",
                            value = timerSeconds,
                            onValueChange = {
                                timerSeconds = it
                                if (error != null) viewModel.clearError()
                            },
                            maxValue = 59
                        )
                    }
                }

                Text(
                    text = "Leave the due date blank to add it to the day the tablet is currently showing.",
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
                        if (isEditing) {
                            viewModel.updateTodo(
                                taskId = taskId.orEmpty(),
                                title = title,
                                emoji = selectedEmoji,
                                dueDateText = dueDate,
                                todoGroup = selectedGroup,
                                repeatRule = repeatRule,
                                timerMinutesText = timerMinutes.toString(),
                                timerSecondsText = timerSeconds.toString(),
                                onSuccess = onSaveSuccess
                            )
                        } else {
                            viewModel.createTodo(
                                title = title,
                                emoji = selectedEmoji,
                                dueDateText = dueDate,
                                todoGroup = selectedGroup,
                                repeatRule = repeatRule,
                                timerMinutesText = timerMinutes.toString(),
                                timerSecondsText = timerSeconds.toString(),
                                onSuccess = onSaveSuccess
                            )
                        }
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
                        Text(if (isEditing) "Save Changes" else "Save To Do")
                    }
                }
            }
        }
    }

    if (showEmojiPicker) {
        TodoEmojiPickerDialog(
            query = emojiSearch,
            onQueryChange = { emojiSearch = it },
            onEmojiSelected = {
                selectedEmoji = it.emoji
                showEmojiPicker = false
                if (error != null) viewModel.clearError()
            },
            onDismiss = { showEmojiPicker = false }
        )
    }
}

private fun repeatValueFrom(repeatRule: String): String {
    return when {
        repeatRule.startsWith("custom:") -> "custom"
        repeatRule.isBlank() -> "once"
        else -> repeatRule
    }
}

private fun parseDateToMillis(value: String): Long? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) {
        return null
    }

    return runCatching {
        val date = LocalDate.parse(trimmed)
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
}

@Composable
private fun TitleSuggestionStrip(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (suggestions.isNotEmpty()) {
            Text(
                text = "Previous To Dos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestions.take(3).forEach { suggestion ->
                    Surface(
                        onClick = { onSuggestionSelected(suggestion) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = suggestion,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoEmojiField(
    selectedEmoji: String,
    onChooseClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        onClick = onChooseClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedEmoji.ifBlank { "🙂" },
                        fontSize = 28.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Picture icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (selectedEmoji.isBlank()) "Tap to search emoji" else "Selected $selectedEmoji",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (selectedEmoji.isNotBlank()) {
                TextButton(onClick = onClearClick) {
                    Text("Clear")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun TodoEmojiPickerDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onEmojiSelected: (TodoEmoji) -> Unit,
    onDismiss: () -> Unit
) {
    val results = remember(query) {
        TodoEmojiCatalog.search(query)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Choose picture icon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search emoji") },
                    singleLine = true
                )

                if (results.isEmpty()) {
                    Text(
                        text = "No matching emoji yet. Try words like brush, shoes, lunch, clean, bed, school, pet, or sports.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 76.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { item ->
                            Surface(
                                onClick = { onEmojiSelected(item) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.emoji,
                                        fontSize = 30.sp
                                    )
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeDateField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (value.isBlank()) placeholder else value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value == "No due date selected") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun NativeSpinnerField(
    label: String,
    value: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                Spinner(context).apply {
                    adapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_item,
                        options
                    ).also { adapter ->
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: android.view.View?,
                            position: Int,
                            id: Long
                        ) {
                            val selected = options.getOrNull(position) ?: return
                            onValueSelected(selected)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
                }
            },
            update = { spinner ->
                val adapter = spinner.adapter as? ArrayAdapter<String>
                if (adapter == null || adapter.count != options.size || options.anyIndexed { index, option -> adapter.getItem(index) != option }) {
                    val newAdapter = ArrayAdapter(
                        spinner.context,
                        android.R.layout.simple_spinner_item,
                        options
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    spinner.adapter = newAdapter
                }
                val index = options.indexOf(value).coerceAtLeast(0)
                if (spinner.selectedItemPosition != index) {
                    spinner.setSelection(index, false)
                }
            }
        )
    }
}

private fun formatDateMillis(value: Long): String {
    return Instant.ofEpochMilli(value)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
}

@Composable
private fun NativeNumberPickerField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                NumberPicker(context).apply {
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    wrapSelectorWheel = false
                    this.minValue = 0
                    this.maxValue = maxValue
                    setOnValueChangedListener { _, _, newVal ->
                        onValueChange(newVal)
                    }
                }
            },
            update = { picker ->
                if (picker.maxValue != maxValue) {
                    picker.maxValue = maxValue
                }
                if (picker.minValue != 0) {
                    picker.minValue = 0
                }
                val safeValue = value.coerceIn(0, maxValue)
                if (picker.value != safeValue) {
                    picker.value = safeValue
                }
            }
        )
    }
}

private inline fun <T> List<T>.anyIndexed(predicate: (index: Int, item: T) -> Boolean): Boolean {
    for (index in indices) {
        if (predicate(index, this[index])) return true
    }
    return false
}
