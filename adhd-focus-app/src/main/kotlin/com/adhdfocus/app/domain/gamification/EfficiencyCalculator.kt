package com.adhdfocus.app.domain.gamification

/**
 * EfficiencyCalculator computes task completion efficiency metrics.
 *
 * Calculates:
 * - Efficiency percentage (actual / estimated * 100)
 * - Efficiency trends over time
 * - Efficiency-based badges
 */
class EfficiencyCalculator {
    /**
     * Calculates efficiency percentage for a task.
     *
     * @param estimatedDurationMinutes Estimated duration in minutes
     * @param actualDurationMinutes Actual duration in minutes
     * @return Efficiency percentage (100 = on time, >100 = faster, <100 = slower)
     */
    fun calculateEfficiency(
        estimatedDurationMinutes: Int?,
        actualDurationMinutes: Int?
    ): Float {
        if (estimatedDurationMinutes == null || actualDurationMinutes == null) {
            return 100f
        }
        if (estimatedDurationMinutes <= 0) {
            return 100f
        }

        return (estimatedDurationMinutes.toFloat() / actualDurationMinutes.toFloat()) * 100f
    }

    /**
     * Gets a human-readable efficiency description.
     *
     * @param efficiencyPercentage Efficiency percentage
     * @return Description like "15% faster than estimated"
     */
    fun getEfficiencyDescription(efficiencyPercentage: Float): String {
        return when {
            efficiencyPercentage > 120 -> "${(efficiencyPercentage - 100).toInt()}% faster than estimated"
            efficiencyPercentage > 100 -> "${(efficiencyPercentage - 100).toInt()}% faster than estimated"
            efficiencyPercentage == 100f -> "Completed on time"
            efficiencyPercentage > 80 -> "${(100 - efficiencyPercentage).toInt()}% slower than estimated"
            else -> "${(100 - efficiencyPercentage).toInt()}% slower than estimated"
        }
    }

    /**
     * Calculates average efficiency over a list of efficiency values.
     *
     * @param efficiencies List of efficiency percentages
     * @return Average efficiency percentage
     */
    fun calculateAverageEfficiency(efficiencies: List<Float>): Float {
        if (efficiencies.isEmpty()) return 100f
        return efficiencies.average().toFloat()
    }

    /**
     * Calculates efficiency trend (improving, stable, declining).
     *
     * @param recentEfficiencies Recent efficiency values (last 7 days)
     * @return Trend: 1 = improving, 0 = stable, -1 = declining
     */
    fun calculateEfficiencyTrend(recentEfficiencies: List<Float>): Int {
        if (recentEfficiencies.size < 2) return 0

        val firstHalf = recentEfficiencies.take(recentEfficiencies.size / 2).average()
        val secondHalf = recentEfficiencies.drop(recentEfficiencies.size / 2).average()

        return when {
            secondHalf > firstHalf + 5 -> 1  // Improving
            secondHalf < firstHalf - 5 -> -1 // Declining
            else -> 0                         // Stable
        }
    }

    /**
     * Checks if efficiency qualifies for a badge.
     *
     * @param efficiencyPercentage Efficiency percentage
     * @return True if efficiency qualifies for a badge
     */
    fun qualifiesForEfficiencyBadge(efficiencyPercentage: Float): Boolean {
        return efficiencyPercentage >= 120 // 20% faster than estimated
    }
}
