package com.tradenex.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.RiskMath
import com.tradenex.app.data.AppState
import com.tradenex.app.data.JalaliCalendar
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)
private fun persianDigits(value: Int): String = value.toString().map { ch ->
    if (ch in '0'..'9') ('۰' + (ch - '0')) else ch
}.joinToString("")

/** Risk lab: Kelly, R-multiples, underwater (drawdown) curve and a bootstrap Monte-Carlo fan chart of past trades. */
@Composable
fun RiskLabScreen(nav: NavController) {
    val account = AppState.selectedAccount
    val trades = AppState.visibleTrades
    val start = account.startingBalance
    val ordered = remember(trades) { trades.sortedWith(compareBy({ it.epochDay }, { it.openedAtMillis }, { it.id })) }
    val pnls = remember(ordered) { ordered.map { it.pnl }.toDoubleArray() }
    var ruinIdx by remember { mutableStateOf(1) }
    var horizonIdx by remember { mutableStateOf(1) }
    val ruinFractions = listOf(0.2, 0.3, 0.5)
    val horizons = listOf(50, 100, 250)

    Page(title = "آزمایشگاه ریسک", onBack = { nav.popBackStack() }) {
        if (pnls.size < 5) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("برای محاسبه‌ی آمار ریسک حداقل ۵ معامله در «${account.name}» لازم است. (${persianDigits(pnls.size)} معامله ثبت شده)", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
            return@Page
        }
        val x = remember(pnls) { NativeCore.performanceMetrics(pnls) }
        val payoff = if (x.averageLoss > 0.0) x.averageWin / x.averageLoss else 0.0
        val kelly = RiskMath.kelly(x.winRate, payoff)
        val r = remember(ordered) { RiskMath.rMultiples(ordered) }
        val curve = remember(pnls, start) { DoubleArray(pnls.size) { index -> start + pnls.take(index + 1).sum() } }
        val dd = remember(curve) { RiskMath.drawdownSeries(curve) }
        val mc = remember(pnls, start, ruinIdx, horizonIdx) { RiskMath.monteCarlo(pnls, start, horizons[horizonIdx], 600, ruinFractions[ruinIdx]) }

        Reveal(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("کِلی کامل", if (kelly > 0) f1(kelly * 100) + "%" else "بدون لبه", if (kelly > 0) Tx.Gold else Tx.Red, Modifier.weight(1f), sub = "سقف نظری ریسک")
                StatTile("نیم‌کِلی", if (kelly > 0) f1(kelly * 50) + "%" else "—", Tx.Gold, Modifier.weight(1f), sub = "محافظه‌کارانه‌تر")
                StatTile("بیشینه افت", f1(dd.minOrNull() ?: 0.0) + "%", Tx.Red, Modifier.weight(1f))
            }
            if (r.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("میانگین R", f2(r.average()), if (r.average() >= 0) Tx.Green else Tx.Red, Modifier.weight(1f), sub = "${Jalali.digits(r.size)} معامله با ریسک ثبت‌شده")
                    StatTile("بهترین R", f2(r.max()), Tx.Green, Modifier.weight(1f))
                    StatTile("بدترین R", f2(r.min()), Tx.Red, Modifier.weight(1f))
                }
            } else {
                Text("برای آمار R-multiple، هنگام ثبت معامله «ریسک برنامه‌ریزی‌شده» را وارد کن.", color = Tx.Muted, fontSize = 10.sp)
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("نمودار افت سرمایه", "فاصله از اوج حساب (٪)")
                    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                        val lo = (dd.minOrNull() ?: -1.0).coerceAtMost(-0.5)
                        val path = Path()
                        dd.forEachIndexed { i, v ->
                            val px = size.width * i / (dd.size - 1).coerceAtLeast(1)
                            val py = (v / lo).toFloat() * size.height * 0.95f
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, 0f), Offset(size.width, 0f), 1f)
                        drawPath(path, Tx.Red, style = Stroke(1.6.dp.toPx()))
                    }
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("شبیه‌سازی مونت‌کارلو", "نمونه‌گیری مجدد از معامله‌های خودت · ۶۰۰ اجرا")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        horizons.forEachIndexed { i, h -> TxChip("${Jalali.digits(h)} معامله", horizonIdx == i, { horizonIdx = i }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ruinFractions.forEachIndexed { i, f -> TxChip("ورشکستگی = افت ${Jalali.digits((f * 100).toInt())}٪", ruinIdx == i, { ruinIdx = i }) }
                    }
                    if (mc != null) {
                        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                            val lo = mc.p5.min().coerceAtMost(start)
                            val hi = mc.p95.max().coerceAtLeast(start)
                            val span = (hi - lo).coerceAtLeast(1e-6)
                            fun y(v: Double) = size.height - (((v - lo) / span) * 0.9 + 0.05).toFloat() * size.height
                            fun x(i: Int) = size.width * i / (mc.p50.size - 1)
                            val band = Path()
                            mc.p95.forEachIndexed { i, v -> if (i == 0) band.moveTo(x(i), y(v)) else band.lineTo(x(i), y(v)) }
                            for (i in mc.p5.indices.reversed()) band.lineTo(x(i), y(mc.p5[i]))
                            band.close()
                            drawPath(band, Tx.Gold.copy(alpha = 0.18f))
                            val mid = Path()
                            mc.p50.forEachIndexed { i, v -> if (i == 0) mid.moveTo(x(i), y(v)) else mid.lineTo(x(i), y(v)) }
                            drawPath(mid, Tx.Gold, style = Stroke(2.dp.toPx()))
                            drawLine(Color.White.copy(alpha = 0.4f), Offset(0f, y(start)), Offset(size.width, y(start)), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                            drawLine(Tx.Red.copy(alpha = 0.6f), Offset(0f, y(start * (1 - ruinFractions[ruinIdx]))), Offset(size.width, y(start * (1 - ruinFractions[ruinIdx]))), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)))
                        }
                        val hide = AppState.settings.hideBalance
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("احتمال ورشکستگی", f1(mc.ruinProbability * 100) + "%", if (mc.ruinProbability > 0.1) Tx.Red else Tx.Green, Modifier.weight(1f))
                            StatTile("میانه‌ی پایان", if (hide) "••••" else String.format(Locale.US, "%,.0f", mc.medianFinal), Tx.Gold, Modifier.weight(1f))
                            StatTile("افت ۹۵٪ بدترین", f1(mc.worstDrawdownP95) + "%", Tx.Red, Modifier.weight(1f))
                        }
                        Text("نوار طلایی = بازه‌ی ۵٪ تا ۹۵٪ مسیرهای ممکن، خط پررنگ = میانه، خط‌چین قرمز = مرز ورشکستگی.", color = Tx.Muted, fontSize = 10.sp)
                    }
                    Text("این‌ها فقط آمار معامله‌های گذشته‌ی خودت است، نه پیش‌بینی و نه توصیه‌ی مالی. نتایج با تعداد کم معامله قابل‌اعتماد نیستند.", color = Tx.Muted, fontSize = 10.sp, fontWeight = FontWeight.Normal)
                }
            }
        }
    }
}
