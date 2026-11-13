package com.tradenex.app.core

import com.tradenex.app.data.Trade
import org.junit.Assert.assertEquals
import org.junit.Test

class DayTotalsTest {
    private fun t(id: Long, day: Long, pnl: Double) = Trade(id = id, symbol = "EURUSD", buy = true, lot = 1.0, pnl = pnl, epochDay = day)

    @Test fun sumsPerDayOldestFirst() {
        val r = DayTotals.lastDays(listOf(t(1, 10, 5.0), t(2, 10, -2.0), t(3, 8, 7.0)), 10, 3)
        assertEquals(listOf(7.0, 0.0, 3.0), r)
    }
}
