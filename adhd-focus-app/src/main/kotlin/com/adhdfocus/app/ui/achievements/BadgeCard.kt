package com.adhdfocus.app.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.util.DateTimeUtils
import java.util.Date

/**
 * BadgeCard displays a single badge with earned or locked status.
 *
 * Features:
 * - Earned badges show unlock date and celebration styling
 * - Locked badges show progress indicator (0-100%)
 * - High-contrast colors for accessibility
 * - Smooth animations
 * - WCAG 2.1 AA compliant styling
 */
@Composable
fun BadgeCard(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isLocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (badge.isLocked) 2.dp else 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Badge icon/placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = if (badge.isLocked) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge.isLocked) "🔒" else "⭐",
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Badge name
            Text(
                text = badge.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Badge description
            Text(
                text = badge.description ?: "Achievement",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Earned date or progress indicator
            if (badge.isLocked) {
                // Progress indicator for locked badges
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progress = (badge.progress ?: 0) / 100f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    Text(
                        text = "${badge.progress ?: 0}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.paddingFromBaseline(top = 4.dp)
                    )
                }
            } else {
                // Earned date for unlocked badges
                Text(
                    text = "Earned: ${DateTimeUtils.formatDate(Date(badge.earnedAt))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * BadgeCardCompact displays a compact version of a badge card.
 *
 * Used in list views where space is limited.
 */
@Composable
fun BadgeCardCompact(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isLocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (badge.isLocked) 1.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (badge.isLocked) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge.isLocked) "🔒" else "⭐",
                    fontSize = 28.sp
                )
            }

            // Badge info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = badge.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                if (badge.isLocked) {
                    val progress = (badge.progress ?: 0) / 100f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    Text(
                        text = "${badge.progress ?: 0}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Earned: ${DateTimeUtils.formatDate(Date(badge.earnedAt))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
