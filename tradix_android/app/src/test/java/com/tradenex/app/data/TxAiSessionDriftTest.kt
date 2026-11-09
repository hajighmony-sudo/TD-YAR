package com.tradenex.app.data

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxAiSessionDriftTest {
    private fun trade(id: Long, openedAt: String, strategyVersion: Int = 1) = Trade(
        id = id,
        symbol = "EURUSD",
        buy = true,
        lot = 0.1,
        pnl = 10.0,
        strategy = "ICT",
        tags = emptyList(),
        epochDay = 20_000L,
        emotion = "آرام",
        accountId = "A",
        openedAtMillis = Instant.parse(openedAt).toEpochMilli(),
        strategyVersion = strategyVersion,
    )

    @Test
    fun flagsTradeOutsideExplicitlyAllowedLondonSession() {
        val tokyoTrade = trade(1, "2026-11-02T07:00:00Z")
        val strategy = Strategy(1, "ICT", listOf("فقط سشن لندن"), true, version = 1)

        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(tokyoTrade), listOf(strategy))

        assertTrue(patterns.any { it.type == TxAiBehaviorPatternType.SESSION_DRIFT })
    }

    @Test
    fun doesNotFlagTradeInsideExplicitlyAllowedSession() {
        val londonTrade = trade(1, "2026-11-02T10:00:00Z")
        val strategy = Strategy(1, "ICT", listOf("فقط سشن لندن"), true, version = 1)

        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(londonTrade), listOf(strategy))

        assertFalse(patterns.any { it.type == TxAiBehaviorPatternType.SESSION_DRIFT })
    }

    @Test
    fun overlappingSessionsDoNotCauseFalseDriftWhenOneIsAllowed() {
        val overlapTrade = trade(1, "2026-11-02T13:00:00Z")
        val strategy = Strategy(1, "ICT", listOf("فقط سشن نیویورک"), true, version = 1)

        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(overlapTrade), listOf(strategy))

        assertFalse(patterns.any { it.type == TxAiBehaviorPatternType.SESSION_DRIFT })
    }

    @Test
    fun usesRecordedHistoricalStrategyRevisionInsteadOfCurrentRules() {
        val londonTrade = trade(1, "2026-11-02T10:00:00Z", strategyVersion = 1)
        val current = Strategy(1, "ICT", listOf("فقط سشن نیویورک"), true, version = 2)
        val revision = StrategyRevision(
            id = 11L,
            strategyId = 1L,
            version = 1,
            name = "ICT",
            rules = listOf("فقط سشن لندن"),
            createdAt = 1L,
        )

        val patterns = TxAiBehavioralPatternAnalyzer.analyze(
            listOf(londonTrade), listOf(current), listOf(revision),
        )

        assertFalse(patterns.any { it.type == TxAiBehaviorPatternType.SESSION_DRIFT })
    }

    @Test
    fun withholdsDriftWhenTheTradeVersionHasNoMatchingRules() {
        val londonTrade = trade(1, "2026-11-02T10:00:00Z", strategyVersion = 1)
        val current = Strategy(1, "ICT", listOf("فقط سشن توکیو"), true, version = 2)

        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(londonTrade), listOf(current))

        assertFalse(patterns.any { it.type == TxAiBehaviorPatternType.SESSION_DRIFT })
    }
}
