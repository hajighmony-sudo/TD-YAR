package com.tradenex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.GrowthMath
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.theme.Tx
import java.util.Locale

private fun num(s: String) = s.trim().replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() }
private fun m(v: Double) = String.format(Locale.US, "%,.2f", v)

/** Compounding planner + drawdown-recovery table. */
@Composable
fun GrowthCalc() {
    var start by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var periods by remember { mutableStateOf("12") }
    var target by remember { mutableStateOf("") }
    val s = num(start); val r = num(rate); val p = num(periods)?.toInt(); val t = num(target)

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("رشد مرکب و هدف", "برنامه‌ریزی")
            Text("بازدهٔ ثابت فرضی است، نه پیش‌بینی؛ فقط برای دیدن اثر ترکیب سود و ریسک.", color = Tx.Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(start, { start = it }, "سرمایه‌ی اولیه", Modifier.weight(1f))
                TxField(rate, { rate = it }, "بازده هر دوره ٪", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(periods, { periods = it }, "تعداد دوره (ماه)", Modifier.weight(1f))
                TxField(target, { target = it }, "هدف سرمایه", Modifier.weight(1f))
            }
            if (s != null && r != null && p != null && p in 1..600) {
                Line("سرمایه پس از $p دوره", m(GrowthMath.compound(s, r, p)))
                Line("سود کل", m(GrowthMath.compound(s, r, p) - s))
            }
            if (s != null && r != null && t != null) {
                val n = GrowthMath.periodsToTarget(s, t, r)
                Line("دوره تا رسیدن به هدف", if (n == null) "—" else String.format(Locale.US, "%.1f", n))
            }
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("جبران افت سرمایه", "چرا ریسک کم مهم است")
            listOf(5.0, 10.0, 20.0, 30.0, 50.0, 75.0).forEach { l ->
                Line("افت ${l.toInt()}٪ ← لازم است ${String.format(Locale.US, "%.1f", GrowthMath.recoveryPct(l))}٪ سود", "")
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Tx.Muted, fontSize = 12.sp)
        Text(value, color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
