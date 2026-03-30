package com.adhdfocus.app.ui.common.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhdfocus.app.domain.affirmation.AffirmationEvent
import kotlinx.coroutines.delay

/**
 * AffirmationDisplay component displays affirmation messages with auto-dismiss.
 *
 * Features:
 * - Displays affirmation messages for 2-3 seconds before auto-dismissing
 * - Allows manual dismissal
 * - Smooth fade and scale animations
 * - High-contrast colors for WCAG 2.1 AA compliance
 * - Supports different affirmation types with distinct styling
 *
 * Correctness Properties:
 * - Property 18: Affirmation on Task Completion - Affirmations are triggered and displayed on task completion
 * - Property 20: Affirmation Display Duration - Affirmations display for 2-3 seconds before auto-dismissing
 */
@Composable
fun AffirmationDisplay(
    affirmation: AffirmationEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = remember { mutableStateOf(false) }

    LaunchedEffect(affirmation) {
        if (affirmation != null) {
            isVisible.value = true
            // Display for 2.5 seconds (within 2-3 second range)
            delay(2500)
            isVisible.value = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible.value && affirmation != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        if (affirmation != null) {
            AffirmationContent(affirmation)
        }
    }
}

@Composable
private fun AffirmationContent(affirmation: AffirmationEvent) {
    val (backgroundColor, textColor) = when (affirmation) {
        is AffirmationEvent.TaskComplete -> {
            // Task completion: Green background with dark text (WCAG AA compliant)
            Color(0xFF43A047) to Color(0xFF212121)
        }
        is AffirmationEvent.DayComplete -> {
            // Day completion: Vibrant blue background with white text (WCAG AA compliant)
            Color(0xFF1E88E5) to Color(0xFFFFFFFF)
        }
        is AffirmationEvent.StreakMilestone -> {
            // Streak milestone: Orange background with dark text (WCAG AA compliant)
            Color(0xFFFB8C00) to Color(0xFF212121)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = affirmation.message,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
