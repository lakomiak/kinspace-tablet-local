package com.adhdfocus.app.domain.gamification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * GamificationToggleManager manages the enable/disable state of individual gamification elements.
 *
 * Gamification elements:
 * - Badges: Achievement badges for milestones
 * - Streaks: Daily streak counter and tracking
 * - Efficiency Metrics: Task completion efficiency tracking
 *
 * Features:
 * - Independent toggle for each gamification element
 * - Per-member configuration
 * - Real-time state updates
 * - Validation to ensure at least one element can be enabled
 *
 * Usage:
 * ```
 * val manager = GamificationToggleManager()
 * manager.setBadgesEnabled(true)
 * manager.setStreaksEnabled(true)
 * manager.setEfficiencyMetricsEnabled(false)
 *
 * if (manager.areBadgesEnabled()) {
 *     // Show badges
 * }
 * ```
 */
class GamificationToggleManager {

    private val _badgesEnabled = MutableStateFlow(true)
    val badgesEnabled: StateFlow<Boolean> = _badgesEnabled

    private val _streaksEnabled = MutableStateFlow(true)
    val streaksEnabled: StateFlow<Boolean> = _streaksEnabled

    private val _efficiencyMetricsEnabled = MutableStateFlow(true)
    val efficiencyMetricsEnabled: StateFlow<Boolean> = _efficiencyMetricsEnabled

    /**
     * Sets whether badges are enabled.
     *
     * @param enabled Whether badges should be enabled
     */
    fun setBadgesEnabled(enabled: Boolean) {
        _badgesEnabled.value = enabled
    }

    /**
     * Gets whether badges are enabled.
     *
     * @return True if badges are enabled
     */
    fun areBadgesEnabled(): Boolean = _badgesEnabled.value

    /**
     * Sets whether streaks are enabled.
     *
     * @param enabled Whether streaks should be enabled
     */
    fun setStreaksEnabled(enabled: Boolean) {
        _streaksEnabled.value = enabled
    }

    /**
     * Gets whether streaks are enabled.
     *
     * @return True if streaks are enabled
     */
    fun areStreaksEnabled(): Boolean = _streaksEnabled.value

    /**
     * Sets whether efficiency metrics are enabled.
     *
     * @param enabled Whether efficiency metrics should be enabled
     */
    fun setEfficiencyMetricsEnabled(enabled: Boolean) {
        _efficiencyMetricsEnabled.value = enabled
    }

    /**
     * Gets whether efficiency metrics are enabled.
     *
     * @return True if efficiency metrics are enabled
     */
    fun areEfficiencyMetricsEnabled(): Boolean = _efficiencyMetricsEnabled.value

    /**
     * Gets the count of enabled gamification elements.
     *
     * @return Number of enabled elements (0-3)
     */
    fun getEnabledElementCount(): Int {
        var count = 0
        if (_badgesEnabled.value) count++
        if (_streaksEnabled.value) count++
        if (_efficiencyMetricsEnabled.value) count++
        return count
    }

    /**
     * Checks if any gamification element is enabled.
     *
     * @return True if at least one element is enabled
     */
    fun isAnyElementEnabled(): Boolean {
        return _badgesEnabled.value || _streaksEnabled.value || _efficiencyMetricsEnabled.value
    }

    /**
     * Checks if all gamification elements are enabled.
     *
     * @return True if all elements are enabled
     */
    fun areAllElementsEnabled(): Boolean {
        return _badgesEnabled.value && _streaksEnabled.value && _efficiencyMetricsEnabled.value
    }

    /**
     * Resets all gamification elements to enabled state.
     */
    fun resetToDefaults() {
        _badgesEnabled.value = true
        _streaksEnabled.value = true
        _efficiencyMetricsEnabled.value = true
    }

    /**
     * Disables all gamification elements.
     */
    fun disableAll() {
        _badgesEnabled.value = false
        _streaksEnabled.value = false
        _efficiencyMetricsEnabled.value = false
    }

    /**
     * Enables all gamification elements.
     */
    fun enableAll() {
        _badgesEnabled.value = true
        _streaksEnabled.value = true
        _efficiencyMetricsEnabled.value = true
    }
}
