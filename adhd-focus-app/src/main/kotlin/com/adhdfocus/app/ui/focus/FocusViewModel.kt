package com.adhdfocus.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.progress.ProgressTracker
import com.adhdfocus.app.domain.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FocusViewModel manages the state for the Daily Focus View.
 *
 * Manages:
 * - Today's tasks
 * - Completion percentage
 * - Current streak
 * - Sync status
 * - Current user
 * - Affirmation frequency setting
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskManager: TaskManager,
    private val progressTracker: ProgressTracker,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _todaysTasks = MutableStateFlow<List<Task>>(emptyList())
    val todaysTasks: StateFlow<List<Task>> = _todaysTasks

    private val _completionPercentage = MutableStateFlow(0)
    val completionPercentage: StateFlow<Int> = _completionPercentage

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentHouseholdId: String = ""
    private var currentUserId: String = ""

    /**
     * Loads today's tasks for a user.
     *
     * @param householdId Household ID
     * @param userId User ID
     */
    fun loadTodaysTasks(householdId: String, userId: String) {
        currentHouseholdId = householdId
        currentUserId = userId
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load user preferences and set affirmation frequency
                val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
                taskManager.setAffirmationFrequency(preferences.affirmationFrequency)
                
                val tasks = taskRepository.getTasksForToday(householdId, userId)
                _todaysTasks.value = tasks
                _completionPercentage.value = progressTracker.calculateCompletionPercentage(tasks)
                _currentStreak.value = progressTracker.getCurrentStreak(userId, householdId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refreshes tasks from cloud.
     *
     * @param householdId Household ID
     * @param userId User ID
     */
    fun refreshFromCloud(householdId: String, userId: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                loadTodaysTasks(householdId, userId)
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    /**
     * Switches to a different user.
     *
     * @param householdId Household ID
     * @param userId New user ID
     */
    fun switchUser(householdId: String, userId: String) {
        loadTodaysTasks(householdId, userId)
    }

    /**
     * Completes a task.
     *
     * @param taskId ID of the task to complete
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskManager.completeTask(taskId)
                // Refresh tasks after completion
                if (currentHouseholdId.isNotBlank() && currentUserId.isNotBlank()) {
                    loadTodaysTasks(currentHouseholdId, currentUserId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Starts a task (marks as in-progress).
     *
     * @param taskId ID of the task to start
     */
    fun startTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskManager.startTask(taskId)
                // Refresh tasks after starting
                if (currentHouseholdId.isNotBlank() && currentUserId.isNotBlank()) {
                    loadTodaysTasks(currentHouseholdId, currentUserId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
