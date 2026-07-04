package com.adhdfocus.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ReportsScreen(
    householdId: String,
    onBackClick: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val selectedMemberId by viewModel.selectedMemberId.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(householdId) {
        viewModel.initialize(householdId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBackClick) {
            Text("Back to Settings")
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Reports",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
            text = "Select a family member to view local tablet stats and timer patterns.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        }

        if (members.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { member ->
                    val isSelected = member.id == selectedMemberId
                    val memberButtonModifier = Modifier.semantics {
                        role = Role.RadioButton
                        selected = isSelected
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                        contentDescription = buildString {
                            append(member.name)
                            append(", ")
                            append(if (isSelected) "selected family member" else "family member")
                        }
                    }
                    if (isSelected) {
                        Button(
                            onClick = { viewModel.selectMember(householdId, member.id) },
                            modifier = memberButtonModifier
                        ) {
                            Text(member.name)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.selectMember(householdId, member.id) },
                            modifier = memberButtonModifier
                        ) {
                            Text(member.name)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            StatsCard("Completed To Dos", summary.completedTodos.toString(), "Tracked completions on this tablet")
            StatsCard("Current Streak", summary.currentStreak.toString(), "Consecutive fully completed days")
            StatsCard("Best Streak", summary.bestStreak.toString(), "Best streak recorded locally")
            TodoMetricBreakdownCard(
                title = "Average Completion Time By To Do",
                description = "Average active timer time before completing each To Do.",
                items = summary.todoBreakdown,
                valueText = { "${it.averageCompletionMinutes.format1()} min" },
                percentageValue = null
            )
            TodoMetricBreakdownCard(
                title = "Average Paused Time By To Do",
                description = "Average share of tracked session time spent paused for each To Do.",
                items = summary.todoBreakdown,
                valueText = { "${it.averagePausedPercent.format1()}%" },
                percentageValue = { it.averagePausedPercent }
            )
            TodoMetricBreakdownCard(
                title = "Restarted Sessions By To Do",
                description = "Timer sessions where the To Do was restarted or reset at least once.",
                items = summary.todoBreakdown,
                valueText = { "${it.restartedSessionPercent.format1()}%" },
                percentageValue = { it.restartedSessionPercent }
            )
            TodoMetricBreakdownCard(
                title = "Canceled Sessions By To Do",
                description = "Timer sessions stopped without completing the To Do.",
                items = summary.todoBreakdown,
                valueText = { "${it.canceledSessionPercent.format1()}%" },
                percentageValue = { it.canceledSessionPercent }
            )
            TodoMetricBreakdownCard(
                title = "Stopped Before End By To Do",
                description = "Timer sessions canceled before the countdown ran out.",
                items = summary.todoBreakdown,
                valueText = { "${it.stoppedBeforeEndPercent.format1()}%" },
                percentageValue = { it.stoppedBeforeEndPercent }
            )
            TodoMetricBreakdownCard(
                title = "Completed After Timer Ended By To Do",
                description = "Completed sessions where the countdown had already ended.",
                items = summary.todoBreakdown,
                valueText = { "${it.completedAfterEndPercent.format1()}%" },
                percentageValue = { it.completedAfterEndPercent }
            )
            StatsCard("Best Success Window", summary.successfulTimeWindow, "When this person most often completes timed todos successfully")
            StatsCard("Timed Sessions", summary.sessionCount.toString(), "Recorded timer sessions available for analysis")
            RecommendationsCard(summary.recommendations)
            TrendCard(
                title = "Last 7 Days of Completed To Dos",
                description = "Daily completion counts on this tablet for the selected family member.",
                items = summary.recentCompletionTrend,
                chartStyle = TrendChartStyle.Bars
            )
            TrendCard(
                title = "Last 7 Days of Completion Time",
                description = "Average active timer minutes for successful timed completions.",
                items = summary.recentCompletionTimeTrend,
                chartStyle = TrendChartStyle.Line
            )
            BreakdownCard(
                title = "Completed To Dos By To Do",
                description = "Which To Dos this person completes most often on the tablet.",
                items = summary.categoryBreakdown
            )
            BreakdownCard(
                title = "Timer Session Outcomes",
                description = "How timed sessions tend to end for this person.",
                items = summary.timerOutcomeBreakdown
            )
        }
    }
}

@Composable
private fun TodoMetricBreakdownCard(
    title: String,
    description: String,
    items: List<TodoReportBreakdown>,
    valueText: (TodoReportBreakdown) -> String,
    percentageValue: ((TodoReportBreakdown) -> Double)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            if (items.isEmpty()) {
                Text(
                    text = "Not enough timed sessions yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.todoTitle,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.sessionCount} sessions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = valueText(item),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (percentageValue != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((percentageValue(item) / 100.0).coerceIn(0.0, 1.0).toFloat())
                                        .height(10.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCard(recommendations: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Suggested Next Moves",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (recommendations.isEmpty()) {
                Text(
                    text = "Keep using the tablet to gather more history before recommendations appear.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                recommendations.forEachIndexed { index, recommendation ->
                    Text(
                        text = "${index + 1}. $recommendation",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TrendCard(
    title: String,
    description: String,
    items: List<ReportTrendPoint>,
    chartStyle: TrendChartStyle
) {
    val accessibilitySummary = remember(items, title, description) {
        buildChartAccessibilitySummary(
            title = title,
            description = description,
            items = items
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            if (items.isEmpty()) {
                Text(
                    text = "Not enough local history yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                when (chartStyle) {
                    TrendChartStyle.Bars -> TrendBarChart(items, accessibilitySummary)
                    TrendChartStyle.Line -> TrendLineChart(items, accessibilitySummary)
                }
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.label,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.secondaryText,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    description: String,
    items: List<ReportBreakdownItem>
) {
    val accessibilitySummary = remember(items, title, description) {
        buildBreakdownAccessibilitySummary(
            title = title,
            description = description,
            items = items
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            if (items.isEmpty()) {
                Text(
                    text = "Not enough local history yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                BreakdownDonutChart(items, accessibilitySummary)
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.label,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.count} - ${item.percentage.format1()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((item.percentage / 100.0).coerceIn(0.0, 1.0).toFloat())
                                    .height(10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class TrendChartStyle {
    Bars,
    Line
}

@Composable
private fun TrendBarChart(
    items: List<ReportTrendPoint>,
    accessibilitySummary: String
) {
    val maxValue = items.maxOfOrNull { it.primaryValue }?.coerceAtLeast(1.0) ?: 1.0
    val barColor = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .semantics {
                contentDescription = accessibilitySummary
            }
    ) {
        val gap = 12f
        val barWidth = ((size.width - (gap * (items.size + 1))) / items.size).coerceAtLeast(8f)
        items.forEachIndexed { index, item ->
            val left = gap + index * (barWidth + gap)
            val normalizedHeight = ((item.primaryValue / maxValue) * size.height).toFloat()
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - normalizedHeight),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
private fun TrendLineChart(
    items: List<ReportTrendPoint>,
    accessibilitySummary: String
) {
    val maxValue = items.maxOfOrNull { it.primaryValue }?.coerceAtLeast(1.0) ?: 1.0
    val minValue = items.minOfOrNull { it.primaryValue } ?: 0.0
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.tertiary
    val pointInnerColor = MaterialTheme.colorScheme.tertiary
    val pointOuterColor = MaterialTheme.colorScheme.background
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .semantics {
                contentDescription = accessibilitySummary
            }
    ) {
        val horizontalStep = if (items.size > 1) size.width / (items.size - 1) else size.width
        val path = Path()
        val points = items.mapIndexed { index, item ->
            val x = horizontalStep * index
            val normalized = ((item.primaryValue - minValue) / range).toFloat()
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }
        points.forEachIndexed { index, offset ->
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        points.forEach { point ->
            drawCircle(
                color = pointOuterColor,
                radius = 8f,
                center = point
            )
            drawCircle(
                color = pointInnerColor,
                radius = 5f,
                center = point
            )
        }
    }
}

@Composable
private fun BreakdownDonutChart(
    items: List<ReportBreakdownItem>,
    accessibilitySummary: String
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFF6AA84F),
        Color(0xFFF1C232),
        Color(0xFFE06666)
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 4.dp)
            .semantics {
                contentDescription = accessibilitySummary
            }
    ) {
        val strokeWidth = 36f
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            val sweep = ((item.percentage / 100.0) * 360.0).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(strokeWidth, strokeWidth / 2),
                size = Size(size.width - strokeWidth * 2, size.height - strokeWidth)
            )
            startAngle += sweep
        }
    }
}

private fun buildChartAccessibilitySummary(
    title: String,
    description: String,
    items: List<ReportTrendPoint>
): String {
    val points = if (items.isEmpty()) {
        "No chart data available yet."
    } else {
        items.joinToString(separator = ". ") { item ->
            "${item.label}: ${item.secondaryText}"
        }
    }
    return "$title. $description. $points"
}

private fun buildBreakdownAccessibilitySummary(
    title: String,
    description: String,
    items: List<ReportBreakdownItem>
): String {
    val points = if (items.isEmpty()) {
        "No breakdown data available yet."
    } else {
        items.joinToString(separator = ". ") { item ->
            "${item.label}: ${item.count}, ${item.percentage.format1()} percent"
        }
    }
    return "$title. $description. $points"
}

private fun Double.format1(): String = String.format("%.1f", this)
