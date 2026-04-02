package com.adhdfocus.app.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adhdfocus.app.domain.gamification.BadgeSystem

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsView(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val earnedBadges by viewModel.earnedBadges.collectAsState()
    val lockedBadges by viewModel.lockedBadges.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val categories = viewModel.getAllCategories()

    val filteredEarned = viewModel.getFilteredEarnedBadges()
    val filteredLocked = viewModel.getFilteredLockedBadges()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak section
                item {
                    StreakSection(
                        currentStreak = currentStreak,
                        bestStreak = bestStreak
                    )
                }

                // Category tabs
                item {
                    CategoryTabs(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }

                // Earned badges section
                if (filteredEarned.isNotEmpty()) {
                    item {
                        Text(
                            text = "Earned Badges (${filteredEarned.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(filteredEarned) { badge ->
                        BadgeCard(badge = badge)
                    }
                }

                // Locked badges section
                if (filteredLocked.isNotEmpty()) {
                    item {
                        Text(
                            text = "Locked Badges (${filteredLocked.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    items(filteredLocked) { badge ->
                        BadgeCard(badge = badge)
                    }
                }

                // Empty state
                if (filteredEarned.isEmpty() && filteredLocked.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No badges yet. Start completing tasks to earn achievements!",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Streaks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
