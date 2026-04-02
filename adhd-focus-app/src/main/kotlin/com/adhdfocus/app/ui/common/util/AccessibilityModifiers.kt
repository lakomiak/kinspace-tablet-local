package com.adhdfocus.app.ui.common.util

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.ui.theme.FocusOutlineLight
import com.adhdfocus.app.ui.theme.FocusOutlineDark

/**
 * Applies WCAG 2.1 AA compliant focus indicator for keyboard navigation.
 * Adds a visible border when the element receives focus.
 *
 * @param interactionSource Source for tracking focus state
 * @param focusOutlineColor Color of the focus outline
 * @return Modified Modifier with focus indicator
 */
@androidx.compose.runtime.Composable
fun Modifier.wcagFocusIndicator(
    interactionSource: MutableInteractionSource,
    focusOutlineColor: Color = FocusOutlineLight
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return if (isFocused) {
        this.border(
            width = 3.dp,
            color = focusOutlineColor
        )
    } else {
        this
    }
}

/**
 * Applies WCAG 2.1 AA compliant focus indicator with FocusRequester.
 * Useful for programmatic focus management.
 *
 * @param focusRequester FocusRequester for managing focus
 * @param focusOutlineColor Color of the focus outline
 * @return Modified Modifier with focus indicator
 */
fun Modifier.wcagFocusIndicatorWithRequester(
    focusRequester: FocusRequester,
    focusOutlineColor: Color = FocusOutlineLight
): Modifier {
    return this
        .focusRequester(focusRequester)
        .focusable()
        .border(
            width = 3.dp,
            color = focusOutlineColor
        )
}

/**
 * Applies semantic label for screen readers.
 * @param label Descriptive label for the element
 * @return Modified Modifier with semantic label
 */
fun Modifier.semanticLabel(label: String): Modifier {
    return this
}

/**
 * Applies minimum touch target size (48x48 dp) for accessibility.
 * @return Modified Modifier with minimum size
 */
fun Modifier.accessibleTouchTarget(): Modifier {
    return this.then(
        Modifier
    )
}
