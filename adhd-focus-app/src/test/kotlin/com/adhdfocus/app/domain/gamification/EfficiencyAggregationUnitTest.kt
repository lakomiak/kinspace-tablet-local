package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.model.EfficiencyMetric
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Unit tests for EfficiencyAggregationManager.
 *
 * Tests specific scenarios and edge cases for efficiency aggregation.
 */
class EfficiencyAggregationUnitTest : FunSpec({

    val efficiencyMetricDao = mock<EfficiencyMetricDao>()
    val efficiencyCalculator = EfficiencyCalculator()
    val manager = EfficiencyAggregationManager(efficiencyMetricDao, efficiencyCalculator)

    test("Daily average efficiency with single task") {
        val userId = "user1"
        val date = LocalDate.now()
        val metric = EfficiencyMetric(
            id = UUID.randomUUID().toString(),
            taskId = UUID.randomUUID().toString(),
            userId = userId,
            householdId = "household1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 30,
            efficiencyPercentage = 100f,
            completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
            .thenReturn(listOf(metric))

        runBlocking {
            val result = manager.calculateDailyAverageEfficiency(userId, date)
            result shouldBeExactly 100f
        }
    }

    test("Daily average efficiency with multiple tasks") {
        val userId = "user1"
        val date = LocalDate.now()
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 120f,
                completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 80f,
                completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateDailyAverageEfficiency(userId, date)
            result shouldBeExactly 100f // (100 + 120 + 80) / 3 = 100
        }
    }

    test("Daily average efficiency with no tasks") {
        val userId = "user1"
        val date = LocalDate.now()

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
            .thenReturn(emptyList())

        runBlocking {
            val result = manager.calculateDailyAverageEfficiency(userId, date)
            result shouldBeExactly 100f
        }
    }

    test("Today's average efficiency") {
        val userId = "user1"
        val today = LocalDate.now()
        val metric = EfficiencyMetric(
            id = UUID.randomUUID().toString(),
            taskId = UUID.randomUUID().toString(),
            userId = userId,
            householdId = "household1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 30,
            efficiencyPercentage = 110f,
            completedAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )

        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
            .thenReturn(listOf(metric))

        runBlocking {
            val result = manager.calculateTodayAverageEfficiency(userId)
            result shouldBeExactly 110f
        }
    }

    test("Weekly average efficiency with tasks across multiple days") {
        val userId = "user1"
        val date = LocalDate.now()
        val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())

        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = startOfWeek.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 90f,
                completedAt = startOfWeek.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val endOfWeek = startOfWeek.plusDays(7)
        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateWeeklyAverageEfficiency(userId, date)
            result shouldBeExactly 100f // (100 + 110 + 90) / 3 = 100
        }
    }

    test("Weekly average efficiency with no tasks") {
        val userId = "user1"
        val date = LocalDate.now()
        val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(7)

        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(emptyList())

        runBlocking {
            val result = manager.calculateWeeklyAverageEfficiency(userId, date)
            result shouldBeExactly 100f
        }
    }

    test("Current week average efficiency") {
        val userId = "user1"
        val today = LocalDate.now()
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())

        val metric = EfficiencyMetric(
            id = UUID.randomUUID().toString(),
            taskId = UUID.randomUUID().toString(),
            userId = userId,
            householdId = "household1",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 30,
            efficiencyPercentage = 105f,
            completedAt = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )

        val endOfWeek = startOfWeek.plusDays(7)
        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(listOf(metric))

        runBlocking {
            val result = manager.calculateCurrentWeekAverageEfficiency(userId)
            result shouldBeExactly 105f
        }
    }

    test("Overall average efficiency with multiple tasks") {
        val userId = "user1"
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = Instant.now()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 120f,
                completedAt = Instant.now()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 80f,
                completedAt = Instant.now()
            )
        )

        whenever(efficiencyMetricDao.getMetricsByUser(userId))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateOverallAverageEfficiency(userId)
            result shouldBeExactly 100f // (100 + 120 + 80) / 3 = 100
        }
    }

    test("Overall average efficiency with no tasks") {
        val userId = "user1"

        whenever(efficiencyMetricDao.getMetricsByUser(userId))
            .thenReturn(emptyList())

        runBlocking {
            val result = manager.calculateOverallAverageEfficiency(userId)
            result shouldBeExactly 100f
        }
    }

    test("Household overall average efficiency") {
        val householdId = "household1"
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user1",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = Instant.now()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user2",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = Instant.now()
            )
        )

        whenever(efficiencyMetricDao.getMetricsByHousehold(householdId))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateHouseholdOverallAverageEfficiency(householdId)
            result shouldBeExactly 105f // (100 + 110) / 2 = 105
        }
    }

    test("Household daily average efficiency") {
        val householdId = "household1"
        val date = LocalDate.now()
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user1",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user2",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 120f,
                completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getHouseholdMetricsInDateRange(householdId, startOfDay, endOfDay))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateHouseholdDailyAverageEfficiency(householdId, date)
            result shouldBeExactly 110f // (100 + 120) / 2 = 110
        }
    }

    test("Household weekly average efficiency") {
        val householdId = "household1"
        val date = LocalDate.now()
        val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(7)

        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user1",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = "user2",
                householdId = householdId,
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = startOfWeek.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getHouseholdMetricsInDateRange(householdId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateHouseholdWeeklyAverageEfficiency(householdId, date)
            result shouldBeExactly 105f // (100 + 110) / 2 = 105
        }
    }

    test("Average efficiency for last N days") {
        val userId = "user1"
        val days = 7
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())
        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateAverageEfficiencyForLastDays(userId, days)
            result shouldBeExactly 105f // (100 + 110) / 2 = 105
        }
    }

    test("Average efficiency for last N weeks") {
        val userId = "user1"
        val weeks = 4
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = LocalDate.now().minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 120f,
                completedAt = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val endDate = LocalDate.now()
        val startDate = endDate.minusWeeks(weeks.toLong())
        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.calculateAverageEfficiencyForLastWeeks(userId, weeks)
            result shouldBeExactly 110f // (100 + 120) / 2 = 110
        }
    }

    test("Efficiency stats with valid data") {
        val userId = "user1"
        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = Instant.now()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 120f,
                completedAt = Instant.now()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 80f,
                completedAt = Instant.now()
            )
        )

        whenever(efficiencyMetricDao.getMetricsByUser(userId))
            .thenReturn(metrics)

        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
            .thenReturn(emptyList())

        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(7)
        val startWeekInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endWeekInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startWeekInstant, endWeekInstant))
            .thenReturn(emptyList())

        runBlocking {
            val stats = manager.getEfficiencyStats(userId)

            stats.dailyAverage shouldBeExactly 100f
            stats.weeklyAverage shouldBeExactly 100f
            stats.overallAverage shouldBeExactly 100f
            stats.bestEfficiency shouldBeExactly 120f
            stats.worstEfficiency shouldBeExactly 80f
            stats.totalTasksCompleted shouldBe 3
        }
    }

    test("Daily efficiency breakdown for date range") {
        val userId = "user1"
        val startDate = LocalDate.now().minusDays(2)
        val endDate = LocalDate.now()

        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = startDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 90f,
                completedAt = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.getDailyEfficiencyBreakdown(userId, startDate, endDate)

            result.size shouldBe 3
            result[startDate] shouldBeExactly 100f
            result[startDate.plusDays(1)] shouldBeExactly 110f
            result[endDate] shouldBeExactly 90f
        }
    }

    test("Weekly efficiency breakdown for date range") {
        val userId = "user1"
        val startDate = LocalDate.now().minusWeeks(1)
        val endDate = LocalDate.now()

        val metrics = listOf(
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 100f,
                completedAt = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            EfficiencyMetric(
                id = UUID.randomUUID().toString(),
                taskId = UUID.randomUUID().toString(),
                userId = userId,
                householdId = "household1",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30,
                efficiencyPercentage = 110f,
                completedAt = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        )

        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
            .thenReturn(metrics)

        runBlocking {
            val result = manager.getWeeklyEfficiencyBreakdown(userId, startDate, endDate)

            result.size shouldBe 2
        }
    }
})
