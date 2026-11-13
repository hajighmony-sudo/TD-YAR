package com.tradenex.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.DayTotals
import com.tradenex.app.data.Trade
import com.tradenex.app.ui.theme.Tx
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

/** Glass "today" card: big result in the candle colour, goal progress and the last 14 daily candles. */
@Composable
fun TodayHero(trades: List<Trade>) {
    val today = LocalDate.now().toEpochDay()
    val pnl = trades.filter { it.epochDay == today }.sumOf { it.pnl }
    val count = trades.count { it.epochDay == today }
    val c by animateColorAsState(CandleStyle.resolve(), tween(450), label = "hero")
    val shape = RoundedCornerShape(26.dp)
    val days = DayTotals.lastDays(trades, today, 14)
    val maxAbs = (days.maxOfOrNull { abs(it) } ?: 0.0).coerceAtLeast(1.0)
    val goal = CandleStyle.dailyGoal
    val goalP by animateFloatAsState(if (goal > 0) (pnl / goal).coerceIn(0.0, 1.0).toFloat() else 0f, tween(700), label = "goal")

    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(c.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, c.copy(alpha = 0.45f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("امروز", color = Tx.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (pnl > 0) "روز سبز" else if (pnl < 0) "روز قرمز" else "سر به سر",
                color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.16f)).border(1.dp, c.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Text(String.format(Locale.US, "%+,.2f", pnl), color = c, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("$count معامله امروز" + if (goal > 0) String.format(Locale.US, "  ·  هدف %.0f", goal) else "", color = Tx.Muted, fontSize = 11.sp)
        if (goal > 0) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(goalP).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { i, v ->
                val col = if (v > 0) CandleStyle.Green else if (v < 0) CandleStyle.Red else CandleStyle.Gold.copy(alpha = 0.5f)
                val h = (6 + abs(v) / maxAbs * 54).dp
                Box(
                    Modifier.width(10.dp).height(h).clip(RoundedCornerShape(3.dp))
                        .background(Brush.verticalGradient(listOf(col, col.copy(alpha = 0.45f))))
                        .then(if (i == days.lastIndex) Modifier.border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp)) else Modifier),
                )
            }
        }
    }
}
