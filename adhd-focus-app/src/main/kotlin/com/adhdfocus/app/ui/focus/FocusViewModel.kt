package com.adhdfocus.app.ui.focus

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.progress.ProgressTracker
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.sync.RestApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * FocusViewModel manages the state for the Home screen.
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
    private val userPreferencesManager: UserPreferencesManager,
    private val setupManager: TabletSetupManager,
    private val taskPersistenceManager: TaskPersistenceManager,
    private val restApiClient: RestApiClient,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    private val tag = "FocusViewModel"

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

    init {
        val householdId = setupManager.getHouseholdId()
        val userId = setupManager.getAssignedMemberId()

        if (!householdId.isNullOrBlank() && !userId.isNullOrBlank()) {
            currentHouseholdId = householdId
            currentUserId = userId
            refreshFromCloud(householdId, userId)
        }
    }

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
                val memberName = setupManager.getAssignedMemberName()
                val tasks = resolveVisibleTasks(householdId, userId, memberName)
                applyDisplayedTasks(tasks, householdId, userId)
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
        currentHouseholdId = householdId
        currentUserId = userId

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                val tasks = withContext(Dispatchers.IO) {
                    restApiClient.fetchTasks(householdId)
                }
                Log.d(tag, "refreshFromCloud householdId=$householdId userId=$userId cloudCount=${tasks.size}")
                taskPersistenceManager.replaceTasksForHousehold(householdId, tasks)
                val memberName = setupManager.getAssignedMemberName()
                val visibleTasks = resolveVisibleTasks(householdId, userId, memberName)
                applyDisplayedTasks(visibleTasks, householdId, userId)
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                Log.e(tag, "refreshFromCloud failed householdId=$householdId userId=$userId", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    fun refreshCurrentTasks() {
        val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
        val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }

        if (householdId.isBlank() || userId.isBlank()) {
            return
        }

        refreshFromCloud(householdId, userId)
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
                val completedTask = taskManager.completeTask(taskId)
                _todaysTasks.value = _todaysTasks.value.map { task ->
                    if (task.id == taskId) completedTask else task
                }
                _completionPercentage.value = progressTracker.calculateCompletionPercentage(_todaysTasks.value)
                val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
                val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }
                if (householdId.isNotBlank() && userId.isNotBlank()) {
                    syncCurrentChanges(householdId, userId)
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

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val now = Instant.now()
                val optimisticTask = _todaysTasks.value.firstOrNull { it.id == taskId }?.let { task ->
                    if (isCompleted) {
                        task.copy(
                            status = com.adhdfocus.app.data.model.TaskStatus.INCOMPLETE,
                            completedAt = null,
                            updatedAt = now,
                            syncStatus = com.adhdfocus.app.data.model.SyncStatus.PENDING
                        )
                    } else {
                        task.copy(
                            status = com.adhdfocus.app.data.model.TaskStatus.COMPLETED,
                            completedAt = now,
                            updatedAt = now,
                            syncStatus = com.adhdfocus.app.data.model.SyncStatus.PENDING
                        )
                    }
                }
                if (optimisticTask != null) {
                    _todaysTasks.value = _todaysTasks.value.map { task ->
                        if (task.id == taskId) optimisticTask else task
                    }
                    _completionPercentage.value = progressTracker.calculateCompletionPercentage(_todaysTasks.value)
                }

                val persistedTask = if (isCompleted) {
                    taskManager.reopenTask(taskId)
                } else {
                    taskManager.completeTask(taskId)
                }

                _todaysTasks.value = _todaysTasks.value.map { task ->
                    if (task.id == taskId) persistedTask else task
                }
                _completionPercentage.value = progressTracker.calculateCompletionPercentage(_todaysTasks.value)

                val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
                val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }
                if (householdId.isNotBlank() && userId.isNotBlank()) {
                    syncCurrentChanges(householdId, userId)
                }
            } catch (e: Exception) {
                Log.e(tag, "toggleTaskCompletion failed taskId=$taskId isCompleted=$isCompleted", e)
            }
        }
    }

    /**
     * Resolves the task list to show on the Home screen.
     *
     * The tablet is pinned to one family member, so we show that member's tasks for today.
     */
    private suspend fun resolveVisibleTasks(
        householdId: String,
        userId: String,
        memberName: String?
    ): List<Task> {
        return taskRepository.getTasksForToday(householdId, userId, memberName)
    }

    private suspend fun applyDisplayedTasks(
        tasks: List<Task>,
        householdId: String,
        userId: String
    ) {
        val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
        taskManager.setAffirmationFrequency(preferences.affirmationFrequency)
        _todaysTasks.value = tasks
        _completionPercentage.value = progressTracker.calculateCompletionPercentage(tasks)
        _currentStreak.value = progressTracker.getCurrentStreak(userId, householdId)
    }

    private suspend fun syncCurrentChanges(householdId: String, userId: String) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val result = withContext(Dispatchers.IO) {
                cloudSyncManager.syncPendingChanges(householdId, userId)
            }
            if (result.success) {
                _syncStatus.value = SyncStatus.SYNCED
            } else {
                _syncStatus.value = SyncStatus.ERROR
            }
        } catch (e: Exception) {
            Log.e(tag, "syncCurrentChanges failed householdId=$householdId userId=$userId", e)
            _syncStatus.value = SyncStatus.ERROR
        }
    }
}
