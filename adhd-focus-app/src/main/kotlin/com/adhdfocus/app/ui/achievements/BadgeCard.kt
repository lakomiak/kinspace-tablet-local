package com.adhdfocus.app.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.util.DateTimeUtils

@Composable
fun BadgeCard(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    BadgeCardCompact(
        badge = badge,
        modifier = modifier.height(140.dp)
    )
}

@Composable
fun BadgeCardCompact(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    var visible by remember(badge.id) { mutableStateOf(false) }

    LaunchedEffect(badge.id, badge.isLocked) {
        visible = true
    }

    AnimatedVisibility(
        visible = if (badge.isLocked) true else visible,
        enter = fadeIn() + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    ) {
        val locked = badge.isLocked
        val progress = (badge.progress ?: 0).coerceIn(0, 100)
        val cardBrush = if (locked) {
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.surface
                )
            )
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(126.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.outline.copy(alpha = if (locked) 0.65f else 0.38f)
                        )
                    ),
                    shape = MaterialTheme.shapes.large
                ),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = if (locked) 1.dp else 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardBrush)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.08f),
                            radius = size.width * 0.55f
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width * 0.18f, size.height * 0.08f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(18f, size.height - 18f),
                        end = Offset(size.width - 22f, 14f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AchievementBadgeArt(
                        badgeType = badge.badgeType,
                        locked = locked,
                        modifier = Modifier.size(78.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = badge.name,
                            fontSize = 17.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp),
                            color = if (locked) Color(0xFF7E7893) else Color(0xFFD99318),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        )

                        Text(
                            text = if (locked) {
                                "$progress%"
                            } else {
                                "Earned ${DateTimeUtils.formatDate(badge.earnedAt)}"
                            },
                            fontSize = 11.sp,
                            color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadgeArt(
    badgeType: String,
    locked: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeSpec = remember(badgeType) { badgeSpecForType(badgeType) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shield = Path().apply {
                moveTo(w * 0.5f, h * 0.05f)
                lineTo(w * 0.88f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.62f)
                quadraticBezierTo(w * 0.5f, h * 0.95f, w * 0.18f, h * 0.62f)
                lineTo(w * 0.12f, h * 0.18f)
                close()
            }

            drawPath(
                path = shield,
                brush = Brush.linearGradient(
                    colors = if (locked) {
                        listOf(Color(0xFFF6F4FA), Color(0xFFB9B5C5), Color(0xFF7E7A91))
                    } else {
                        listOf(Color(0xFFFFF3BA), Color(0xFFE4A936), Color(0xFF8E5F09))
                    },
                    start = Offset(w * 0.2f, 0f),
                    end = Offset(w, h)
                )
            )
            drawPath(
                path = shield,
                color = if (locked) Color(0xFF6D687C) else Color(0xFF704A07),
                style = Stroke(width = w * 0.055f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(w * 0.38f, h * 0.22f),
                    radius = w * 0.5f
                ),
                center = Offset(w * 0.38f, h * 0.22f),
                radius = w * 0.46f
            )

            drawRoundRect(
                color = if (locked) Color(0xFFE9E7F0) else Color(0xFFFFF3C0),
                topLeft = Offset(w * 0.27f, h * 0.27f),
                size = Size(w * 0.46f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
            )
            drawRoundRect(
                color = if (locked) Color(0xFF7F7A91) else Color(0xFF7D5308),
                topLeft = Offset(w * 0.27f, h * 0.27f),
                size = Size(w * 0.46f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                style = Stroke(width = w * 0.035f)
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = if (badgeSpec.label.length <= 2) w * 0.34f else w * 0.22f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    color = if (locked) {
                        android.graphics.Color.rgb(255, 255, 255)
                    } else {
                        android.graphics.Color.rgb(96, 61, 0)
                    }
                    setShadowLayer(w * 0.035f, 0f, w * 0.025f, android.graphics.Color.argb(120, 0, 0, 0))
                }
                val y = h * 0.53f - ((paint.descent() + paint.ascent()) / 2f)
                canvas.nativeCanvas.drawText(badgeSpec.label, w * 0.5f, y, paint)
            }

            repeat(4) { index ->
                val left = w * (0.31f + index * 0.1f)
                drawRoundRect(
                    color = if (locked) Color(0xFF7F7A91) else Color(0xFF6E4908),
                    topLeft = Offset(left, h * 0.72f),
                    size = Size(w * 0.06f, h * 0.045f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.015f)
                )
            }
        }

        if (locked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = Color(0xFFF5F1FF),
                shadowElevation = 5.dp,
                tonalElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF55516A),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private data class BadgeVisualSpec(val label: String)

private fun badgeSpecForType(badgeType: String): BadgeVisualSpec {
    val label = when (badgeType) {
        "FIRST_TASK_COMPLETE" -> "OK"
        "FIVE_TASK_DAY" -> "5"
        "PERFECT_DAY" -> "ALL"
        "ONE_DAY_STREAK" -> "1"
        "THREE_DAY_STREAK" -> "3"
        "SEVEN_DAY_STREAK" -> "7"
        "FOURTEEN_DAY_STREAK" -> "14"
        "THIRTY_DAY_STREAK" -> "30"
        "SIXTY_DAY_STREAK" -> "60"
        "NINETY_DAY_STREAK" -> "90"
        "ONE_EIGHTY_DAY_STREAK" -> "180"
        "TWO_SEVENTY_DAY_STREAK" -> "270"
        "YEAR_STREAK" -> "365"
        "SPEED_DEMON" -> "SPD"
        else -> "BADGE"
    }
    return BadgeVisualSpec(label)
}
