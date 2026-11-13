package com.tradenex.app.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.Tx
import java.time.LocalDate

/** Colour behaviour of the candle navigation bar. Auto = green on a profitable day, red on a losing day, gold when flat. */
enum class CandleMode(val key: String) { Auto("auto"), Green("green"), Red("red"), Gold("gold") }

object CandleStyle {
    val Green = Color(0xFF2EE59D)
    val Red = Color(0xFFFF4D6D)
    val Gold = Color(0xFFF5C451)

    var mode by mutableStateOf(CandleMode.Auto)
    /** Daily profit goal and daily loss limit (0 = off). */
    var dailyGoal by mutableStateOf(0.0)
    var dailyLimit by mutableStateOf(0.0)
    private var loaded = false

    fun setGoal(context: Context, v: Double) {
        dailyGoal = v
        context.getSharedPreferences("tradix_ui", Context.MODE_PRIVATE).edit().putFloat("daily_goal", v.toFloat()).apply()
    }

    fun setLimit(context: Context, v: Double) {
        dailyLimit = v
        context.getSharedPreferences("tradix_ui", Context.MODE_PRIVATE).edit().putFloat("daily_limit", v.toFloat()).apply()
    }

    /** True once today's loss has reached the limit the user set. */
    fun limitBreached(): Boolean = dailyLimit > 0 && todayPnl() <= -dailyLimit

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val sp = context.getSharedPreferences("tradix_ui", Context.MODE_PRIVATE)
        dailyGoal = sp.getFloat("daily_goal", 0f).toDouble()
        dailyLimit = sp.getFloat("daily_limit", 0f).toDouble()
        val k = sp.getString("candle_mode", "auto")
        mode = CandleMode.values().firstOrNull { it.key == k } ?: CandleMode.Auto
    }

    fun set(context: Context, m: CandleMode) {
        mode = m
        context.getSharedPreferences("tradix_ui", Context.MODE_PRIVATE).edit().putString("candle_mode", m.key).apply()
    }

    fun todayPnl(): Double {
        val today = LocalDate.now().toEpochDay()
        return AppState.visibleTrades.filter { it.epochDay == today }.sumOf { it.pnl }
    }

    fun resolve(): Color = when (mode) {
        CandleMode.Green -> Green
        CandleMode.Red -> Red
        CandleMode.Gold -> Gold
        CandleMode.Auto -> todayPnl().let { if (it > 0) Green else if (it < 0) Red else Gold }
    }
}

private val moreRoutes = setOf("more", "sessions", "strategies", "calendar", "settings", "tags", "accounts", "trades", "weekly", "weekly_reminder", "calculator", "risk_lab", "metatrader", "tv_share", "insights", "tx_ai", "tx_ai_legacy", "positions", "psychology")

