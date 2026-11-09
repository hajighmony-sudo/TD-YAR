package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.JalaliCalendar
import com.tradenex.app.data.Trade
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

private val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

private fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

private data class PeriodCell(val label: String, val title: String, val pnl: Double?, val trades: Int)

private fun weekStartOf(epochDay: Long): Long = epochDay - Analytics.column(epochDay)

/**
 * Weekly (12 Saturday-start weeks) and monthly (12 Jalali months) heatmaps; tap a cell for details.
 * Complements the daily heatmap on the performance screen.
 */
@Composable
fun HeatmapPeriods(trades: List<Trade>) {
    var monthly by remember { mutableStateOf(false) }
    var picked by remember(monthly) { mutableStateOf<PeriodCell?>(null) }
    val hide = AppState.settings.hideBalance
    val today = AppState.today()

    val cells = remember(trades, monthly, today) {
        if (!monthly) {
            val ws = weekStartOf(today)
            val sums = trades.groupBy { weekStartOf(it.epochDay) }
            (11 downTo 0).map { i ->
                val k = ws - 7L * i
                val j = JalaliCalendar.fromGregorian(LocalDate.ofEpochDay(k))
                val list = sums[k].orEmpty()
                PeriodCell("${fa(j.day)} ${monthNames[j.month - 1]}", "هفته از ${fa(j.day)} ${monthNames[j.month - 1]} ${fa(j.year)}", if (list.isEmpty()) null else list.sumOf { it.pnl }, list.size)
            }
        } else {
            val nowJ = JalaliCalendar.fromGregorian(LocalDate.ofEpochDay(today))
            val groups = trades.groupBy {
                val j = JalaliCalendar.fromGregorian(LocalDate.ofEpochDay(it.epochDay))
                j.year * 12 + j.month - 1
            }
            val nowIdx = nowJ.year * 12 + nowJ.month - 1
            (11 downTo 0).map { i ->
                val idx = nowIdx - i
                val y = Math.floorDiv(idx, 12)
                val m = Math.floorMod(idx, 12) + 1
                val list = groups[idx].orEmpty()
                PeriodCell(monthNames[m - 1], "${monthNames[m - 1]} ${fa(y)}", if (list.isEmpty()) null else list.sumOf { it.pnl }, list.size)
            }
        }
    }
    val maxAbs = cells.maxOf { abs(it.pnl ?: 0.0) }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(if (monthly) "نقشه حرارتی ماهانه" else "نقشه حرارتی هفتگی", if (monthly) "۱۲ ماه شمسی" else "۱۲ هفته · شروع از شنبه")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TxChip("هفتگی", !monthly, { monthly = false })
                TxChip("ماهانه", monthly, { monthly = true })
            }
            cells.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { cell ->
                        val pnl = cell.pnl
                        val k = if (pnl == null) 0f else (abs(pnl) / max(maxAbs, 1e-9)).toFloat().coerceIn(0f, 1f)
                        val bg = when {
                            pnl == null -> Color.White.copy(alpha = 0.08f)
                            pnl >= 0 -> Tx.Green.copy(alpha = 0.25f + 0.75f * k)
                            else -> Tx.Red.copy(alpha = 0.25f + 0.75f * k)
                        }
                        Box(
                            Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { picked = cell },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cell.label, color = if (k > 0.6f) Tx.Ink else Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            val p = picked
            if (p == null) {
                Text("روی هر خانه بزن تا جمع سود و زیانش را ببینی.", color = Tx.Muted, fontSize = 10.sp)
            } else {
                Text(p.title, color = Tx.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                val v = p.pnl
                Text(
                    if (v == null) "معامله‌ای ثبت نشده" else (if (hide) "••••" else Analytics.money(v)) + " · ${fa(p.trades)} معامله",
                    color = if (v == null) Tx.Muted else if (v >= 0) Tx.Green else Tx.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
