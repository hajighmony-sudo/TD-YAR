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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.TetherMath
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private fun n(s: String) = s.trim().replace(",", "").replace("٫", ".").toDoubleOrNull()?.takeIf { it.isFinite() }
private fun money(v: Double) = String.format(Locale.US, "%,.0f", v)
private fun usd(v: Double) = String.format(Locale.US, "%,.2f", v)

/** Latest USDT/IRT price from Nobitex's public market stats (rial → toman). Throws on any failure. */
private fun fetchUsdtToman(): Double {
    val c = URL("https://api.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls").openConnection() as HttpURLConnection
    c.connectTimeout = 10000; c.readTimeout = 10000
    try {
        val o = JSONObject(c.inputStream.bufferedReader().readText())
        val latest = o.getJSONObject("stats").getJSONObject("usdt-rls").getString("latest").toDouble()
        return latest / 10.0
    } finally { c.disconnect() }
}

/** USDT calculator: conversion, buy→sell profit, network fee. Inspired by the calculators on arzdigital.com and tabdeal.org. */
@Composable
fun TetherCalc() {
    var tab by remember { mutableStateOf(0) }
    var buyPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("0.35") }
    var amount by remember { mutableStateOf("") }
    var networkFee by remember { mutableStateOf("1") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val buy = n(buyPrice); val sell = n(sellPrice) ?: buy; val f = n(fee) ?: 0.0; val a = n(amount); val nf = n(networkFee) ?: 0.0

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("ماشین حساب تتر (USDT)", "قیمت به تومان")
            Text("قیمت را خودت وارد کن یا با دکمهٔ زیر از قیمت لحظه‌ای نوبیتکس بگیر. کارمزد صرافی‌ها فرق دارد؛ عدد خودت را بگذار.", color = Tx.Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(buyPrice, { buyPrice = it }, "قیمت خرید (تومان)", Modifier.weight(1f))
                TxField(sellPrice, { sellPrice = it }, "قیمت فروش (تومان)", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(fee, { fee = it }, "کارمزد ٪", Modifier.weight(1f))
                TxField(amount, { amount = it }, "مقدار", Modifier.weight(1f))
            }
            GoldButton(if (busy) "در حال دریافت…" else "قیمت لحظه‌ای نوبیتکس", Modifier.fillMaxWidth()) {
                if (busy) return@GoldButton
                busy = true; status = ""
                scope.launch {
                    val r = withContext(Dispatchers.IO) { runCatching { fetchUsdtToman() } }
                    busy = false
                    r.onSuccess { p -> buyPrice = "%.0f".format(Locale.US, p); if (sellPrice.isBlank()) sellPrice = buyPrice; status = "قیمت آخرین معامله: ${money(p)} تومان" }
                        .onFailure { status = "دریافت قیمت ممکن نشد؛ قیمت را دستی وارد کن." }
                }
            }
            if (status.isNotBlank()) Text(status, color = Tx.Gold, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("خرید", "فروش", "سود و زیان", "شبکه").forEachIndexed { i, t -> TxChip(t, tab == i, { tab = i }) }
            }
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val ready = buy != null && buy > 0 && a != null && a > 0
            if (!ready) Text("قیمت و مقدار را وارد کن.", color = Tx.Muted, fontSize = 12.sp)
            else when (tab) {
                0 -> {
                    Text("اگر «مقدار» تومان باشد:", color = Tx.Muted, fontSize = 11.sp)
                    Res("تتر دریافتی", usd(TetherMath.usdtForToman(a!!, buy!!, f)) + " USDT")
                    Text("اگر «مقدار» تتر باشد:", color = Tx.Muted, fontSize = 11.sp)
                    Res("تومان لازم برای خرید", money(TetherMath.tomanToBuy(a, buy, f)) + " تومان")
                }
                1 -> Res("تومان دریافتی از فروش", money(TetherMath.tomanForSell(a!!, sell ?: buy!!, f)) + " تومان")
                2 -> {
                    val r = TetherMath.roundTrip(a!!, buy!!, sell ?: buy, f)
                    Res("هزینهٔ خرید", money(r.spent) + " تومان")
                    Res("دریافتی فروش", money(r.received) + " تومان")
                    Res("سود / زیان", money(r.profit) + " تومان  (" + String.format(Locale.US, "%+.2f", r.profitPct) + "٪)")
                    Res("قیمت فروش سربه‌سر", money(r.breakEvenSell) + " تومان")
                }
                else -> {
                    Res("تتر رسیده پس از کارمزد شبکه", usd(TetherMath.afterNetworkFee(a!!, nf)) + " USDT")
                    TxField(networkFee, { networkFee = it }, "کارمزد شبکه (USDT) — مثلاً TRC20 حدود ۱")
                }
            }
            if (buy != null && sell != null && sell > 0 && sell < buy) Text("فاصلهٔ خرید/فروش: " + String.format(Locale.US, "%.2f", TetherMath.spreadPct(buy, sell)) + "٪", color = Tx.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Res(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Tx.Muted, fontSize = 12.sp)
        Text(value, color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
