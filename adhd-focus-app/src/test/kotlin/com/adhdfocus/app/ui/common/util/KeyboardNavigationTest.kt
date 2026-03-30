package com.adhdfocus.app.ui.common.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for keyboard navigation support.
 */
class KeyboardNavigationTest {

    @Test
    fun testTabKeyIsNavigationKey() {
        val keyEvent = createKeyEvent(Key.Tab)
        assertTrue(KeyboardNavigation.isNavigationKey(keyEvent))
    }

    @Test
    fun testArrowKeysAreNavigationKeys() {
        assertTrue(KeyboardNavigation.isNavigationKey(createKeyEvent(Key.DirectionUp)))
        assertTrue(KeyboardNavigation.isNavigationKey(createKeyEvent(Key.DirectionDown)))
        assertTrue(KeyboardNavigation.isNavigationKey(createKeyEvent(Key.DirectionLeft)))
        assertTrue(KeyboardNavigation.isNavigationKey(createKeyEvent(Key.DirectionRight)))
    }

    @Test
    fun testEnterKeyIsNavigationKey() {
        val keyEvent = createKeyEvent(Key.Enter)
        assertTrue(KeyboardNavigation.isNavigationKey(keyEvent))
    }

    @Test
    fun testSpacebarIsNavigationKey() {
        val keyEvent = createKeyEvent(Key.Spacebar)
        assertTrue(KeyboardNavigation.isNavigationKey(keyEvent))
    }

    @Test
    fun testNonNavigationKeyIsNotNavigationKey() {
        val keyEvent = createKeyEvent(Key.A)
        assertFalse(KeyboardNavigation.isNavigationKey(keyEvent))
    }

    @Test
    fun testEnterKeyIsActivationKey() {
        val keyEvent = createKeyEvent(Key.Enter)
        assertTrue(KeyboardNavigation.isActivationKey(keyEvent))
    }

    @Test
    fun testSpacebarIsActivationKey() {
        val keyEvent = createKeyEvent(Key.Spacebar)
        assertTrue(KeyboardNavigation.isActivationKey(keyEvent))
    }

    @Test
    fun testTabKeyIsNotActivationKey() {
        val keyEvent = createKeyEvent(Key.Tab)
        assertFalse(KeyboardNavigation.isActivationKey(keyEvent))
    }

    @Test
    fun testArrowKeyIsNotActivationKey() {
        val keyEvent = createKeyEvent(Key.DirectionUp)
        assertFalse(KeyboardNavigation.isActivationKey(keyEvent))
    }

    @Test
    fun testTabKeyIsTabKey() {
        val keyEvent = createKeyEvent(Key.Tab)
        assertTrue(KeyboardNavigation.isTabKey(keyEvent))
    }

    @Test
    fun testEnterKeyIsNotTabKey() {
        val keyEvent = createKeyEvent(Key.Enter)
        assertFalse(KeyboardNavigation.isTabKey(keyEvent))
    }

    @Test
    fun testArrowKeysAreArrowKeys() {
        assertTrue(KeyboardNavigation.isArrowKey(createKeyEvent(Key.DirectionUp)))
        assertTrue(KeyboardNavigation.isArrowKey(createKeyEvent(Key.DirectionDown)))
        assertTrue(KeyboardNavigation.isArrowKey(createKeyEvent(Key.DirectionLeft)))
        assertTrue(KeyboardNavigation.isArrowKey(createKeyEvent(Key.DirectionRight)))
    }

    @Test
    fun testTabKeyIsNotArrowKey() {
        val keyEvent = createKeyEvent(Key.Tab)
        assertFalse(KeyboardNavigation.isArrowKey(keyEvent))
    }

    @Test
    fun testEnterKeyIsNotArrowKey() {
        val keyEvent = createKeyEvent(Key.Enter)
        assertFalse(KeyboardNavigation.isArrowKey(keyEvent))
    }

    @Test
    fun testGetFocusDirectionUp() {
        val keyEvent = createKeyEvent(Key.DirectionUp)
        val direction = KeyboardNavigation.getFocusDirection(keyEvent)
        assertTrue(direction != null)
    }

    @Test
    fun testGetFocusDirectionDown() {
        val keyEvent = createKeyEvent(Key.DirectionDown)
        val direction = KeyboardNavigation.getFocusDirection(keyEvent)
        assertTrue(direction != null)
    }

    @Test
    fun testGetFocusDirectionLeft() {
        val keyEvent = createKeyEvent(Key.DirectionLeft)
        val direction = KeyboardNavigation.getFocusDirection(keyEvent)
        assertTrue(direction != null)
    }

    @Test
    fun testGetFocusDirectionRight() {
        val keyEvent = createKeyEvent(Key.DirectionRight)
        val direction = KeyboardNavigation.getFocusDirection(keyEvent)
        assertTrue(direction != null)
    }

    @Test
    fun testGetFocusDirectionNonArrowKey() {
        val keyEvent = createKeyEvent(Key.Tab)
        val direction = KeyboardNavigation.getFocusDirection(keyEvent)
        assertTrue(direction == null)
    }

    // Helper function to create KeyEvent for testing
    private fun createKeyEvent(key: Key): KeyEvent {
        return KeyEvent(key)
    }
}
