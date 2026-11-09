package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTradeCandleTest {
    @Test
    fun createsOneNamedCandleForEachTradeInExecutionOrder() {
        val trades = listOf(
            Trade(2, "EURUSD", true, 0.1, -5.0, "ICT", emptyList(), 10, "", openedAtMillis = 2000, tradeName = "London retest"),
            Trade(1, "GBPUSD", false, 0.2, 10.0, "Breakout", emptyList(), 10, "", openedAtMillis = 1000, tradeName = "NY breakout"),
        )
        val candles = Analytics.tradeCandleSeries(trades, 1_000.0)
        assertEquals(2, candles.size)
        assertEquals("NY breakout", candles[0].tradeName)
        assertEquals("London retest", candles[1].tradeName)
        assertEquals(1_000.0, candles[0].candle.o, 0.001)
        assertEquals(1_010.0, candles[0].candle.c, 0.001)
        assertEquals(1_010.0, candles[1].candle.o, 0.001)
        assertEquals(1_005.0, candles[1].candle.c, 0.001)
        assertEquals("SELL", candles[0].side)
        assertEquals(0.2, candles[0].lot, 0.001)
    }

    @Test
    fun blankTradeNameFallsBackToSymbolAndLimitKeepsLatestCandles() {
        val trades = (1L..3L).map {
            Trade(it, "EURUSD", true, 0.1, it.toDouble(), "", emptyList(), it, "")
        }
        val candles = Analytics.tradeCandleSeries(trades, 100.0, limit = 2)
        assertEquals(2, candles.size)
        assertEquals("EURUSD", candles.first().tradeName)
        // Equity remains cumulative across trades omitted from the visible tail.
        assertEquals(101.0, candles.first().candle.o, 0.001)
        assertEquals(106.0, candles.last().candle.c, 0.001)
    }

    @Test
    fun dailyWeeklyAndMonthlyCandlesAggregateEquityFromRecordedTrades() {
        val d1 = java.time.LocalDate.of(2026, 11, 2).toEpochDay()
        val d2 = java.time.LocalDate.of(2026, 11, 3).toEpochDay()
        val d3 = java.time.LocalDate.of(2026, 11, 10).toEpochDay()
        val trades = listOf(
            Trade(1, "EURUSD", true, 0.1, 12.0, "", emptyList(), d1, ""),
            Trade(2, "GBPUSD", false, 0.2, -4.0, "", emptyList(), d2, ""),
            Trade(3, "USDJPY", true, 0.1, 7.0, "", emptyList(), d3, ""),
        )
        val daily = Analytics.bucketTradeCandleSeries(trades, Period.DAY, 1_000.0)
        assertEquals(3, daily.size)
        assertEquals(1_012.0, daily[0].candle.c, 0.001)
        assertEquals(1_008.0, daily[1].candle.c, 0.001)
        assertEquals(1_015.0, daily[2].candle.c, 0.001)

        val weekly = Analytics.bucketTradeCandleSeries(trades, Period.WEEK, 1_000.0)
        assertEquals(2, weekly.size)
        assertEquals(8.0, weekly[0].pnl, 0.001)
        assertEquals(1_008.0, weekly[0].candle.c, 0.001)

        val monthly = Analytics.bucketTradeCandleSeries(trades, Period.MONTH, 1_000.0)
        assertEquals(1, monthly.size)
        assertEquals(15.0, monthly[0].pnl, 0.001)
        assertEquals(1_015.0, monthly[0].candle.c, 0.001)
    }

    @Test
    fun saturdayFirstColumnAndFridayWeekendAreCorrect() {
        val saturday = java.time.LocalDate.of(2024, 3, 23).toEpochDay()
        val friday = java.time.LocalDate.of(2024, 3, 29).toEpochDay()
        assertEquals(0, Analytics.column(saturday))
        assertEquals(6, Analytics.column(friday))
        assertEquals(1, Analytics.noTradeDays(emptyList(), friday, 1, weekendHoliday = true))
        assertEquals(0, Analytics.noTradeDays(emptyList(), saturday, 1, weekendHoliday = true))
        assertTrue(Analytics.noTradeDays(emptyList(), friday, 1, weekendHoliday = false) == 1)
    }
}
