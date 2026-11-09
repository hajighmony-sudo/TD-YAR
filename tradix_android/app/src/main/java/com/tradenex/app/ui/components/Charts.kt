package com.tradenex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.SessionState
import com.tradenex.app.data.Candle
import com.tradenex.app.data.TradeCandle
import com.tradenex.app.ui.theme.Tx
import kotlin.math.max
import kotlin.math.min

/**
 * One candle per recorded trade (ordered by time).
 * Bodies stay deliberately narrow; chart scrolls horizontally when there are many trades.
 * Tapping a candle shows trade name, symbol, side, lot and P&L.
 */
@Composable
fun TradeCandleChart(series: List<TradeCandle>, modifier: Modifier = Modifier) {
    var selectedIndex by remember(series) { mutableStateOf((series.size - 1).coerceAtLeast(0)) }
    val progress = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    LaunchedEffect(series) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        selectedIndex = (series.size - 1).coerceAtLeast(0)
        // Keep the newest trades in view.
        scroll.scrollTo(scroll.maxValue)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val minSlotPx = with(density) { 10.dp.toPx() }
            val available = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val needWidth = if (series.isEmpty()) available else max(available, series.size * minSlotPx)
            val chartWidthDp = with(density) { needWidth.toDp() }
            // Read the measured BoxWithConstraints height from constraints to avoid
            // the receiver-resolution clash with the maxHeight property in this scope.
            val chartHeightDp = with(density) { constraints.maxHeight.toDp() }

            Row(Modifier.horizontalScroll(scroll)) {
                Canvas(
                    Modifier
                        .width(chartWidthDp)
                        .height(chartHeightDp)
                        .pointerInput(series, needWidth) {
                            detectTapGestures { tap ->
                                if (series.isNotEmpty()) {
                                    val slot = needWidth / series.size.toFloat()
                                    selectedIndex = (tap.x / slot).toInt().coerceIn(0, series.lastIndex)
                                }
                            }
                        },
                ) {
                    if (series.isEmpty()) return@Canvas
                    val candles = series.map { it.candle }
                    val hi = candles.maxOf { it.h }
                    val lo = candles.minOf { it.l }
                    val span = (hi - lo).coerceAtLeast(1e-6)
                    val slot = size.width / candles.size
                    // Keep candle bodies small even with few trades.
                    val bodyW = (slot * 0.28f).coerceIn(1.0.dp.toPx(), 3.6.dp.toPx())
                    val wickW = 0.7.dp.toPx()
                    val mid = size.height / 2f
                    val p = progress.value
                    fun y(v: Double): Float {
                        val raw = size.height - (((v - lo) / span) * 0.88 + 0.06).toFloat() * size.height
                        return mid + (raw - mid) * p
                    }
                    // Soft grid
                    for (i in 1..3) {
                        val gy = size.height * i / 4f
                        drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, gy), Offset(size.width, gy), 1f)
                    }
                    series.forEachIndexed { i, item ->
                        val candle = item.candle
                        val color = if (candle.c >= candle.o) Tx.Green else Tx.Red
                        val x = slot * i + slot / 2f
                        if (i == selectedIndex) {
                            drawLine(Color.White.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, size.height), 1.2.dp.toPx())
                        }
                        // Wick
                        drawLine(color, Offset(x, y(candle.h)), Offset(x, y(candle.l)), wickW, StrokeCap.Round)
                        // Body
                        val top = y(max(candle.o, candle.c))
                        val bottom = y(min(candle.o, candle.c))
                        drawRoundRect(
                            color,
                            Offset(x - bodyW / 2f, top),
                            Size(bodyW, (bottom - top).coerceAtLeast(1.2.dp.toPx())),
                            CornerRadius(0.8.dp.toPx()),
                        )
                    }
                }
            }
            // Fixed price axis, last-price line and crosshair tag, so it reads like a real chart.
            if (series.isNotEmpty()) {
                val allHi = series.maxOf { it.candle.h }
                val allLo = series.minOf { it.candle.l }
                val selClose = series[selectedIndex.coerceIn(0, series.lastIndex)].candle.c
                val lastClose = series.last().candle.c
                Canvas(Modifier.fillMaxSize()) {
                    val span = (allHi - allLo).coerceAtLeast(1e-6)
                    fun yy(v: Double): Float = size.height - (((v - allLo) / span) * 0.88 + 0.06).toFloat() * size.height
                    val label = TextStyle(color = Tx.Muted, fontSize = 9.sp)
                    for (i in 0..3) {
                        val v = allLo + span * (0.06 + 0.88 * i / 3.0 - 0.06) / 0.88
                        val txt = measurer.measure(String.format(java.util.Locale.US, "%,.0f", v), label)
                        drawText(txt, topLeft = Offset(size.width - txt.size.width - 2.dp.toPx(), yy(v) - txt.size.height / 2f))
                    }
                    fun tag(v: Double, bg: Color, dashed: Boolean) {
                        val y = yy(v)
                        drawLine(bg.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null)
                        val txt = measurer.measure(String.format(java.util.Locale.US, "%,.2f", v), TextStyle(color = Tx.Ink, fontSize = 9.sp))
                        val w = txt.size.width + 8.dp.toPx()
                        val h = txt.size.height + 3.dp.toPx()
                        drawRoundRect(bg, Offset(size.width - w, y - h / 2f), Size(w, h), CornerRadius(4.dp.toPx()))
                        drawText(txt, topLeft = Offset(size.width - w + 4.dp.toPx(), y - txt.size.height / 2f))
                    }
                    tag(lastClose, if (lastClose >= series.first().candle.o) Tx.Green else Tx.Red, true)
                    if (selectedIndex.coerceIn(0, series.lastIndex) != series.lastIndex) tag(selClose, Tx.Gold, false)
                }
            }
        }

        if (series.isNotEmpty()) {
            val selected = series[selectedIndex.coerceIn(0, series.lastIndex)]
            Text(
                "${selected.tradeName} · ${selected.symbol} · ${selected.side} · ${selected.lot} lot · ${if (selected.pnl >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", selected.pnl)}",
                color = if (selected.pnl >= 0) Tx.Green else Tx.Red,
                fontSize = 10.sp,
                maxLines = 2,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                series.forEachIndexed { index, item ->
                    Text(
                        text = item.tradeName.ifBlank { item.symbol },
                        color = if (index == selectedIndex) Tx.Gold else Tx.Muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** Legacy OHLC-only chart retained for callers without trade metadata. */
@Composable
fun CandleChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(candles) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (candles.isEmpty()) return@Canvas
        val hi = candles.maxOf { it.h }
        val lo = candles.minOf { it.l }
        val span = (hi - lo).coerceAtLeast(1e-6)
        val slot = size.width / candles.size
        val bodyW = (slot * 0.28f).coerceIn(1.0.dp.toPx(), 3.6.dp.toPx())
        val mid = size.height / 2f
        fun y(v: Double): Float {
            val raw = size.height - (((v - lo) / span) * 0.88 + 0.06).toFloat() * size.height
            return mid + (raw - mid) * progress.value
        }
        candles.forEachIndexed { i, candle ->
            val color = if (candle.c >= candle.o) Tx.Green else Tx.Red
            val x = slot * i + slot / 2f
            drawLine(color, Offset(x, y(candle.h)), Offset(x, y(candle.l)), 0.7.dp.toPx(), StrokeCap.Round)
            val top = y(max(candle.o, candle.c))
            val bottom = y(min(candle.o, candle.c))
            drawRoundRect(
                color,
                Offset(x - bodyW / 2f, top),
                Size(bodyW, (bottom - top).coerceAtLeast(1.2.dp.toPx())),
                CornerRadius(0.8.dp.toPx()),
            )
        }
    }
}

/** 24h bars for the four sessions in the chosen time zone, with a white "now" marker. */
@Composable
fun SessionTimeline(states: List<SessionState>, nowLocalMinute: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (states.isEmpty()) return@Canvas
        val rowH = size.height / states.size
        val barH = (rowH * 0.55f).coerceAtMost(8.dp.toPx())
        states.forEachIndexed { i, s ->
            val y = rowH * i + (rowH - barH) / 2f
            val color = s.def.color.copy(alpha = if (s.open) 1f else 0.5f)
            val x0 = s.localStart / 1440f * size.width
            val w = s.length / 1440f * size.width
            val r = CornerRadius(barH / 2f)
            if (x0 + w <= size.width) {
                drawRoundRect(color, Offset(x0, y), Size(w, barH), r)
            } else {
                drawRoundRect(color, Offset(x0, y), Size(size.width - x0, barH), r)
                drawRoundRect(color, Offset(0f, y), Size(x0 + w - size.width, barH), r)
            }
        }
        val nx = nowLocalMinute / 1440f * size.width
        drawLine(Color.White, Offset(nx, 0f), Offset(nx, size.height), 2.dp.toPx())
    }
}
