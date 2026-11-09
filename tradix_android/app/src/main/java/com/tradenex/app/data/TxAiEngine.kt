package com.tradenex.app.data

import com.tradenex.app.core.SessionEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class TxAiSlice(
    val label: String,
    val trades: Int,
    val net: Double,
    val winRate: Double,
    val average: Double,
)

data class TxAiScores(
    val discipline: Int?,
    val risk: Int?,
    val strategy: Int?,
    val decision: Int?,
    val monthLabel: String,
    val sampleSize: Int,
)

data class TxAiReport(
    val totalTrades: Int,
    val net: Double,
    val winRate: Double,
    val bestSession: TxAiSlice?,
    val sessions: List<TxAiSlice>,
    val timeframes: List<TxAiSlice>,
    val marketConditions: List<TxAiSlice>,
    val emotions: List<TxAiSlice>,
    val symbols: List<TxAiSlice>,
    val withoutOutsideStrategyNet: Double,
    val withoutOutsideStrategyDelta: Double,
    val weekdays: List<TxAiSlice>,
    val days: List<TxAiSlice>,
    val personality: String,
    val weeks: List<TxAiSlice>,
    val bestStrategy: TxAiSlice?,
    val outsideStrategyCount: Int,
    val outsideStrategyNet: Double,
    val allNet: Double,
    val halfRiskNet: Double,
    val halfRiskDelta: Double,
    val scores: TxAiScores,
    val insights: List<String>,
    val monthlyProgress: List<TxAiScores>,
    val timeframeDataAvailable: Boolean = false,
    val marketRegimeDataAvailable: Boolean = false,
    val worstSession: TxAiSlice? = null,
    val strategyVersions: List<TxAiSlice> = emptyList(),
    val timeOfDay: List<TxAiSlice> = emptyList(),
    val averageHoldMinutes: Double? = null,
    val medianHoldMinutes: Double? = null,
    val averageRMultiple: Double? = null,
    val maxWinStreak: Int = 0,
    val maxLossStreak: Int = 0,
)

