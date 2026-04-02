package com.adhdfocus.app.ui.common.util

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Utility functions for keyboard navigation support.
 * Provides accessible keyboard navigation for all interactive elements.
 */
object KeyboardNavigation {

    /**
     * Checks if a key event is a navigation key (arrow keys, Tab, Enter).
     * @param keyEvent The key event to check
     * @return True if the event is a navigation key
     */
    fun isNavigationKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.key in listOf(
            Key.Tab,
            Key.DirectionUp,
            Key.DirectionDown,
            Key.DirectionLeft,
            Key.DirectionRight,
            Key.Enter,
            Key.Spacebar
        )
    }

    /**
     * Checks if a key event is an activation key (Enter or Spacebar).
     * @param keyEvent The key event to check
     * @return True if the event is an activation key
     */
    fun isActivationKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.key in listOf(Key.Enter, Key.Spacebar)
    }

    /**
     * Checks if a key event is a Tab key.
     * @param keyEvent The key event to check
     * @return True if the event is a Tab key
     */
    fun isTabKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.key == Key.Tab
    }

    /**
     * Checks if a key event is an arrow key.
     * @param keyEvent The key event to check
     * @return True if the event is an arrow key
     */
    fun isArrowKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.key in listOf(
            Key.DirectionUp,
            Key.DirectionDown,
            Key.DirectionLeft,
            Key.DirectionRight
        )
    }

    /**
     * Gets the focus direction from an arrow key event.
     * @param keyEvent The key event
     * @return FocusDirection or null if not an arrow key
     */
    fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        return when (keyEvent.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> null
        }
    }
}

/**
 * Applies keyboard navigation support to a modifier.
 * Handles Tab, arrow keys, and Enter/Spacebar for activation.
 *
 * @param focusManager The focus manager for navigation
 * @param onActivate Callback when activation key is pressed
 * @return Modified Modifier with keyboard navigation
 */
fun Modifier.keyboardNavigable(
    focusManager: FocusManager,
    onActivate: () -> Unit = {}
): Modifier {
    return this.onKeyEvent { keyEvent ->
        when {
            KeyboardNavigation.isActivationKey(keyEvent) -> {
                onActivate()
                true
            }
            KeyboardNavigation.isTabKey(keyEvent) -> {
                // Tab navigation is handled by default focus system
                false
            }
            KeyboardNavigation.isArrowKey(keyEvent) -> {
                val direction = KeyboardNavigation.getFocusDirection(keyEvent)
                if (direction != null) {
                    focusManager.moveFocus(direction)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}

/**
 * Applies keyboard navigation with custom focus requester.
 * @param focusRequester The focus requester for this element
 * @param focusManager The focus manager for navigation
 * @param onActivate Callback when activation key is pressed
 * @return Modified Modifier with keyboard navigation
 */
fun Modifier.keyboardNavigableWithRequester(
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    onActivate: () -> Unit = {}
): Modifier {
    return this
        .focusRequester(focusRequester)
        .keyboardNavigable(focusManager, onActivate)
}

/**
 * Applies focus indicator that shows when element has keyboard focus.
 * @param interactionSource The interaction source for tracking focus
 * @return Modified Modifier with focus indicator
 */
@androidx.compose.runtime.Composable
fun Modifier.keyboardFocusIndicator(
    interactionSource: MutableInteractionSource
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return if (isFocused) {
        this.wcagFocusIndicator(interactionSource)
    } else {
        this
    }
}

/**
 * Applies tab order to an element for keyboard navigation.
 * Lower values are focused first.
 * @param tabOrder The tab order value
 * @return Modified Modifier with tab order
 */
fun Modifier.tabOrder(tabOrder: Int): Modifier {
    // Tab order is typically handled by the composition order in Compose
    // This is a placeholder for future implementation if needed
    return this
}

/**
 * Marks an element as keyboard accessible.
 * @return Modified Modifier marked as keyboard accessible
 */
fun Modifier.keyboardAccessible(): Modifier {
    return this
}
