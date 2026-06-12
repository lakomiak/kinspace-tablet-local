package com.adhdfocus.app.ui.reports

import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val tabletSetupPrefs by lazy {
        context.getSharedPreferences("tablet_setup", Context.MODE_PRIVATE)
    }

    private val reportsDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        "kinspace_reports"
    ).apply { mkdirs() }

    fun exportSummary(memberName: String, householdId: String, summary: ReportSummary): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val safeMemberName = memberName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "member" }
        val reportFile = File(reportsDir, "report_${safeMemberName}_$timestamp.txt")
        reportFile.writeText(buildSummaryText(memberName, householdId, summary))
        return reportFile.absolutePath
    }

    fun exportSummaryCsv(memberName: String, householdId: String, summary: ReportSummary): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val safeMemberName = memberName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "member" }
        val reportFile = File(reportsDir, "report_${safeMemberName}_$timestamp.csv")
        reportFile.writeText(buildSummaryCsv(memberName, householdId, summary))
        return reportFile.absolutePath
    }

    fun getReportsDirectoryPath(): String = reportsDir.absolutePath

    fun exportSummaryToUri(memberName: String, householdId: String, summary: ReportSummary, targetUri: Uri): Boolean {
        return writeTextToUri(targetUri, buildSummaryText(memberName, householdId, summary))
    }

    fun exportSummaryCsvToUri(memberName: String, householdId: String, summary: ReportSummary, targetUri: Uri): Boolean {
        return writeTextToUri(targetUri, buildSummaryCsv(memberName, householdId, summary))
    }

    private fun buildSummaryText(memberName: String, householdId: String, summary: ReportSummary): String {
        val lines = mutableListOf<String>()
        lines += "Kinspace Tablet Local - Caregiver Summary"
        lines += "Generated: ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date())}"
        lines += "Member: $memberName"
        lines += "Household ID: $householdId"
        lines += "Install Type: ${tabletSetupPrefs.getString("install_type", "STANDARD")}"
        lines += ""
        lines += "Overview"
        lines += "Completed To Dos: ${summary.completedTodos}"
        lines += "Current Streak: ${summary.currentStreak}"
        lines += "Best Streak: ${summary.bestStreak}"
        lines += "Average Completion Time: ${summary.averageCompletionMinutes.format1()} min"
        lines += "Average Paused Time: ${summary.averagePausedPercent.format1()}%"
        lines += "Restarted Sessions: ${summary.restartedSessionPercent.format1()}%"
        lines += "Canceled Sessions: ${summary.canceledSessionPercent.format1()}%"
        lines += "Stopped Before End: ${summary.stoppedBeforeEndPercent.format1()}%"
        lines += "Completed After Timer Ended: ${summary.completedAfterEndPercent.format1()}%"
        lines += "Best Success Window: ${summary.successfulTimeWindow}"
        lines += "Timed Sessions: ${summary.sessionCount}"
        lines += ""
        lines += "Suggested Next Moves"
        if (summary.recommendations.isEmpty()) {
            lines += "- Keep using the tablet to gather more history before recommendations appear."
        } else {
            summary.recommendations.forEach { lines += "- $it" }
        }
        lines += ""
        lines += "Completed To Dos By Category"
        if (summary.categoryBreakdown.isEmpty()) {
            lines += "- Not enough local history yet."
        } else {
            summary.categoryBreakdown.forEach {
                lines += "- ${it.label}: ${it.count} (${it.percentage.format1()}%)"
            }
        }
        lines += ""
        lines += "Timer Session Outcomes"
        if (summary.timerOutcomeBreakdown.isEmpty()) {
            lines += "- Not enough local history yet."
        } else {
            summary.timerOutcomeBreakdown.forEach {
                lines += "- ${it.label}: ${it.count} (${it.percentage.format1()}%)"
            }
        }
        lines += ""
        lines += "Last 7 Days of Completed To Dos"
        summary.recentCompletionTrend.forEach {
            lines += "- ${it.label}: ${it.secondaryText}"
        }
        lines += ""
        lines += "Last 7 Days of Completion Time"
        summary.recentCompletionTimeTrend.forEach {
            lines += "- ${it.label}: ${it.secondaryText}"
        }
        return lines.joinToString(separator = System.lineSeparator())
    }

    private fun buildSummaryCsv(memberName: String, householdId: String, summary: ReportSummary): String {
        val rows = mutableListOf<List<String>>()
        rows += listOf("section", "label", "value", "detail")
        rows += listOf("meta", "generated_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()), "")
        rows += listOf("meta", "member", memberName, "")
        rows += listOf("meta", "household_id", householdId, "")
        rows += listOf("meta", "install_type", tabletSetupPrefs.getString("install_type", "STANDARD").orEmpty(), "")
        rows += listOf("overview", "completed_todos", summary.completedTodos.toString(), "")
        rows += listOf("overview", "current_streak", summary.currentStreak.toString(), "")
        rows += listOf("overview", "best_streak", summary.bestStreak.toString(), "")
        rows += listOf("overview", "average_completion_minutes", summary.averageCompletionMinutes.format1(), "")
        rows += listOf("overview", "average_paused_percent", summary.averagePausedPercent.format1(), "")
        rows += listOf("overview", "restarted_session_percent", summary.restartedSessionPercent.format1(), "")
        rows += listOf("overview", "canceled_session_percent", summary.canceledSessionPercent.format1(), "")
        rows += listOf("overview", "stopped_before_end_percent", summary.stoppedBeforeEndPercent.format1(), "")
        rows += listOf("overview", "completed_after_end_percent", summary.completedAfterEndPercent.format1(), "")
        rows += listOf("overview", "best_success_window", summary.successfulTimeWindow, "")
        rows += listOf("overview", "timed_sessions", summary.sessionCount.toString(), "")

        if (summary.recommendations.isEmpty()) {
            rows += listOf("recommendation", "1", "Keep using the tablet to gather more history before recommendations appear.", "")
        } else {
            summary.recommendations.forEachIndexed { index, recommendation ->
                rows += listOf("recommendation", (index + 1).toString(), recommendation, "")
            }
        }

        summary.categoryBreakdown.forEach {
            rows += listOf("category_breakdown", it.label, it.count.toString(), "${it.percentage.format1()}%")
        }

        summary.timerOutcomeBreakdown.forEach {
            rows += listOf("timer_outcome_breakdown", it.label, it.count.toString(), "${it.percentage.format1()}%")
        }

        summary.recentCompletionTrend.forEach {
            rows += listOf("completion_trend", it.label, it.primaryValue.toInt().toString(), it.secondaryText)
        }

        summary.recentCompletionTimeTrend.forEach {
            rows += listOf("completion_time_trend", it.label, it.primaryValue.format1(), it.secondaryText)
        }

        return rows.joinToString(separator = System.lineSeparator()) { row ->
            row.joinToString(separator = ",") { value -> escapeCsv(value) }
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun writeTextToUri(targetUri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(text)
            } ?: return false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
}
