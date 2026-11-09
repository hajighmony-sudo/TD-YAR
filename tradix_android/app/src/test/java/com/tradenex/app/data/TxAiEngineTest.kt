package com.tradenex.app.data

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TxAiEngineTest {
    private val account = TradingAccount("a", "Test", "", "REAL", "USD", 10_000.0)

    private fun trade(
        id: Long,
        pnl: Double,
        strategy: String = "ICT",
        tags: List<String> = listOf("#discipline"),
    ) = Trade(
        id = id,
        symbol = "EURUSD",
        buy = true,
        lot = 0.1,
        pnl = pnl,
        strategy = strategy,
        tags = tags,
        epochDay = LocalDate.of(2026, 11, 2).plusDays(id - 1).toEpochDay(),
        emotion = "آرام",
        accountId = "a",
        openedAtMillis = LocalDate.of(2026, 11, 2).plusDays(id - 1)
            .atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        entryPrice = 1.1,
        exitPrice = 1.101,
        stopLossPrice = 1.099,
        plannedRisk = 100.0,
        timeframe = "M15",
        marketCondition = "رونددار",
    )

    @Test
    fun counterfactualExcludesTradesFlaggedOutsideStrategy() {
        val rows = (1L..6L).map { id ->
            if (id == 6L) trade(id, -80.0, strategy = "", tags = listOf("#fomo"))
            else trade(id, 20.0)
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(6, report.totalTrades)
        assertEquals(1, report.outsideStrategyCount)
        assertEquals(100.0, report.withoutOutsideStrategyNet, 0.001)
        assertEquals(-80.0, report.outsideStrategyNet, 0.001)
        assertEquals(80.0, report.withoutOutsideStrategyDelta, 0.001)
    }

    @Test
    fun disciplineScoreIsWithheldWhenBehaviorTagCoverageIsLow() {
        val rows = (1L..6L).map { id ->
            trade(id, 10.0, tags = if (id <= 3) listOf("#discipline") else emptyList())
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(null, report.scores.discipline)
        assertEquals(6, report.monthlyProgress.size)
    }

    @Test
    fun missingStrategyNameAloneIsNotClassifiedAsOffPlan() {
        val rows = (1L..5L).map { id -> trade(id, 15.0, strategy = "", tags = listOf("#discipline")) }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(0, report.outsideStrategyCount)
        assertEquals(0.0, report.withoutOutsideStrategyDelta, 0.001)
    }

    @Test
    fun strategyScoreUsesExplicitComplianceAndOffPlanCounterfactual() {
        val rows = (1L..6L).map { id ->
            trade(id, if (id <= 4) 25.0 else -30.0).copy(strategyFollowed = id <= 4)
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(67, report.scores.strategy)
        assertEquals(2, report.outsideStrategyCount)
        assertEquals(100.0, report.withoutOutsideStrategyNet, 0.001)
        assertEquals(60.0, report.withoutOutsideStrategyDelta, 0.001)
    }

    @Test
    fun strategyScoreIsWithheldWithoutEnoughExplicitComplianceData() {
        val rows = (1L..6L).map { id ->
            trade(id, 10.0).copy(strategyFollowed = if (id <= 3) true else null)
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(null, report.scores.strategy)
    }

    @Test
    fun reportIncludesEmotionalAndSymbolBreakdowns() {
        val report = TxAiEngine.analyze((1L..6L).map { trade(it, 12.0) }, account, 1.0)
        assertEquals("آرام", report.emotions.single().label)
        assertEquals("EURUSD", report.symbols.single().label)
    }

    @Test
    fun advancedAnalyticsCompareStrategyVersionsTimeBucketsHoldTimeRAndStreaks() {
        val base = LocalDate.of(2026, 11, 2).atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val rows = listOf(
            trade(1, 10.0).copy(openedAtMillis = base + 60_000, closedAtMillis = base + 6 * 60_000, plannedRisk = 5.0, strategyVersion = 1),
            trade(2, 20.0).copy(openedAtMillis = base + 10 * 60_000, closedAtMillis = base + 20 * 60_000, plannedRisk = 10.0, strategyVersion = 1),
            trade(3, -10.0).copy(openedAtMillis = base + 16 * 60_000, closedAtMillis = base + 31 * 60_000, plannedRisk = 5.0, strategyVersion = 1),
            trade(4, -20.0).copy(openedAtMillis = base + 17 * 60_000, closedAtMillis = base + 37 * 60_000, plannedRisk = 10.0, strategyVersion = 2),
            trade(5, -5.0).copy(openedAtMillis = base + 18 * 60_000, closedAtMillis = base + 43 * 60_000, plannedRisk = 5.0, strategyVersion = 2),
        )
        val report = TxAiEngine.analyze(rows, account, 1.0, ZoneOffset.UTC)

        assertEquals(2, report.strategyVersions.size)
        assertTrue(report.strategyVersions.any { it.label == "ICT · v1" && it.trades == 3 })
        assertTrue(report.strategyVersions.any { it.label == "ICT · v2" && it.trades == 2 })
        assertTrue(report.timeOfDay.any { it.label == "09:00–09:15" && it.trades == 2 })
        assertTrue(report.timeOfDay.any { it.label == "09:15–09:30" && it.trades == 3 })
        assertEquals(15.0, report.averageHoldMinutes!!, 0.001)
        assertEquals(15.0, report.medianHoldMinutes!!, 0.001)
        assertEquals(-0.2, report.averageRMultiple!!, 0.001)
        assertEquals(2, report.maxWinStreak)
        assertEquals(3, report.maxLossStreak)
    }

    @Test
    fun reportGroupsContextAndComputesHalfRisk() {
        val report = TxAiEngine.analyze((1L..6L).map { trade(it, 40.0) }, account, 1.0)
        assertEquals("M15", report.timeframes.single().label)
        assertEquals("رونددار", report.marketConditions.single().label)
        assertEquals(120.0, report.halfRiskNet, 0.001)
        assertEquals(6, report.weeks.sumOf { it.trades })
        assertTrue(report.sessions.any { it.label == "London" })
    }
}
