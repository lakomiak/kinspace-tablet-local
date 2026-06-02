package com.adhdfocus.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.adhdfocus.app.data.dao.TaskDayCompletionDao
import com.adhdfocus.app.data.dao.TaskSessionMetricDao
import com.adhdfocus.app.data.model.TaskSessionMetric
import com.adhdfocus.app.data.model.TaskSessionOutcome
import com.adhdfocus.app.data.repository.TaskRepository
import com.adhdfocus.app.data.repository.StreakRepository
import com.adhdfocus.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import javax.inject.Inject

data class ReportMember(
    val id: String,
    val name: String
)

data class ReportTrendPoint(
    val label: String,
    val primaryValue: Double,
    val secondaryText: String
)

data class ReportSummary(
    val completedTodos: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val averageCompletionMinutes: Double = 0.0,
    val averagePausedPercent: Double = 0.0,
    val restartedSessionPercent: Double = 0.0,
    val canceledSessionPercent: Double = 0.0,
    val stoppedBeforeEndPercent: Double = 0.0,
    val completedAfterEndPercent: Double = 0.0,
    val successfulTimeWindow: String = "Not enough data",
    val sessionCount: Int = 0,
    val categoryBreakdown: List<ReportBreakdownItem> = emptyList(),
    val timerOutcomeBreakdown: List<ReportBreakdownItem> = emptyList(),
    val recentCompletionTrend: List<ReportTrendPoint> = emptyList(),
    val recentCompletionTimeTrend: List<ReportTrendPoint> = emptyList(),
    val recommendations: List<String> = emptyList()
)

