package com.adhdfocus.app.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adhdfocus.app.domain.gamification.BadgeSystem
import java.time.LocalDate

/**
 * AchievementsView displays all badges and achievements organized by category.
 *
 * Features:
 * - Earned badges with unlock date and celebration styling
 * - Locked badges with progress indicator (0-100%)
 * - Category tabs for filtering badges
 * - Smooth scrolling with LazyColumn
 * - Streak display with history
 * - Efficiency statistics
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
    val categories = viewModel.getAllCategories()

    val filteredEarned = viewModel.getFilteredEarnedBadges()
    val filteredLocked = viewModel.getFilteredLockedBadges()
    val allVisibleBadges = filteredEarned + filteredLocked
    val seasonYear = allVisibleBadges.map { it.seasonYear }.maxOrNull() ?: LocalDate.now().year
    val categoriesToShow = (selectedCategory?.let { listOf(it) } ?: categories)
        .sortedBy { categorySortOrder(it) }

    Scaffold(
        containerColor = Color(0xFFF5F0FA),
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
                    containerColor = Color(0xFFEDE5F7),
                    titleContentColor = Color(0xFF101336),
                    navigationIconContentColor = Color(0xFF101336)
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
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF8FF),
                                Color(0xFFF1EAF7),
                                Color(0xFFDCD5E3)
                            ),
                            radius = 1200f
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
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFC9BED8))),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFCF8FF),
                            Color(0xFFF1E8FA),
                            Color(0xFFE1D8EA)
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
                color = Color(0xFF101336)
            )
            Text(
                text = "Collect the full year, one milestone at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF25214A)
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
                color = Color(0xFF101336)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF504B66)
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                color = Color.White.copy(alpha = 0.82f),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFDF9FF),
                            Color(0xFFF4ECFB)
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
                color = Color(0xFF101336)
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
                        color = Color(0xFF6F7483)
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 12.sp,
                        color = Color(0xFF101336)
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp),
                    color = Color(0xFFBFB5CF)
                )

                // Best streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bestStreak.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F7483)
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 12.sp,
                        color = Color(0xFF101336)
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
        BadgeSystem.BadgeCategory.WEEKLY_ACHIEVEMENTS -> "Weekly"
        BadgeSystem.BadgeCategory.STREAK_MILESTONES -> "Streaks"
        BadgeSystem.BadgeCategory.EFFICIENCY_BADGES -> "Efficiency"
    }
}

private fun categorySortOrder(category: BadgeSystem.BadgeCategory): Int {
    return when (category) {
        BadgeSystem.BadgeCategory.DAILY_MILESTONES -> 0
        BadgeSystem.BadgeCategory.STREAK_MILESTONES -> 1
        BadgeSystem.BadgeCategory.WEEKLY_ACHIEVEMENTS -> 2
        BadgeSystem.BadgeCategory.EFFICIENCY_BADGES -> 3
    }
}
