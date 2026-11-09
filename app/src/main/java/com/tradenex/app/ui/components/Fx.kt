package com.tradenex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.ThemeController
import com.tradenex.app.ui.theme.Tx

/** One shared 0..1 clock for every shimmering border, provided by [TxBackground]. */
val LocalShimmerPhase = staticCompositionLocalOf<State<Float>> { mutableStateOf(0f) }

/**
 * Gold light that sweeps along the card edge. Only active in the Gold theme, when the user has
 * "برق لبه‌ها" on and animations are not "low". The phase is read while drawing, so a running
 * shimmer never recomposes anything.
 */
fun Modifier.shimmerBorder(shape: Shape, width: Dp = 1.4.dp, offset: Float = 0f): Modifier = composed {
    val on = Tx.isGilded && ThemeController.shimmer && AppState.settings.animations > 0
    if (!on) {
        this
    } else {
        val phase = LocalShimmerPhase.current
        val dir = LocalLayoutDirection.current
        this.drawWithContent {
            drawContent()
            val p = (phase.value + offset) % 1f
            val x0 = -size.width + 2.4f * size.width * p
            val brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Tx.GoldHi.copy(alpha = 0.95f), Color.Transparent),
                start = Offset(x0, 0f),
                end = Offset(x0 + size.width * 0.9f, size.height),
            )
            drawOutline(shape.createOutline(size, dir, this), brush, style = Stroke(width.toPx()))
        }
    }
}

/** Gold dust drifting upwards. Positions are simulated by the Rust core (NativeCore.particlesStep). */
@Composable
fun AmbientDust(modifier: Modifier = Modifier.fillMaxSize(), count: Int = 54) {
    val stride = NativeCore.PARTICLE_STRIDE
    val state = remember(count) { FloatArray(count * stride).also { NativeCore.particlesInit(it, 7) } }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(count) {
        var prev = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (prev != 0L) {
                    NativeCore.particlesStep(state, (now - prev) / 1_000_000_000f, now / 1_000_000_000f, 7)
                }
                prev = now
                frame = now
            }
        }
    }
    Canvas(modifier) {
        if (frame == 0L) return@Canvas
        val core = Tx.GoldHi
        for (i in 0 until count) {
            val o = i * stride
            val a = state[o + 6]
            if (a <= 0.01f) continue
            val r = (0.7f + state[o + 5] * 1.8f) * density
            val c = Offset(state[o] * size.width, state[o + 1] * size.height)
            drawCircle(core.copy(alpha = a * 0.16f * Tx.glowLevel.coerceAtLeast(0.4f)), r * 3.2f, c)
            drawCircle(core.copy(alpha = a * 0.75f), r, c)
        }
    }
}

/** Small crown mark (Gold theme logo ornament) with a slow glow pulse. */
@Composable
fun CrownMark(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "crown")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.35f)
            lineTo(w * 0.25f, h * 0.65f)
            lineTo(w * 0.5f, 0f)
            lineTo(w * 0.75f, h * 0.65f)
            lineTo(w, h * 0.35f)
            lineTo(w * 0.9f, h)
            lineTo(w * 0.1f, h)
            close()
        }
        drawCircle(Tx.Gold.copy(alpha = 0.28f * pulse), radius = w * 0.75f, center = Offset(w / 2, h * 0.55f))
        drawPath(path, Brush.verticalGradient(listOf(Tx.GoldPale, Tx.Gold, Tx.GoldDeep)))
        for (x in listOf(0f, 0.5f, 1f)) {
            drawCircle(Tx.GoldPale, radius = h * 0.1f, center = Offset(w * x, if (x == 0.5f) 0f else h * 0.35f))
        }
    }
}

/** Gold ripple that expands from the top of the screen whenever the theme is switched. */
@Composable
fun ThemeRipple() {
    val tick = ThemeController.switchTick
    val p = remember { Animatable(1f) }
    LaunchedEffect(tick) {
        if (tick == 0 || AppState.settings.animations == 0) return@LaunchedEffect
        p.snapTo(0f)
        p.animateTo(1f, tween(1100, easing = LinearEasing))
    }
    if (p.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.12f)
        val maxR = size.height * 1.05f
        for (k in 0..2) {
            val q = (p.value - k * 0.12f).coerceIn(0f, 1f)
            if (q <= 0f) continue
            drawCircle(
                Tx.GoldHi.copy(alpha = (1f - q) * (0.55f - k * 0.15f)),
                radius = maxR * q,
                center = c,
                style = Stroke((5f - k * 1.5f) * density * (1f - q * 0.6f)),
            )
        }
        drawRect(Tx.Gold.copy(alpha = (1f - p.value) * 0.07f))
    }
}
