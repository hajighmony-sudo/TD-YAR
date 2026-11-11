package com.tradenex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.navigation.NavController
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.util.Locale

private fun number(value: String): Double? = value.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() }

@Composable
fun CalculatorScreen(nav: NavController) {
    var mode by remember { mutableStateOf(0) }
    var display by remember { mutableStateOf("0") }
    var stored by remember { mutableStateOf<Double?>(null) }
    var operation by remember { mutableStateOf<String?>(null) }
    var fresh by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }

    var broker by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var riskPercent by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf("") }
    var stop by remember { mutableStateOf("") }
    var pipSize by remember { mutableStateOf("") }
    var pipValue by remember { mutableStateOf("") }

    val b = number(balance)
    val rp = number(riskPercent)
    val en = number(entry)
    val sl = number(stop)
    val ps = number(pipSize)
    val pv = number(pipValue)
    val riskCash = if (b != null && rp != null && rp > 0.0) b * rp / 100.0 else null
    val stopPips = if (en != null && sl != null && ps != null && ps > 0.0) kotlin.math.abs(en - sl) / ps else null
    val lot = if (riskCash != null && stopPips != null && pv != null && stopPips > 0.0 && pv > 0.0) riskCash / (stopPips * pv) else null

    Page(title = "ماشین حساب", onBack = { nav.popBackStack() }) {
        SectionHero("CALCULATION DECK / TOOLS", "میز ابزار محاسباتی", "محاسبات عمومی، حجم، ریسک و تبدیل USDT را با ورودی‌های قابل‌کنترل انجام بده.", Tx.Blue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TxChip("معمولی", mode == 0, { mode = 0 }, Modifier.weight(1f))
            TxChip("فارکس", mode == 1, { mode = 1 }, Modifier.weight(1f))
            TxChip("تتر USDT", mode == 2, { mode = 2 }, Modifier.weight(1f))
            TxChip("رشد", mode == 3, { mode = 3 }, Modifier.weight(1f))
        }
        if (mode == 0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(expression.ifBlank { "محاسبهٔ سریع" }, color = Tx.Muted, fontSize = 12.sp)
                    Text(display, color = Tx.Text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    val keys = listOf(
                        listOf("AC", "⌫", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "−"),
                        listOf("1", "2", "3", "+"),
                        listOf("±", "0", ".", "="),
                    )
                    keys.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                val color = if (key in listOf("÷", "×", "−", "+", "=")) Tx.Gold else Tx.Text
                                Text(
                                    key,
                                    color = color,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            when (key) {
                                                "AC" -> { display = "0"; stored = null; operation = null; expression = ""; fresh = false }
                                                "⌫" -> display = if (display.length <= 1) "0" else display.dropLast(1)
                                                "%" -> number(display)?.let { display = fmt(it / 100.0) }
                                                "±" -> number(display)?.let { display = fmt(-it) }
                                                "+", "−", "×", "÷" -> {
                                                    stored = number(display); operation = key; expression = display + " " + key; fresh = true
                                                }
                                                "=" -> {
                                                    val left = stored; val right = number(display); val op = operation
                                                    if (left != null && right != null && op != null) {
                                                        val result = when (op) { "+" -> left + right; "−" -> left - right; "×" -> left * right; else -> if (right == 0.0) Double.NaN else left / right }
                                                        if (result.isFinite()) display = fmt(result)
                                                        expression = ""; stored = null; operation = null; fresh = true
                                                    }
                                                }
                                                "." -> if (!display.contains(".")) display += "."
                                                else -> {
                                                    if (fresh || display == "0") display = key else display += key
                                                    fresh = false
                                                }
                                            }
                                        }.padding(vertical = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else if (mode == 1) {
            var forexTab by remember { mutableStateOf(0) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxChip("حجم لات", forexTab == 0, { forexTab = 0 }, Modifier.weight(1f))
                TxChip("ابزارهای بیشتر", forexTab == 1, { forexTab = 1 }, Modifier.weight(1f))
            }
            if (forexTab == 1) ForexTools() else GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ماشین حساب حجم معامله و ریسک", color = Tx.Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("اطلاعات را مطابق مشخصات نماد و قرارداد بروکر خودت وارد کن؛ هیچ مقدار بروکر به‌صورت پیش‌فرض فرض نمی‌شود.", color = Tx.Muted, fontSize = 11.sp)
                    TxField(broker, { broker = it }, "نام بروکر (اختیاری)")
                    TxField(symbol, { symbol = it }, "نماد دلخواه، مثلاً EURUSD یا XAUUSD")
                    TxField(balance, { balance = it }, "بالانس حساب")
                    TxField(riskPercent, { riskPercent = it }, "ریسک معامله به درصد")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxField(entry, { entry = it }, "قیمت ورود", Modifier.weight(1f))
                        TxField(stop, { stop = it }, "قیمت حد ضرر", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxField(pipSize, { pipSize = it }, "اندازه پیپ", Modifier.weight(1f))
                        TxField(pipValue, { pipValue = it }, "ارزش هر پیپ در ۱ لات", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("نتیجه محاسبه", color = Tx.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultCell("مبلغ ریسک", riskCash?.let(::fmt) ?: "—", Modifier.weight(1f))
                        ResultCell("فاصله استاپ", stopPips?.let { fmt(it) + " pip" } ?: "—", Modifier.weight(1f))
                    }
                    ResultCell("حجم پیشنهادی", lot?.let { fmt(it) + " lot" } ?: "اطلاعات را کامل کن", Modifier.fillMaxWidth(), Tx.Green)
                    Text("فرمول: حجم لات = مبلغ ریسک ÷ (فاصله استاپ به پیپ × ارزش هر پیپ در یک لات).", color = Tx.Muted, fontSize = 10.sp)
                    Text("برای نمادهای فلزات، شاخص‌ها، کریپتو و حساب‌های غیر USD، اندازه قرارداد و تبدیل ارز بروکر را جداگانه بررسی کن؛ مقدار pip value را از مشخصات همان نماد وارد کن.", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        } else if (mode == 3) {
            GrowthCalc()
        } else {
            TetherCalculator()
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TetherCalculator() {
    var direction by remember { mutableStateOf(0) }
    var rate by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("100") }
    var fee by remember { mutableStateOf("0") }
    val r = number(rate)
    val a = number(amount)
    val f = number(fee)
    val valid = r != null && r > 0.0 && a != null && a >= 0.0 && f != null && f in 0.0..100.0
    val gross = if (valid) {
        if (direction == 0) a!! * r!! else a!! / r!!
    } else null
    val feeValue = if (gross != null) gross * f!! / 100.0 else null
    val net = if (gross != null && feeValue != null) gross - feeValue else null
    val unit = if (direction == 0) "تومان" else "USDT"

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("USDT CONVERTER  /  TRADIX", color = Tx.Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ماشین حساب تتر", color = Tx.Text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("تبدیل سریع تتر و تومان با نرخ دلخواه", color = Tx.Muted, fontSize = 11.sp)
                }
                Text("₮", color = Tx.Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TxChip("تتر ← تومان", direction == 0, { direction = 0 }, Modifier.weight(1f))
                TxChip("تومان ← تتر", direction == 1, { direction = 1 }, Modifier.weight(1f))
            }
            TxField(rate, { rate = it }, "نرخ هر ۱ تتر به تومان")
            TxField(amount, { amount = it }, if (direction == 0) "مقدار تتر (USDT)" else "مبلغ تومان")
            TxField(fee, { fee = it }, "کارمزد فرضی (%)")
            Text("مقدارهای سریع", color = Tx.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (direction == 0) listOf("10", "50", "100", "500") else listOf("1000000", "5000000", "10000000"))
                    .forEach { quick -> TxChip(quick, amount == quick, { amount = quick }) }
            }
            GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold.copy(alpha = 0.45f)) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("خروجی محاسبه", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (valid) fmt(net ?: 0.0) else "نرخ و مقدار معتبر وارد کن", color = Tx.Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(unit, color = Tx.Muted, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultCell("مبلغ قبل از کارمزد", gross?.let(::fmt) ?: "—", Modifier.weight(1f))
                        ResultCell("کارمزد فرضی", feeValue?.let(::fmt) ?: "—", Modifier.weight(1f), Tx.Red)
                    }
                }
            }
            Text("نرخ را دستی وارد کن و پیش از استفاده، زمان به‌روزرسانی و اختلاف نرخ منابع مختلف را بررسی کن. این ابزار فقط تبدیل عددی انجام می‌دهد و نرخ زنده یا پیشنهاد خریدوفروش ارائه نمی‌کند.", color = Tx.Muted, fontSize = 10.sp)
            Text("الهام از نمایش قیمت، تغییرات و مبدل‌های تتر در منابع معرفی‌شده؛ بدون کپی‌کردن رابط یا داده‌ی آن‌ها.", color = Tx.Muted, fontSize = 10.sp)
        }
    }
}

private fun fmt(v: Double): String = String.format(Locale.US, "%,.4f", v).trimEnd('0').trimEnd('.')

@Composable
private fun ResultCell(label: String, value: String, modifier: Modifier, valueColor: Color = Tx.Text) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Tx.Muted, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
