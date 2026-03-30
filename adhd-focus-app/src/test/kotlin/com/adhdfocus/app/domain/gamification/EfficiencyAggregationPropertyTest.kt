package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.model.EfficiencyMetric
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Property-based tests for EfficiencyAggregationManager.
 *
 * **Validates: Requirement 7.9 - Efficiency Aggregation**
 *
 * Tests verify that efficiency aggregation correctly calculates average efficiency
 * across different time periods (daily, weekly, overall) for various task configurations.
 */
class EfficiencyAggregationPropertyTest : FunSpec({

    val efficiencyMetricDao = mock<EfficiencyMetricDao>()
    val efficiencyCalculator = EfficiencyCalculator()
    val manager = EfficiencyAggregationManager(efficiencyMetricDao, efficiencyCalculator)

    test("Property 29: Daily average efficiency is correct for any set of tasks completed in a day") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..10),
            Arb.string(1..20),
            Arb.localDate()
        ) { efficiencies, userId, date ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateDailyAverageEfficiency(userId, date)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Daily average efficiency returns 100f when no tasks completed") {
        checkAll(
            Arb.string(1..20),
            Arb.localDate()
        ) { userId, date ->
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
                .thenReturn(emptyList())

            runBlocking {
                val result = manager.calculateDailyAverageEfficiency(userId, date)
                result shouldBe 100f
            }
        }
    }

    test("Property 29: Weekly average efficiency is correct for any set of tasks in a week") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..20),
            Arb.string(1..20),
            Arb.localDate()
        ) { efficiencies, userId, date ->
            val metrics = efficiencies.mapIndexed { index, efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = date.plusDays(index.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
            val endOfWeek = startOfWeek.plusDays(7)
            val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateWeeklyAverageEfficiency(userId, date)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Weekly average efficiency returns 100f when no tasks completed") {
        checkAll(
            Arb.string(1..20),
            Arb.localDate()
        ) { userId, date ->
            val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
            val endOfWeek = startOfWeek.plusDays(7)
            val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(emptyList())

            runBlocking {
                val result = manager.calculateWeeklyAverageEfficiency(userId, date)
                result shouldBe 100f
            }
        }
    }

    test("Property 29: Overall average efficiency is correct for all tasks ever completed") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..50),
            Arb.string(1..20)
        ) { efficiencies, userId ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = Instant.now()
                )
            }

            whenever(efficiencyMetricDao.getMetricsByUser(userId))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateOverallAverageEfficiency(userId)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Overall average efficiency returns 100f when no tasks completed") {
        checkAll(Arb.string(1..20)) { userId ->
            whenever(efficiencyMetricDao.getMetricsByUser(userId))
                .thenReturn(emptyList())

            runBlocking {
                val result = manager.calculateOverallAverageEfficiency(userId)
                result shouldBe 100f
            }
        }
    }

    test("Property 29: Average efficiency is always between 0 and 200 for valid inputs") {
        checkAll(
            Arb.list(Arb.float(0f..200f), 1..20),
            Arb.string(1..20)
        ) { efficiencies, userId ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = Instant.now()
                )
            }

            whenever(efficiencyMetricDao.getMetricsByUser(userId))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateOverallAverageEfficiency(userId)
                result shouldBeGreaterThanOrEqual 0f
                result shouldBeLessThanOrEqual 200f
            }
        }
    }

    test("Property 29: Household overall average efficiency is correct for all household tasks") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..50),
            Arb.string(1..20)
        ) { efficiencies, householdId ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = UUID.randomUUID().toString(),
                    householdId = householdId,
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = Instant.now()
                )
            }

            whenever(efficiencyMetricDao.getMetricsByHousehold(householdId))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateHouseholdOverallAverageEfficiency(householdId)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Average efficiency for last N days is correct") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..30),
            Arb.string(1..20),
            Arb.int(1..30)
        ) { efficiencies, userId, days ->
            val metrics = efficiencies.mapIndexed { index, efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = LocalDate.now().minusDays(index.toLong())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(days.toLong())
            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateAverageEfficiencyForLastDays(userId, days)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Average efficiency for last N weeks is correct") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..50),
            Arb.string(1..20),
            Arb.int(1..12)
        ) { efficiencies, userId, weeks ->
            val metrics = efficiencies.mapIndexed { index, efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = LocalDate.now().minusWeeks(index.toLong())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(weeks.toLong())
            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateAverageEfficiencyForLastWeeks(userId, weeks)
                val expected = efficiencies.average().toFloat()

                result shouldBe expected
            }
        }
    }

    test("Property 29: Daily efficiency breakdown is correct for date range") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..30),
            Arb.string(1..20),
            Arb.localDate()
        ) { efficiencies, userId, startDate ->
            val endDate = startDate.plusDays(7)
            val metrics = efficiencies.mapIndexed { index, efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = startDate.plusDays((index % 7).toLong())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.getDailyEfficiencyBreakdown(userId, startDate, endDate)

                // Verify all values are valid efficiencies
                result.values.forEach { efficiency ->
                    efficiency shouldBeGreaterThanOrEqual 0f
                    efficiency shouldBeLessThanOrEqual 200f
                }
            }
        }
    }

    test("Property 29: Weekly efficiency breakdown is correct for date range") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..50),
            Arb.string(1..20),
            Arb.localDate()
        ) { efficiencies, userId, startDate ->
            val endDate = startDate.plusWeeks(4)
            val metrics = efficiencies.mapIndexed { index, efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = startDate.plusDays((index % 28).toLong())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            }

            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startInstant, endInstant))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.getWeeklyEfficiencyBreakdown(userId, startDate, endDate)

                // Verify all values are valid efficiencies
                result.values.forEach { efficiency ->
                    efficiency shouldBeGreaterThanOrEqual 0f
                    efficiency shouldBeLessThanOrEqual 200f
                }
            }
        }
    }

    test("Property 29: Efficiency stats contain valid values for any user") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 1..50),
            Arb.string(1..20)
        ) { efficiencies, userId ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = Instant.now()
                )
            }

            whenever(efficiencyMetricDao.getMetricsByUser(userId))
                .thenReturn(metrics)

            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startOfDay, endOfDay))
                .thenReturn(metrics.filter { it.completedAt >= startOfDay && it.completedAt < endOfDay })

            val startOfWeek = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
            val endOfWeek = startOfWeek.plusDays(7)
            val startWeekInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endWeekInstant = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            whenever(efficiencyMetricDao.getMetricsInDateRange(userId, startWeekInstant, endWeekInstant))
                .thenReturn(metrics.filter { it.completedAt >= startWeekInstant && it.completedAt < endWeekInstant })

            runBlocking {
                val stats = manager.getEfficiencyStats(userId)

                stats.dailyAverage shouldBeGreaterThanOrEqual 0f
                stats.weeklyAverage shouldBeGreaterThanOrEqual 0f
                stats.overallAverage shouldBeGreaterThanOrEqual 0f
                stats.bestEfficiency shouldBeGreaterThanOrEqual stats.overallAverage
                stats.worstEfficiency shouldBeLessThanOrEqual stats.overallAverage
                stats.totalTasksCompleted shouldBe metrics.size
            }
        }
    }

    test("Property 29: Efficiency aggregation preserves all efficiency values in average") {
        checkAll(
            Arb.list(Arb.float(50f..150f), 2..20),
            Arb.string(1..20)
        ) { efficiencies, userId ->
            val metrics = efficiencies.map { efficiency ->
                EfficiencyMetric(
                    id = UUID.randomUUID().toString(),
                    taskId = UUID.randomUUID().toString(),
                    userId = userId,
                    householdId = "household1",
                    estimatedDurationMinutes = 30,
                    actualDurationMinutes = 30,
                    efficiencyPercentage = efficiency,
                    completedAt = Instant.now()
                )
            }

            whenever(efficiencyMetricDao.getMetricsByUser(userId))
                .thenReturn(metrics)

            runBlocking {
                val result = manager.calculateOverallAverageEfficiency(userId)
                val minEfficiency = efficiencies.minOrNull() ?: 100f
                val maxEfficiency = efficiencies.maxOrNull() ?: 100f

                // Average should be between min and max
                result shouldBeGreaterThanOrEqual minEfficiency
                result shouldBeLessThanOrEqual maxEfficiency
            }
        }
    }
})
