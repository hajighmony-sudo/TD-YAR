package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.util.Locale
import kotlin.math.abs

private fun num(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun f(v: Double, d: Int = 2): String = String.format(Locale.US, "%,.${d}f", v)

/**
 * Extra forex tools. Nothing is pre-filled from a "default broker": every contract size,
 * pip size and rate comes from the trader (the chips only fill in common conventions on tap).
 * Formulas are the standard ones used by lot-size / pip-value / margin calculators.
 */
@Composable
fun ForexTools() {
    var tool by remember { mutableStateOf(0) }
    val tools = listOf("ارزش پیپ", "سود و زیان", "مارجین", "ریسک به ریوارد")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tools.forEachIndexed { i, t -> TxChip(t, tool == i, { tool = i }, Modifier.weight(1f)) }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            when (tool) {
                0 -> PipValueTool()
                1 -> ProfitTool()
                2 -> MarginTool()
                else -> RewardTool()
            }
        }
    }
}

@Composable
private fun Result(label: String, value: String, color: Color = Tx.Text, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.05f)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Tx.Muted, fontSize = 10.sp)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Title(t: String, hint: String) {
    Text(t, color = Tx.Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    Text(hint, color = Tx.Muted, fontSize = 11.sp)
}

@Composable
private fun PipValueTool() {
    var lots by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }
    var pip by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    Title("ارزش پیپ", "ارزش هر پیپ = حجم(لات) × اندازه قرارداد × اندازه پیپ × نرخ تبدیل ارز مظنه به ارز حساب")
    TxField(lots, { lots = it }, "حجم (لات)")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TxChip("قرارداد ۱۰۰٬۰۰۰", false, { contract = "100000" })
        TxChip("قرارداد ۱۰۰", false, { contract = "100" })
    }
    TxField(contract, { contract = it }, "اندازه قرارداد (مثلاً ۱۰۰۰۰۰)")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TxChip("پیپ ۰.۰۰۰۱", false, { pip = "0.0001" })
        TxChip("پیپ ۰.۰۱ (ین)", false, { pip = "0.01" })
    }
    TxField(pip, { pip = it }, "اندازه پیپ")
    TxField(rate, { rate = it }, "نرخ تبدیل به ارز حساب (اگر مظنه همان ارز حساب است ۱)")
    val l = num(lots); val c = num(contract); val p = num(pip); val r = num(rate)
    val v = if (l != null && c != null && p != null && r != null) l * c * p * r else null
    Result("ارزش هر پیپ", v?.let { f(it) } ?: "—", Tx.Green, Modifier.fillMaxWidth())
    if (v != null) Result("ارزش هر ۱۰ پیپ", f(v * 10), Tx.Text, Modifier.fillMaxWidth())
}

@Composable
private fun ProfitTool() {
    var buy by remember { mutableStateOf(true) }
    var lots by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf("") }
    var exit by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    Title("سود و زیان", "سود = (قیمت خروج − قیمت ورود) × جهت × لات × اندازه قرارداد × نرخ تبدیل")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TxChip("خرید", buy, { buy = true }, Modifier.weight(1f))
        TxChip("فروش", !buy, { buy = false }, Modifier.weight(1f))
    }
    TxField(lots, { lots = it }, "حجم (لات)")
    TxField(contract, { contract = it }, "اندازه قرارداد")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TxField(entry, { entry = it }, "قیمت ورود", Modifier.weight(1f))
        TxField(exit, { exit = it }, "قیمت خروج", Modifier.weight(1f))
    }
    TxField(rate, { rate = it }, "نرخ تبدیل به ارز حساب (۱ اگر یکی است)")
    val l = num(lots); val c = num(contract); val e = num(entry); val x = num(exit); val r = num(rate)
    val pnl = if (l != null && c != null && e != null && x != null && r != null) (x - e) * (if (buy) 1 else -1) * l * c * r else null
    Result("سود / زیان", pnl?.let { (if (it >= 0) "+" else "") + f(it) } ?: "—", if ((pnl ?: 0.0) >= 0) Tx.Green else Tx.Red, Modifier.fillMaxWidth())
}

@Composable
private fun MarginTool() {
    var lots by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var leverage by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    Title("مارجین", "مارجین = لات × اندازه قرارداد × قیمت ÷ لوریج × نرخ تبدیل به ارز حساب")
    TxField(lots, { lots = it }, "حجم (لات)")
    TxField(contract, { contract = it }, "اندازه قرارداد")
    TxField(price, { price = it }, "قیمت فعلی نماد")
    TxField(leverage, { leverage = it }, "لوریج (مثلاً ۱۰۰ برای ۱:۱۰۰)")
    TxField(rate, { rate = it }, "نرخ تبدیل به ارز حساب (۱ اگر یکی است)")
    val l = num(lots); val c = num(contract); val p = num(price); val lev = num(leverage); val r = num(rate)
    val m = if (l != null && c != null && p != null && lev != null && lev > 0 && r != null) l * c * p / lev * r else null
    Result("مارجین لازم", m?.let { f(it) } ?: "—", Tx.Gold, Modifier.fillMaxWidth())
}

@Composable
private fun RewardTool() {
    var entry by remember { mutableStateOf("") }
    var stop by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var winRate by remember { mutableStateOf("") }
    Title("ریسک به ریوارد", "نسبت = |هدف − ورود| ÷ |ورود − حد ضرر|؛ امید ریاضی = نرخ برد × R − (۱ − نرخ برد)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TxField(entry, { entry = it }, "ورود", Modifier.weight(1f))
        TxField(stop, { stop = it }, "حد ضرر", Modifier.weight(1f))
        TxField(target, { target = it }, "هدف", Modifier.weight(1f))
    }
    TxField(winRate, { winRate = it }, "نرخ برد به درصد (اختیاری)")
    val e = num(entry); val s = num(stop); val t = num(target)
    val risk = if (e != null && s != null) abs(e - s) else null
    val reward = if (e != null && t != null) abs(t - e) else null
    val rr = if (risk != null && reward != null && risk > 0) reward / risk else null
    val wr = num(winRate)?.let { it / 100.0 }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Result("ریسک به ریوارد", rr?.let { "1 : " + f(it) } ?: "—", Tx.Gold, Modifier.weight(1f))
        Result("حداقل نرخ برد سربه‌سر", rr?.let { f(100.0 / (1.0 + it), 1) + "%" } ?: "—", Tx.Text, Modifier.weight(1f))
    }
    if (rr != null && wr != null) {
        val ev = wr * rr - (1 - wr)
        Result("امید ریاضی هر معامله", (if (ev >= 0) "+" else "") + f(ev) + " R", if (ev >= 0) Tx.Green else Tx.Red, Modifier.fillMaxWidth())
    }
}
