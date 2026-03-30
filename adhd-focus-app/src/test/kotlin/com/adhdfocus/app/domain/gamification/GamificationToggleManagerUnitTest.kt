package com.adhdfocus.app.domain.gamification

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for GamificationToggleManager.
 *
 * Tests cover:
 * - Individual element toggle functionality
 * - State queries (areBadgesEnabled, areStreaksEnabled, etc.)
 * - Element counting
 * - Batch operations (enableAll, disableAll, resetToDefaults)
 * - Edge cases
 */
class GamificationToggleManagerUnitTest {

    private lateinit var manager: GamificationToggleManager

    @Before
    fun setUp() {
        manager = GamificationToggleManager()
    }

    // ============ Individual Element Tests ============

    @Test
    fun testBadgesEnabledByDefault() {
        assertTrue(manager.areBadgesEnabled())
    }

    @Test
    fun testStreaksEnabledByDefault() {
        assertTrue(manager.areStreaksEnabled())
    }

    @Test
    fun testEfficiencyMetricsEnabledByDefault() {
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    @Test
    fun testSetBadgesEnabled() {
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
    }

    @Test
    fun testSetStreaksEnabled() {
        manager.setStreaksEnabled(false)
        assertFalse(manager.areStreaksEnabled())
        
        manager.setStreaksEnabled(true)
        assertTrue(manager.areStreaksEnabled())
    }

    @Test
    fun testSetEfficiencyMetricsEnabled() {
        manager.setEfficiencyMetricsEnabled(false)
        assertFalse(manager.areEfficiencyMetricsEnabled())
        
        manager.setEfficiencyMetricsEnabled(true)
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    // ============ Element Count Tests ============

    @Test
    fun testGetEnabledElementCountAllEnabled() {
        manager.enableAll()
        assertEquals(3, manager.getEnabledElementCount())
    }

    @Test
    fun testGetEnabledElementCountAllDisabled() {
        manager.disableAll()
        assertEquals(0, manager.getEnabledElementCount())
    }

    @Test
    fun testGetEnabledElementCountOneEnabled() {
        manager.disableAll()
        manager.setBadgesEnabled(true)
        assertEquals(1, manager.getEnabledElementCount())
    }

    @Test
    fun testGetEnabledElementCountTwoEnabled() {
        manager.disableAll()
        manager.setBadgesEnabled(true)
        manager.setStreaksEnabled(true)
        assertEquals(2, manager.getEnabledElementCount())
    }

    // ============ Any/All Element Tests ============

    @Test
    fun testIsAnyElementEnabledWhenAllEnabled() {
        manager.enableAll()
        assertTrue(manager.isAnyElementEnabled())
    }

    @Test
    fun testIsAnyElementEnabledWhenAllDisabled() {
        manager.disableAll()
        assertFalse(manager.isAnyElementEnabled())
    }

    @Test
    fun testIsAnyElementEnabledWhenOneEnabled() {
        manager.disableAll()
        manager.setBadgesEnabled(true)
        assertTrue(manager.isAnyElementEnabled())
    }

    @Test
    fun testAreAllElementsEnabledWhenAllEnabled() {
        manager.enableAll()
        assertTrue(manager.areAllElementsEnabled())
    }

    @Test
    fun testAreAllElementsEnabledWhenAllDisabled() {
        manager.disableAll()
        assertFalse(manager.areAllElementsEnabled())
    }

    @Test
    fun testAreAllElementsEnabledWhenOneDisabled() {
        manager.enableAll()
        manager.setBadgesEnabled(false)
        assertFalse(manager.areAllElementsEnabled())
    }

    // ============ Batch Operation Tests ============

    @Test
    fun testEnableAll() {
        manager.disableAll()
        manager.enableAll()
        
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    @Test
    fun testDisableAll() {
        manager.enableAll()
        manager.disableAll()
        
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertFalse(manager.areEfficiencyMetricsEnabled())
    }

    @Test
    fun testResetToDefaults() {
        manager.disableAll()
        manager.resetToDefaults()
        
        assertTrue(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    // ============ State Flow Tests ============

    @Test
    fun testBadgesEnabledStateFlow() {
        val flow = manager.badgesEnabled
        assertTrue(flow.value)
        
        manager.setBadgesEnabled(false)
        assertFalse(flow.value)
    }

    @Test
    fun testStreaksEnabledStateFlow() {
        val flow = manager.streaksEnabled
        assertTrue(flow.value)
        
        manager.setStreaksEnabled(false)
        assertFalse(flow.value)
    }

    @Test
    fun testEfficiencyMetricsEnabledStateFlow() {
        val flow = manager.efficiencyMetricsEnabled
        assertTrue(flow.value)
        
        manager.setEfficiencyMetricsEnabled(false)
        assertFalse(flow.value)
    }

    // ============ Edge Cases ============

    @Test
    fun testMultipleToggles() {
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        manager.setStreaksEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        manager.setEfficiencyMetricsEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertFalse(manager.areEfficiencyMetricsEnabled())
    }

    @Test
    fun testToggleSameElementMultipleTimes() {
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
        
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
    }

    @Test
    fun testIndependentElementToggling() {
        // Disable badges
        manager.setBadgesEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertTrue(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        // Disable streaks
        manager.setStreaksEnabled(false)
        assertFalse(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
        
        // Re-enable badges
        manager.setBadgesEnabled(true)
        assertTrue(manager.areBadgesEnabled())
        assertFalse(manager.areStreaksEnabled())
        assertTrue(manager.areEfficiencyMetricsEnabled())
    }

    @Test
    fun testEnabledElementCountAfterMultipleOperations() {
        assertEquals(3, manager.getEnabledElementCount())
        
        manager.setBadgesEnabled(false)
        assertEquals(2, manager.getEnabledElementCount())
        
        manager.setStreaksEnabled(false)
        assertEquals(1, manager.getEnabledElementCount())
        
        manager.setBadgesEnabled(true)
        assertEquals(2, manager.getEnabledElementCount())
        
        manager.disableAll()
        assertEquals(0, manager.getEnabledElementCount())
        
        manager.enableAll()
        assertEquals(3, manager.getEnabledElementCount())
    }
}
