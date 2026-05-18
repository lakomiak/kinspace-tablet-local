package com.adhdfocus.app.ui.focus

import android.util.Log
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.Streak
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.data.repository.StreakRepository
import com.adhdfocus.app.domain.affirmation.AffirmationTriggerManager
import com.adhdfocus.app.domain.gamification.BadgeSystem
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.progress.ProgressTracker
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.sync.RestApiClient
import com.adhdfocus.app.domain.completion.TaskDayCompletionRepository
import com.adhdfocus.app.domain.streak.StreakCalculationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration
import java.util.UUID
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
    private val cloudSyncManager: CloudSyncManager,
    private val taskDayCompletionRepository: TaskDayCompletionRepository,
    private val streakRepository: StreakRepository,
    private val streakCalculationManager: StreakCalculationManager,
    private val badgeSystem: BadgeSystem,
    private val affirmationTriggerManager: AffirmationTriggerManager
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

    private val _allowTodoEditing = MutableStateFlow(false)
    val allowTodoEditing: StateFlow<Boolean> = _allowTodoEditing

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

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
            _selectedDate.value = LocalDate.now()
            refreshFromCloud(householdId, userId, _selectedDate.value)
        }

        startDayRolloverWatcher()
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
        _selectedDate.value = LocalDate.now()
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val memberName = setupManager.getAssignedMemberName()
                val tasks = resolveVisibleTasks(householdId, userId, memberName, _selectedDate.value)
                applyDisplayedTasks(tasks, householdId, userId, memberName)
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
    fun refreshFromCloud(
        householdId: String,
        userId: String,
        targetDate: LocalDate = _selectedDate.value
    ) {
        currentHouseholdId = householdId
        currentUserId = userId

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                val cloudSnapshot = withContext(Dispatchers.IO) {
                    restApiClient.fetchTasks(householdId, targetDate, userId)
                }
                Log.d(
                    tag,
                    "refreshFromCloud householdId=$householdId userId=$userId targetDate=$targetDate cloudCount=${cloudSnapshot.tasks.size} dayCompletionCount=${cloudSnapshot.dayCompletions.size}"
                )
                taskPersistenceManager.replaceTasksForHousehold(householdId, cloudSnapshot.tasks)
                importCloudCompletionsForDate(
                    householdId = householdId,
                    userId = userId,
                    targetDate = targetDate,
                    dayCompletions = cloudSnapshot.dayCompletions
                )
                val memberName = setupManager.getAssignedMemberName()
                val visibleTasks = resolveVisibleTasks(householdId, userId, memberName, targetDate)
                applyDisplayedTasks(visibleTasks, householdId, userId, memberName)
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                Log.e(tag, "refreshFromCloud failed householdId=$householdId userId=$userId", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    private suspend fun importCloudCompletionsForDate(
        householdId: String,
        userId: String,
        targetDate: LocalDate,
        dayCompletions: List<com.adhdfocus.app.domain.sync.CloudTaskDayCompletion>
    ) {
        val completedTaskIds = dayCompletions
            .asSequence()
            .filter { it.familyMemberId == userId }
            .filter { it.targetDate == targetDate }
            .filter { it.isCompleted }
            .map { it.taskId }
            .toList()

        Log.d(
            tag,
            "importCloudCompletionsForDate householdId=$householdId userId=$userId targetDate=$targetDate completedCount=${completedTaskIds.size}"
        )
        taskDayCompletionRepository.replaceCompletionsForDate(
            householdId = householdId,
            userId = userId,
            date = targetDate,
            completedTaskIds = completedTaskIds
        )
    }

    fun refreshCurrentTasks(fromCloud: Boolean = true) {
        val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
        val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }

        if (householdId.isBlank() || userId.isBlank()) {
            return
        }

        if (fromCloud) {
            refreshFromCloud(householdId, userId, _selectedDate.value)
        } else {
            loadTodaysTasks(householdId, userId)
        }
    }

    /**
     * Switches to a different user.
     *
     * @param householdId Household ID
     * @param userId New user ID
     */
    fun switchUser(householdId: String, userId: String) {
        _selectedDate.value = LocalDate.now()
        loadTodaysTasks(householdId, userId)
    }

    fun showPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        refreshCurrentTasks(fromCloud = true)
    }

    fun showToday() {
        _selectedDate.value = LocalDate.now()
        refreshCurrentTasks(fromCloud = true)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        refreshCurrentTasks(fromCloud = true)
    }

    private fun startDayRolloverWatcher() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            var lastObservedDate = LocalDate.now(zone)

            while (isActive) {
                val nextDayStart = lastObservedDate
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                val delayMs = Duration.between(Instant.now(), nextDayStart).toMillis().coerceAtLeast(1_000L)
                delay(delayMs)

                val currentDate = LocalDate.now(zone)
                if (currentDate != lastObservedDate) {
                    val wasShowingToday = _selectedDate.value == lastObservedDate
                    lastObservedDate = currentDate
                    if (wasShowingToday) {
                        _selectedDate.value = currentDate
                        refreshCurrentTasks(fromCloud = true)
                    }
                }
            }
        }
    }

    /**
     * Completes a task.
     *
     * @param taskId ID of the task to complete
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d(tag, "completeTask requested taskId=$taskId selectedDate=${_selectedDate.value}")
                updateTaskCompletion(taskId, true)
            } catch (e: Exception) {
                Log.e(tag, "completeTask failed taskId=$taskId", e)
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
                Log.d(tag, "startTask requested taskId=$taskId")
                taskManager.startTask(taskId)
                // Refresh tasks after starting
                if (currentHouseholdId.isNotBlank() && currentUserId.isNotBlank()) {
                    loadTodaysTasks(currentHouseholdId, currentUserId)
                }
            } catch (e: Exception) {
                Log.e(tag, "startTask failed taskId=$taskId", e)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskManager.deleteTask(taskId)
                _todaysTasks.value = _todaysTasks.value.filterNot { it.id == taskId }
                _completionPercentage.value = progressTracker.calculateCompletionPercentage(_todaysTasks.value)
            } catch (e: Exception) {
                Log.e(tag, "deleteTask failed taskId=$taskId", e)
            }
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(tag, "toggleTaskCompletion requested taskId=$taskId isCompleted=$isCompleted selectedDate=${_selectedDate.value}")
                updateTaskCompletion(taskId, !isCompleted)
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
        memberName: String?,
        targetDate: LocalDate
    ): List<Task> {
        val tasks = taskRepository.getTasksForDate(householdId, userId, targetDate, memberName)
        return applyCompletionStateForDate(tasks, householdId, userId, targetDate)
    }

    private suspend fun applyCompletionStateForDate(
        tasks: List<Task>,
        householdId: String,
        userId: String,
        targetDate: LocalDate
    ): List<Task> {
        val completions = taskDayCompletionRepository.getCompletionsForDate(householdId, userId, targetDate)
            .associateBy { it.taskId }
        val today = LocalDate.now()

        return tasks.map { task ->
            val completion = completions[task.id]
            val isCompletedForDate = when {
                targetDate == today -> completion?.isCompleted == true || task.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED
                else -> completion?.isCompleted == true
            }

            if (isCompletedForDate) {
                task.copy(
                    status = com.adhdfocus.app.data.model.TaskStatus.COMPLETED,
                    completedAt = completion?.updatedAt ?: task.completedAt,
                    syncStatus = task.syncStatus
                )
            } else {
                task.copy(
                    status = com.adhdfocus.app.data.model.TaskStatus.INCOMPLETE,
                    completedAt = null,
                    syncStatus = task.syncStatus
                )
            }
        }
    }

    private fun refreshVisibleTasksFromLocal() {
        val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
        val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }
        if (householdId.isBlank() || userId.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val memberName = setupManager.getAssignedMemberName()
                val tasks = resolveVisibleTasks(householdId, userId, memberName, _selectedDate.value)
                applyDisplayedTasks(tasks, householdId, userId, memberName)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun applyDisplayedTasks(
        tasks: List<Task>,
        householdId: String,
        userId: String,
        memberName: String? = null,
        triggerAffirmations: Boolean = false
    ) {
        val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
        taskManager.setAffirmationFrequency(preferences.affirmationFrequency)
        _allowTodoEditing.value = preferences.enableTodoEditing
        _todaysTasks.value = tasks
        _completionPercentage.value = progressTracker.calculateCompletionPercentage(tasks)

        val resolvedMemberName = memberName ?: setupManager.getAssignedMemberName()
        val selectedDate = _selectedDate.value
        val selectedDayStreak = recalculateCurrentStreak(
            householdId = householdId,
            userId = userId,
            memberName = resolvedMemberName,
            referenceDate = selectedDate
        )
        val todayStreak = recalculateCurrentStreak(
            householdId = householdId,
            userId = userId,
            memberName = resolvedMemberName,
            referenceDate = LocalDate.now()
        )
        badgeSystem.ensureCurrentSeasonBadgeCatalog(userId, householdId)
        val displayStreak = if (selectedDate == LocalDate.now()) todayStreak else selectedDayStreak
        _currentStreak.value = displayStreak
        if (selectedDate == LocalDate.now()) {
            saveCalculatedStreak(userId, householdId, todayStreak, LocalDate.now())
        } else {
            saveCalculatedStreak(userId, householdId, selectedDayStreak, selectedDate)
        }
        updateBadges(userId, householdId, tasks, selectedDayStreak)

        if (triggerAffirmations) {
            maybeTriggerDayCompleteAffirmation(tasks)
            maybeTriggerStreakMilestoneAffirmation(selectedDayStreak)
        }
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

    private suspend fun updateTaskCompletion(taskId: String, complete: Boolean) {
        val selectedDate = _selectedDate.value
        val householdId = currentHouseholdId.ifBlank { setupManager.getHouseholdId().orEmpty() }
        val userId = currentUserId.ifBlank { setupManager.getAssignedMemberId().orEmpty() }
        val memberName = setupManager.getAssignedMemberName()
        val isToday = selectedDate == LocalDate.now()
        Log.d(
            tag,
            "updateTaskCompletion start taskId=$taskId complete=$complete selectedDate=$selectedDate isToday=$isToday householdId=$householdId userId=$userId"
        )

        if (isToday) {
            val persistedTask = if (complete) {
                taskManager.completeTask(taskId)
            } else {
                taskManager.reopenTask(taskId)
            }
            Log.d(
                tag,
                "updateTaskCompletion today persistedTask id=${persistedTask.id} status=${persistedTask.status} completedAt=${persistedTask.completedAt}"
            )
            _todaysTasks.value = _todaysTasks.value.map { task ->
                if (task.id == taskId) persistedTask else task
            }
            _completionPercentage.value = progressTracker.calculateCompletionPercentage(_todaysTasks.value)
            applyDisplayedTasks(
                tasks = _todaysTasks.value,
                householdId = householdId,
                userId = userId,
                memberName = memberName,
                triggerAffirmations = true
            )
            taskDayCompletionRepository.setCompletionForDate(
                householdId = householdId,
                userId = userId,
                taskId = taskId,
                date = selectedDate,
                isCompleted = complete
            )
            if (householdId.isNotBlank() && userId.isNotBlank()) {
                syncCurrentChanges(householdId, userId)
            }
            return
        }

        if (householdId.isBlank() || userId.isBlank()) {
            return
        }

        Log.d(tag, "updateTaskCompletion legacyDatePath taskId=$taskId complete=$complete selectedDate=$selectedDate")
        taskDayCompletionRepository.setCompletionForDate(
            householdId = householdId,
            userId = userId,
            taskId = taskId,
            date = selectedDate,
            isCompleted = complete
        )

        runCatching {
            val completion = withContext(Dispatchers.IO) {
                restApiClient.syncDayCompletion(
                    householdId = householdId,
                    taskId = taskId,
                    familyMemberId = userId,
                    targetDate = selectedDate,
                    isCompleted = complete,
                    completedAt = if (complete) Instant.now() else null
                )
            }
            Log.d(
                tag,
                "syncDayCompletion householdId=$householdId userId=$userId taskId=$taskId date=$selectedDate complete=$complete cloudCompleted=${completion.isCompleted}"
            )
        }.onFailure { error ->
            Log.e(
                tag,
                "syncDayCompletion failed householdId=$householdId userId=$userId taskId=$taskId date=$selectedDate complete=$complete",
                error
            )
        }

        val refreshedTasks = resolveVisibleTasks(householdId, userId, memberName, selectedDate)
        Log.d(tag, "updateTaskCompletion refreshedTasks count=${refreshedTasks.size} after taskId=$taskId complete=$complete")
        applyDisplayedTasks(
            tasks = refreshedTasks,
            householdId = householdId,
            userId = userId,
            memberName = memberName,
            triggerAffirmations = true
        )
        if (householdId.isNotBlank() && userId.isNotBlank()) {
            runCatching {
                syncCurrentChanges(householdId, userId)
            }.onFailure { error ->
                Log.e(
                    tag,
                    "Immediate cloud sync after historical completion failed householdId=$householdId userId=$userId taskId=$taskId date=$selectedDate",
                    error
                )
            }
        }
    }

    private suspend fun recalculateCurrentStreak(
        householdId: String,
        userId: String,
        memberName: String?,
        referenceDate: LocalDate
    ): Int {
        var streak = 0
        var date = referenceDate
        repeat(365) {
            val tasks = resolveVisibleTasks(householdId, userId, memberName, date)
            if (tasks.isEmpty() || tasks.any { it.status != com.adhdfocus.app.data.model.TaskStatus.COMPLETED }) {
                return streak
            }
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }

    private suspend fun saveCalculatedStreak(
        userId: String,
        householdId: String,
        currentStreak: Int,
        referenceDate: LocalDate
    ) {
        val existing = streakRepository.getStreak(userId, householdId)
        val best = maxOf(existing?.bestCount ?: 0, currentStreak)
        val lastDate = if (currentStreak > 0) referenceDate else existing?.lastCompletionDate
        val startDate = if (currentStreak > 0) {
            referenceDate.minusDays((currentStreak - 1).toLong())
        } else {
            existing?.startDate
        }
        val updated = (existing ?: Streak(
            id = UUID.randomUUID().toString(),
            userId = userId,
            householdId = householdId,
            currentCount = currentStreak,
            bestCount = best,
            lastCompletionDate = lastDate,
            startDate = startDate,
            updatedAt = Instant.now()
        )).copy(
            currentCount = currentStreak,
            bestCount = best,
            lastCompletionDate = lastDate,
            startDate = startDate,
            updatedAt = Instant.now()
        )
        streakRepository.saveStreak(updated)
    }

    private suspend fun updateBadges(
        userId: String,
        householdId: String,
        tasks: List<Task>,
        currentStreak: Int
    ) {
        val completedTasks = tasks.count { it.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED }
        val totalTasks = tasks.size
        if (totalTasks <= 0) return
        runCatching {
            badgeSystem.reconcileBadgeStates(
                userId = userId,
                householdId = householdId,
                completedTasksToday = completedTasks,
                totalTasksToday = totalTasks,
                currentStreak = currentStreak,
                efficiencyPercentage = 0f
            )
        }.onFailure { error ->
            Log.w(tag, "Unable to update badges householdId=$householdId userId=$userId", error)
        }
    }

    private fun maybeTriggerDayCompleteAffirmation(tasks: List<Task>) {
        if (tasks.isNotEmpty() && tasks.all { it.status == com.adhdfocus.app.data.model.TaskStatus.COMPLETED }) {
            affirmationTriggerManager.checkAndTriggerDayCompleteAffirmation(tasks)
        }
    }

    private fun maybeTriggerStreakMilestoneAffirmation(currentStreak: Int) {
        if (streakCalculationManager.isAtMilestone(currentStreak)) {
            affirmationTriggerManager.checkAndTriggerStreakMilestoneAffirmation(currentStreak)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
