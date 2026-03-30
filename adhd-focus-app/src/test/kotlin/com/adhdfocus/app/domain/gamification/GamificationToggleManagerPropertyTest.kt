package com.adhdfocus.app.domain.gamification

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for GamificationToggleManager.
 *
 * **Validates: Requirements 18, Property: Gamification Element Toggles**
 *
 * Tests verify universal properties that should hold across all valid inputs:
 * - Toggle state persistence
 * - Element count accuracy
 * - Batch operation correctness
 * - State consistency
 */
class GamificationToggleManagerPropertyTest {

    /**
     * Property: For any gamification element, toggling it changes its state.
     *
     * Verifies that setting an element to enabled/disabled actually changes its state.
     */
    @Test
    fun testPropertyToggleChangesState() {
        val manager = GamificationToggleManager()
        
        // Test badges
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
        
        // Test streaks
        manager.setStreaksEnabled(false)
        assertFalse(manager.areStreaksEnabled())
        manager.setStreaksEnabled(true)
        assertTrue(manager.areStreaksEnabled())
        
        // Test efficiency metrics
        manager.setEfficiencyMetricsEnabled(false)
        assertFalse(manager.areEfficiencyMetricsEnabled())
        manager.setEfficiencyMetricsEnabled(true)
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    /**
     * Property: For any element, its state is independent of other elements.
     *
     * Verifies that toggling one element doesn't affect others.
     */
    @Test
    fun testPropertyElementIndependence() {
        val manager = GamificationToggleManager()
        
        // Disable badges, verify others unchanged
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        // Disable streaks, verify badges still disabled and efficiency still enabled
        manager.setStreaksEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        // Enable badges, verify streaks still disabled and efficiency still enabled
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    /**
     * Property: For any state, getEnabledElementCount returns correct count.
     *
     * Verifies that the count of enabled elements is always accurate.
     */
    @Test
    fun testPropertyEnabledElementCountAccuracy() {
        val manager = GamificationToggleManager()
        
        // All enabled
        manager.enableAll()
        assertEquals(3, manager.getEnabledElementCount())
        
        // One disabled
        manager.setBadgesEnabled(false)
        assertEquals(2, manager.getEnabledElementCount())
        
        // Two disabled
        manager.setStreaksEnabled(false)
        assertEquals(1, manager.getEnabledElementCount())
        
        // All disabled
        manager.setEfficiencyMetricsEnabled(false)
        assertEquals(0, manager.getEnabledElementCount())
        
        // Re-enable one
        manager.setBadgesEnabled(true)
        assertEquals(1, manager.getEnabledElementCount())
    }

    /**
     * Property: For any state, isAnyElementEnabled is true iff at least one element is enabled.
     *
     * Verifies that isAnyElementEnabled correctly reflects whether any element is enabled.
     */
    @Test
    fun testPropertyIsAnyElementEnabledAccuracy() {
        val manager = GamificationToggleManager()
        
        // All enabled
        manager.enableAll()
        assertTrue(manager.isAnyElementEnabled())
        
        // All disabled
        manager.disableAll()
        assertFalse(manager.isAnyElementEnabled())
        
        // One enabled
        manager.setBadgesEnabled(true)
        assertTrue(manager.isAnyElementEnabled())
        
        // All disabled again
        manager.setBadgesEnabled(false)
        assertFalse(manager.isAnyElementEnabled())
        
        // Two enabled
        manager.setStreaksEnabled(true)
        manager.setEfficiencyMetricsEnabled(true)
        assertTrue(manager.isAnyElementEnabled())
    }

    /**
     * Property: For any state, areAllElementsEnabled is true iff all elements are enabled.
     *
     * Verifies that areAllElementsEnabled correctly reflects whether all elements are enabled.
     */
    @Test
    fun testPropertyAreAllElementsEnabledAccuracy() {
        val manager = GamificationToggleManager()
        
        // All enabled
        manager.enableAll()
        assertTrue(manager.areAllElementsEnabled())
        
        // One disabled
        manager.setBadgesEnabled(false)
        assertFalse(manager.areAllElementsEnabled())
        
        // Re-enable
        manager.setBadgesEnabled(true)
        assertTrue(manager.areAllElementsEnabled())
        
        // All disabled
        manager.disableAll()
        assertFalse(manager.areAllElementsEnabled())
    }

    /**
     * Property: For any state, enableAll makes all elements enabled.
     *
     * Verifies that enableAll correctly enables all elements regardless of initial state.
     */
    @Test
    fun testPropertyEnableAllCorrectness() {
        val manager = GamificationToggleManager()
        
        // Start with all disabled
        manager.disableAll()
        manager.enableAll()
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        assertEquals(3, manager.getEnabledElementCount())
        
        // Start with mixed state
        manager.setBadgesEnabled(false)
        manager.setStreaksEnabled(true)
        manager.setEfficiencyMetricsEnabled(false)
        manager.enableAll()
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        assertEquals(3, manager.getEnabledElementCount())
    }

    /**
     * Property: For any state, disableAll makes all elements disabled.
     *
     * Verifies that disableAll correctly disables all elements regardless of initial state.
     */
    @Test
    fun testPropertyDisableAllCorrectness() {
        val manager = GamificationToggleManager()
        
        // Start with all enabled
        manager.enableAll()
        manager.disableAll()
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertFalse(manager.areEfficiencyMetricsEnabled())
        assertEquals(0, manager.getEnabledElementCount())
        
        // Start with mixed state
        manager.setBadgesEnabled(true)
        manager.setStreaksEnabled(false)
        manager.setEfficiencyMetricsEnabled(true)
        manager.disableAll()
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertFalse(manager.areEfficiencyMetricsEnabled())
        assertEquals(0, manager.getEnabledElementCount())
    }

    /**
     * Property: For any state, resetToDefaults makes all elements enabled.
     *
     * Verifies that resetToDefaults correctly resets to default state (all enabled).
     */
    @Test
    fun testPropertyResetToDefaultsCorrectness() {
        val manager = GamificationToggleManager()
        
        // Start with all disabled
        manager.disableAll()
        manager.resetToDefaults()
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        // Start with mixed state
        manager.setBadgesEnabled(false)
        manager.setStreaksEnabled(true)
        manager.setEfficiencyMetricsEnabled(false)
        manager.resetToDefaults()
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    /**
     * Property: For any sequence of toggles, the final state is consistent.
     *
     * Verifies that multiple toggle operations result in consistent state.
     */
    @Test
    fun testPropertyToggleSequenceConsistency() {
        val manager = GamificationToggleManager()
        
        // Sequence 1: disable, enable, disable
        manager.setBadgesEnabled(false)
        manager.setBadgesEnabled(true)
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        
        // Sequence 2: enable, disable, enable, disable
        manager.setStreaksEnabled(true)
        manager.setStreaksEnabled(false)
        manager.setStreaksEnabled(true)
        manager.setStreaksEnabled(false)
        assertFalse(manager.areStreaksEnabled())
        
        // Sequence 3: multiple toggles
        manager.setEfficiencyMetricsEnabled(false)
        manager.setEfficiencyMetricsEnabled(true)
        manager.setEfficiencyMetricsEnabled(true)
        manager.setEfficiencyMetricsEnabled(false)
        manager.setEfficiencyMetricsEnabled(true)
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    /**
     * Property: For any state, StateFlow values reflect current state.
     *
     * Verifies that StateFlow values are always in sync with the manager state.
     */
    @Test
    fun testPropertyStateFlowConsistency() {
        val manager = GamificationToggleManager()
        
        // Check initial state
        assertEquals(manager.areBadgesEnabled(), manager.badgesEnabled.value)
        assertEquals(manager.areStreaksEnabled(), manager.streaksEnabled.value)
        assertEquals(manager.areEfficiencyMetricsEnabled(), manager.efficiencyMetricsEnabled.value)
        
        // Toggle and verify
        manager.setBadgesEnabled(false)
        assertEquals(manager.areBadgesEnabled(), manager.badgesEnabled.value)
        
        manager.setStreaksEnabled(false)
        assertEquals(manager.areStreaksEnabled(), manager.streaksEnabled.value)
        
        manager.setEfficiencyMetricsEnabled(false)
        assertEquals(manager.areEfficiencyMetricsEnabled(), manager.efficiencyMetricsEnabled.value)
        
        // Enable all and verify
        manager.enableAll()
        assertEquals(manager.areBadgesEnabled(), manager.badgesEnabled.value)
        assertEquals(manager.areStreaksEnabled(), manager.streaksEnabled.value)
        assertEquals(manager.areEfficiencyMetricsEnabled(), manager.efficiencyMetricsEnabled.value)
    }

    /**
     * Property: For any state, element count equals sum of enabled elements.
     *
     * Verifies that getEnabledElementCount equals the sum of individual enabled states.
     */
    @Test
    fun testPropertyElementCountEqualsSum() {
        val manager = GamificationToggleManager()
        
        // Test all combinations
        for (badges in listOf(true, false)) {
            for (streaks in listOf(true, false)) {
                for (efficiency in listOf(true, false)) {
                    manager.setBadgesEnabled(badges)
                    manager.setStreaksEnabled(streaks)
                    manager.setEfficiencyMetricsEnabled(efficiency)
                    
                    var expectedCount = 0
                    if (badges) expectedCount++
                    if (streaks) expectedCount++
                    if (efficiency) expectedCount++
                    
                    assertEquals(expectedCount, manager.getEnabledElementCount())
                }
            }
        }
    }

    /**
     * Property: For any state, isAnyElementEnabled equals (count > 0).
     *
     * Verifies that isAnyElementEnabled is true iff count is greater than 0.
     */
    @Test
    fun testPropertyIsAnyElementEnabledEqualsCountGreaterThanZero() {
        val manager = GamificationToggleManager()
        
        // Test all combinations
        for (badges in listOf(true, false)) {
            for (streaks in listOf(true, false)) {
                for (efficiency in listOf(true, false)) {
                    manager.setBadgesEnabled(badges)
                    manager.setStreaksEnabled(streaks)
                    manager.setEfficiencyMetricsEnabled(efficiency)
                    
                    val expectedAnyEnabled = manager.getEnabledElementCount() > 0
                    assertEquals(expectedAnyEnabled, manager.isAnyElementEnabled())
                }
            }
        }
    }

    /**
     * Property: For any state, areAllElementsEnabled equals (count == 3).
     *
     * Verifies that areAllElementsEnabled is true iff count equals 3.
     */
    @Test
    fun testPropertyAreAllElementsEnabledEqualsCountEqualsThree() {
        val manager = GamificationToggleManager()
        
        // Test all combinations
        for (badges in listOf(true, false)) {
            for (streaks in listOf(true, false)) {
                for (efficiency in listOf(true, false)) {
                    manager.setBadgesEnabled(badges)
                    manager.setStreaksEnabled(streaks)
                    manager.setEfficiencyMetricsEnabled(efficiency)
                    
                    val expectedAllEnabled = manager.getEnabledElementCount() == 3
                    assertEquals(expectedAllEnabled, manager.areAllElementsEnabled())
                }
            }
        }
    }
}