object TxAiEngine {
    fun analyze(
        trades: List<Trade>,
        account: TradingAccount,
        riskPercent: Double,
        localZone: ZoneId = ZoneId.systemDefault(),
    ): TxAiReport {
        val ordered = trades.sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis })
        val sessions = ordered.filter { it.openedAtMillis > 0L }.groupBy { sessionOf(it.openedAtMillis) }
            .filterKeys { it != "نامشخص" }.map { (name, rows) -> slice(name, rows) }
            .sortedByDescending { it.average }
        val timeframes = ordered.filter { it.timeframe.isNotBlank() }.groupBy { it.timeframe }.map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val marketConditions = ordered.filter { it.marketCondition.isNotBlank() }.groupBy { it.marketCondition }.map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val emotions = ordered.filter { it.emotion.isNotBlank() }.groupBy { it.emotion }.map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val symbols = ordered.filter { it.symbol.isNotBlank() }.groupBy { it.symbol.uppercase() }.map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val weekdays = ordered.groupBy { LocalDate.ofEpochDay(it.epochDay).dayOfWeek.value }
            .map { (day, rows) -> slice(dayName(day), rows) }.sortedByDescending { it.average }
        val days = ordered.groupBy { LocalDate.ofEpochDay(it.epochDay) }.toSortedMap().map { (date, rows) -> slice(date.toString(), rows) }
        val weeks = ordered.groupBy {
            val d = LocalDate.ofEpochDay(it.epochDay)
            d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        }.toSortedMap().map { (start, rows) -> slice("${start.dayOfMonth}/${start.monthValue}", rows) }
        val strategySlices = ordered.filter { it.strategy.isNotBlank() }.groupBy { it.strategy }
            .map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val strategyVersions = ordered.filter { it.strategy.isNotBlank() }
            .groupBy { "${it.strategy} · v${it.strategyVersion}" }
            .map { (name, rows) -> slice(name, rows) }
            .sortedByDescending { it.average }
        val timeOfDay = ordered.filter { it.openedAtMillis > 0L }.groupBy { trade ->
            val local = Instant.ofEpochMilli(trade.openedAtMillis).atZone(localZone).toLocalTime()
            (local.hour * 60 + local.minute) / 15 * 15
        }.map { (startMinute, rows) ->
            val endMinute = (startMinute + 15) % (24 * 60)
            slice("${clockLabel(startMinute)}–${clockLabel(endMinute)}", rows)
        }.sortedByDescending { it.average }
        val holdMinutes = ordered.mapNotNull { trade ->
            if (trade.openedAtMillis > 0L && trade.closedAtMillis >= trade.openedAtMillis && trade.closedAtMillis > 0L) {
                (trade.closedAtMillis - trade.openedAtMillis) / 60_000.0
            } else null
        }.sorted()
        val averageHoldMinutes = holdMinutes.takeIf { it.isNotEmpty() }?.average()
        val medianHoldMinutes = holdMinutes.takeIf { it.isNotEmpty() }?.let { values ->
            if (values.size % 2 == 1) values[values.size / 2]
            else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        }
        val knownRiskTrades = ordered.filter { it.plannedRisk != null && it.plannedRisk > 0.0 && it.plannedRisk.isFinite() && it.pnl.isFinite() }
        val averageRMultiple = knownRiskTrades.takeIf { it.isNotEmpty() }
            ?.map { it.pnl / it.plannedRisk!! }?.average()
        val streaks = streaks(ordered)
        val outside = ordered.filter { isOutsideStrategy(it) }
        val currentMonth = java.time.YearMonth.now()
        val monthlyProgress = (5 downTo 0).map { monthsAgo ->
            val monthKey = currentMonth.minusMonths(monthsAgo.toLong())
            val rows = ordered.filter { java.time.YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == monthKey }
            scoreMonth(rows, account, riskPercent, monthKey.toString())
        }
        val monthTrades = ordered.filter { java.time.YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == currentMonth }
        val scores = scoreMonth(monthTrades, account, riskPercent, currentMonth.toString())
        val dominantSession = sessions.filter { it.label != "بین سشن‌ها" }.maxByOrNull { it.trades }
        val dominantSessionShare = dominantSession?.trades?.toDouble()?.div(ordered.size.coerceAtLeast(1)) ?: 0.0
        val recordedStrategyShare = monthTrades.count { it.strategy.isNotBlank() }.toDouble() / monthTrades.size.coerceAtLeast(1)
        val personality = when {
            ordered.size < 10 -> "داده ناکافی برای تشخیص پایدار"
            dominantSession != null && dominantSession.trades >= 5 && dominantSessionShare >= 0.5 -> "معامله‌گر سشن‌محور"
            recordedStrategyShare >= 0.8 -> "معامله‌گر استراتژی‌محور"
            else -> "سبک ترکیبی و انعطاف‌پذیر"
        }
        val halfRisk = ordered.sumOf { it.pnl * 0.5 }
        val insights = buildList {
            if (ordered.size < 10) add("برای تشخیص الگوی پایدار، حداقل ۱۰ معامله ثبت کن؛ نتیجه فعلی مقدماتی است.")
            sessions.firstOrNull { it.trades >= 5 && it.label != "بین سشن‌ها" }?.let { add("بهترین میانگین نتیجه ثبت‌شده در سشن ${it.label} است؛ ${it.trades} معامله با میانگین ${money(it.average)}.") }
            timeframes.firstOrNull { it.trades >= 5 }?.let { add("بهترین تایم‌فریم ثبت‌شده: ${it.label}؛ این نتیجه فقط از معاملات دارای برچسب تایم‌فریم محاسبه شده است.") }
            marketConditions.firstOrNull { it.trades >= 5 }?.let { add("بهترین شرایط بازار ثبت‌شده: ${it.label}؛ نمونه ${it.trades} معامله.") }
            emotions.firstOrNull { it.trades >= 5 }?.let { add("بهترین نتیجه در حالت ذهنی «${it.label}» ثبت شده؛ نمونه ${it.trades} معامله.") }
            weekdays.firstOrNull { it.trades >= 3 }?.let { add("بهترین روز هفته از نظر میانگین هر معامله: ${it.label}؛ نمونه ${it.trades} معامله.") }
            strategySlices.firstOrNull { it.trades >= 5 }?.let { add("استراتژی ${it.label} در داده‌های فعلی بهترین میانگین را دارد؛ نمونه ${it.trades} معامله.") }
            strategyVersions.firstOrNull { it.trades >= 5 }?.let { add("نسخه شاخص استراتژی: ${it.label}؛ نتیجه فقط از معاملات دارای نسخه ثبت‌شده محاسبه شده است.") }
            timeOfDay.firstOrNull { it.trades >= 5 }?.let { add("بازه ۱۵ دقیقه‌ای شاخص: ${it.label}؛ نمونه ${it.trades} معامله.") }
            if (outside.isNotEmpty()) add("${outside.size} معامله با نشانه‌های خارج از پلن/بدون استراتژی شناسایی شد؛ سود و زیان خالص آن‌ها ${money(outside.sumOf { it.pnl })} است.")
            if (monthTrades.count { it.plannedRisk != null && it.plannedRisk > 0.0 } < 3) add("برای امتیاز دقیق مدیریت ریسک، ریسک برنامه‌ریزی‌شده را هنگام ثبت هر معامله وارد کن.")
        }
        return TxAiReport(
            totalTrades = ordered.size, net = ordered.sumOf { it.pnl },
            winRate = if (ordered.isEmpty()) 0.0 else ordered.count { it.pnl > 0 }.toDouble() / ordered.size,
            bestSession = sessions.firstOrNull { it.trades >= 5 && it.label != "بین سشن‌ها" }, sessions = sessions, timeframes = timeframes, marketConditions = marketConditions,
            emotions = emotions, symbols = symbols,
            withoutOutsideStrategyNet = ordered.filterNot { isOutsideStrategy(it) }.sumOf { it.pnl },
            withoutOutsideStrategyDelta = ordered.filterNot { isOutsideStrategy(it) }.sumOf { it.pnl } - ordered.sumOf { it.pnl },
            weekdays = weekdays, days = days, weeks = weeks, personality = personality,
            bestStrategy = strategySlices.firstOrNull { it.trades >= 5 }, outsideStrategyCount = outside.size,
            outsideStrategyNet = outside.sumOf { it.pnl }, allNet = ordered.sumOf { it.pnl },
            halfRiskNet = halfRisk, halfRiskDelta = halfRisk - ordered.sumOf { it.pnl },
            scores = scores,
            insights = insights, monthlyProgress = monthlyProgress,
            timeframeDataAvailable = timeframes.isNotEmpty(),
            marketRegimeDataAvailable = marketConditions.isNotEmpty(),
            worstSession = sessions.filter { it.trades >= 5 && it.label != "بین سشن‌ها" }.minByOrNull { it.average },
            strategyVersions = strategyVersions,
            timeOfDay = timeOfDay,
            averageHoldMinutes = averageHoldMinutes,
            medianHoldMinutes = medianHoldMinutes,
            averageRMultiple = averageRMultiple,
            maxWinStreak = streaks.first,
            maxLossStreak = streaks.second,
        )
    }


    private fun scoreMonth(rows: List<Trade>, account: TradingAccount, riskPercent: Double, label: String): TxAiScores {
        val tagged = rows.filter { it.tags.isNotEmpty() }
        // Do not award a high discipline score when most trades have no behavioral review.
        val tagCoverage = tagged.size.toDouble() / rows.size.coerceAtLeast(1)
        val discipline = if (rows.size >= 5 && tagged.size >= 3 && tagCoverage >= 0.70) {
            val violations = tagged.count { hasBehaviorFlag(it) }
            ((1.0 - violations.toDouble() / tagged.size) * 100).roundToInt().coerceIn(0, 100)
        } else null
        val complianceRows = rows.filter { it.strategyFollowed != null }
        val complianceCoverage = complianceRows.size.toDouble() / rows.size.coerceAtLeast(1)
        val strategy = if (rows.size >= 5 && complianceRows.size >= 3 && complianceCoverage >= 0.70) {
            (complianceRows.count { it.strategyFollowed == true }.toDouble() / complianceRows.size * 100).roundToInt()
        } else null
        val knownRisk = rows.filter { it.plannedRisk != null && it.plannedRisk > 0.0 }
        val risk = if (knownRisk.size >= 3) {
            val allowed = account.startingBalance * riskPercent.coerceIn(0.1, 100.0) / 100.0
            (knownRisk.count { it.plannedRisk!! <= allowed }.toDouble() / knownRisk.size * 100).roundToInt()
        } else null
        val decision = if (rows.size >= 5) {
            (rows.count { it.entryPrice != null && it.exitPrice != null && it.stopLossPrice != null && it.strategy.isNotBlank() }.toDouble() / rows.size * 100).roundToInt()
        } else null
        return TxAiScores(discipline, risk, strategy, decision, label, rows.size)
    }

    private fun clockLabel(minutes: Int): String =
        String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    /** Returns maximum consecutive winning and losing closed-trade streaks. */
    private fun streaks(trades: List<Trade>): Pair<Int, Int> {
        val chronological = trades.sortedWith(
            compareBy<Trade> { it.closedAtMillis.takeIf { value -> value > 0L } ?: it.epochDay * 86_400_000L }
                .thenBy { it.id },
        )
        var wins = 0
        var losses = 0
        var maxWins = 0
        var maxLosses = 0
        chronological.forEach { trade ->
            when {
                trade.pnl > 0.0 -> {
                    wins++
                    losses = 0
                    maxWins = maxOf(maxWins, wins)
                }
                trade.pnl < 0.0 -> {
                    losses++
                    wins = 0
                    maxLosses = maxOf(maxLosses, losses)
                }
                else -> {
                    wins = 0
                    losses = 0
                }
            }
        }
        return maxWins to maxLosses
    }

    private fun sessionOf(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        return SessionEngine.defs.firstOrNull { def ->
            val local = instant.atZone(def.zoneId).toLocalTime()
            if (def.localOpen < def.localClose) !local.isBefore(def.localOpen) && local.isBefore(def.localClose)
            else !local.isBefore(def.localOpen) || local.isBefore(def.localClose)
        }?.name ?: "بین سشن‌ها"
    }

    private fun isOutsideStrategy(t: Trade): Boolean {
        val tags = t.tags.map { it.lowercase() }
        return t.strategyFollowed == false || tags.any {
            it.contains("fomo") || it.contains("revenge") || it.contains("no-plan") ||
                it.contains("off-strategy") || it.contains("خارج از استراتژی") || it.contains("بدون پلن")
        }
    }

    private fun hasBehaviorFlag(t: Trade): Boolean = t.tags.any {
        val tag = it.lowercase()
        tag.contains("fomo") || tag.contains("revenge") || tag.contains("overtrade") ||
            tag.contains("no-plan") || tag.contains("off-strategy") || tag.contains("خارج از استراتژی") || tag.contains("بدون پلن")
    }

    private fun slice(label: String, rows: List<Trade>) = TxAiSlice(
        label, rows.size, rows.sumOf { it.pnl },
        if (rows.isEmpty()) 0.0 else rows.count { it.pnl > 0 }.toDouble() / rows.size,
        if (rows.isEmpty()) 0.0 else rows.sumOf { it.pnl } / rows.size,
    )

    private fun dayName(day: Int) = when (day) {
        1 -> "دوشنبه"; 2 -> "سه‌شنبه"; 3 -> "چهارشنبه"; 4 -> "پنجشنبه"
        5 -> "جمعه"; 6 -> "شنبه"; else -> "یکشنبه"
    }

    private fun money(v: Double) = String.format(java.util.Locale.US, "%+.2f", v)
}
