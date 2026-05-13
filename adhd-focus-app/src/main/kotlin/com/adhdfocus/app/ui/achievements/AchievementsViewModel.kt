package com.adhdfocus.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.repository.BadgeRepository
import com.adhdfocus.app.data.repository.StreakRepository
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.gamification.BadgeSystem
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AchievementsViewModel manages the state for the Achievements View.
 *
 * Manages:
 * - Earned badges organized by category
 * - Locked badges with progress indicators
 * - Current and best streak
 * - Category filtering
 * - Current user
 */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val badgeSystem: BadgeSystem,
    private val streakRepository: StreakRepository,
    private val userRepository: UserRepository,
    private val setupManager: TabletSetupManager
) : ViewModel() {

    private val _earnedBadges = MutableStateFlow<List<Badge>>(emptyList())
    val earnedBadges: StateFlow<List<Badge>> = _earnedBadges

    private val _lockedBadges = MutableStateFlow<List<Badge>>(emptyList())
    val lockedBadges: StateFlow<List<Badge>> = _lockedBadges

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _bestStreak = MutableStateFlow(0)
    val bestStreak: StateFlow<Int> = _bestStreak

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _selectedCategory = MutableStateFlow<BadgeSystem.BadgeCategory?>(null)
    val selectedCategory: StateFlow<BadgeSystem.BadgeCategory?> = _selectedCategory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentHouseholdId: String = ""
    private var currentUserId: String = ""

    /**
     * Loads achievements for a user.
     *
     * @param householdId Household ID
     * @param userId User ID
     */
    fun loadAchievements(householdId: String, userId: String) {
        currentHouseholdId = householdId
        currentUserId = userId

        viewModelScope.launch {
            _isLoading.value = true
            try {
                badgeSystem.ensureCurrentSeasonBadgeCatalog(userId, householdId)
                // Load user
                val user = userRepository.getUserById(userId)
                _currentUser.value = user

                // Load badges
                val earned = badgeRepository.getEarnedBadges(userId, householdId)
                val locked = badgeRepository.getLockedBadges(userId, householdId)
                val normalized = normalizeBadges(earned + locked)

                _earnedBadges.value = normalized.filter { !it.isLocked }
                _lockedBadges.value = normalized.filter { it.isLocked }

                // Load streak data
                val streak = streakRepository.getStreak(userId, householdId)
                _currentStreak.value = streak?.currentCount ?: 0
                _bestStreak.value = streak?.bestCount ?: 0
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filters badges by category.
     *
     * @param category Badge category to filter by, or null to show all
     */
    fun selectCategory(category: BadgeSystem.BadgeCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Gets earned badges filtered by selected category.
     *
     * @return List of earned badges for selected category
     */
    fun getFilteredEarnedBadges(): List<Badge> {
        val category = _selectedCategory.value ?: return _earnedBadges.value

        return _earnedBadges.value.filter { badge ->
            val milestone = badgeSystem.getBadgeMilestone(badge.badgeType)
            milestone?.category == category
        }
    }

    /**
     * Gets locked badges filtered by selected category.
     *
     * @return List of locked badges for selected category
     */
    fun getFilteredLockedBadges(): List<Badge> {
        val category = _selectedCategory.value ?: return _lockedBadges.value

        return _lockedBadges.value.filter { badge ->
            val milestone = badgeSystem.getBadgeMilestone(badge.badgeType)
            milestone?.category == category
        }
    }

    /**
     * Gets all badge categories.
     *
     * @return List of all badge categories
     */
    fun getAllCategories(): List<BadgeSystem.BadgeCategory> {
        return BadgeSystem.BadgeCategory.values().toList()
    }

    fun getBadgeCategory(badgeType: String): BadgeSystem.BadgeCategory? {
        return badgeSystem.getBadgeMilestone(badgeType)?.category
    }

    /**
     * Refreshes achievements from repository.
     */
    fun refreshAchievements() {
        val householdId = currentHouseholdId.ifEmpty { setupManager.getHouseholdId().orEmpty() }
        val userId = currentUserId.ifEmpty { setupManager.getAssignedMemberId().orEmpty() }
        if (householdId.isNotEmpty() && userId.isNotEmpty()) {
            loadAchievements(householdId, userId)
        }
    }

    private fun normalizeBadges(badges: List<Badge>): List<Badge> {
        val badgeOrder = badgeSystem.getAllBadgeMilestones()
            .mapIndexed { index, milestone -> milestone.badgeType to index }
            .toMap()

        return badges
            .groupBy { badge -> badge.badgeType to badge.seasonYear }
            .values
            .map { badgeGroup ->
                badgeGroup
                    .sortedWith(
                        compareByDescending<Badge> { !it.isLocked }
                            .thenByDescending { it.earnedAt }
                            .thenByDescending { it.progress ?: -1 }
                    )
                    .first()
            }
            .sortedWith(
                compareBy<Badge> { badgeOrder[it.badgeType] ?: Int.MAX_VALUE }
                    .thenByDescending { it.earnedAt }
                    .thenBy { it.name }
            )
    }
}