/** Bottom navigation drawn as a horizontal glass candle (body + wicks); the colour follows the day's result or the user's pick. */
@Composable
fun CandleNavBar(current: String, onNavigateRaw: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val onNavigate: (String) -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateRaw(it) }
    val breached = CandleStyle.limitBreached()
    val pulse by rememberInfiniteTransition(label = "limit").animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    val borderAlpha = if (breached) pulse else 0.75f
    val todayNow = CandleStyle.todayPnl()
    LaunchedEffect(Unit) { CandleStyle.load(context) }
    val c by animateColorAsState(CandleStyle.resolve(), tween(450), label = "candle")
    var palette by remember { mutableStateOf(false) }
    val selected = when {
        current == "home" -> 0
        current == "perf" -> 1
        current == "edu" -> 3
        current in moreRoutes -> 4
        else -> -1
    }
    val shape = RoundedCornerShape(22.dp)

    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).navigationBarsPadding().height(76.dp)) {
        // wicks
        Box(Modifier.align(Alignment.CenterStart).width(14.dp).height(2.dp).background(c))
        Box(Modifier.align(Alignment.CenterStart).width(2.dp).height(24.dp).background(c))
        Box(Modifier.align(Alignment.CenterEnd).width(14.dp).height(2.dp).background(c))
        Box(Modifier.align(Alignment.CenterEnd).width(2.dp).height(24.dp).background(c))

        // body
        BoxWithConstraints(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(62.dp)
                .shadow(16.dp, shape, ambientColor = c, spotColor = c)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.5f), c.copy(alpha = 0.16f))))
                .border(if (breached) 2.dp else 1.dp, (if (breached) CandleStyle.Red else c).copy(alpha = borderAlpha), shape),
        ) {
            val slotW = maxWidth / 5
            val knobX by animateDpAsState(slotW * (if (selected < 0) 0 else selected), spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "knob")
            if (selected >= 0) {
                Box(
                    Modifier
                        .offset(x = knobX + 5.dp, y = 5.dp)
                        .width(slotW - 10.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.1f))))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(17.dp)),
                )
            }
            Row(Modifier.fillMaxSize()) {
                Slot("خانه", Icons.Rounded.Home, selected == 0, Modifier.weight(1f)) { onNavigate("home") }
                Slot("گزارش‌ها", Icons.Rounded.BarChart, selected == 1, Modifier.weight(1f)) { onNavigate("perf") }
                Box(Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNavigate("add") }, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(38.dp).shadow(10.dp, CircleShape, ambientColor = c, spotColor = c).clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.78f)))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "ثبت معامله جدید", tint = c, modifier = Modifier.size(24.dp)) }
                }
                Slot("آموزش", Icons.Rounded.MenuBook, selected == 3, Modifier.weight(1f)) { onNavigate("edu") }
                Slot("بیشتر", Icons.Rounded.GridView, selected == 4, Modifier.weight(1f)) { onNavigate("more") }
            }
        }

        // price tag: today's result, like the last-price label on a chart
        Box(
            Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(9.dp)).background(c).padding(horizontal = 7.dp, vertical = 2.dp),
        ) { Text(String.format(java.util.Locale.US, "%+.1f", todayNow), color = Color(0xFF07080D), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }

        // palette handle
        Box(
            Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(CandleStyle.Green, CandleStyle.Red, CandleStyle.Gold, CandleStyle.Green)))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable { palette = !palette },
        )
        if (palette) {
            val up = with(LocalDensity.current) { (-84).dp.roundToPx() }
            Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, up), onDismissRequest = { palette = false }) {
                Column(
                    Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xE6101320)).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("رنگ کندل", color = Tx.Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CandleMode.values().forEach { m ->
                            val on = CandleStyle.mode == m
                            val fill = when (m) {
                                CandleMode.Auto -> Brush.sweepGradient(listOf(CandleStyle.Green, CandleStyle.Red, CandleStyle.Gold, CandleStyle.Green))
                                CandleMode.Green -> Brush.linearGradient(listOf(CandleStyle.Green, CandleStyle.Green))
                                CandleMode.Red -> Brush.linearGradient(listOf(CandleStyle.Red, CandleStyle.Red))
                                CandleMode.Gold -> Brush.linearGradient(listOf(CandleStyle.Gold, CandleStyle.Gold))
                            }
                            Box(
                                Modifier.size(46.dp).clip(CircleShape).background(fill)
                                    .border(if (on) 2.dp else 1.dp, if (on) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); CandleStyle.set(context, m); palette = false },
                                contentAlignment = Alignment.Center,
                            ) { if (m == CandleMode.Auto) Text("A", color = Color(0xFF07080D), fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                    Text(if (CandleStyle.mode == CandleMode.Auto) "سبز در سود، قرمز در زیان، طلایی سر به سر" else "رنگ ثابت؛ برای حالت خودکار «A» را بزن", color = Tx.Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Slot(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) Color.White else Color.White.copy(alpha = 0.62f), label = "slot")
    val lift by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "lift")
    Column(
        modifier.fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = lift; scaleY = lift; translationY = if (selected) -2.dp.toPx() else 0f })
        Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Three-line menu glyph that morphs into a cross. */
@Composable
fun HamburgerIcon(open: Boolean, color: Color = Color.White) {
    val p by animateFloatAsState(if (open) 1f else 0f, tween(350, easing = FastOutSlowInEasing), label = "burger")
    Box(Modifier.size(width = 20.dp, height = 14.dp)) {
        Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(2.dp).graphicsLayer { translationY = 6.dp.toPx() * p; rotationZ = 45f * p }.background(color, RoundedCornerShape(1.dp)))
        Box(Modifier.align(Alignment.CenterStart).width(14.dp).height(2.dp).graphicsLayer { alpha = 1f - p }.background(CandleStyle.resolve(), RoundedCornerShape(1.dp)))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).graphicsLayer { translationY = -6.dp.toPx() * p; rotationZ = -45f * p }.background(color, RoundedCornerShape(1.dp)))
    }
}

private data class DrawerItem(val label: String, val icon: ImageVector, val route: String, val tag: String = "")

