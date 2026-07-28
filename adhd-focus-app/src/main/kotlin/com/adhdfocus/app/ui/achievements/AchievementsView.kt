package com.adhdfocus.app.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adhdfocus.app.data.model.TokenTransaction
import com.adhdfocus.app.domain.gamification.BadgeSystem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * AchievementsView displays all badges and achievements organized by category.
 *
 * Features:
 * - Earned badges with unlock date and celebration styling
 * - Locked badges with progress indicator (0-100%)
 * - Category tabs for filtering badges
 * - Smooth scrolling with LazyColumn
 * - Streak display with history
 * - WCAG 2.1 AA compliant styling
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AchievementsView(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.refreshAchievements()
    }

    val currentStreak by viewModel.currentStreak.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPuzzleAgeBand by viewModel.selectedPuzzleAgeBand.collectAsState()
    val currentPuzzle by viewModel.currentPuzzle.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val yearStats by viewModel.yearStats.collectAsState()
    val tokenBalance by viewModel.tokenBalance.collectAsState()
    val tokensEarnedThisWeek by viewModel.tokensEarnedThisWeek.collectAsState()
    val recentTokenTransactions by viewModel.recentTokenTransactions.collectAsState()
    val tokenMessage by viewModel.tokenMessage.collectAsState()
    val tokenWeekGrid by viewModel.tokenWeekGrid.collectAsState()
    var redeemAmount by remember { mutableStateOf("1") }
    val categories = viewModel.getAllCategories()

    val filteredEarned = viewModel.getFilteredEarnedBadges()
    val filteredLocked = viewModel.getFilteredLockedBadges()
    val allVisibleBadges = filteredEarned + filteredLocked
    val seasonYear = allVisibleBadges.map { it.seasonYear }.maxOrNull() ?: LocalDate.now().year
    val categoriesToShow = (selectedCategory?.let { listOf(it) } ?: categories)
        .sortedBy { categorySortOrder(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Achievements",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                            )
                        )
                    ),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SeasonHeader(
                        seasonYear = seasonYear,
                        badgeCount = allVisibleBadges.size,
                        earnedCount = filteredEarned.size,
                        lockedCount = filteredLocked.size
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    YearKpiSection(
                        stats = yearStats
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    TokenEconomyWeekSection(
                        tokenBalance = tokenBalance,
                        tokensEarnedThisWeek = tokensEarnedThisWeek,
                        weekGrid = tokenWeekGrid,
                        recentTransactions = recentTokenTransactions,
                        redeemAmount = redeemAmount,
                        tokenMessage = tokenMessage,
                        onRedeemAmountChange = {
                            redeemAmount = it.filter { char -> char.isDigit() }.take(3)
                            viewModel.clearTokenMessage()
                        },
                        onRedeem = {
                            viewModel.redeemTokens(redeemAmount.toIntOrNull() ?: 0)
                        }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    StreakSection(
                        currentStreak = currentStreak,
                        bestStreak = bestStreak
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    PuzzleSection(
                        selectedAgeBand = selectedPuzzleAgeBand,
                        currentPuzzle = currentPuzzle
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    CategoryTabs(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }

                categoriesToShow.forEach { category ->
                    val categoryBadges = allVisibleBadges.filter { badge ->
                        viewModel.getBadgeCategory(badge.badgeType) == category
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BadgeSectionHeader(
                            title = category.displayName(),
                            subtitle = "${categoryBadges.size} collectible badge${if (categoryBadges.size == 1) "" else "s"}"
                        )
                    }

                    if (categoryBadges.isNotEmpty()) {
                        items(
                            items = categoryBadges,
                            key = { badge -> "${badge.badgeType}-${badge.seasonYear}-${badge.id}" }
                        ) { badge ->
                            BadgeCardCompact(
                                badge = badge,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyCategoryState(category.displayName())
                        }
                    }
                }

                if (allVisibleBadges.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyCategoryState("Achievements")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearKpiSection(
    stats: AchievementYearStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiTile(
            label = "To Do's completed this year",
            value = stats.completedTodoCount.toString(),
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            label = "Perfect completion days",
            value = stats.perfectDayCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TokenEconomyWeekSection(
    tokenBalance: Int,
    tokensEarnedThisWeek: Int,
    weekGrid: TokenWeekGridUiState,
    recentTransactions: List<TokenTransaction>,
    redeemAmount: String,
    tokenMessage: String?,
    onRedeemAmountChange: (String) -> Unit,
    onRedeem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFE08A), MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ),
                shape = MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "This week: $tokensEarnedThisWeek token${if (tokensEarnedThisWeek == 1) "" else "s"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (tokensEarnedThisWeek > 0) {
                            "Keep it up. You're doing great!"
                        } else {
                            "Complete today's To Do's to earn tokens."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color(0xFFFFF2C2),
                    contentColor = Color(0xFF7C4A03)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tokenBalance.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "available",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TokenEconomyGrid(weekGrid = weekGrid)

            TokenLegend()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = redeemAmount,
                    onValueChange = onRedeemAmountChange,
                    label = { Text("Turn in") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRedeem,
                    enabled = tokenBalance > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Turn In Tokens")
                }
            }

            tokenMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Recent token activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (recentTransactions.isEmpty()) {
                    Text(
                        text = "No token activity yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentTransactions.forEach { transaction ->
                        TokenTransactionRow(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenEconomyGrid(
    weekGrid: TokenWeekGridUiState,
    modifier: Modifier = Modifier
) {
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val scrollState = rememberScrollState()
    val taskColumnWidth = 156.dp
    val dayColumnWidth = 88.dp
    val headerHeight = 86.dp
    val groupHeight = 34.dp
    val rowHeight = 78.dp
    val groupedRows = remember(weekGrid.rows) {
        weekGrid.rows.groupBy { row -> row.todoGroup.ifBlank { "Other" } }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.width(taskColumnWidth)) {
            Text(
                text = "To Do",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (weekGrid.rows.isEmpty()) {
                Text(
                    text = "No To Do's",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groupedRows.forEach { (group, rows) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(groupHeight)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    rows.forEachIndexed { index, tokenRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .background(
                                    if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                    }
                                )
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tokenRow.emoji?.takeIf { it.isNotBlank() } ?: "",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = tokenRow.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .width(dayColumnWidth * 7)
                    .height(headerHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekGrid.days.forEach { day ->
                    Column(
                        modifier = Modifier.width(dayColumnWidth),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (day == weekGrid.today) {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = dayFormatter.format(day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = dateFormatter.format(day),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (weekGrid.rows.isEmpty()) {
                Text(
                    text = "No token To Do's are due this week.",
                    modifier = Modifier
                        .width(dayColumnWidth * 7)
                        .padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                groupedRows.forEach { (_, rows) ->
                    Spacer(
                        modifier = Modifier
                            .width(dayColumnWidth * 7)
                            .height(groupHeight)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    )

                    rows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .width(dayColumnWidth * 7)
                                .height(rowHeight)
                                .background(
                                    if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.cells.forEach { cell ->
                                Box(
                                    modifier = Modifier.width(dayColumnWidth),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TokenEconomyCell(cell)
                                }
                            }
                        }
                    }
                }
        }
    }
}
}

@Composable
private fun TokenEconomyCell(cell: TokenTaskCellUiState) {
    when (cell.state) {
        TokenCellState.EARNED -> TokenStarCell(
            value = cell.tokenValue,
            filled = true
        )
        TokenCellState.DUE -> TokenStarCell(
            value = cell.tokenValue,
            filled = false
        )
        TokenCellState.MISSED -> Surface(
            modifier = Modifier.size(width = 58.dp, height = 52.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
        TokenCellState.NOT_DUE -> Surface(
            modifier = Modifier.size(width = 58.dp, height = 52.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                )
            }
        }
    }
}

@Composable
private fun TokenStarCell(
    value: Int,
    filled: Boolean
) {
    val badgeSize = 58.dp
    val valueFontSize = when {
        value >= 100 -> 9.sp
        value >= 10 -> 14.sp
        else -> 18.sp
    }
    val starColor = if (filled) Color(0xFFFFB703) else Color(0xFF4F7FE5)
    Box(
        modifier = Modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(badgeSize)) {
            val starPath = createTokenStarPath(
                center = Offset(size.width / 2f, size.height / 2f),
                outerRadius = min(size.width, size.height) * 0.47f,
                innerRadius = min(size.width, size.height) * 0.22f
            )
            if (filled) {
                drawPath(
                    path = starPath,
                    color = starColor
                )
            } else {
                drawPath(
                    path = starPath,
                    color = starColor,
                    style = Stroke(width = 2.8.dp.toPx())
                )
            }
        }
        Text(
            text = value.toString(),
            modifier = Modifier.width(44.dp),
            fontSize = valueFontSize,
            lineHeight = valueFontSize,
            fontWeight = FontWeight.Black,
            color = if (filled) Color.White else starColor,
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

private fun createTokenStarPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    repeat(10) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + index * PI / 5.0
        val x = center.x + (cos(angle) * radius).toFloat()
        val y = center.y + (sin(angle) * radius).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

@Composable
private fun TokenLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(symbol = "★", label = "Completed", color = Color(0xFFFFB703))
        LegendItem(symbol = "☆", label = "Due", color = Color(0xFF4F7FE5))
        LegendItem(symbol = "-", label = "Missed", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LegendItem(symbol = "-", label = "Not due", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
    }
}

@Composable
private fun LegendItem(
    symbol: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TokenTransactionRow(transaction: TokenTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transaction.note ?: transaction.type.name.replace('_', ' '),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "${if (transaction.amount > 0) "+" else ""}${transaction.amount}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (transaction.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SeasonHeader(
    seasonYear: Int,
    badgeCount: Int,
    earnedCount: Int,
    lockedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color.White, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))),
                shape = MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "$seasonYear Badge Season",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Collect the full year, one milestone at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text("$badgeCount total") })
                AssistChip(onClick = {}, label = { Text("$earnedCount earned") })
                AssistChip(onClick = {}, label = { Text("$lockedCount locked") })
            }
        }
    }
}

@Composable
private fun BadgeSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCategoryState(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = "No $categoryName badges yet. Keep going and this wall will fill up.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * StreakSection displays current and best streak with visual emphasis.
 */
@Composable
private fun StreakSection(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
                shape = MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Streaks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Current streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentStreak.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )

                // Best streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bestStreak.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * CategoryTabs displays category filter tabs for badge organization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    categories: List<BadgeSystem.BadgeCategory>,
    selectedCategory: BadgeSystem.BadgeCategory?,
    onCategorySelected: (BadgeSystem.BadgeCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // "All" tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                modifier = Modifier.height(40.dp)
            )

            // Category tabs
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.displayName()) },
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

/**
 * Extension function to get display name for badge category.
 */
private fun BadgeSystem.BadgeCategory.displayName(): String {
    return when (this) {
        BadgeSystem.BadgeCategory.DAILY_MILESTONES -> "Daily"
        BadgeSystem.BadgeCategory.STREAK_MILESTONES -> "Streaks"
    }
}

private fun categorySortOrder(category: BadgeSystem.BadgeCategory): Int {
    return when (category) {
        BadgeSystem.BadgeCategory.DAILY_MILESTONES -> 0
        BadgeSystem.BadgeCategory.STREAK_MILESTONES -> 1
    }
}
