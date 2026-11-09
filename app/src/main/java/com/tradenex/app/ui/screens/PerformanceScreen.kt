package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Period
import com.tradenex.app.data.JalaliCalendar
import com.tradenex.app.ui.components.TradeCandleChart
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun PerformanceScreen(nav: NavController) {
    val settings = AppState.settings
    val accountStartBalance = AppState.selectedAccount.startingBalance.coerceAtLeast(1.0)
    var viewMode by remember { mutableStateOf("روزانه") }
    val period = when (viewMode) { "روزانه" -> Period.DAY; "هفتگی" -> Period.WEEK; "ماهانه" -> Period.MONTH; else -> Period.MONTH }
    var selectedHeatmapDay by remember { mutableStateOf<Long?>(null) }
    val trades = AppState.visibleTrades.toList()
    val symbolGroups = remember(trades) {
        trades.groupBy { it.symbol }.toList()
            .sortedByDescending { (_, items) -> items.sumOf { it.pnl } }
            .take(6)
    }
    val strategyGroups = remember(trades) {
        trades.groupBy { it.strategy.ifBlank { "بدون استراتژی" } }.toList()
            .sortedByDescending { (_, items) -> items.sumOf { it.pnl } }
            .take(6)
    }
    val today = AppState.today()
    val earliestDay = trades.minOfOrNull { it.epochDay } ?: today
    val windowDays = when (viewMode) {
        "لحظه‌ای" -> (today - earliestDay + 1L).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        "روزانه" -> 30
        "هفتگی" -> 84
        else -> 365
    }
    val windowStartDay = today - (windowDays - 1L)
    val windowed = remember(period, trades) { trades.filter { it.epochDay >= windowStartDay } }
    val pnlArray = remember(windowed) { windowed.map { it.pnl }.toDoubleArray() }
    val stats = remember(pnlArray) { NativeCore.stats(pnlArray) }
    val ext = remember(pnlArray) { NativeCore.performanceMetrics(pnlArray) }
    val openingEquity = accountStartBalance + trades.filter { it.epochDay < windowStartDay }.sumOf { it.pnl }
    // هر کندل = یک معامله ثبت‌شده (با نام معامله)
    val candles = remember(viewMode, windowed, openingEquity) {
        if (viewMode == "لحظه‌ای") Analytics.tradeCandleSeries(windowed, openingEquity, 240)
        else Analytics.bucketTradeCandleSeries(windowed, period, openingEquity)
    }
    val dayPnl = remember(trades) { Analytics.dayPnl(trades) }
    val inWindow = dayPnl.filterKeys { it > today - windowDays }
    val heatScale = (dayPnl.filterKeys { it > today - 35 && it <= today }.values.maxOfOrNull { abs(it) } ?: 0.0).coerceAtLeast(1.0)
    val best = inWindow.values.maxOrNull() ?: 0.0
    val worst = inWindow.values.minOrNull() ?: 0.0
    val noTrade = Analytics.noTradeDays(trades, today, 35, settings.weekendHoliday)
    val netColor = if (stats.net >= 0) Tx.Green else Tx.Red
    val coreLabel = if (NativeCore.isNative) "Rust" else "Kotlin"

    Page(title = "عملکرد من") {
        Reveal(0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("لحظه‌ای", "روزانه", "هفتگی", "ماهانه").forEach { mode ->
                    TxChip(mode, viewMode == mode, { viewMode = mode }, Modifier.weight(1f))
                }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("عملکرد $viewMode · هسته $coreLabel", color = Tx.Muted, fontSize = 11.sp)
                            Text(
                                if (settings.hideBalance) "•••" else String.format(java.util.Locale.US, "%+.2f%%", stats.net / accountStartBalance * 100),
                                color = netColor,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Text(if (settings.hideBalance) "••••" else Analytics.money(stats.net), color = netColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (candles.isEmpty()) {
                        Text("برای ساخت نمودار، ابتدا معامله‌ای در این بازه ثبت کن.", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 28.dp))
                    } else {
                        TradeCandleChart(candles, Modifier.fillMaxWidth().height(200.dp))
                        Text(
                            if (viewMode == "لحظه‌ای") "هر کندل = یک معامله، به ترتیب ثبت. لمس کندل، جزئیات همان معامله را نشان می‌دهد." else "هر کندل = عملکرد تجمیعی $viewMode. لمس کندل، سود و زیان و تعداد معاملات آن بازه را نشان می‌دهد.",
                            color = Tx.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
        Reveal(2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("معاملات", windowed.size.toString(), Tx.Text, Modifier.weight(1f))
                    StatTile("نرخ برد", String.format(java.util.Locale.US, "%.0f%%", stats.winRate * 100), Tx.Gold, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("ضریب سود", String.format(java.util.Locale.US, "%.2f", stats.profitFactor), Tx.Green, Modifier.weight(1f))
                    StatTile("افت سرمایه", String.format(java.util.Locale.US, "%,.0f", stats.maxDrawdown), Tx.Red, Modifier.weight(1f))
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("جزئیات آماری ($coreLabel)", "بر اساس معاملات همین بازه")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("انتظار", String.format(java.util.Locale.US, "%+.2f", ext.expectancy), if (ext.expectancy >= 0) Tx.Green else Tx.Red, Modifier.weight(1f))
                        StatTile("میانگین برد", Analytics.money(ext.averageWin), Tx.Green, Modifier.weight(1f))
                        StatTile("میانگین باخت", Analytics.money(-ext.averageLoss), Tx.Red, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("بازیابی", String.format(java.util.Locale.US, "%.2f", ext.recoveryFactor), Tx.Gold, Modifier.weight(1f))
                        StatTile("سهم بزرگ‌ترین برد", String.format(java.util.Locale.US, "%.0f%%", ext.largestWinShare * 100), Tx.Green, Modifier.weight(1f))
                        StatTile("سهم بزرگ‌ترین باخت", String.format(java.util.Locale.US, "%.0f%%", ext.largestLossShare * 100), Tx.Red, Modifier.weight(1f))
                    }
                    Text("نمونه آماری: ${ext.sampleSize} معامله", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
        Reveal(4) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("نقشه حرارتی ۵ هفته اخیر", "شنبه تا جمعه · تقویم شمسی")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("شنبه", "یک", "دوش", "سه", "چهار", "پنج", "جمعه", "جمع").forEach {
                            Text(it, color = Tx.Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                    val weekStart = today - Analytics.column(today)
                    for (r in 0..4) {
                        val start = weekStart - 7L * (4 - r)
                        var sum = 0.0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (c in 0..6) {
                                val d = start + c
                                val pnl = dayPnl[d]
                                if (pnl != null) sum += pnl
                                HeatCell(
                                    day = JalaliCalendar.fromGregorian(LocalDate.ofEpochDay(d)).day,
                                    pnl = pnl,
                                    strength = if (pnl == null) 0f else (abs(pnl) / heatScale).toFloat().coerceIn(0f, 1f),
                                    holiday = settings.weekendHoliday && c < 2,
                                    future = d > today,
                                    selected = selectedHeatmapDay == d,
                                    onClick = { if (d <= today) selectedHeatmapDay = d },
                                    index = r * 7 + c,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                (if (sum >= 0) "+" else "-") + abs(sum).toInt(),
                                color = if (sum >= 0) Tx.Green else Tx.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(top = 10.dp),
                            )
                        }
                    }
                    selectedHeatmapDay?.let { selectedDay ->
                        val selectedTrades = trades.filter { it.epochDay == selectedDay }.sortedBy { it.openedAtMillis }
                        val selectedPnl = dayPnl[selectedDay] ?: 0.0
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "جزئیات " + jalaliText(LocalDate.ofEpochDay(selectedDay)) + " · " + selectedTrades.size + " معامله",
                            color = Tx.Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            Analytics.money(selectedPnl),
                            color = if (selectedPnl >= 0) Tx.Green else Tx.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        selectedTrades.take(12).forEach { trade ->
                            Text(
                                (trade.tradeName.ifBlank { trade.symbol }) +
                                    " · " + trade.symbol +
                                    " · " + (if (trade.buy) "خرید" else "فروش") +
                                    " · " + Analytics.money(trade.pnl),
                                color = Tx.Muted,
                                fontSize = 10.sp,
                            )
                        }
                        if (selectedTrades.size > 12) {
                            Text("… و ${selectedTrades.size - 12} معامله دیگر", color = Tx.Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Reveal(5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("بهترین روز", Analytics.money(best), Tx.Green, Modifier.weight(1f))
                StatTile("بدترین روز", Analytics.money(worst), Tx.Red, Modifier.weight(1f))
                StatTile("بدون معامله", "$noTrade روز", Tx.Gold, Modifier.weight(1f), sub = "۵ هفته اخیر")
            }
        }
        Reveal(6) { HeatmapPeriods(trades) }
        Reveal(7) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("عملکرد بر اساس نماد", "تا ۶ نماد")
                    if (symbolGroups.isEmpty()) {
                        Text("پس از ثبت معامله، مقایسهٔ نمادها اینجا نمایش داده می‌شود.", color = Tx.Muted, fontSize = 11.sp)
                    } else {
                        symbolGroups.forEach { (symbol, items) -> BreakdownRow(symbol, items) }
                    }
                    Spacer(Modifier.height(4.dp))
                    SectionTitle("عملکرد بر اساس استراتژی", "تا ۶ استراتژی")
                    if (strategyGroups.isEmpty()) {
                        Text("هنوز معامله‌ای با استراتژی ثبت نشده است.", color = Tx.Muted, fontSize = 11.sp)
                    } else {
                        strategyGroups.forEach { (strategy, items) -> BreakdownRow(strategy, items) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HeatCell(
    day: Int,
    pnl: Double?,
    strength: Float,
    holiday: Boolean,
    future: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    modifier: Modifier,
) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 18L)
        on = true
    }
    val a by animateFloatAsState(if (on) 1f else 0f, tween(400), label = "cell")
    val shape = RoundedCornerShape(9.dp)
    val (bg, fg) = when {
        holiday -> Color.Transparent to Tx.Muted.copy(alpha = 0.5f)
        future -> Color.Transparent to Tx.Muted.copy(alpha = 0.35f)
        pnl == null -> Color.White.copy(alpha = 0.10f) to Tx.Muted
        pnl >= 0 -> Tx.Green.copy(alpha = 0.14f + strength * 0.82f) to (if (strength > 0.65f) Tx.Ink else Color(0xFFE9FFF6))
        else -> Tx.Red.copy(alpha = 0.14f + strength * 0.82f) to Color.White
    }
    Box(
        modifier
            .height(36.dp)
            .graphicsLayer {
                alpha = a
                scaleX = 0.6f + 0.4f * a
                scaleY = 0.6f + 0.4f * a
            }
            .clip(shape)
            .background(bg)
            .clickable(enabled = !future, onClick = onClick)
            .then(
                when {
                    selected -> Modifier.border(1.5.dp, Tx.Gold, shape)
                    holiday -> Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), shape)
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.toString(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BreakdownRow(label: String, trades: List<com.tradenex.app.data.Trade>) {
    val net = trades.sumOf { it.pnl }
    val winRate = trades.count { it.pnl > 0.0 }.toDouble() / trades.size.coerceAtLeast(1) * 100.0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${trades.size} معامله · نرخ برد ${String.format(java.util.Locale.US, "%.0f", winRate)}٪", color = Tx.Muted, fontSize = 10.sp)
        }
        Text(Analytics.money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
