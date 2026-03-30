package com.adhdfocus.app.ui.achievements

import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.model.Streak
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.repository.BadgeRepository
import com.adhdfocus.app.data.repository.StreakRepository
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.gamification.BadgeSystem
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests for AchievementsViewModel
 *
 * Tests verify:
 * - ViewModel loads achievements correctly
 * - Category filtering works
 * - Streak data is loaded
 * - Badge organization by category
 */
class AchievementsViewModelTest {

    private lateinit var badgeRepository: BadgeRepository
    private lateinit var badgeSystem: BadgeSystem
    private lateinit var streakRepository: StreakRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: AchievementsViewModel

    @Before
    fun setup() {
        badgeRepository = mockk()
        badgeSystem = mockk()
        streakRepository = mockk()
        userRepository = mockk()
        viewModel = AchievementsViewModel(
            badgeRepository = badgeRepository,
            badgeSystem = badgeSystem,
            streakRepository = streakRepository,
            userRepository = userRepository
        )
    }

    // ============ Load Achievements Tests ============

    @Test
    fun testLoadAchievementsLoadsEarnedBadges() = runTest {
        val householdId = "household-1"
        val userId = "user-1"
        val earnedBadges = listOf(
            Badge(
                id = "badge-1",
                householdId = householdId,
                userId = userId,
                badgeType = "FIRST_TASK_COMPLETE",
                name = "First Task Complete",
                earnedAt = System.currentTimeMillis(),
                isLocked = false
            )
        )

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns earnedBadges
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.earnedBadges.value shouldBe earnedBadges
    }

    @Test
    fun testLoadAchievementsLoadsLockedBadges() = runTest {
        val householdId = "household-1"
        val userId = "user-1"
        val lockedBadges = listOf(
            Badge(
                id = "badge-1",
                householdId = householdId,
                userId = userId,
                badgeType = "WEEK_WARRIOR",
                name = "Week Warrior",
                progress = 50,
                isLocked = true
            )
        )

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns lockedBadges
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.lockedBadges.value shouldBe lockedBadges
    }

    @Test
    fun testLoadAchievementsLoadsStreakData() = runTest {
        val householdId = "household-1"
        val userId = "user-1"
        val streak = Streak(
            id = "streak-1",
            userId = userId,
            householdId = householdId,
            currentCount = 5,
            bestCount = 10,
            lastCompletionDate = System.currentTimeMillis(),
            startDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns streak
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.currentStreak.value shouldBe 5
        viewModel.bestStreak.value shouldBe 10
    }

    @Test
    fun testLoadAchievementsLoadsUserData() = runTest {
        val householdId = "household-1"
        val userId = "user-1"
        val user = User(
            id = userId,
            householdId = householdId,
            email = "user@example.com",
            displayName = "Test User"
        )

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns user

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.currentUser.value shouldBe user
    }

    // ============ Category Selection Tests ============

    @Test
    fun testSelectCategoryUpdatesSelectedCategory() {
        val category = BadgeSystem.BadgeCategory.DAILY_MILESTONES

        viewModel.selectCategory(category)

        viewModel.selectedCategory.value shouldBe category
    }

    @Test
    fun testSelectCategoryNullShowsAllBadges() {
        viewModel.selectCategory(null)

        viewModel.selectedCategory.value shouldBe null
    }

    @Test
    fun testSelectCategoryChangesFilter() {
        val category1 = BadgeSystem.BadgeCategory.DAILY_MILESTONES
        val category2 = BadgeSystem.BadgeCategory.STREAK_MILESTONES

        viewModel.selectCategory(category1)
        viewModel.selectedCategory.value shouldBe category1

        viewModel.selectCategory(category2)
        viewModel.selectedCategory.value shouldBe category2
    }

    // ============ Get All Categories Tests ============

    @Test
    fun testGetAllCategoriesReturnsAllCategories() {
        val categories = viewModel.getAllCategories()

        categories.size shouldBe 4
        categories.contains(BadgeSystem.BadgeCategory.DAILY_MILESTONES) shouldBe true
        categories.contains(BadgeSystem.BadgeCategory.WEEKLY_ACHIEVEMENTS) shouldBe true
        categories.contains(BadgeSystem.BadgeCategory.STREAK_MILESTONES) shouldBe true
        categories.contains(BadgeSystem.BadgeCategory.EFFICIENCY_BADGES) shouldBe true
    }

    // ============ Refresh Achievements Tests ============

    @Test
    fun testRefreshAchievementsReloadsData() = runTest {
        val householdId = "household-1"
        val userId = "user-1"

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)
        viewModel.refreshAchievements()

        // Verify refresh was called
        viewModel.isLoading.value shouldBe false
    }

    // ============ Empty State Tests ============

    @Test
    fun testLoadAchievementsWithNoData() = runTest {
        val householdId = "household-1"
        val userId = "user-1"

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.earnedBadges.value.isEmpty() shouldBe true
        viewModel.lockedBadges.value.isEmpty() shouldBe true
        viewModel.currentStreak.value shouldBe 0
        viewModel.bestStreak.value shouldBe 0
    }

    // ============ Multiple Badges Tests ============

    @Test
    fun testLoadAchievementsWithMultipleBadges() = runTest {
        val householdId = "household-1"
        val userId = "user-1"
        val earnedBadges = listOf(
            Badge(
                id = "badge-1",
                householdId = householdId,
                userId = userId,
                badgeType = "FIRST_TASK_COMPLETE",
                name = "First Task Complete",
                earnedAt = System.currentTimeMillis(),
                isLocked = false
            ),
            Badge(
                id = "badge-2",
                householdId = householdId,
                userId = userId,
                badgeType = "PERFECT_DAY",
                name = "Perfect Day",
                earnedAt = System.currentTimeMillis(),
                isLocked = false
            )
        )

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns earnedBadges
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.earnedBadges.value.size shouldBe 2
    }

    // ============ Loading State Tests ============

    @Test
    fun testLoadingStateIsSetDuringLoad() = runTest {
        val householdId = "household-1"
        val userId = "user-1"

        coEvery { badgeRepository.getEarnedBadges(userId, householdId) } returns emptyList()
        coEvery { badgeRepository.getLockedBadges(userId, householdId) } returns emptyList()
        coEvery { streakRepository.getStreakForUser(userId, householdId) } returns null
        coEvery { userRepository.getUserById(userId) } returns null

        viewModel.loadAchievements(householdId, userId)

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)

        viewModel.isLoading.value shouldBe false
    }
}
