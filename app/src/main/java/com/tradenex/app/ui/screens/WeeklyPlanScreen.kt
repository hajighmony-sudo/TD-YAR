package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.DayPlan
import com.tradenex.app.data.WeeklyPlannerStore
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val weekdays = listOf(
    DayOfWeek.SATURDAY to "شنبه",
    DayOfWeek.SUNDAY to "یکشنبه",
    DayOfWeek.MONDAY to "دوشنبه",
    DayOfWeek.TUESDAY to "سه‌شنبه",
    DayOfWeek.WEDNESDAY to "چهارشنبه",
    DayOfWeek.THURSDAY to "پنجشنبه",
    DayOfWeek.FRIDAY to "جمعه",
)

private fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

@Composable
fun WeeklyPlanScreen(nav: NavController, openToday: Boolean = false) {
    var weekOffset by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(if (openToday && LocalDate.now().dayOfWeek.value in 1..5) LocalDate.now() else null) }
    val start = weekStart(LocalDate.now()).plusWeeks(weekOffset.toLong())
    val tradingDays = (2..6).map { start.plusDays(it.toLong()) }
    val weekPlans = tradingDays.map { WeeklyPlannerStore.plan(it) }
    val previousWeekStart = start.minusWeeks(1)
    val previousWeekPlans = (2..6).map { WeeklyPlannerStore.plan(previousWeekStart.plusDays(it.toLong())) }
    val previousWeekRecorded = previousWeekPlans.filter { it.confirmed }
    val confirmedCount = weekPlans.count { it.confirmed }
    val weekEnded = LocalDate.now().isAfter(start.plusDays(6))
    val recordedPlans = weekPlans.filter { it.confirmed }
    val totalTrades = weekPlans.sumOf { it.tradeCount }
    val profit = weekPlans.sumOf { it.totalProfit }
    val loss = weekPlans.sumOf { it.totalLoss }
    val net = profit - loss

    Page(title = "برنامه هفتگی", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Icons.Rounded.ChevronRight, "هفته قبل") { weekOffset-- }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("برنامه معاملاتی هفته", color = Tx.Text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text("${start} تا ${start.plusDays(6)}", color = Tx.Muted, fontSize = 10.sp)
                    }
                    IconBox(Icons.Rounded.ChevronLeft, "هفته بعد") { weekOffset++ }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    weekdays.forEachIndexed { index, (day, label) ->
                        val date = start.plusDays(index.toLong())
                        val weekend = index < 2
                        val plan = WeeklyPlannerStore.plan(date)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (weekend) Color.White.copy(alpha = 0.025f) else Tx.Gold.copy(alpha = 0.07f))
                                .clickable(enabled = !weekend) { selectedDate = date }
                                .padding(vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(label.take(2), color = if (weekend) Tx.Muted else Tx.Text, fontSize = 10.sp,
                                textDecoration = if (weekend) TextDecoration.LineThrough else TextDecoration.None)
                            Text(date.dayOfMonth.toString(), color = if (weekend) Tx.Muted else Tx.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                textDecoration = if (weekend) TextDecoration.LineThrough else TextDecoration.None)
                            if (weekend) Text("تعطیل", color = Tx.Muted, fontSize = 8.sp)
                            else Box(Modifier.size(6.dp).clip(CircleShape).background(if (plan.confirmed) Tx.Green else Tx.Gold.copy(alpha = 0.55f)))
                        }
                    }
                }
            }
        }

        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("پیشرفت برنامه", "${confirmedCount} از ۵ روز ثبت و تأیید شده")
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                        Box(Modifier.fillMaxWidth(confirmedCount / 5f).height(7.dp).clip(CircleShape).background(Tx.Green))
                    }
                    Text("برای ثبت عملکرد، روی یکی از روزهای دوشنبه تا جمعه بزن.", color = Tx.Muted, fontSize = 11.sp)
                }
            }
        }

        if (recordedPlans.isNotEmpty()) {
            Reveal(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val nets = recordedPlans.map { it.totalProfit - it.totalLoss }
                        val winDays = nets.count { it > 0 }
                        SectionTitle("تحلیل روزهای ثبت‌شده", "${recordedPlans.size} روز")
                        Text("روزهای سودده: $winDays از ${recordedPlans.size}", color = Tx.Text, fontSize = 12.sp)
                        Text("بهترین روز: ${money(nets.max())}  ·  بدترین روز: ${money(nets.min())}", color = Tx.Muted, fontSize = 12.sp)
                        Text("پایبندی به پلن: ${recordedPlans.count { it.followedPlan }} از ${recordedPlans.size} روز", color = Tx.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        if (weekOffset == 0 && LocalDate.now().dayOfWeek.value in 6..7) {
            Reveal(2) {
                GlassCard(Modifier.fillMaxWidth(), border = Tx.Green) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("گزارش خودکار هفته گذشته", "خلاصه عملکرد هفته به پایان رسیده")
                        val prevTrades = previousWeekRecorded.sumOf { it.tradeCount }
                        val prevProfit = previousWeekRecorded.sumOf { it.totalProfit }
                        val prevLoss = previousWeekRecorded.sumOf { it.totalLoss }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryValue("معاملات", prevTrades.toString(), Tx.Blue)
                            SummaryValue("مجموع سود", money(prevProfit), Tx.Green)
                            SummaryValue("مجموع ضرر", money(prevLoss), Tx.Red)
                        }
                        Text("نتیجه خالص: ${money(prevProfit - prevLoss)} · روزهای ثبت‌شده: ${previousWeekRecorded.size}/5",
                            color = if (prevProfit >= prevLoss) Tx.Green else Tx.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (previousWeekRecorded.isEmpty()) {
                            Text("هفته گذشته گزارشی ثبت نشده؛ از این هفته عملکرد روزانه را ثبت کن تا تحلیل دقیق‌تری داشته باشی.", color = Tx.Muted, fontSize = 11.sp)
                        } else if (previousWeekRecorded.any { !it.followedPlan }) {
                            Text("پیشنهاد: روزهایی که از پلن خارج شدی را بررسی و علت را در یادداشت همان روز مرور کن.", color = Tx.Muted, fontSize = 11.sp)
                        } else {
                            Text("پیشنهاد: قوانین معاملاتی و کیفیت ستاپ‌های موفق را برای هفته جدید حفظ کن.", color = Tx.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (confirmedCount > 0) {
            Reveal(3) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        SectionTitle(if (confirmedCount == 5 || weekEnded) "گزارش نهایی هفته" else "خلاصه تا این لحظه",
                            when {
                                confirmedCount == 5 -> "همه روزهای معاملاتی ثبت شده‌اند"
                                weekEnded -> "هفته تمام شده؛ روزهای ثبت‌نشده با داده صفر محاسبه شده‌اند"
                                else -> "گزارش با ثبت روزهای بعد به‌روزرسانی می‌شود"
                            })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryValue("تعداد معاملات", totalTrades.toString(), Tx.Blue)
                            SummaryValue("مجموع سود", money(profit), Tx.Green)
                            SummaryValue("مجموع ضرر", money(loss), Tx.Red)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سود/زیان خالص", color = Tx.Muted, fontSize = 12.sp)
                            Text(money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (confirmedCount == 5 || weekEnded) {
                            val adherenceDays = recordedPlans.count { it.followedPlan }
                            val averageMood = if (recordedPlans.isEmpty()) 0.0 else recordedPlans.map { it.mood }.average()
                            val checklistDone = recordedPlans.sumOf { it.doneTasks.size }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SummaryValue("پایبندی به پلن", "$adherenceDays/5 روز", if (adherenceDays >= 4) Tx.Green else Tx.Red)
                                SummaryValue("میانگین تمرکز", if (recordedPlans.isEmpty()) "—" else String.format(java.util.Locale.US, "%.1f/5", averageMood), Tx.Blue)
                                SummaryValue("چک‌لیست", "$checklistDone/30", if (checklistDone >= 24) Tx.Green else Tx.Gold)
                            }
                            if (totalTrades > 0) {
                                Text("میانگین خالص به‌ازای هر معامله: ${money(net / totalTrades)}", color = Tx.Muted, fontSize = 11.sp)
                            }
                            val tips = buildList {
                                if (net < 0) add("این هفته زیان‌ده بوده؛ حجم معاملات و کیفیت ورودها را بازبینی کن.")
                                if (loss > profit * 0.7) add("نسبت ضرر به سود بالاست؛ حد ضرر و نسبت ریسک به بازده را بررسی کن.")
                                if (totalTrades > 25) add("تعداد معاملات زیاد است؛ مراقب بیش‌معامله‌گری باش.")
                                if (recordedPlans.count { it.followedPlan } < 4) add("پایبندی به پلن در کمتر از چهار روز بوده؛ قبل از ورود قوانینت را مرور کن.")
                                if (recordedPlans.isNotEmpty() && recordedPlans.map { it.mood }.average() < 3.0) add("میانگین تمرکز یا حال روانی پایین بوده؛ کیفیت تصمیم‌گیری را بر تعداد معاملات مقدم کن.")
                                if (recordedPlans.sumOf { it.doneTasks.size } < 20) add("چک‌لیست انضباطی کامل نشده؛ برای هر روز یک روال ثابت قبل و بعد از معامله بساز.")
                                if (weekPlans.any { it.tradeCount == 0 && it.noTradeReason.isBlank() }) add("برای روزهای بدون معامله، دلیل مشخص ثبت کن.")
                                if (isEmpty()) add("عملکرد هفته مثبت و منظم بوده؛ قوانین فعلی را حفظ و کیفیت ستاپ‌ها را بررسی کن.")
                            }
                            tips.forEach { Text("• $it", color = Tx.Text, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }

        if (selectedDate != null) {
            DayEditor(selectedDate!!, onClose = { selectedDate = null })
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DayEditor(date: LocalDate, onClose: () -> Unit) {
    val dayName = weekdays.firstOrNull { it.first == date.dayOfWeek }?.second ?: ""
    val initial = WeeklyPlannerStore.plan(date)
    var trades by remember(date) { mutableStateOf(initial.tradeCount) }
    var profit by remember(date) { mutableStateOf(if (initial.totalProfit == 0.0) "" else initial.totalProfit.toString()) }
    var loss by remember(date) { mutableStateOf(if (initial.totalLoss == 0.0) "" else initial.totalLoss.toString()) }
    var goal by remember(date) { mutableStateOf(initial.dailyGoal) }
    var notes by remember(date) { mutableStateOf(initial.notes) }
    var reason by remember(date) { mutableStateOf(initial.noTradeReason) }
    var risk by remember(date) { mutableStateOf(initial.maxRiskPercent.toString()) }
    var mood by remember(date) { mutableStateOf(initial.mood) }
    var followed by remember(date) { mutableStateOf(initial.followedPlan) }
    var tasks by remember(date) { mutableStateOf(initial.doneTasks) }

    GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("عملکرد $dayName", date.toString())
            Text("مقدار معاملات", color = Tx.Muted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconBox(Icons.Rounded.Remove, "کاهش تعداد") { trades = (trades - 1).coerceAtLeast(0) }
                Text(trades.toString(), color = Tx.Text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 24.dp))
                IconBox(Icons.Rounded.Add, "افزایش تعداد") { trades = (trades + 1).coerceAtMost(100) }
            }
            Text("مجموع سود", color = Tx.Muted, fontSize = 12.sp)
            TxField(profit, { profit = it }, "مثلاً 250.50")
            Text("مجموع ضرر", color = Tx.Muted, fontSize = 12.sp)
            TxField(loss, { loss = it }, "مثلاً 80")
            Text("هدف امروز", color = Tx.Muted, fontSize = 12.sp)
            TxField(goal, { goal = it }, "مثلاً فقط ستاپ‌های A+")
            Text("حداکثر ریسک روزانه (%)", color = Tx.Muted, fontSize = 12.sp)
            TxField(risk, { risk = it }, "مثلاً 1")
            if (trades == 0) {
                Text("امروز معامله‌ای نداشتی؛ دلیلش را ثبت کن تا گزارش هفتگی دقیق‌تر شود.", color = Tx.Gold, fontSize = 12.sp)
                WeeklyPlannerStore.noTradeReasons.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { option ->
                            TxChip(option, reason.contains(option), { reason = option }, Modifier.weight(1f))
                        }
                    }
                }
                TxField(reason, { reason = it }, "توضیح بیشتر یا دلیل شخصی")
            }
            Text("چک‌لیست انضباط معاملاتی", color = Tx.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            WeeklyPlannerStore.checklist.forEach { task ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task in tasks, onCheckedChange = { checked ->
                        tasks = if (checked) tasks + task else tasks - task
                    })
                    Text(task, color = Tx.Muted, fontSize = 11.sp)
                }
            }
            Text("پایبندی به پلن معاملاتی", color = Tx.Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { followed = true }) { Text(if (followed) "✓ پایبند بودم" else "پایبند بودم", color = if (followed) Tx.Green else Tx.Muted) }
                TextButton(onClick = { followed = false }) { Text(if (!followed) "✓ خارج از پلن" else "خارج از پلن", color = if (!followed) Tx.Red else Tx.Muted) }
            }
            Text("حال و تمرکز امروز: $mood از ۵", color = Tx.Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { value ->
                    Box(Modifier.size(38.dp).clip(CircleShape).background(if (mood == value) Tx.Gold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .clickable { mood = value }, contentAlignment = Alignment.Center) {
                        Text(value.toString(), color = if (mood == value) Tx.Gold else Tx.Text, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("یادداشت روز", color = Tx.Muted, fontSize = 12.sp)
            TxField(notes, { notes = it }, "تحلیل، احساسات، اشتباهات یا نکات مهم")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("انصراف", color = Tx.Muted) }
                TextButton(onClick = {
                    WeeklyPlannerStore.save(DayPlan(
                        date = date.toString(),
                        tradeCount = trades,
                        totalProfit = profit.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        totalLoss = loss.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        dailyGoal = goal,
                        maxRiskPercent = risk.toDoubleOrNull()?.coerceIn(0.1, 10.0) ?: 1.0,
                        notes = notes,
                        noTradeReason = reason,
                        mood = mood,
                        followedPlan = followed,
                        confirmed = true,
                        doneTasks = tasks,
                    ))
                    onClose()
                }) { Text("ثبت و تأیید روز", color = Tx.Gold, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(label, color = Tx.Muted, fontSize = 9.sp)
    }
}

private fun money(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
