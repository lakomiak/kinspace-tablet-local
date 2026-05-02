package com.adhdfocus.app.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adhdfocus.app.data.model.Task

/**
 * Composable for displaying update notifications for new To Do's.
 *
 * Features:
 * - High-contrast green background for new To Do indicator
 * - To Do title and group display
 * - Estimated duration display
 * - Dismiss button
 * - Smooth animations
 * - Accessible design
 */
@Composable
fun UpdateNotificationComposable(
    viewModel: UpdateNotificationViewModel = hiltViewModel()
) {
    val isVisible = viewModel.isVisible.collectAsState()
    val currentNotification = viewModel.currentNotification.collectAsState()

    AnimatedVisibility(
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        currentNotification.value?.let { task ->
            NotificationCard(
                task = task,
                onDismiss = { viewModel.dismissNotification() }
            )
        }
    }
}

@Composable
private fun NotificationCard(
    task: Task,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color(0xFF43A047), // High-contrast green
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "New To Do",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Text(
                        text = task.todoGroup,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    task.estimatedDurationMinutes?.let { duration ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "~${duration}m",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = Color.White
                )
            }
        }
    }
}
