package com.adhdfocus.app.ui.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.ui.theme.IncompleteRed
import com.adhdfocus.app.ui.theme.InProgressOrange
import com.adhdfocus.app.ui.theme.CompletedGreen

/**
 * TaskStatusIndicator displays a visual indicator for To Do status.
 *
 * Shows:
 * - Red for incomplete To Do's
 * - Orange for in-progress To Do's
 * - Green for completed To Do's
 */
@Composable
fun TaskStatusIndicator(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        TaskStatus.INCOMPLETE -> IncompleteRed
        TaskStatus.IN_PROGRESS -> InProgressOrange
        TaskStatus.COMPLETED -> CompletedGreen
    }

    val label = when (status) {
        TaskStatus.INCOMPLETE -> "●"
        TaskStatus.IN_PROGRESS -> "◐"
        TaskStatus.COMPLETED -> "✓"
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
