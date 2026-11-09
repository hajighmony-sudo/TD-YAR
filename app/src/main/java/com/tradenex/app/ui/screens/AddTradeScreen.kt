package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.notifications.NexNotify
import com.tradenex.app.data.JalaliCalendar
import com.tradenex.app.data.Trade
import com.tradenex.app.domain.FxInstrument
import com.tradenex.app.domain.FxPnlInput
import com.tradenex.app.domain.FxSide
import com.tradenex.app.domain.ForexPnlCalculator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlin.math.abs
import kotlin.math.round

private val symbols = listOf("EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD", "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY", "AUDJPY", "EURCHF")
private val lotPresets = listOf(0.01, 0.05, 0.1, 0.5, 1.0, 2.0)
private val amountPresets = listOf(10.0, 50.0, 100.0, 250.0)
private val emotions = listOf("آرام", "ترس", "طمع", "انتقام", "خسته")

private fun round2(v: Double) = round(v * 100) / 100

private fun stepLot(v: Double, dir: Int): Double {
    val step = when {
        v < 0.1 || (v == 0.1 && dir < 0) -> 0.01
        v < 1.0 || (v == 1.0 && dir < 0) -> 0.1
        else -> 0.5
    }
    return round2(v + dir * step).coerceIn(0.01, 50.0)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTradeScreen(nav: NavController) {
    val settings = AppState.settings
    val haptic = LocalHapticFeedback.current
    var buy by remember { mutableStateOf(true) }
    var symbol by remember { mutableStateOf("EURUSD") }
    var lot by remember { mutableStateOf(settings.defaultLot) }
    var win by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf(250.0) }
    var usePriceCalculation by remember { mutableStateOf(false) }
    var entryText by remember { mutableStateOf("") }
    var exitText by remember { mutableStateOf("") }
    var conversionText by remember { mutableStateOf(if (symbol.endsWith(AppState.selectedAccount.baseCurrency)) "1.0" else "") }
    LaunchedEffect(symbol, AppState.selectedAccountId) {
        conversionText = if (symbol.endsWith(AppState.selectedAccount.baseCurrency)) "1.0" else ""
    }
    var commissionText by remember { mutableStateOf("0") }
    var swapText by remember { mutableStateOf("0") }
    var feeText by remember { mutableStateOf("0") }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var tradeNameText by remember { mutableStateOf("") }
    var tradeDateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var tradeTimeText by remember { mutableStateOf(java.time.LocalTime.now().withSecond(0).withNano(0).toString()) }
    var timeframe by remember { mutableStateOf("") }
    var marketCondition by remember { mutableStateOf("") }
    var plannedRiskText by remember { mutableStateOf("") }
    val parsedTradeDate = remember(tradeDateText) { JalaliCalendar.parseDateInput(tradeDateText) }
    val parsedTradeTime = remember(tradeTimeText) { runCatching { java.time.LocalTime.parse(tradeTimeText.trim()) }.getOrNull() }
    var strategy by remember { mutableStateOf(AppState.strategies.firstOrNull { it.active }?.name ?: "") }
    var strategyFollowed by remember { mutableStateOf<Boolean?>(null) }
    var emotion by remember { mutableStateOf("آرام") }
    var tags by remember { mutableStateOf(setOf<String>()) }

    val standardFxSymbol = remember(symbol) { runCatching { FxInstrument.standard(symbol) }.isSuccess }
    val priceResult = remember(symbol, standardFxSymbol, buy, lot, entryText, exitText, conversionText, commissionText, swapText, feeText) {
        if (!standardFxSymbol) null else runCatching {
            ForexPnlCalculator.calculate(
                FxPnlInput(
                    instrument = FxInstrument.standard(symbol),
                    side = if (buy) FxSide.BUY else FxSide.SELL,
                    lots = BigDecimal.valueOf(lot),
                    entryPrice = entryText.trim().toBigDecimal(),
                    exitPrice = exitText.trim().toBigDecimal(),
                    quoteToAccountRate = conversionText.trim().toBigDecimal(),
                    commissionAccountCurrency = commissionText.trim().toBigDecimal(),
                    swapAccountCurrency = swapText.trim().toBigDecimal(),
                    otherFeesAccountCurrency = feeText.trim().toBigDecimal(),
                ),
            )
        }.getOrNull()
    }
    val signed = if (usePriceCalculation) priceResult?.netAccountCurrency?.toDouble() ?: 0.0 else if (win) amount else -amount
    val animated by animateFloatAsState(signed.toFloat(), tween(450), label = "net")
    val resColor = if (signed >= 0.0) Tx.Green else Tx.Red
    val riskUsd = AppState.selectedAccount.startingBalance * settings.riskPercent / 100.0
    val overRisk = if (usePriceCalculation) signed < -riskUsd else !win && amount > riskUsd

    Page(title = "ثبت معامله", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideButton("خرید (Buy)", true, buy, Tx.Green, Modifier.weight(1f)) { buy = true }
                SideButton("فروش (Sell)", false, !buy, Tx.Red, Modifier.weight(1f)) { buy = false }
            }
        }
        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("نماد یا نام دارایی", color = Tx.Muted, fontSize = 11.sp)
                TxField(symbol, { value ->
                    symbol = value.uppercase(java.util.Locale.ROOT)
                        .filter { it.isLetterOrDigit() || it in "._/-" }
                        .take(24)
                }, "مثلاً EURUSD، XAUUSD، BTCUSDT")
                Text("نماد دلخواه برای ثبت دستی هم قابل استفاده است؛ محاسبهٔ قیمت فقط برای جفت‌ارزهای پشتیبانی‌شده فعال است.", color = Tx.Muted, fontSize = 10.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    symbols.forEach { TxChip(it, symbol == it, { symbol = it }) }
                }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("حجم معامله (لات)", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    Stepper(
                        valueText = String.format(java.util.Locale.US, "%.2f", lot),
                        onMinus = { lot = stepLot(lot, -1) },
                        onPlus = { lot = stepLot(lot, 1) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lotPresets.forEach { TxChip(it.toString(), lot == it, { lot = it }) }
                    }
                    Text("تاریخ معامله", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    TxField(tradeDateText, { tradeDateText = it }, "میلادی YYYY-MM-DD یا شمسی 1405/08/17")
                    if (parsedTradeDate != null) {
                        Text("تاریخ شمسی: " + jalaliText(parsedTradeDate), color = Tx.Gold, fontSize = 10.sp)
                    }
                    Text("ساعت بازشدن معامله", color = Tx.Muted, fontSize = 11.sp)
                    TxField(tradeTimeText, { tradeTimeText = it }, "HH:mm")
                    Text("نام معامله یا ستاپ (اختیاری)", color = Tx.Muted, fontSize = 11.sp)
                    TxField(tradeNameText, { tradeNameText = it }, "مثلاً London Breakout · ستاپ ICT")
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("روش ثبت نتیجه", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TxChip("مبلغ دستی", !usePriceCalculation, { usePriceCalculation = false })
                        TxChip("محاسبه از قیمت", usePriceCalculation, { usePriceCalculation = true })
                    }
                    if (usePriceCalculation && !standardFxSymbol) {
                        Text("برای این نماد، ثبت دستی نتیجه را انتخاب کن. محاسبهٔ خودکار قیمت فعلاً فقط برای جفت‌ارزهای استاندارد فارکس فعال است.", color = Tx.Gold, fontSize = 11.sp)
                    } else if (usePriceCalculation) {
                        Text("قیمت ورود و خروج", color = Tx.Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TxField(entryText, { entryText = it }, "قیمت ورود", Modifier.weight(1f))
                            TxField(exitText, { exitText = it }, "قیمت خروج", Modifier.weight(1f))
                        }
                        Text("نرخ تبدیل ارز مظنه به ارز حساب (${AppState.selectedAccount.baseCurrency})", color = Tx.Muted, fontSize = 10.sp)
                        TxField(conversionText, { conversionText = it }, "مثلاً 1 یا نرخ تبدیل واقعی")
                        Text("هزینه‌ها به ارز حساب", color = Tx.Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TxField(commissionText, { commissionText = it }, "کمیسیون", Modifier.weight(1f))
                            TxField(swapText, { swapText = it }, "سواپ", Modifier.weight(1f))
                        }
                        TxField(feeText, { feeText = it }, "سایر هزینه‌ها (اختیاری)")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TxField(stopLossText, { stopLossText = it }, "Stop Loss", Modifier.weight(1f))
                            TxField(takeProfitText, { takeProfitText = it }, "Take Profit", Modifier.weight(1f))
                        }
                        Text(
                            if (priceResult == null) "برای محاسبه، قیمت ورود، خروج و نرخ تبدیل معتبر وارد کن."
                            else "حرکت ${priceResult.pips} پیپ · ارزش هر پیپ ${priceResult.pipValueQuoteCurrency}",
                            color = if (priceResult == null) Tx.Red else Tx.Muted,
                            fontSize = 10.sp,
                        )
                    } else {
                        Text("نتیجه خالص را به ارز حساب وارد کن.", color = Tx.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Reveal(4) {
            GlassCard(Modifier.fillMaxWidth(), border = resColor.copy(alpha = 0.5f)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!usePriceCalculation) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResultToggle("سود (Profit)", win, Tx.Green, Modifier.weight(1f)) { win = true }
                            ResultToggle("ضرر (Loss)", !win, Tx.Red, Modifier.weight(1f)) { win = false }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!usePriceCalculation) {
                        Stepper(
                            valueText = AppState.selectedAccount.baseCurrency + " " + String.format(java.util.Locale.US, "%,.2f", amount),
                            onMinus = { amount = (amount - 10).coerceAtLeast(0.0) },
                            onPlus = { amount += 10 },
                            valueColor = resColor,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            amountPresets.forEach { p ->
                                TxChip("+${p.toInt()}", false, { amount += p })
                            }
                        }
                        Text("تغییر مبلغ دستی: هر بار ۱۰ واحد", color = Tx.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Reveal(5) {
            GlassCard(Modifier.fillMaxWidth(), border = resColor.copy(alpha = 0.5f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نتیجه خالص", color = Tx.Muted, fontSize = 11.sp)
                        Text(Analytics.money(animated.toDouble()), color = resColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f)) {
                        RowLabelCompact("هر لات", Analytics.money(signed / lot), resColor)
                        RowLabelCompact("از بالانس", String.format(java.util.Locale.US, "%+.2f%%", signed / AppState.selectedAccount.startingBalance.coerceAtLeast(1.0) * 100), resColor)
                        RowLabelCompact("ریسک مجاز", AppState.selectedAccount.baseCurrency + " " + String.format(java.util.Locale.US, "%,.0f", riskUsd), Tx.Muted)
                    }
                }
            }
        }
        run {
            val en = entryText.trim().toDoubleOrNull()
            val sl = stopLossText.trim().toDoubleOrNull()
            val tp = takeProfitText.trim().toDoubleOrNull()
            val ex = exitText.trim().toDoubleOrNull()
            if (usePriceCalculation && en != null && sl != null && sl != en) {
                val dir = if (buy) 1.0 else -1.0
                val risk = (en - sl) * dir
                val wrongSide = risk <= 0.0
                Reveal(5) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (wrongSide) {
                                Text("استاپ‌لاس در سمت اشتباهِ ${if (buy) "Buy" else "Sell"} است.", color = Tx.Red, fontSize = 11.sp)
                            } else {
                                if (tp != null) {
                                    val rr = (tp - en) * dir / risk
                                    RowLabelCompact("نسبت ریسک به ریوارد (برنامه)", String.format(java.util.Locale.US, "1 : %.2f", rr), if (rr >= 1.0) Tx.Green else Tx.Red)
                                }
                                if (ex != null) {
                                    val r = (ex - en) * dir / risk
                                    RowLabelCompact("نتیجه به R", String.format(java.util.Locale.US, "%+.2f R", r), if (r >= 0) Tx.Green else Tx.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (overRisk) {
            Text(
                "ضرر از ریسک تعیین‌شده (${settings.riskPercent}٪ بالانس) بیشتر است؛ در دفترچه علامت می‌خورد.",
                color = Tx.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Reveal(6) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("استراتژی", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppState.strategies.filter { it.active }.forEach { TxChip(it.name, strategy == it.name, { strategy = it.name }) }
                }
                Text("پایبندی واقعی به پلن (برای امتیاز دقیق TX AI)", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TxChip("طبق پلن", strategyFollowed == true, { strategyFollowed = true })
                    TxChip("خارج از پلن", strategyFollowed == false, { strategyFollowed = false })
                    TxChip("ثبت نشده", strategyFollowed == null, { strategyFollowed = null })
                }
                Text("تایم‌فریم تحلیل (اختیاری؛ فقط اگر واقعاً ثبتش کرده‌ای)", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("M1", "M5", "M15", "M30", "H1", "H4", "D1", "W1").forEach { TxChip(it, timeframe == it, { timeframe = it }) }
                }
                Text("شرایط بازار (اختیاری؛ بدون حدس)", color = Tx.Muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("رونددار", "رنج", "پرنوسان", "کم‌نوسان", "نزدیک خبر", "شکست ساختار").forEach { TxChip(it, marketCondition == it, { marketCondition = it }) }
                }
                Text("ریسک برنامه‌ریزی‌شده به ارز حساب", color = Tx.Muted, fontSize = 11.sp)
                TxField(plannedRiskText, { plannedRiskText = it }, "مثلاً 100")
                Text("حال ذهنی هنگام معامله", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emotions.forEach { TxChip(it, emotion == it, { emotion = it }) }
                }
                Text("هشتگ‌ها", color = Tx.Muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppState.hashtags.forEach { h ->
                        TxChip(h, h in tags, { tags = if (h in tags) tags - h else tags + h })
                    }
                }
            }
        }
        Reveal(7) {
            TxField(notesText, { notesText = it }, "یادداشت معامله (اختیاری)")
            if (parsedTradeDate == null) Text("تاریخ باید با قالب YYYY-MM-DD وارد شود.", color = Tx.Red, fontSize = 11.sp)
            if (parsedTradeTime == null) Text("ساعت باید با قالب HH:mm وارد شود.", color = Tx.Red, fontSize = 11.sp)
            GoldButton("ذخیره معامله در دفترچه") {
                if (symbol.isNotBlank() && parsedTradeDate != null && parsedTradeTime != null && (!usePriceCalculation || priceResult != null)) {
                    AppState.addTrade(
                    Trade(
                        id = System.currentTimeMillis(),
                        symbol = symbol,
                        buy = buy,
                        lot = lot,
                        pnl = signed,
                        strategy = strategy,
                        strategyVersion = AppState.strategies.firstOrNull { it.name == strategy }?.version ?: 1,
                        tags = tags.toList(),
                        epochDay = parsedTradeDate.toEpochDay(),
                        emotion = emotion,
                        accountId = AppState.selectedAccountId,
                        openedAtMillis = parsedTradeDate.atTime(parsedTradeTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        closedAtMillis = parsedTradeDate.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        entryPrice = entryText.trim().toDoubleOrNull(),
                        exitPrice = exitText.trim().toDoubleOrNull(),
                        commission = commissionText.trim().toDoubleOrNull() ?: 0.0,
                        swap = swapText.trim().toDoubleOrNull() ?: 0.0,
                        otherFees = feeText.trim().toDoubleOrNull() ?: 0.0,
                        stopLossPrice = stopLossText.trim().toDoubleOrNull(),
                        takeProfitPrice = takeProfitText.trim().toDoubleOrNull(),
                        notes = notesText.trim(),
                        plannedRisk = plannedRiskText.trim().toDoubleOrNull(),
                        timeframe = timeframe,
                        marketCondition = marketCondition,
                        strategyFollowed = strategyFollowed,
                        tradeName = tradeNameText.trim(),
                    ),
                    )
                    NexNotify.tradeLogged(context, symbol)
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    nav.popBackStack()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SideButton(label: String, isBuy: Boolean, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(if (selected) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(if (isBuy) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown, null, tint = if (selected) Tx.Ink else Tx.Muted)
        Text(label, color = if (selected) Tx.Ink else Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ResultToggle(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) color else Tx.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowLabelCompact(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
