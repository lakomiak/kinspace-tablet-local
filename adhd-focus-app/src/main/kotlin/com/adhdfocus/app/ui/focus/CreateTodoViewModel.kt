package com.adhdfocus.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.domain.visibility.TodoGroupVisibilityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class CreateTodoViewModel @Inject constructor(
    private val taskManager: TaskManager,
    private val taskRepository: TaskRepository,
    private val setupManager: TabletSetupManager,
    private val todoGroupVisibilityManager: TodoGroupVisibilityManager
) : ViewModel() {
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask

    private val _todoGroups = MutableStateFlow<List<String>>(listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other"))
    val todoGroups: StateFlow<List<String>> = _todoGroups

    private val _pastTodoTitles = MutableStateFlow<List<String>>(emptyList())
    val pastTodoTitles: StateFlow<List<String>> = _pastTodoTitles

    init {
        viewModelScope.launch {
            refreshTodoGroups()
            refreshPastTodoTitles()
        }
    }

    fun createTodo(
        title: String,
        emoji: String?,
        dueDateText: String,
        todoGroup: String,
        repeatRule: String,
        timerMinutesText: String,
        timerSecondsText: String,
        tokenValueText: String,
        onSuccess: () -> Unit
    ) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        val memberId = setupManager.getAssignedMemberId().orEmpty()
        if (householdId.isBlank() || memberId.isBlank()) {
            _error.value = "This tablet is not linked to a family member."
            return
        }

        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _error.value = "Please enter a To Do title."
            return
        }

        val selectedFocusDate = setupManager.getCurrentFocusDate() ?: LocalDate.now()
        val dueDate = parseDueDate(dueDateText) ?: selectedFocusDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        if (dueDateText.isNotBlank() && parseDueDate(dueDateText) == null) {
            _error.value = "Use YYYY-MM-DD for the due date."
            return
        }

        val resolvedGroup = todoGroup.trim().ifBlank { "Other" }
        val durationMinutes = timerMinutesText.trim().toIntOrNull() ?: 0
        val durationSeconds = timerSecondsText.trim().toIntOrNull() ?: 0
        if (durationMinutes < 0) {
            _error.value = "Timer minutes must be 0 or greater."
            return
        }
        if (durationSeconds < 0 || durationSeconds > 59) {
            _error.value = "Timer seconds must be between 0 and 59."
            return
        }
        val tokenValue = tokenValueText.trim().toIntOrNull() ?: 1
        if (tokenValue < 0) {
            _error.value = "Tokens earned must be 0 or greater."
            return
        }

        val estimatedDurationMinutes = durationMinutes.takeIf { it >= 0 }
        val estimatedDurationSeconds = durationSeconds.takeIf { it >= 0 }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                taskManager.createTask(
                    title = trimmedTitle,
                    emoji = emoji?.trim()?.takeIf { it.isNotBlank() },
                    todoGroup = resolvedGroup,
                    householdId = householdId,
                    assignedUserId = memberId,
                    assignedMemberName = setupManager.getAssignedMemberName(),
                    dueDate = dueDate,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    estimatedDurationSeconds = estimatedDurationSeconds,
                    tokenValue = tokenValue,
                    repeatRule = repeatRule
                )
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to create To Do."
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun loadTaskForEdit(taskId: String) {
        if (taskId.isBlank()) {
            _editingTask.value = null
            return
        }
        viewModelScope.launch {
            _editingTask.value = taskManager.getTaskById(taskId)
        }
    }

    fun clearTaskForEdit() {
        _editingTask.value = null
    }

    fun refreshTodoGroups() {
        viewModelScope.launch {
            val memberId = setupManager.getAssignedMemberId().orEmpty()
            if (memberId.isBlank()) {
                _todoGroups.value = todoGroupVisibilityManager.getAllTodoGroups()
            } else {
                _todoGroups.value = todoGroupVisibilityManager.getAllTodoGroups(memberId)
            }
        }
    }

    fun refreshPastTodoTitles() {
        viewModelScope.launch {
            val householdId = setupManager.getHouseholdId().orEmpty()
            _pastTodoTitles.value = if (householdId.isBlank()) {
                emptyList()
            } else {
                taskRepository.getDistinctTaskTitlesByHousehold(householdId)
            }
        }
    }

    fun updateTodo(
        taskId: String,
        title: String,
        emoji: String?,
        dueDateText: String,
        todoGroup: String,
        repeatRule: String,
        timerMinutesText: String,
        timerSecondsText: String,
        tokenValueText: String,
        onSuccess: () -> Unit
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _error.value = "Please enter a To Do title."
            return
        }

        val dueDate = parseDueDate(dueDateText)
        if (dueDateText.isNotBlank() && dueDate == null) {
            _error.value = "Use YYYY-MM-DD for the due date."
            return
        }

        val resolvedGroup = todoGroup.trim().ifBlank { "Other" }
        val durationMinutes = timerMinutesText.trim().toIntOrNull() ?: 0
        val durationSeconds = timerSecondsText.trim().toIntOrNull() ?: 0
        if (durationMinutes < 0) {
            _error.value = "Timer minutes must be 0 or greater."
            return
        }
        if (durationSeconds < 0 || durationSeconds > 59) {
            _error.value = "Timer seconds must be between 0 and 59."
            return
        }
        val tokenValue = tokenValueText.trim().toIntOrNull() ?: 1
        if (tokenValue < 0) {
            _error.value = "Tokens earned must be 0 or greater."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                taskManager.updateTask(
                    taskId = taskId,
                    title = trimmedTitle,
                    emoji = emoji?.trim()?.takeIf { it.isNotBlank() },
                    clearEmoji = emoji.isNullOrBlank(),
                    todoGroup = resolvedGroup,
                    dueDate = dueDate,
                    clearDueDate = dueDateText.isBlank(),
                    estimatedDurationMinutes = durationMinutes,
                    estimatedDurationSeconds = durationSeconds,
                    tokenValue = tokenValue,
                    repeatRule = repeatRule
                )
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to update To Do."
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun parseDueDate(value: String): Instant? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return null
        }

        return runCatching {
            LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant()
        }.getOrNull()
    }
}
