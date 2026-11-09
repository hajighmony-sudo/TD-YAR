package com.tradenex.app.core

import com.tradenex.app.data.Trade
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Pure journal analytics: per-group breakdowns, streaks and headline ratios. */
object Insights {
    data class Group(val key: String, val count: Int, val pnl: Double, val wins: Int) {
        val winRate: Double get() = if (count == 0) 0.0 else wins * 100.0 / count
        val avg: Double get() = if (count == 0) 0.0 else pnl / count
    }

    fun groupBy(trades: List<Trade>, key: (Trade) -> List<String>): List<Group> {
        val m = LinkedHashMap<String, DoubleArray>() // count, pnl, wins
        for (t in trades) for (k in key(t).filter { it.isNotBlank() }.distinct()) {
            val a = m.getOrPut(k) { DoubleArray(3) }
            a[0] += 1.0; a[1] += t.pnl; if (t.pnl > 0) a[2] += 1.0
        }
        return m.map { Group(it.key, it.value[0].toInt(), it.value[1], it.value[2].toInt()) }.sortedByDescending { it.pnl }
    }

    fun bySymbol(t: List<Trade>) = groupBy(t) { listOf(it.symbol.uppercase()) }
    fun byStrategy(t: List<Trade>) = groupBy(t) { listOf(it.strategy) }
    fun byEmotion(t: List<Trade>) = groupBy(t) { listOf(it.emotion) }
    fun byTag(t: List<Trade>) = groupBy(t) { it.tags }
    fun bySide(t: List<Trade>) = groupBy(t) { listOf(if (it.buy) "Buy" else "Sell") }

    private val weekNames = mapOf(
        DayOfWeek.SATURDAY to "شنبه", DayOfWeek.SUNDAY to "یکشنبه", DayOfWeek.MONDAY to "دوشنبه", DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه", DayOfWeek.THURSDAY to "پنجشنبه", DayOfWeek.FRIDAY to "جمعه",
    )
    private val weekOrder = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    fun byWeekday(t: List<Trade>): List<Group> {
        val g = groupBy(t) { listOf(weekNames.getValue(LocalDate.ofEpochDay(it.epochDay).dayOfWeek)) }.associateBy { it.key }
        return weekOrder.mapNotNull { g[weekNames.getValue(it)] }
    }

    /** Four-hour buckets of the close time (only trades that have a close timestamp). */
    fun byHourBucket(t: List<Trade>, zone: ZoneId = ZoneId.systemDefault()): List<Group> {
        val withTime = t.filter { it.closedAtMillis > 0 }
        val g = groupBy(withTime) {
            val h = java.time.Instant.ofEpochMilli(it.closedAtMillis).atZone(zone).hour
            val s = h / 4 * 4
            listOf("%02d–%02d".format(s, s + 4))
        }
        return g.sortedBy { it.key }
    }

    data class Streaks(val bestWin: Int, val worstLoss: Int, val current: Int)

    /** current > 0 = running wins, < 0 = running losses. Trades ordered by day then id. */
    fun streaks(trades: List<Trade>): Streaks {
        var bw = 0; var wl = 0; var run = 0
        for (t in trades.sortedWith(compareBy({ it.epochDay }, { it.id }))) {
            if (t.pnl > 0) { run = if (run > 0) run + 1 else 1 }
            else if (t.pnl < 0) { run = if (run < 0) run - 1 else -1 }
            else continue
            if (run > bw) bw = run
            if (-run > wl) wl = -run
        }
        return Streaks(bw, wl, run)
    }

    data class Headline(val profitFactor: Double, val expectancy: Double, val avgWin: Double, val avgLoss: Double, val payoff: Double, val bestDay: Double, val worstDay: Double)

    fun headline(trades: List<Trade>): Headline {
        val w = trades.filter { it.pnl > 0 }.map { it.pnl }
        val l = trades.filter { it.pnl < 0 }.map { -it.pnl }
        val gp = w.sum(); val gl = l.sum()
        val days = trades.groupBy { it.epochDay }.map { d -> d.value.sumOf { it.pnl } }
        val aw = if (w.isEmpty()) 0.0 else gp / w.size
        val al = if (l.isEmpty()) 0.0 else gl / l.size
        return Headline(
            if (gl == 0.0) (if (gp > 0) Double.POSITIVE_INFINITY else 0.0) else gp / gl,
            if (trades.isEmpty()) 0.0 else trades.sumOf { it.pnl } / trades.size,
            aw, al, if (al == 0.0) 0.0 else aw / al,
            days.maxOrNull() ?: 0.0, days.minOrNull() ?: 0.0,
        )
    }
}
