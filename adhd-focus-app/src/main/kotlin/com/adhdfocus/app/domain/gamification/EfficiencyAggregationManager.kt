package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.model.EfficiencyMetric
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * EfficiencyAggregationManager aggregates efficiency metrics across different time periods.
 *
 * Calculates:
 * - Daily average efficiency from all tasks completed in a day
 * - Weekly average efficiency from all tasks completed in a week
 * - Overall average efficiency from all tasks ever completed
 * - Efficiency trends over time
 */
class EfficiencyAggregationManager @Inject constructor(
    private val efficiencyMetricDao: EfficiencyMetricDao,
    private val efficiencyCalculator: EfficiencyCalculator
) {
    /**
     * Calculates daily average efficiency for a specific date.
     *
     * @param userId User ID
     * @param date The date to calculate efficiency for
     * @return Average efficiency percentage for the day, or 100f if no tasks completed
     */
    suspend fun calculateDailyAverageEfficiency(userId: String, date: LocalDate): Float {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val metrics = efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates daily average efficiency for today.
     *
     * @param userId User ID
     * @return Average efficiency percentage for today, or 100f if no tasks completed
     */
    suspend fun calculateTodayAverageEfficiency(userId: String): Float {
        return calculateDailyAverageEfficiency(userId, LocalDate.now())
    }

    /**
     * Calculates weekly average efficiency for a specific week.
     *
     * @param userId User ID
     * @param date Any date within the week (Monday-Sunday)
     * @return Average efficiency percentage for the week, or 100f if no tasks completed
     */
    suspend fun calculateWeeklyAverageEfficiency(userId: String, date: LocalDate): Float {
        val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(7)

        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        val metrics = efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates weekly average efficiency for the current week.
     *
     * @param userId User ID
     * @return Average efficiency percentage for the current week, or 100f if no tasks completed
     */
    suspend fun calculateCurrentWeekAverageEfficiency(userId: String): Float {
        return calculateWeeklyAverageEfficiency(userId, LocalDate.now())
    }

    /**
     * Calculates overall average efficiency from all tasks ever completed.
     *
     * @param userId User ID
     * @return Average efficiency percentage across all tasks, or 100f if no tasks completed
     */
    suspend fun calculateOverallAverageEfficiency(userId: String): Float {
        val metrics = efficiencyMetricDao.getMetricsByUser(userId)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates overall average efficiency for a household.
     *
     * @param householdId Household ID
     * @return Average efficiency percentage across all household tasks, or 100f if no tasks completed
     */
    suspend fun calculateHouseholdOverallAverageEfficiency(householdId: String): Float {
        val metrics = efficiencyMetricDao.getMetricsByHousehold(householdId)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates daily average efficiency for a household on a specific date.
     *
     * @param householdId Household ID
     * @param date The date to calculate efficiency for
     * @return Average efficiency percentage for the day, or 100f if no tasks completed
     */
    suspend fun calculateHouseholdDailyAverageEfficiency(householdId: String, date: LocalDate): Float {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val metrics = efficiencyMetricDao.getHouseholdMetricsInDateRange(householdId, startOfDay, endOfDay)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates weekly average efficiency for a household for a specific week.
     *
     * @param householdId Household ID
     * @param date Any date within the week (Monday-Sunday)
     * @return Average efficiency percentage for the week, or 100f if no tasks completed
     */
    suspend fun calculateHouseholdWeeklyAverageEfficiency(householdId: String, date: LocalDate): Float {
        val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(7)

        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        val metrics = efficiencyMetricDao.getHouseholdMetricsInDateRange(householdId, startInstant, endInstant)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Gets efficiency metrics for a specific date range.
     *
     * @param userId User ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of efficiency metrics in the date range
     */
    suspend fun getEfficiencyMetricsInDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<EfficiencyMetric> {
        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant)
    }

    /**
     * Gets efficiency metrics for the last N days.
     *
     * @param userId User ID
     * @param days Number of days to look back
     * @return List of efficiency metrics for the last N days
     */
    suspend fun getEfficiencyMetricsForLastDays(userId: String, days: Int): List<EfficiencyMetric> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())
        return getEfficiencyMetricsInDateRange(userId, startDate, endDate)
    }

    /**
     * Calculates average efficiency for the last N days.
     *
     * @param userId User ID
     * @param days Number of days to look back
     * @return Average efficiency percentage for the last N days, or 100f if no tasks completed
     */
    suspend fun calculateAverageEfficiencyForLastDays(userId: String, days: Int): Float {
        val metrics = getEfficiencyMetricsForLastDays(userId, days)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Gets efficiency metrics for the last N weeks.
     *
     * @param userId User ID
     * @param weeks Number of weeks to look back
     * @return List of efficiency metrics for the last N weeks
     */
    suspend fun getEfficiencyMetricsForLastWeeks(userId: String, weeks: Int): List<EfficiencyMetric> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusWeeks(weeks.toLong())
        return getEfficiencyMetricsInDateRange(userId, startDate, endDate)
    }

    /**
     * Calculates average efficiency for the last N weeks.
     *
     * @param userId User ID
     * @param weeks Number of weeks to look back
     * @return Average efficiency percentage for the last N weeks, or 100f if no tasks completed
     */
    suspend fun calculateAverageEfficiencyForLastWeeks(userId: String, weeks: Int): Float {
        val metrics = getEfficiencyMetricsForLastWeeks(userId, weeks)
        return if (metrics.isEmpty()) {
            100f
        } else {
            metrics.mapNotNull { it.efficiencyPercentage }.average().toFloat()
        }
    }

    /**
     * Calculates efficiency trend over time.
     *
     * @param userId User ID
     * @param days Number of days to analyze
     * @return Trend: 1 = improving, 0 = stable, -1 = declining
     */
    suspend fun calculateEfficiencyTrend(userId: String, days: Int = 7): Int {
        val metrics = getEfficiencyMetricsForLastDays(userId, days)
        val efficiencies = metrics.mapNotNull { it.efficiencyPercentage }
        return efficiencyCalculator.calculateEfficiencyTrend(efficiencies)
    }

    /**
     * Gets daily efficiency breakdown for a date range.
     *
     * @param userId User ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of date to average efficiency for that day
     */
    suspend fun getDailyEfficiencyBreakdown(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Float> {
        val metrics = getEfficiencyMetricsInDateRange(userId, startDate, endDate)
        val breakdown = mutableMapOf<LocalDate, MutableList<Float>>()

        for (metric in metrics) {
            val date = metric.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            if (date in startDate..endDate) {
                breakdown.getOrPut(date) { mutableListOf() }.add(metric.efficiencyPercentage ?: 100f)
            }
        }

        return breakdown.mapValues { (_, efficiencies) ->
            if (efficiencies.isEmpty()) 100f else efficiencies.average().toFloat()
        }
    }

    /**
     * Gets weekly efficiency breakdown for a date range.
     *
     * @param userId User ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of week start date to average efficiency for that week
     */
    suspend fun getWeeklyEfficiencyBreakdown(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Float> {
        val metrics = getEfficiencyMetricsInDateRange(userId, startDate, endDate)
        val breakdown = mutableMapOf<LocalDate, MutableList<Float>>()

        for (metric in metrics) {
            val date = metric.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong())
            breakdown.getOrPut(weekStart) { mutableListOf() }.add(metric.efficiencyPercentage ?: 100f)
        }

        return breakdown.mapValues { (_, efficiencies) ->
            if (efficiencies.isEmpty()) 100f else efficiencies.average().toFloat()
        }
    }

    /**
     * Gets efficiency statistics for a user.
     *
     * @param userId User ID
     * @return EfficiencyStats containing various efficiency metrics
     */
    suspend fun getEfficiencyStats(userId: String): EfficiencyStats {
        val allMetrics = efficiencyMetricDao.getMetricsByUser(userId)
        val todayMetrics = getEfficiencyMetricsForLastDays(userId, 1)
        val weekMetrics = getEfficiencyMetricsForLastWeeks(userId, 1)

        val allEfficiencies = allMetrics.mapNotNull { it.efficiencyPercentage }
        val todayEfficiencies = todayMetrics.mapNotNull { it.efficiencyPercentage }
        val weekEfficiencies = weekMetrics.mapNotNull { it.efficiencyPercentage }

        return EfficiencyStats(
            dailyAverage = if (todayEfficiencies.isEmpty()) 100f else todayEfficiencies.average().toFloat(),
            weeklyAverage = if (weekEfficiencies.isEmpty()) 100f else weekEfficiencies.average().toFloat(),
            overallAverage = if (allEfficiencies.isEmpty()) 100f else allEfficiencies.average().toFloat(),
            bestEfficiency = allEfficiencies.maxOrNull() ?: 100f,
            worstEfficiency = allEfficiencies.minOrNull() ?: 100f,
            totalTasksCompleted = allMetrics.size,
            trend = efficiencyCalculator.calculateEfficiencyTrend(allEfficiencies)
        )
    }

    /**
     * Data class for efficiency statistics.
     */
    data class EfficiencyStats(
        val dailyAverage: Float,
        val weeklyAverage: Float,
        val overallAverage: Float,
        val bestEfficiency: Float,
        val worstEfficiency: Float,
        val totalTasksCompleted: Int,
        val trend: Int // 1 = improving, 0 = stable, -1 = declining
    )
}