/** Glass user drawer: profile, quick stats and the most used destinations. */
@Composable
fun UserDrawer(open: Boolean, onClose: () -> Unit, onNavigate: (String) -> Unit) {
    val t by animateFloatAsState(if (open) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "drawer")
    if (t <= 0.001f) return
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val width = 306.dp
    val c = CandleStyle.resolve()
    val trades = AppState.visibleTrades
    val wins = trades.count { it.pnl > 0 }
    val winRate = if (trades.isEmpty()) 0 else wins * 100 / trades.size
    val today = CandleStyle.todayPnl()
    val s = AppState.settings
    val initials = (s.profileFirstName.take(1) + s.profileLastName.take(1)).ifBlank { "TX" }
    val name = listOf(s.profileFirstName, s.profileLastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "نام و نام‌خانوادگی" }
    val items = listOf(
        DrawerItem("حساب‌ها", Icons.Rounded.AccountBalanceWallet, "accounts"),
        DrawerItem("معامله‌ها", Icons.Rounded.List, "trades"),
        DrawerItem("بینش‌ها", Icons.Rounded.Insights, "insights"),
        DrawerItem("متاترید", Icons.Rounded.SwapHoriz, "metatrader", "LIVE"),
        DrawerItem("مشترک · تلویزیون", Icons.Rounded.Cast, "tv_share", "TV"),
        DrawerItem("ماشین‌حساب", Icons.Rounded.Calculate, "calculator"),
        DrawerItem("تقویم", Icons.Rounded.CalendarMonth, "calendar"),
        DrawerItem("تنظیمات", Icons.Rounded.Settings, "settings"),
    )
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF04050A).copy(alpha = 0.62f * t)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose))
        val panelShape = if (rtl) RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp) else RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = (1f - t) * width.toPx() * 1.05f * (if (rtl) 1f else -1f) }
                .width(width)
                .fillMaxHeight()
                .clip(panelShape)
                .background(Brush.verticalGradient(listOf(Color(0xF2141728), Color(0xF20B0D16))))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), panelShape)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(c, Color(0xFF6D5BFF), Color(0xFF19D3C5), c))).padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF10121C)), contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text("امروز  " + String.format(java.util.Locale.US, "%+.2f", today), color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawerStat("${trades.size}", "معامله", Color.White, Modifier.weight(1f))
                DrawerStat("$winRate%", "نرخ برد", Color(0xFF8FA2FF), Modifier.weight(1f))
                DrawerStat(String.format(java.util.Locale.US, "%,.0f", trades.sumOf { it.pnl }), "سود کل", c, Modifier.weight(1f))
            }
            WeekCandles(c)
            GoalGuard(c, today)
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .clickable { onClose(); onNavigate(item.route) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(item.icon, null, tint = c, modifier = Modifier.size(20.dp))
                    Text(item.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (item.tag.isNotEmpty()) Text(item.tag, color = Tx.Muted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DrawerStat(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Tx.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun WeekCandles(accent: Color) {
    val today = LocalDate.now().toEpochDay()
    val vals = com.tradenex.app.core.DayTotals.lastDays(AppState.visibleTrades, today, 7)
    val maxAbs = (vals.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0).coerceAtLeast(1.0)
    val names = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("۷ کندل اخیر", color = Tx.Muted, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth().height(76.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            vals.forEachIndexed { i, v ->
                val col = if (v > 0) CandleStyle.Green else if (v < 0) CandleStyle.Red else CandleStyle.Gold
                val h = (8 + kotlin.math.abs(v) / maxAbs * 48).dp
                val dow = LocalDate.ofEpochDay(today - (6 - i)).dayOfWeek.value // Mon=1..Sun=7
                val label = names[(dow + 1) % 7]
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(Modifier.width(12.dp).height(h).clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(col, col.copy(alpha = 0.5f)))).then(if (i == 6) Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)) else Modifier))
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = if (i == 6) Color.White else Tx.Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun GoalGuard(accent: Color, today: Double) {
    val context = LocalContext.current
    val goal = CandleStyle.dailyGoal
    val limit = CandleStyle.dailyLimit
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("هدف و حد ضرر روزانه", color = Tx.Muted, fontSize = 11.sp)
        if (goal > 0) {
            val p = (today / goal).coerceIn(0.0, 1.0).toFloat()
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(4.dp)).background(CandleStyle.Green))
            }
            Text(if (today >= goal) "هدف امروز محقق شد" else String.format(java.util.Locale.US, "%.0f از %.0f", today.coerceAtLeast(0.0), goal), color = CandleStyle.Green, fontSize = 11.sp)
        }
        if (limit > 0) {
            val used = (-today / limit).coerceIn(0.0, 1.0).toFloat()
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(used).clip(RoundedCornerShape(4.dp)).background(CandleStyle.Red))
            }
            Text(if (CandleStyle.limitBreached()) "حد ضرر امروز پر شد؛ امروز دیگر معامله نکن" else String.format(java.util.Locale.US, "%.0f تا حد ضرر", limit + today.coerceAtMost(0.0)), color = CandleStyle.Red, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Text("هدف", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            listOf(0.0, 50.0, 100.0, 250.0).forEach { v -> MiniChip(if (v == 0.0) "خاموش" else v.toInt().toString(), goal == v) { CandleStyle.setGoal(context, v) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Text("ضرر", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            listOf(0.0, 50.0, 100.0, 200.0).forEach { v -> MiniChip(if (v == 0.0) "خاموش" else v.toInt().toString(), limit == v) { CandleStyle.setLimit(context, v) } }
        }
    }
}

@Composable
private fun MiniChip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (on) Color(0xFF07080D) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) Color.White else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
