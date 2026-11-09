package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.Insights
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlin.math.abs

private fun fmt(v: Double) = if (v.isInfinite()) "∞" else "%.2f".format(v)

@Composable
fun InsightsScreen(nav: NavController) {
    val trades = AppState.visibleTrades
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("نماد", "روز هفته", "ساعت", "استراتژی", "احساس", "هشتگ", "جهت")
    val groups = when (tab) {
        0 -> Insights.bySymbol(trades)
        1 -> Insights.byWeekday(trades)
        2 -> Insights.byHourBucket(trades)
        3 -> Insights.byStrategy(trades)
        4 -> Insights.byEmotion(trades)
        5 -> Insights.byTag(trades)
        else -> Insights.bySide(trades)
    }
    val h = Insights.headline(trades)
    val s = Insights.streaks(trades)

    Page(title = "بینش‌ها", onBack = { nav.popBackStack() }) {
        if (trades.isEmpty()) {
            Reveal(0) { Text("هنوز معامله‌ای در این حساب نیست. چند معامله ثبت یا وارد کن تا بینش‌ها ساخته شوند.", color = Tx.Muted, fontSize = 12.sp) }
            return@Page
        }
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("خلاصه", "${trades.size} معامله")
                    Stat("Profit Factor", fmt(h.profitFactor))
                    Stat("امید ریاضی هر معامله", fmt(h.expectancy))
                    Stat("میانگین برد / باخت", "${fmt(h.avgWin)} / ${fmt(h.avgLoss)}")
                    Stat("نسبت Payoff", fmt(h.payoff))
                    Stat("بهترین / بدترین روز", "${fmt(h.bestDay)} / ${fmt(h.worstDay)}")
                    Stat("بیشترین برد پیاپی", "${s.bestWin}")
                    Stat("بیشترین باخت پیاپی", "${s.worstLoss}")
                    Stat("وضعیت فعلی", if (s.current >= 0) "${s.current} برد پیاپی" else "${-s.current} باخت پیاپی")
                }
            }
        }
        Reveal(1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.chunked(4).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { i, name -> val idx = r * 4 + i; TxChip(name, tab == idx, { tab = idx }) }
                        }
                    }
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("سود و زیان به تفکیک ${tabs[tab]}")
                    if (groups.isEmpty()) Text("داده‌ای برای این دسته نیست.", color = Tx.Muted, fontSize = 11.sp)
                    val maxAbs = groups.maxOfOrNull { abs(it.pnl) }?.takeIf { it > 0 } ?: 1.0
                    groups.forEach { g ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(g.key, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${fmt(g.pnl)}  ·  ${g.count} معامله  ·  ${"%.0f".format(g.winRate)}٪ برد", color = if (g.pnl >= 0) Tx.Green else Tx.Red, fontSize = 11.sp)
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Tx.Muted.copy(alpha = 0.15f))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth((abs(g.pnl) / maxAbs).toFloat().coerceIn(0.02f, 1f)).clip(RoundedCornerShape(3.dp)).background(if (g.pnl >= 0) Tx.Green else Tx.Red))
                            }
                        }
                    }
                    if (groups.size >= 2) {
                        Text("بهترین: ${groups.first().key}  ·  ضعیف‌ترین: ${groups.last().key}", color = Tx.Gold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Tx.Muted, fontSize = 12.sp)
        Text(value, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
