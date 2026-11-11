package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.ForexFactoryCalendarStore
import com.tradenex.app.data.ForexFactoryEvent
import com.tradenex.app.data.JalaliCalendar
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val jalaliMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

/** Gregorian -> Jalali (year, month, day). */
fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val value = JalaliCalendar.fromGregorian(LocalDate.of(gy, gm, gd))
    return Triple(value.year, value.month, value.day)
}

private fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

fun jalaliText(d: LocalDate): String {
    val (y, m, day) = toJalali(d.year, d.monthValue, d.dayOfMonth)
    return "${fa(day)} ${jalaliMonths[m - 1]} ${fa(y)}"
}

@Composable
fun CalendarScreen(nav: NavController) {
    var tab by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf(0) }
    var remind by remember { mutableStateOf(setOf<Int>()) }
    val context = LocalContext.current
    var refreshCounter by remember { mutableStateOf(0) }
    var selectedNewsEvent by remember { mutableStateOf<ForexFactoryEvent?>(null) }
    var newsEvents by remember { mutableStateOf(emptyList<ForexFactoryEvent>()) }
    var newsLoading by remember { mutableStateOf(true) }
    var newsMessage by remember { mutableStateOf("در حال اتصال به Forex Factory…") }
    var newsFetchedAt by remember { mutableStateOf(0L) }

    LaunchedEffect(refreshCounter) {
        newsLoading = true
        val result = ForexFactoryCalendarStore.load(context, forceRefresh = refreshCounter > 0)
        newsEvents = result.events
        newsFetchedAt = result.fetchedAtMillis
        newsMessage = result.message
        newsLoading = false
    }

    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    val todayJalali = JalaliCalendar.fromGregorian(today)
    val monthIndex = todayJalali.year * 12 + todayJalali.month - 1 + offset
    val selectedJalaliYear = Math.floorDiv(monthIndex, 12)
    val selectedJalaliMonth = Math.floorMod(monthIndex, 12) + 1
    val trades = AppState.visibleTrades.toList()
    val dayPnl = remember(trades) { Analytics.dayPnl(trades) }
    val selectedTrades = remember(trades, selectedDate) {
        trades.filter { it.epochDay == selectedDate.toEpochDay() }.sortedBy { it.openedAtMillis }
    }
    val settings = AppState.settings

    fun moveMonth(delta: Int) {
        val nextOffset = offset + delta
        val nextIndex = todayJalali.year * 12 + todayJalali.month - 1 + nextOffset
        val nextYear = Math.floorDiv(nextIndex, 12)
        val nextMonth = Math.floorMod(nextIndex, 12) + 1
        offset = nextOffset
        selectedDate = JalaliCalendar.toGregorian(nextYear, nextMonth, 1)
    }

    Page(title = "تقویم", onBack = { nav.popBackStack() }, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Rounded.Refresh, "تازه‌سازی اخبار") { if (!newsLoading) refreshCounter++ }
            TxChip("امروز", false, { offset = 0; selectedDate = today })
        }
    }) {
        Reveal(0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxChip("تقویم معامله‌ها", tab == 0, { tab = 0 }, Modifier.weight(1f))
                TxChip("تقویم خبری", tab == 1, { tab = 1 }, Modifier.weight(1f))
            }
        }
        Text("امروز: ${jalaliText(today)}", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        if (tab == 0) {
            Reveal(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBox(Icons.Rounded.KeyboardArrowRight, "ماه قبل") { moveMonth(-1) }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    jalaliMonths[selectedJalaliMonth - 1] + " " + fa(selectedJalaliYear),
                                    color = Tx.Text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                val firstGregorian = JalaliCalendar.toGregorian(selectedJalaliYear, selectedJalaliMonth, 1)
                                Text(
                                    firstGregorian.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + firstGregorian.year,
                                    color = Tx.Muted,
                                    fontSize = 11.sp,
                                )
                            }
                            IconBox(Icons.Rounded.KeyboardArrowLeft, "ماه بعد") { moveMonth(1) }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            listOf("شنبه", "یک", "دوش", "سه", "چهار", "پنج", "جمعه").forEachIndexed { i, weekday ->
                                Text(
                                    weekday,
                                    color = if (i < 2) Tx.Muted.copy(alpha = 0.6f) else Tx.Muted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        val first = JalaliCalendar.toGregorian(selectedJalaliYear, selectedJalaliMonth, 1)
                        val daysInJalaliMonth = JalaliCalendar.daysInMonth(selectedJalaliYear, selectedJalaliMonth)
                        val lead = Analytics.column(first.toEpochDay())
                        val rows = (lead + daysInJalaliMonth + 6) / 7
                        for (r in 0 until rows) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0..6) {
                                    val day = r * 7 + c - lead + 1
                                    if (day in 1..daysInJalaliMonth) {
                                        val date = JalaliCalendar.toGregorian(selectedJalaliYear, selectedJalaliMonth, day)
                                        DayCell(
                                            day = day,
                                            isToday = date == today,
                                            selected = date == selectedDate,
                                            holiday = settings.weekendHoliday && c < 2,
                                            pnl = dayPnl[date.toEpochDay()],
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedDate = date },
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("معاملات ${jalaliText(selectedDate)}", color = Tx.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (selectedTrades.isEmpty()) {
                            Text("برای این روز معامله‌ای ثبت نشده است.", color = Tx.Muted, fontSize = 11.sp)
                        } else {
                            selectedTrades.forEach { trade ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        trade.tradeName.ifBlank { trade.symbol } + " · " + trade.symbol + " · " + if (trade.buy) "خرید" else "فروش",
                                        color = Tx.Text,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        Analytics.money(trade.pnl),
                                        color = if (trade.pnl >= 0) Tx.Green else Tx.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Reveal(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBox(Icons.Rounded.KeyboardArrowRight, "ماه قبل") { moveMonth(-1) }
                            Text(jalaliMonths[selectedJalaliMonth - 1] + " " + fa(selectedJalaliYear), color = Tx.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            IconBox(Icons.Rounded.KeyboardArrowLeft, "ماه بعد") { moveMonth(1) }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEachIndexed { i, label ->
                                Text(label, color = if (i < 2) Tx.Muted.copy(alpha = 0.45f) else Tx.Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                        val firstNewsDay = JalaliCalendar.toGregorian(selectedJalaliYear, selectedJalaliMonth, 1)
                        val newsDays = JalaliCalendar.daysInMonth(selectedJalaliYear, selectedJalaliMonth)
                        val newsLead = Analytics.column(firstNewsDay.toEpochDay())
                        val newsRows = (newsLead + newsDays + 6) / 7
                        for (row in 0 until newsRows) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (col in 0..6) {
                                    val day = row * 7 + col - newsLead + 1
                                    if (day in 1..newsDays) {
                                        val date = JalaliCalendar.toGregorian(selectedJalaliYear, selectedJalaliMonth, day)
                                        val isWeekend = col < 2
                                        val count = newsEvents.count { Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date }
                                        Column(
                                            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(9.dp))
                                                .background(if (date == selectedDate) Tx.Gold.copy(alpha = 0.22f) else Color.Transparent)
                                                .clickable { selectedDate = date }.padding(vertical = 3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(fa(day), color = if (date == selectedDate) Tx.Gold else if (isWeekend) Tx.Muted.copy(alpha = 0.45f) else Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (isWeekend) Text("تعطیل", color = Tx.Muted.copy(alpha = 0.5f), fontSize = 7.sp)
                                            else if (count > 0) Text("• $count", color = Tx.Green, fontSize = 8.sp)
                                        }
                                    } else Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            run {
                val nowMs = System.currentTimeMillis()
                val next = newsEvents.filter { it.impact == 2 && it.timestampMillis > nowMs }.minByOrNull { it.timestampMillis }
                if (next != null) {
                    val mins = ((next.timestampMillis - nowMs) / 60000).toInt()
                    val txt = if (mins >= 60) "${mins / 60} ساعت و ${mins % 60} دقیقه" else "$mins دقیقه"
                    Reveal(2) {
                        GlassCard(Modifier.fillMaxWidth(), border = Tx.Red.copy(alpha = 0.5f)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("خبر مهم بعدی (${next.currency})", color = Tx.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(next.title, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                Text("$txt دیگر · قبل از آن معامله‌ی تازه باز نکن یا ریسک را کم کن", color = Tx.Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Reveal(2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("همه", "High", "USD", "EUR", "GBP", "JPY").forEachIndexed { i, label ->
                        TxChip(label, filter == i, { filter = i }, Modifier.weight(1f))
                    }
                }
            }
            Reveal(2) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionTitle("تقویم اقتصادی Forex Factory", "هفته جاری · ساعت محلی")
                        Text(newsMessage, color = if (newsEvents.isNotEmpty()) Tx.Muted else Tx.Gold, fontSize = 10.sp)
                        if (newsFetchedAt > 0L) {
                            val updated = Instant.ofEpochMilli(newsFetchedAt)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
                            Text("آخرین دریافت: " + updated, color = Tx.Muted, fontSize = 10.sp)
                        }

                        val minImpact = when (settings.newsImpact) {
                            1 -> 1
                            2 -> 2
                            else -> 0
                        }
                        val weekendClosed = selectedDate.dayOfWeek.value == 6 || selectedDate.dayOfWeek.value == 7
                        val list = newsEvents.filter { event ->
                            Instant.ofEpochMilli(event.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate &&
                                !weekendClosed && event.impact >= minImpact && when (filter) {
                                1 -> event.impact == 2
                                2 -> event.currency == "USD"
                                3 -> event.currency == "EUR"
                                4 -> event.currency == "GBP"
                                5 -> event.currency == "JPY"
                                else -> true
                            }
                        }

                        if (newsLoading && newsEvents.isEmpty()) {
                            Text(
                                "در حال دریافت رویدادهای اقتصادی…",
                                color = Tx.Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 18.dp),
                            )
                        } else if (list.isEmpty()) {
                            Text(
                                if (newsEvents.isEmpty()) "داده‌ای دریافت نشد؛ اینترنت را بررسی و دوباره تازه‌سازی کن."
                                else "برای این فیلتر رویدادی پیدا نشد.",
                                color = Tx.Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                        } else {
                            list.forEach { event ->
                                val impactColor = when (event.impact) {
                                    2 -> Tx.Red
                                    1 -> Tx.Gold
                                    -1 -> Tx.Blue
                                    else -> Tx.Muted
                                }
                                val local = Instant.ofEpochMilli(event.timestampMillis).atZone(ZoneId.systemDefault())
                                val time = local.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
                                val dateLabel = jalaliText(local.toLocalDate())
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { selectedNewsEvent = event },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(Modifier.width(66.dp)) {
                                        Text(time, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(dateLabel, color = Tx.Muted, fontSize = 9.sp)
                                    }
                                    Box(
                                        Modifier.width(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.07f))
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(event.currency, color = Tx.Text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(event.title, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                        Text(
                                            when (event.impact) {
                                                2 -> "تأثیر بالا"
                                                1 -> "تأثیر متوسط"
                                                -1 -> "تعطیلی"
                                                else -> "تأثیر کم"
                                            },
                                            color = impactColor,
                                            fontSize = 10.sp,
                                        )
                                        Text(
                                            "قبلی: " + event.previous.ifBlank { "—" } +
                                                " · پیش‌بینی: " + event.forecast.ifBlank { "—" } +
                                                " · واقعی: " + event.actual.ifBlank { "—" },
                                            color = Tx.Muted,
                                            fontSize = 9.sp,
                                            maxLines = 2,
                                        )
                                    }
                                    val hasReminder = event.id in remind
                                    Icon(
                                        if (hasReminder) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                                        "یادآور",
                                        tint = if (hasReminder) Tx.Gold else Tx.Muted,
                                        modifier = Modifier.size(22.dp).clickable {
                                            val ok = com.tradenex.app.reminders.ReminderScheduler.scheduleNews(
                                                context, event.id, event.title, event.currency, event.timestampMillis, !hasReminder,
                                            )
                                            if (ok) remind = if (hasReminder) remind - event.id else remind + event.id
                                            else android.widget.Toast.makeText(context, "زمان این خبر (منهای ۱۵ دقیقه) گذشته است.", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                            }
                        }
                    }
                }
            }
            Text(
                "منبع: فید هفتگی Forex Factory؛ داده‌ها محلی ذخیره می‌شوند و ساعت رویدادها با منطقه زمانی دستگاه نمایش داده می‌شود.",
                color = Tx.Muted,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    selected: Boolean,
    holiday: Boolean,
    pnl: Double?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(11.dp)
    val base = modifier.height(42.dp).clip(shape).clickable(onClick = onClick)
    val styled = when {
        isToday -> base.background(Brush.verticalGradient(listOf(Tx.GoldHi, Tx.GoldDeep)))
        holiday -> base.border(1.dp, Color.White.copy(alpha = 0.10f), shape)
        else -> base.background(Color.White.copy(alpha = 0.04f))
    }.then(if (selected && !isToday) Modifier.border(1.dp, Tx.Gold, shape) else Modifier)
    Column(styled, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            fa(day),
            color = when {
                isToday -> Tx.Ink
                holiday -> Tx.Muted.copy(alpha = 0.6f)
                else -> Tx.Text
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (pnl == null) Color.Transparent else if (pnl >= 0) Tx.Green else Tx.Red))
    }
}
