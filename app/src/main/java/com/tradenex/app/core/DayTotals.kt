package com.tradenex.app.core

import com.tradenex.app.data.Trade

object DayTotals {
    /** Net P&L for each of the last [n] days ending at epoch day [today] (oldest first). */
    fun lastDays(trades: List<Trade>, today: Long, n: Int): List<Double> {
        val byDay = trades.groupBy { it.epochDay }.mapValues { d -> d.value.sumOf { it.pnl } }
        return (n - 1 downTo 0).map { byDay[today - it] ?: 0.0 }
    }
}
