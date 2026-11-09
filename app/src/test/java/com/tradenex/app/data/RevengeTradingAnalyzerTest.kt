package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevengeTradingAnalyzerTest {
    private fun trade(
        id: Long, pnl: Double, minute: Long, risk: Double? = 100.0,
        lot: Double = 0.1, tags: List<String> = emptyList(),
        followed: Boolean? = true, account: String = "A", emotion: String = "آرام",
    ) = Trade(
        id = id, symbol = "EURUSD", buy = true, lot = lot, pnl = pnl,
        strategy = "ICT", tags = tags, epochDay = 20_000,
        emotion = emotion, accountId = account, openedAtMillis = minute * 60_000L,
        plannedRisk = risk, strategyFollowed = followed,
    )

    @Test fun plannedQuickReentryIsNotFlaggedWithoutBehavioralEvidence() {
        val rows = listOf(trade(1, -20.0, 100), trade(2, 10.0, 105))
        assertTrue(RevengeTradingAnalyzer.analyze(rows).isEmpty())
    }

    @Test fun quickReentryAndRiskIncreaseCreateStrongEvidenceEpisode() {
        val rows = listOf(trade(1, -20.0, 100), trade(2, -30.0, 105, risk = 140.0, lot = 0.2))
        val episode = RevengeTradingAnalyzer.analyze(rows).single()
        assertEquals(2, episode.tradeIds.size)
        assertEquals(1L, episode.previousLossTradeId)
        assertEquals(2L, episode.triggerTradeId)
        assertEquals(RevengeClass.STRONG_EVIDENCE, episode.evidence.classification)
        assertTrue(episode.evidence.score >= 45)
    }

    @Test fun explicitRevengeTagIsShownAsUserTaggedNotPsychologicalDiagnosis() {
        val rows = listOf(trade(1, -20.0, 100), trade(2, 5.0, 180, tags = listOf("#revenge")))
        val evidence = RevengeTradingAnalyzer.analyze(rows).single().evidence
        assertEquals(RevengeClass.USER_TAGGED, evidence.classification)
        assertTrue(evidence.explicitRevengeTag)
    }

    @Test fun accountsAreIsolatedAndMissingTimestampsDoNotCreateRapidSignal() {
        val rows = listOf(
            trade(1, -20.0, 0, account = "A"),
            trade(2, 10.0, 0, tags = listOf("#revenge"), account = "B"),
        )
        val episodes = RevengeTradingAnalyzer.analyze(rows)
        assertTrue(episodes.isEmpty())
    }

    @Test fun missingRiskIsNotInferred() {
        val rows = listOf(trade(1, -20.0, 100, risk = null), trade(2, 10.0, 105, risk = null, followed = false))
        val evidence = RevengeTradingAnalyzer.analyze(rows).single().evidence
        assertFalse(evidence.riskIncreased)
        assertTrue(evidence.explicitPlanViolation)
    }
}
