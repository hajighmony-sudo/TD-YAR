package com.tradenex.app.core

import com.tradenex.app.data.Trade
import org.junit.Assert.assertEquals
import java.time.LocalDate
import org.junit.Test

class InsightsTest {
    private fun t(id: Long, pnl: Double, day: Long = id, sym: String = "EURUSD") =
        Trade(id = id, symbol = sym, buy = true, lot = 1.0, pnl = pnl, strategy = "S", tags = listOf("a"), epochDay = day, emotion = "calm")

    @Test fun streaksAndHeadline() {
        val l = listOf(t(1, 10.0), t(2, 5.0), t(3, -4.0), t(4, -6.0), t(5, -1.0), t(6, 20.0))
        val s = Insights.streaks(l)
        assertEquals(2, s.bestWin); assertEquals(3, s.worstLoss); assertEquals(1, s.current)
        assertEquals(35.0 / 11.0, Insights.headline(l).profitFactor, 1e-9)
    }

    @Test fun groupsBySymbolSortedByPnl() {
        val g = Insights.bySymbol(listOf(t(1, 5.0, sym = "xauusd"), t(2, -9.0, sym = "EURUSD"), t(3, 7.0, sym = "XAUUSD")))
        assertEquals("XAUUSD", g[0].key); assertEquals(12.0, g[0].pnl, 1e-9); assertEquals(2, g[0].count)
    }

    @Test fun groupsByMonthInChronologicalOrder() {
        val jan = LocalDate.of(2026, 1, 12).toEpochDay()
        val feb = LocalDate.of(2026, 2, 2).toEpochDay()
        val groups = Insights.byMonth(listOf(t(1, 8.0, feb), t(2, -3.0, jan), t(3, 4.0, jan)))
        assertEquals(listOf("2026-01", "2026-02"), groups.map { it.key })
        assertEquals(1.0, groups[0].pnl, 1e-9)
        assertEquals(8.0, groups[1].pnl, 1e-9)
    }
}