data class ReportBreakdownItem(
    val label: String,
    val count: Int,
    val percentage: Double
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val streakRepository: StreakRepository,
    private val taskDayCompletionDao: TaskDayCompletionDao,
    private val taskSessionMetricDao: TaskSessionMetricDao,
    private val taskRepository: TaskRepository,
    private val reportExportManager: ReportExportManager
) : ViewModel() {

    private val _members = MutableStateFlow<List<ReportMember>>(emptyList())
    val members: StateFlow<List<ReportMember>> = _members

    private val _selectedMemberId = MutableStateFlow<String?>(null)
    val selectedMemberId: StateFlow<String?> = _selectedMemberId

    private val _summary = MutableStateFlow(ReportSummary())
    val summary: StateFlow<ReportSummary> = _summary

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _reportsDirectory = MutableStateFlow(reportExportManager.getReportsDirectoryPath())
    val reportsDirectory: StateFlow<String> = _reportsDirectory

    fun initialize(householdId: String) {
        if (householdId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val members = userRepository.getUsersByHousehold(householdId)
                .sortedBy { it.displayName.lowercase() }
                .map { ReportMember(it.id, it.displayName) }
            _members.value = members
            val selected = _selectedMemberId.value ?: members.firstOrNull()?.id
            _selectedMemberId.value = selected
            if (selected != null) {
                _summary.value = buildSummary(householdId, selected)
            }
            _isLoading.value = false
        }
    }

    fun selectMember(householdId: String, userId: String) {
        _selectedMemberId.value = userId
        viewModelScope.launch {
            _isLoading.value = true
            _summary.value = buildSummary(householdId, userId)
            _isLoading.value = false
        }
    }

    fun exportCurrentSummary(householdId: String) {
        val userId = _selectedMemberId.value ?: return
        val memberName = _members.value.firstOrNull { it.id == userId }?.name ?: "Family Member"
        val summary = _summary.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val textPath = reportExportManager.exportSummary(memberName, householdId, summary)
                val csvPath = reportExportManager.exportSummaryCsv(memberName, householdId, summary)
                _exportStatus.value = "Summary exported as text and CSV.\nTXT: $textPath\nCSV: $csvPath"
            } catch (e: Exception) {
                _exportStatus.value = "Could not export summary: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }

    fun exportSummaryTextToUri(householdId: String, uri: Uri) {
        val userId = _selectedMemberId.value ?: return
        val memberName = _members.value.firstOrNull { it.id == userId }?.name ?: "Family Member"
        val summary = _summary.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                if (reportExportManager.exportSummaryToUri(memberName, householdId, summary, uri)) {
                    _exportStatus.value = "Text summary saved successfully."
                } else {
                    _exportStatus.value = "Could not save the text summary."
                }
            } catch (e: Exception) {
                _exportStatus.value = "Could not save the text summary: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportSummaryCsvToUri(householdId: String, uri: Uri) {
        val userId = _selectedMemberId.value ?: return
        val memberName = _members.value.firstOrNull { it.id == userId }?.name ?: "Family Member"
        val summary = _summary.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                if (reportExportManager.exportSummaryCsvToUri(memberName, householdId, summary, uri)) {
                    _exportStatus.value = "CSV summary saved successfully."
                } else {
                    _exportStatus.value = "Could not save the CSV summary."
                }
            } catch (e: Exception) {
                _exportStatus.value = "Could not save the CSV summary: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    private suspend fun buildSummary(householdId: String, userId: String): ReportSummary {
        val streak = streakRepository.getStreak(userId, householdId)
        val sessions = taskSessionMetricDao.getSessionsForUser(householdId, userId)
        val completedTodos = taskDayCompletionDao.getCompletedCountForUser(householdId, userId)
        val completedEntries = taskDayCompletionDao.getCompletedEntriesForUser(householdId, userId)
        val tasksById = taskRepository.getTasksByHousehold(householdId).associateBy { it.id }

        val completedSessions = sessions.filter { it.completedTask }
        val canceledSessions = sessions.filter { !it.completedTask }
        val restartedSessions = sessions.filter { it.resetCount > 0 }
        val stoppedBeforeEndSessions = sessions.filter { it.stoppedBeforeTimerEnded }
        val completedAfterEndSessions = sessions.filter { it.completedAfterTimerEnded }
        val completionMinutes = completedSessions.map { it.activeDurationSeconds / 60.0 }
        val categoryBreakdown = completedEntries
            .groupingBy { entry -> tasksById[entry.taskId]?.todoGroup?.ifBlank { "Other" } ?: "Other" }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { (group, count) ->
                ReportBreakdownItem(
                    label = group,
                    count = count,
                    percentage = count.percentageOf(completedEntries.size)
                )
            }
        val timerOutcomeBreakdown = TaskSessionOutcome.entries.map { outcome ->
            val count = sessions.count { it.outcome == outcome }
            ReportBreakdownItem(
                label = outcome.toDisplayLabel(),
                count = count,
                percentage = count.percentageOf(sessions.size)
            )
        }.filter { it.count > 0 }
        val recentCompletionTrend = buildRecentCompletionTrend(completedEntries)
        val recentCompletionTimeTrend = buildRecentCompletionTimeTrend(completedSessions)
        val recommendations = buildRecommendations(
            completedSessions = completedSessions,
            sessions = sessions,
            recentCompletionTrend = recentCompletionTrend,
            completionMinutes = completionMinutes,
            successfulTimeWindow = buildSuccessfulTimeWindow(completedSessions)
        )

        val pausedPercentages = sessions.mapNotNull { session ->
            val totalTracked = session.activeDurationSeconds + session.totalPausedSeconds
            if (totalTracked <= 0) null else (session.totalPausedSeconds.toDouble() / totalTracked.toDouble()) * 100.0
        }

        return ReportSummary(
            completedTodos = completedTodos,
            currentStreak = streak?.currentCount ?: 0,
            bestStreak = streak?.bestCount ?: 0,
            averageCompletionMinutes = completionMinutes.averageOrZero(),
            averagePausedPercent = pausedPercentages.averageOrZero(),
            restartedSessionPercent = restartedSessions.percentageOf(sessions),
            canceledSessionPercent = canceledSessions.percentageOf(sessions),
            stoppedBeforeEndPercent = stoppedBeforeEndSessions.percentageOf(sessions),
            completedAfterEndPercent = completedAfterEndSessions.percentageOf(sessions),
            successfulTimeWindow = buildSuccessfulTimeWindow(completedSessions),
            sessionCount = sessions.size,
            categoryBreakdown = categoryBreakdown,
            timerOutcomeBreakdown = timerOutcomeBreakdown,
            recentCompletionTrend = recentCompletionTrend,
            recentCompletionTimeTrend = recentCompletionTimeTrend,
            recommendations = recommendations
        )
    }

    private fun buildSuccessfulTimeWindow(completedSessions: List<TaskSessionMetric>): String {
        if (completedSessions.isEmpty()) return "Not enough data"
        val zone = ZoneId.systemDefault()
        val grouped = completedSessions.groupBy { session ->
            when (session.endedAt.atZone(zone).hour) {
                in 5..10 -> "Morning"
                in 11..15 -> "Midday"
                in 16..20 -> "Afternoon"
                else -> "Evening"
            }
        }
        return grouped.maxByOrNull { it.value.size }?.key ?: "Not enough data"
    }

    private fun buildRecentCompletionTrend(
        completedEntries: List<com.adhdfocus.app.data.model.TaskDayCompletion>
    ): List<ReportTrendPoint> {
        val formatter = DateTimeFormatter.ofPattern("M/d")
        val countsByDate = completedEntries.groupingBy { it.targetDate }.eachCount()
        return (6 downTo 0).map { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val count = countsByDate[date.toString()] ?: 0
            ReportTrendPoint(
                label = date.format(formatter),
                primaryValue = count.toDouble(),
                secondaryText = "$count done"
            )
        }
    }

    private fun buildRecentCompletionTimeTrend(completedSessions: List<TaskSessionMetric>): List<ReportTrendPoint> {
        val formatter = DateTimeFormatter.ofPattern("M/d")
        val zone = ZoneId.systemDefault()
        val minutesByDate = completedSessions.groupBy { it.endedAt.atZone(zone).toLocalDate() }
            .mapValues { (_, sessions) -> sessions.map { it.activeDurationSeconds / 60.0 }.averageOrZero() }

        return (6 downTo 0).map { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val avgMinutes = minutesByDate[date] ?: 0.0
            ReportTrendPoint(
                label = date.format(formatter),
                primaryValue = avgMinutes,
                secondaryText = if (avgMinutes > 0.0) "${avgMinutes.format1()} min" else "No timed wins"
            )
        }
    }

    private fun buildRecommendations(
        completedSessions: List<TaskSessionMetric>,
        sessions: List<TaskSessionMetric>,
        recentCompletionTrend: List<ReportTrendPoint>,
        completionMinutes: List<Double>,
        successfulTimeWindow: String
    ): List<String> {
        val insights = mutableListOf<String>()
        val avgMinutes = completionMinutes.averageOrZero()
        val avgConfiguredMinutes = completedSessions
            .mapNotNull { it.configuredDurationSeconds?.takeIf { seconds -> seconds > 0 }?.div(60.0) }
            .averageOrZero()
        val avgPausePercent = sessions.mapNotNull { session ->
            val totalTracked = session.activeDurationSeconds + session.totalPausedSeconds
            if (totalTracked <= 0) null else (session.totalPausedSeconds.toDouble() / totalTracked.toDouble()) * 100.0
        }.averageOrZero()
        val resetPercent = sessions.filter { it.resetCount > 0 }.percentageOf(sessions)
        val afterEndPercent = sessions.filter { it.completedAfterTimerEnded }.percentageOf(sessions)
        val stoppedEarlyPercent = sessions.filter { it.stoppedBeforeTimerEnded }.percentageOf(sessions)
        val activeDays = recentCompletionTrend.count { it.primaryValue > 0.0 }

        if (successfulTimeWindow != "Not enough data") {
            insights += "Try starting important timed todos in the $successfulTimeWindow, when this person most often finishes successfully."
        }

        if (avgConfiguredMinutes > 0.0 && avgMinutes > 0.0) {
            when {
                avgMinutes > avgConfiguredMinutes * 1.2 ->
                    insights += "The current timer looks a little short. Average completion is ${avgMinutes.format1()} minutes against a planned ${avgConfiguredMinutes.format1()} minutes, so increasing the timer may reduce frustration."
                avgMinutes < avgConfiguredMinutes * 0.65 ->
                    insights += "The timer may be longer than needed. Average completion is ${avgMinutes.format1()} minutes against a planned ${avgConfiguredMinutes.format1()} minutes, so shortening it could keep momentum up."
            }
        }

        if (avgPausePercent >= 25.0) {
            insights += "Pause time is running high at ${avgPausePercent.format1()}% of tracked session time. Shorter task chunks or fewer distractions nearby may help."
        }

        if (resetPercent >= 30.0) {
            insights += "Timer resets happen in ${resetPercent.format1()}% of sessions. Consider breaking large todos into smaller steps before starting the timer."
        }

        if (afterEndPercent >= 35.0) {
            insights += "A lot of completions happen after the timer ends (${afterEndPercent.format1()}%). Adding a small buffer to the timer would likely make success feel more attainable."
        }

        if (stoppedEarlyPercent >= 35.0) {
            insights += "Many sessions stop before time runs out (${stoppedEarlyPercent.format1()}%). Starting with the easiest step first may make it easier to stay engaged."
        }

        if (activeDays in 1..3) {
            insights += "Only $activeDays of the last 7 days show completed todos on this tablet. A lighter starting routine could help rebuild consistency."
        }

        if (insights.isEmpty()) {
            insights += "This person’s timer and completion patterns look fairly steady right now. Keep the current routine and keep gathering history for stronger recommendations."
        }

        return insights.take(4)
    }
}

private fun Iterable<Double>.averageOrZero(): Double {
    val list = toList()
    return if (list.isEmpty()) 0.0 else list.average()
}

private fun <T> List<T>.percentageOf(total: List<*>): Double {
    if (total.isEmpty()) return 0.0
    return (size.toDouble() / total.size.toDouble()) * 100.0
}

private fun Int.percentageOf(total: Int): Double {
    if (total <= 0) return 0.0
    return (toDouble() / total.toDouble()) * 100.0
}

private fun Double.format1(): String = String.format("%.1f", this)

private fun TaskSessionOutcome.toDisplayLabel(): String = when (this) {
    TaskSessionOutcome.COMPLETED_BEFORE_TIME_END -> "Completed Before Time Ended"
    TaskSessionOutcome.COMPLETED_AFTER_TIME_END -> "Completed After Time Ended"
    TaskSessionOutcome.CANCELED_BEFORE_TIME_END -> "Canceled Before Time Ended"
    TaskSessionOutcome.CANCELED_AFTER_TIME_END -> "Canceled After Time Ended"
}

