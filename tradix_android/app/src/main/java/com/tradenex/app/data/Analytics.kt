package com.tradenex.app.data

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.min

data class Candle(val o: Double, val h: Double, val l: Double, val c: Double)

data class TradeCandle(
    val tradeId: Long,
    val tradeName: String,
    val symbol: String,
    val side: String,
    val lot: Double,
    val pnl: Double,
    val candle: Candle,
)

/** Equity-curve candles are built per bucket of [bucket] days. Day 0 (1970-01-01) was a Thursday. */
enum class Period(val label: String, val count: Int) {
    DAY("روزانه", 24),
    WEEK("هفتگی", 16),
    MONTH("ماهانه", 12),
    YEAR("سالانه", 3);

    fun bucket(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        return when (this) {
            DAY -> epochDay
            WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
            MONTH -> date.year.toLong() * 12L + date.monthValue - 1L
            YEAR -> date.year.toLong()
        }
    }
}

object Analytics {
    /**
     * One equity candle per recorded trade, ordered by execution time.
     * With journal-only data the high/low are the two observable equity points
     * (before and after the trade); intratrade excursions are intentionally not invented.
     */
    fun tradeCandleSeries(trades: List<Trade>, startBalance: Double, limit: Int = 60): List<TradeCandle> {
        require(startBalance.isFinite()) { "Starting balance must be finite." }
        var equity = startBalance
        val out = ArrayList<TradeCandle>(trades.size)
        val ordered = trades.asSequence()
            .filter { it.pnl.isFinite() }
            .sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis }.thenBy { it.id })
        for (trade in ordered) {
            val open = equity
            val close = equity + trade.pnl
            out += TradeCandle(
                tradeId = trade.id,
                tradeName = trade.tradeName.trim().ifEmpty { trade.symbol },
                symbol = trade.symbol,
                side = if (trade.buy) "BUY" else "SELL",
                lot = trade.lot,
                pnl = trade.pnl,
                candle = Candle(open, max(open, close), min(open, close), close),
            )
            equity = close
        }
        return out.takeLast(limit.coerceAtLeast(1))
    }

    fun tradeCandles(trades: List<Trade>, startBalance: Double, limit: Int = 60): List<Candle> =
        tradeCandleSeries(trades, startBalance, limit).map { it.candle }

    fun bucketTradeCandleSeries(
        trades: List<Trade>,
        period: Period,
        startBalance: Double,
    ): List<TradeCandle> {
        require(startBalance.isFinite()) { "Starting balance must be finite." }
        val ordered = trades.filter { it.pnl.isFinite() }
            .sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis }.thenBy { it.id })
        val groups = ordered.groupBy { period.bucket(it.epochDay) }.toSortedMap()
        var equity = startBalance
        return groups.map { (bucket, rows) ->
            val open = equity
            var high = open
            var low = open
            rows.forEach { trade ->
                equity += trade.pnl
                high = max(high, equity)
                low = min(low, equity)
            }
            val date = when (period) {
                Period.MONTH -> {
                    val year = Math.floorDiv(bucket, 12L).toInt()
                    val month = Math.floorMod(bucket, 12L).toInt() + 1
                    LocalDate.of(year, month, 1)
                }
                Period.YEAR -> LocalDate.of(bucket.toInt(), 1, 1)
                else -> LocalDate.ofEpochDay(bucket)
            }
            val label = when (period) {
                Period.DAY -> date.toString()
                Period.WEEK -> "هفتهٔ " + date.toString()
                Period.MONTH -> "%04d-%02d".format(java.util.Locale.US, date.year, date.monthValue)
                Period.YEAR -> date.year.toString()
            }
            TradeCandle(
                tradeId = rows.last().id,
                tradeName = label,
                symbol = rows.size.toString() + " معامله",
                side = period.name,
                lot = rows.sumOf { it.lot },
                pnl = rows.sumOf { it.pnl },
                candle = Candle(open, high, low, equity),
            )
        }
    }

    /** Legacy period aggregation retained for callers that explicitly need bucket candles. */
    fun candles(trades: List<Trade>, period: Period, startBalance: Double): List<Candle> {
        val groups = trades.sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis }.thenBy { it.id })
            .groupBy { period.bucket(it.epochDay) }
        var equity = startBalance
        val out = ArrayList<Candle>()
        for ((_, list) in groups) {
            val open = equity
            var hi = equity
            var lo = equity
            for (t in list) {
                equity += t.pnl
                hi = max(hi, equity)
                lo = min(lo, equity)
            }
            out.add(Candle(open, hi, lo, equity))
        }
        return out.takeLast(period.count)
    }

    fun dayPnl(trades: List<Trade>): Map<Long, Double> =
        trades.groupBy { it.epochDay }.mapValues { (_, v) -> v.sumOf { it.pnl } }

    /** Saturday-first column index: 0 Sat, 1 Sun, 2 Mon ... 6 Fri. */
    fun column(epochDay: Long): Int = Math.floorMod(epochDay + 5L, 7L).toInt()

    fun noTradeDays(trades: List<Trade>, today: Long, lookback: Int, weekendHoliday: Boolean): Int {
        val days = trades.map { it.epochDay }.toSet()
        var n = 0
        for (d in (today - lookback + 1)..today) {
            if (weekendHoliday && column(d) < 2) continue
            if (d !in days) n++
        }
        return n
    }

    fun money(v: Double, currency: String = AppState.selectedAccount.baseCurrency): String {
        val digits = if (currency == "JPY") 0 else 2
        return (if (v >= 0) "+" else "-") + currency + " " +
            String.format(java.util.Locale.US, "%,.${digits}f", kotlin.math.abs(v))
    }
}
