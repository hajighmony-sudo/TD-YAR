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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var buy by remember { mutableStateOf(true) }
    var sideChosen by remember { mutableStateOf(false) }
    var limitConfirmed by remember { mutableStateOf(false) }
    var symbol by remember { mutableStateOf("") }
    var lot by remember { mutableStateOf(settings.defaultLot.coerceAtLeast(0.0)) }
    var lotText by remember { mutableStateOf(if (settings.defaultLot > 0.0) String.format(java.util.Locale.US, "%.2f", settings.defaultLot) else "") }
    var win by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf(0.0) }
    var amountText by remember { mutableStateOf("") }
    var resultChosen by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var usePriceCalculation by remember { mutableStateOf(false) }
    var entryText by remember { mutableStateOf("") }
    var exitText by remember { mutableStateOf("") }
    var conversionText by remember { mutableStateOf(if (AppState.selectedAccount.baseCurrency.isNotBlank() && symbol.endsWith(AppState.selectedAccount.baseCurrency)) "1.0" else "") }
    LaunchedEffect(symbol, AppState.selectedAccountId) {
        conversionText = if (AppState.selectedAccount.baseCurrency.isNotBlank() && symbol.endsWith(AppState.selectedAccount.baseCurrency)) "1.0" else ""
    }
    var commissionText by remember { mutableStateOf("0") }
    var swapText by remember { mutableStateOf("0") }
    var feeText by remember { mutableStateOf("0") }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var tradeNameText by remember { mutableStateOf("") }
    var tradeDateText by remember { mutableStateOf("") }
    var tradeTimeText by remember { mutableStateOf("") }
    var timeframe by remember { mutableStateOf("") }
    var marketCondition by remember { mutableStateOf("") }
    var plannedRiskText by remember { mutableStateOf("") }
    val parsedTradeDate = remember(tradeDateText) { JalaliCalendar.parseDateInput(tradeDateText) }
    val parsedTradeTime = remember(tradeTimeText) { runCatching { java.time.LocalTime.parse(tradeTimeText.trim()) }.getOrNull() }
    var strategy by remember { mutableStateOf("") }
    var strategyFollowed by remember { mutableStateOf<Boolean?>(null) }
    var emotion by remember { mutableStateOf("") }
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
    val signed = if (usePriceCalculation) priceResult?.netAccountCurrency?.toDouble() ?: 0.0 else if (!resultChosen) 0.0 else if (win) amount else -amount
    val animated by animateFloatAsState(signed.toFloat(), tween(450), label = "net")
    val resColor = if (signed >= 0.0) Tx.Green else Tx.Red
    val riskUsd = AppState.selectedAccount.startingBalance * settings.riskPercent / 100.0
    val overRisk = if (usePriceCalculation) signed < -riskUsd else !win && amount > riskUsd
    val existingToday = AppState.visibleTrades.filter { it.epochDay == parsedTradeDate?.toEpochDay() }
    val realizedLossToday = existingToday.sumOf { if (it.pnl < 0.0) -it.pnl else 0.0 }
    val dailyLossLimitCash = AppState.selectedAccount.startingBalance * settings.dailyLossLimitPercent / 100.0
    val projectedDailyLoss = realizedLossToday + if (signed < 0.0) -signed else 0.0
    val exceedsDailyLoss = projectedDailyLoss > dailyLossLimitCash
    val exceedsTradeCount = existingToday.size >= settings.maxTradesPerDay

    Page(title = "ثبت معامله", onBack = { nav.popBackStack() }) {
        SectionHero("TRADE DESK / ENTRY", "ثبت دقیق، مرور هوشمند", "جزئیات معامله، ریسک برنامه‌ریزی‌شده، شرایط بازار و ژورنال شخصی را یک‌جا ثبت کن.")
        if (AppState.accounts.none { !it.archived }) {
            GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold.copy(alpha = 0.45f)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("ابتدا حساب خودت را ثبت کن", color = Tx.Gold, fontWeight = FontWeight.ExtraBold)
                    Text("تا زمانی که حساب و ارز پایه را وارد نکنی، معامله‌ای با موجودی فرضی ثبت نمی‌شود.", color = Tx.Muted, fontSize = 11.sp)
                    GoldButton("رفتن به بخش حساب‌ها") { nav.navigate("accounts") }
                }
            }
        }
        Reveal(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideButton("خرید (Buy)", true, sideChosen && buy, Tx.Green, Modifier.weight(1f)) { buy = true; sideChosen = true }
                SideButton("فروش (Sell)", false, sideChosen && !buy, Tx.Red, Modifier.weight(1f)) { buy = false; sideChosen = true }
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
                    TxField(lotText, { value ->
                        lotText = value
                        lot = value.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    }, "حجم لات را تایپ کن")
                    Stepper(
                        valueText = String.format(java.util.Locale.US, "%.2f", lot),
                        onMinus = { lot = stepLot(lot, -1); lotText = String.format(java.util.Locale.US, "%.2f", lot) },
                        onPlus = { lot = stepLot(lot, 1); lotText = String.format(java.util.Locale.US, "%.2f", lot) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lotPresets.forEach { TxChip(it.toString(), lot == it, { lot = it; lotText = String.format(java.util.Locale.US, "%.2f", it) }) }
                    }
                    Text("تاریخ معامله", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    TxField(tradeDateText, { tradeDateText = it }, "میلادی YYYY-MM-DD یا شمسی 1405/08/17")
                    if (parsedTradeDate != null) {
                        Text("تاریخ شمسی: " + jalaliText(parsedTradeDate), color = Tx.Gold, fontSize = 10.sp)
                    }
                    Text("ساعت بازشدن معامله", color = Tx.Muted, fontSize = 11.sp)
                    TxField(tradeTimeText, { tradeTimeText = it }, "ساعت معامله را وارد کن؛ HH:mm")
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
                            ResultToggle("سود (Profit)", resultChosen && win, Tx.Green, Modifier.weight(1f)) { win = true; resultChosen = true }
                            ResultToggle("ضرر (Loss)", resultChosen && !win, Tx.Red, Modifier.weight(1f)) { win = false; resultChosen = true }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!usePriceCalculation) {
                        TxField(amountText, { value ->
                            amountText = value
                            amount = value.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                        }, "مبلغ سود یا ضرر را تایپ کن")
                        Stepper(
                            valueText = AppState.selectedAccount.baseCurrency + " " + String.format(java.util.Locale.US, "%,.2f", amount),
                            onMinus = { amount = (amount - 10).coerceAtLeast(0.0); amountText = if (amount == 0.0) "" else String.format(java.util.Locale.US, "%.2f", amount) },
                            onPlus = { amount += 10; amountText = String.format(java.util.Locale.US, "%.2f", amount) },
                            valueColor = resColor,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            amountPresets.forEach { p ->
                                TxChip("+${p.toInt()}", false, { amount += p; amountText = String.format(java.util.Locale.US, "%.2f", amount) })
                            }
                        }
                        Text("مبلغ را می‌توانی مستقیم تایپ کنی یا با کنترل‌ها تغییر بدهی.", color = Tx.Muted, fontSize = 10.sp)
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
        if (exceedsDailyLoss) {
            Text(
                "هشدار سقف ضرر روزانه: ضرر ثبت‌شده به‌همراه این نتیجه حدود ${String.format(java.util.Locale.US, "%.2f", projectedDailyLoss)} است؛ سقف تنظیم‌شده ${settings.dailyLossLimitPercent}٪ بالانس است.",
                color = Tx.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (exceedsTradeCount) {
            Text(
                "هشدار تعداد معاملات: امروز ${existingToday.size} معامله ثبت شده و سقف شخصی تو ${settings.maxTradesPerDay} معامله است.",
                color = Tx.Gold,
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
            if (saveMessage.isNotBlank()) Text(saveMessage, color = Tx.Red, fontSize = 11.sp)
            GoldButton("ذخیره معامله در دفترچه") {
                when {
                    AppState.accounts.none { !it.archived } -> saveMessage = "ابتدا حساب معاملاتی خودت را ایجاد کن."
                    !sideChosen -> saveMessage = "جهت معامله (خرید یا فروش) را انتخاب کن."
                    symbol.isBlank() -> saveMessage = "نماد یا نام دارایی را وارد کن."
                    lot <= 0.0 || !lot.isFinite() -> saveMessage = "حجم معامله را وارد کن."
                    parsedTradeDate == null -> saveMessage = "تاریخ معامله معتبر نیست."
                    parsedTradeTime == null -> saveMessage = "ساعت معامله معتبر نیست."
                    usePriceCalculation && priceResult == null -> saveMessage = "قیمت ورود، خروج و نرخ تبدیل را کامل و معتبر وارد کن."
                    !usePriceCalculation && (!resultChosen || amount <= 0.0) -> saveMessage = "نوع نتیجه و مبلغ سود یا ضرر را مشخص کن."
                    com.tradenex.app.ui.components.CandleStyle.limitBreached() && !limitConfirmed -> {
                        limitConfirmed = true
                        saveMessage = "حد ضرر امروز پر شده است. اگر مطمئنی، دوباره «ذخیره» را بزن."
                    }
                    else -> {
                        val saved = runCatching {
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
                        }
                        saved.onSuccess {
                            NexNotify.tradeLogged(context, symbol)
                            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            nav.popBackStack()
                        }.onFailure { saveMessage = it.message ?: "ثبت معامله انجام نشد." }
                    }
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
