package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxAiBehavioralPatternAnalyzerTest {
    private fun trade(
        id: Long,
        pnl: Double,
        opened: Long,
        closed: Long = opened + 5 * 60_000L,
        lot: Double = 0.1,
        buy: Boolean = true,
        entry: Double? = null,
        exit: Double? = null,
        target: Double? = null,
        account: String = "A",
    ) = Trade(
        id = id,
        symbol = "EURUSD",
        buy = buy,
        lot = lot,
        pnl = pnl,
        strategy = "ICT",
        tags = emptyList(),
        epochDay = 20_000L,
        emotion = "",
        accountId = account,
        openedAtMillis = opened,
        closedAtMillis = closed,
        entryPrice = entry,
        exitPrice = exit,
        takeProfitPrice = target,
    )

    @Test
    fun activityClusterAfterWinRequiresThreeTimestampedTradesWithinTwoHours() {
        val winner = trade(1, 20.0, 1_000_000L, closed = 1_300_000L)
        val next = (2L..4L).map { trade(it, -5.0, 1_300_000L + (it - 1) * 10 * 60_000L) }
        val episodes = TxAiBehavioralPatternAnalyzer.analyze(listOf(winner) + next)
        val episode = episodes.single { it.type == TxAiBehaviorPatternType.OVERTRADING_AFTER_WIN }
        assertEquals(listOf(1L, 2L, 3L, 4L), episode.linkedTradeIds)
        assertTrue(episode.reasons.any { it.contains("3 معامله") })
    }

    @Test
    fun activityClusterIsNotInferredWhenTimestampsAreMissingOrOutsideWindow() {
        val winner = trade(1, 20.0, 0L, closed = 0L)
        val next = (2L..4L).map { trade(it, -5.0, 0L, closed = 0L) }
        assertFalse(
            TxAiBehavioralPatternAnalyzer.analyze(listOf(winner) + next)
                .any { it.type == TxAiBehaviorPatternType.OVERTRADING_AFTER_WIN },
        )
    }

    @Test
    fun sizeIncreaseAfterWinIsAnObservablePattern() {
        val previous = trade(1, 20.0, 1_000_000L, closed = 1_100_000L, lot = 0.1)
        val current = trade(2, -10.0, 1_200_000L, lot = 0.14)
        val episode = TxAiBehavioralPatternAnalyzer.analyze(listOf(previous, current))
            .single { it.type == TxAiBehaviorPatternType.SIZE_UP_AFTER_WIN }
        assertEquals(listOf(1L, 2L), episode.linkedTradeIds)
        assertEquals(25, episode.evidenceScore)
    }

    @Test
    fun sizeIncreaseAfterLossIsRecordedWithoutCallingItRevenge() {
        val previous = trade(1, -20.0, 1_000_000L, closed = 1_100_000L, lot = 0.1)
        val current = trade(2, -10.0, 1_200_000L, lot = 0.14)
        val episode = TxAiBehavioralPatternAnalyzer.analyze(listOf(previous, current))
            .single { it.type == TxAiBehaviorPatternType.SIZE_UP_AFTER_LOSS }
        assertEquals(listOf(1L, 2L), episode.linkedTradeIds)
        assertTrue(episode.reasons.any { it.contains("اثبات‌کننده") })
    }

    @Test
    fun exitBeforeTargetRequiresExplicitEntryExitAndTargetPrices() {
        val early = trade(1, 10.0, 1_000_000L, entry = 1.1000, exit = 1.1050, target = 1.1100)
        val complete = trade(2, 10.0, 2_000_000L, entry = 1.1000, exit = 1.1100, target = 1.1100)
        val missing = trade(3, 10.0, 3_000_000L, entry = null, exit = 1.1050, target = 1.1100)
        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(early, complete, missing))
        assertEquals(listOf(1L), patterns.filter {
            it.type == TxAiBehaviorPatternType.EXIT_BEFORE_TARGET
        }.map { it.triggerTradeId })
    }

    @Test
    fun sellExitBeforeTargetIsHandledAndAccountsStayIsolated() {
        val sell = trade(
            1, 10.0, 1_000_000L, buy = false,
            entry = 1.1000, exit = 1.0950, target = 1.0900, account = "B",
        )
        val patterns = TxAiBehavioralPatternAnalyzer.analyze(listOf(sell))
        assertEquals(1, patterns.size)
        assertEquals("B", patterns.single().accountId)
    }
}
